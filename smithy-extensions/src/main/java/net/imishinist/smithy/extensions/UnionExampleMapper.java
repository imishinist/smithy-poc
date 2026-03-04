package net.imishinist.smithy.extensions;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;

import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites examples so that union values expressed as {"memberName": {...}}
 * are flattened to just {...} to match OpenAPI discriminator format.
 */
public class UnionExampleMapper implements OpenApiMapper {

    private static final ShapeId DISCRIMINATOR_FIELD = ShapeId.from("net.imishinist.traits#discriminatorField");

    @Override
    public byte getOrder() {
        return 123;
    }

    @Override
    public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        Model model = context.getModel();

        Set<String> unionMemberNames = new HashSet<>();
        for (UnionShape union : model.getUnionShapes()) {
            if (union.findTrait(DISCRIMINATOR_FIELD).isPresent()) {
                unionMemberNames.addAll(union.getAllMembers().keySet());
            }
        }
        if (unionMemberNames.isEmpty()) {
            return node;
        }

        return rewriteNode(node, unionMemberNames).expectObjectNode();
    }

    private Node rewriteNode(Node node, Set<String> unionMemberNames) {
        if (node.isObjectNode()) {
            ObjectNode obj = node.expectObjectNode();
            if (obj.size() == 1) {
                String key = obj.getMembers().keySet().iterator().next().getValue();
                if (unionMemberNames.contains(key)) {
                    return rewriteNode(obj.getMembers().values().iterator().next(), unionMemberNames);
                }
            }
            ObjectNode result = obj;
            for (var entry : obj.getMembers().entrySet()) {
                Node rewritten = rewriteNode(entry.getValue(), unionMemberNames);
                if (rewritten != entry.getValue()) {
                    result = result.withMember(entry.getKey().getValue(), rewritten);
                }
            }
            return result;
        } else if (node.isArrayNode()) {
            ArrayNode arr = node.expectArrayNode();
            var elements = new java.util.ArrayList<Node>();
            boolean changed = false;
            for (Node element : arr.getElements()) {
                Node rewritten = rewriteNode(element, unionMemberNames);
                elements.add(rewritten);
                if (rewritten != element) changed = true;
            }
            return changed ? ArrayNode.fromNodes(elements) : arr;
        }
        return node;
    }
}
