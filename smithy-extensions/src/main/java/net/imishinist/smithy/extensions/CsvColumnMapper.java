package net.imishinist.smithy.extensions;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;

public class CsvColumnMapper implements OpenApiMapper {

    private static final ShapeId CSV_COLUMN = ShapeId.from("net.imishinist.traits#csvColumn");

    @Override
    public byte getOrder() {
        return 122;
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

        for (StructureShape struct : model.getStructureShapes()) {
            String name = struct.getId().getName();
            if (!updatedSchemas.containsMember(name)) {
                continue;
            }

            ObjectNode schema = updatedSchemas.expectObjectMember(name);
            ObjectNode properties = schema.getObjectMember("properties").orElse(null);
            if (properties == null) {
                continue;
            }

            ObjectNode updatedProperties = properties;
            boolean changed = false;

            for (MemberShape member : struct.getAllMembers().values()) {
                var traitOpt = member.findTrait(CSV_COLUMN);
                if (traitOpt.isEmpty()) {
                    continue;
                }
                int col = traitOpt.get().toNode().expectNumberNode().getValue().intValue();
                String memberName = member.getMemberName();
                if (!updatedProperties.containsMember(memberName)) {
                    continue;
                }

                ObjectNode prop = updatedProperties.expectObjectMember(memberName);
                prop = prop.withMember("x-csv-column", Node.from(col));

                String desc = prop.getStringMember("description").map(n -> n.getValue()).orElse("");
                String csvNote = "CSV列: " + col;
                if (desc.isEmpty()) {
                    prop = prop.withMember("description", Node.from(csvNote));
                } else if (!desc.contains(csvNote)) {
                    prop = prop.withMember("description", Node.from(desc + " (" + csvNote + ")"));
                }

                updatedProperties = updatedProperties.withMember(memberName, prop);
                changed = true;
            }

            if (changed) {
                updatedSchemas = updatedSchemas.withMember(name, schema.withMember("properties", updatedProperties));
            }
        }

        if (updatedSchemas == schemas) {
            return node;
        }

        ObjectNode components = node.expectObjectMember("components")
                .withMember("schemas", updatedSchemas);
        return node.withMember("components", components);
    }
}
