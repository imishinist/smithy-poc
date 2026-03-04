package net.imishinist.smithy.openapi;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class UnionDiscriminatorMapper implements OpenApiMapper {

    private static final ShapeId DISCRIMINATOR_VALUE = ShapeId.from("net.imishinist.traits#discriminatorValue");
    private static final ShapeId DISCRIMINATOR_FIELD = ShapeId.from("net.imishinist.traits#discriminatorField");

    @Override
    public byte getOrder() {
        return 120;
    }

    @Override
    public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        Model model = context.getModel();
        ObjectNode schemas = node.getObjectMember("components")
                .flatMap(c -> c.getObjectMember("schemas"))
                .orElse(null);
        if (schemas == null) {
            return node;
        }

        ObjectNode updatedSchemas = schemas;

        for (UnionShape union : model.getUnionShapes()) {
            Optional<String> fieldOpt = union.findTrait(DISCRIMINATOR_FIELD)
                    .map(t -> t.toNode().expectStringNode().getValue());
            if (fieldOpt.isEmpty()) {
                continue;
            }
            String discriminatorField = fieldOpt.get();
            String unionName = union.getId().getName();
            if (!schemas.containsMember(unionName)) {
                continue;
            }

            var refs = new java.util.ArrayList<Node>();
            Map<String, String> mapping = new LinkedHashMap<>();

            for (MemberShape member : union.getAllMembers().values()) {
                StructureShape target = model.expectShape(member.getTarget(), StructureShape.class);
                String schemaName = target.getId().getName();
                String ref = "#/components/schemas/" + schemaName;
                refs.add(ObjectNode.builder().withMember("$ref", ref).build());

                target.findTrait(DISCRIMINATOR_VALUE)
                        .map(t -> t.toNode().expectStringNode().getValue())
                        .ifPresent(v -> mapping.put(v, ref));
            }

            ObjectNode.Builder discBuilder = ObjectNode.builder()
                    .withMember("propertyName", Node.from(discriminatorField));
            if (!mapping.isEmpty()) {
                discBuilder.withMember("mapping", ObjectNode.fromStringMap(mapping));
            }

            ObjectNode newSchema = ObjectNode.builder()
                    .withMember("oneOf", ArrayNode.fromNodes(refs))
                    .withMember("discriminator", discBuilder.build())
                    .build();

            updatedSchemas = updatedSchemas.withMember(unionName, newSchema);
        }

        if (updatedSchemas == schemas) {
            return node;
        }

        ObjectNode components = node.expectObjectMember("components")
                .withMember("schemas", updatedSchemas);
        return node.withMember("components", components);
    }
}
