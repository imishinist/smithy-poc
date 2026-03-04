package net.imishinist.smithy.extensions;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.*;

/**
 * Validates that paths referenced in @complexValidation rules
 * actually exist in the target structure.
 */
public class ComplexValidationValidator extends AbstractValidator {

    private static final ShapeId COMPLEX_VALIDATION = ShapeId.from("net.imishinist.traits#complexValidation");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (StructureShape struct : model.getStructureShapes()) {
            var traitOpt = struct.findTrait(COMPLEX_VALIDATION);
            if (traitOpt.isEmpty()) continue;

            ArrayNode rules = traitOpt.get().toNode().expectArrayNode();
            for (Node rule : rules.getElements()) {
                ObjectNode ruleObj = rule.expectObjectNode();
                String ruleId = ruleObj.expectStringMember("id").getValue();

                // Validate condition paths
                ruleObj.getObjectMember("condition").ifPresent(cond ->
                        validateConditionPaths(model, struct, cond, ruleId, events));

                // Validate effect target path
                ruleObj.getObjectMember("effect").ifPresent(effect -> {
                    String target = effect.expectStringMember("target").getValue();
                    if (!resolvePath(model, struct, target)) {
                        events.add(error(struct, String.format(
                                "Rule '%s': effect target path '%s' does not exist in structure '%s'.",
                                ruleId, target, struct.getId().getName())));
                    }
                });
            }
        }

        return events;
    }

    private void validateConditionPaths(Model model, StructureShape struct,
                                         ObjectNode condition, String ruleId,
                                         List<ValidationEvent> events) {
        condition.getStringMember("path").ifPresent(path -> {
            if (!resolvePath(model, struct, path.getValue())) {
                events.add(error(struct, String.format(
                        "Rule '%s': condition path '%s' does not exist in structure '%s'.",
                        ruleId, path.getValue(), struct.getId().getName())));
            }
        });

        condition.getArrayMember("all").ifPresent(all -> {
            for (Node child : all.getElements()) {
                validateConditionPaths(model, struct, child.expectObjectNode(), ruleId, events);
            }
        });

        condition.getArrayMember("any").ifPresent(any -> {
            for (Node child : any.getElements()) {
                validateConditionPaths(model, struct, child.expectObjectNode(), ruleId, events);
            }
        });
    }

    /**
     * Resolves a JSON path like "$.type" or "$.geo.location.latitude" against a structure.
     */
    private boolean resolvePath(Model model, StructureShape root, String path) {
        if (!path.startsWith("$.")) return false;
        String[] segments = path.substring(2).split("\\.");

        Shape current = root;
        for (String segment : segments) {
            if (!current.isStructureShape()) return false;
            Optional<MemberShape> memberOpt = current.asStructureShape().get().getMember(segment);
            if (memberOpt.isEmpty()) return false;
            current = model.expectShape(memberOpt.get().getTarget());
        }
        return true;
    }
}
