package net.imishinist.smithy.openapi;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;

public class JsonExampleMapper implements OpenApiMapper {

    private static final ShapeId JSON_EXAMPLE = ShapeId.from("net.imishinist.traits#jsonExample");

    @Override
    public byte getOrder() {
        return 121;
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
            var traitOpt = struct.findTrait(JSON_EXAMPLE);
            if (traitOpt.isEmpty()) {
                continue;
            }
            String name = struct.getId().getName();
            if (!updatedSchemas.containsMember(name)) {
                continue;
            }
            Node example = traitOpt.get().toNode();
            ObjectNode schema = updatedSchemas.expectObjectMember(name);
            updatedSchemas = updatedSchemas.withMember(name, schema.withMember("example", example));
        }

        if (updatedSchemas == schemas) {
            return node;
        }

        ObjectNode components = node.expectObjectMember("components")
                .withMember("schemas", updatedSchemas);
        return node.withMember("components", components);
    }
}
