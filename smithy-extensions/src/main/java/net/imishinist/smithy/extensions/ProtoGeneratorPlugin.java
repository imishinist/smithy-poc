package net.imishinist.smithy.extensions;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ProtoGeneratorPlugin implements SmithyBuildPlugin {

    private static final ShapeId PROTO_FIELD = ShapeId.from("net.imishinist.traits#protoField");
    private static final ShapeId DISCRIMINATOR_FIELD = ShapeId.from("net.imishinist.traits#discriminatorField");
    private static final ShapeId CSV_COLUMN = ShapeId.from("net.imishinist.traits#csvColumn");

    @Override
    public String getName() {
        return "proto-gen";
    }

    @Override
    public void execute(PluginContext context) {
        Model model = context.getModel();
        ObjectNode settings = context.getSettings();
        String packageName = settings.getStringMemberOrDefault("package", "net.imishinist");
        StringBuilder sb = new StringBuilder();
        sb.append("syntax = \"proto3\";\n\n");
        sb.append("package ").append(packageName).append(";\n\n");

        // Emit options from settings
        settings.getObjectMember("options").ifPresent(opts -> {
            for (var entry : opts.getStringMap().entrySet()) {
                sb.append("option ").append(entry.getKey()).append(" = \"")
                  .append(entry.getValue().expectStringNode().getValue()).append("\";\n");
            }
            sb.append("\n");
        });

        // Collect shapes to generate
        Set<ShapeId> generated = new LinkedHashSet<>();

        // Enums
        for (EnumShape enumShape : model.getEnumShapes()) {
            if (!isInNamespace(enumShape)) continue;
            generateEnum(sb, enumShape);
            generated.add(enumShape.getId());
        }

        // Structures (skip mixins)
        for (StructureShape struct : model.getStructureShapes()) {
            if (!isInNamespace(struct)) continue;
            if (struct.hasTrait("smithy.api#mixin")) continue;
            generateMessage(sb, model, struct);
            generated.add(struct.getId());
        }

        // Unions → oneof wrapper message
        for (UnionShape union : model.getUnionShapes()) {
            if (!isInNamespace(union)) continue;
            generateUnionMessage(sb, model, union);
            generated.add(union.getId());
        }

        Path outputDir = context.getFileManifest().getBaseDir();
        Path outputFile = outputDir.resolve("model.proto");
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write proto file", e);
        }
        context.getFileManifest().addFile(outputFile);
    }

    private boolean isInNamespace(Shape shape) {
        return shape.getId().getNamespace().startsWith("net.imishinist");
    }

    private void generateEnum(StringBuilder sb, EnumShape enumShape) {
        appendDoc(sb, enumShape, "");
        sb.append("enum ").append(enumShape.getId().getName()).append(" {\n");
        sb.append("    ").append(enumShape.getId().getName().toUpperCase()).append("_UNSPECIFIED = 0;\n");
        int index = 1;
        for (MemberShape member : enumShape.getAllMembers().values()) {
            String value = member.expectTrait(EnumValueTrait.class).expectStringValue();
            appendDoc(sb, member, "    ");
            sb.append("    ").append(toProtoEnumName(enumShape.getId().getName(), value))
              .append(" = ").append(index++).append(";\n");
        }
        sb.append("}\n\n");
    }

    private void generateMessage(StringBuilder sb, Model model, StructureShape struct) {
        appendDoc(sb, struct, "");
        sb.append("message ").append(struct.getId().getName()).append(" {\n");
        int autoField = 1;
        for (MemberShape member : struct.getAllMembers().values()) {
            int fieldNum = member.findTrait(PROTO_FIELD)
                    .map(t -> t.toNode().expectNumberNode().getValue().intValue())
                    .orElse(autoField);
            autoField = Math.max(autoField, fieldNum) + 1;

            String protoType = toProtoType(model, member.getTarget());
            boolean isOptional = !member.isRequired() && !protoType.startsWith("repeated ");
            appendMemberDoc(sb, member, "    ");
            sb.append("    ");
            if (isOptional) sb.append("optional ");
            sb.append(protoType).append(" ").append(toSnakeCase(member.getMemberName()))
              .append(" = ").append(fieldNum).append(";\n");
        }
        sb.append("}\n\n");
    }

    private void generateUnionMessage(StringBuilder sb, Model model, UnionShape union) {
        appendDoc(sb, union, "");
        sb.append("message ").append(union.getId().getName()).append(" {\n");
        sb.append("    oneof value {\n");
        int fieldNum = 1;
        for (MemberShape member : union.getAllMembers().values()) {
            String typeName = model.expectShape(member.getTarget()).getId().getName();
            sb.append("        ").append(typeName).append(" ").append(toSnakeCase(member.getMemberName()))
              .append(" = ").append(fieldNum++).append(";\n");
        }
        sb.append("    }\n");
        sb.append("}\n\n");
    }

    private String toProtoType(Model model, ShapeId targetId) {
        Shape target = model.expectShape(targetId);
        if (target.isStructureShape() || target.isUnionShape() || target.isEnumShape()) {
            return target.getId().getName();
        }
        return switch (target.getType()) {
            case STRING -> "string";
            case INTEGER -> "int32";
            case LONG -> "int64";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case BOOLEAN -> "bool";
            case TIMESTAMP -> "string";
            case LIST -> {
                ShapeId itemTarget = target.asListShape().get().getMember().getTarget();
                yield "repeated " + toProtoType(model, itemTarget);
            }
            default -> "string";
        };
    }

    private void appendDoc(StringBuilder sb, Shape shape, String indent) {
        shape.getTrait(DocumentationTrait.class).ifPresent(doc -> {
            sb.append(indent).append("// ").append(doc.getValue()).append("\n");
        });
    }

    private void appendMemberDoc(StringBuilder sb, MemberShape member, String indent) {
        List<String> parts = new ArrayList<>();
        member.getTrait(DocumentationTrait.class).ifPresent(doc -> parts.add(doc.getValue()));
        member.findTrait(CSV_COLUMN).ifPresent(t -> {
            parts.add("CSV列: " + t.toNode().expectNumberNode().getValue().intValue());
        });
        if (!parts.isEmpty()) {
            sb.append(indent).append("// ").append(String.join(" | ", parts)).append("\n");
        }
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String toProtoEnumName(String enumName, String value) {
        return (enumName + "_" + value).toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }
}
