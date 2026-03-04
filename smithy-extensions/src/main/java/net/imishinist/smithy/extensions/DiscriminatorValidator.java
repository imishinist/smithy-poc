package net.imishinist.smithy.extensions;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.*;
import java.util.stream.Collectors;

public class DiscriminatorValidator extends AbstractValidator {

    private static final ShapeId DISCRIMINATOR_FIELD = ShapeId.from("net.imishinist.traits#discriminatorField");
    private static final ShapeId DISCRIMINATOR_VALUE = ShapeId.from("net.imishinist.traits#discriminatorValue");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (UnionShape union : model.getUnionShapes()) {
            union.findTrait(DISCRIMINATOR_FIELD).ifPresent(trait -> {
                String fieldName = trait.toNode().expectStringNode().getValue();
                Set<String> usedValues = new HashSet<>();

                for (MemberShape member : union.getAllMembers().values()) {
                    Shape target = model.expectShape(member.getTarget());
                    if (!target.isStructureShape()) {
                        events.add(error(union, String.format(
                                "Union member `%s` must target a structure when @discriminatorField is used.",
                                member.getMemberName())));
                        continue;
                    }
                    StructureShape struct = target.asStructureShape().get();

                    // 1. Check field exists
                    Optional<MemberShape> fieldOpt = struct.getMember(fieldName);
                    if (fieldOpt.isEmpty()) {
                        events.add(error(struct, String.format(
                                "Structure `%s` is missing discriminator field `%s` required by union `%s`.",
                                struct.getId().getName(), fieldName, union.getId().getName())));
                        continue;
                    }

                    // 2. Check @discriminatorValue is present
                    Optional<String> valueOpt = struct.findTrait(DISCRIMINATOR_VALUE)
                            .map(t -> t.toNode().expectStringNode().getValue());
                    if (valueOpt.isEmpty()) {
                        events.add(error(struct, String.format(
                                "Structure `%s` must have @discriminatorValue trait (member of union `%s`).",
                                struct.getId().getName(), union.getId().getName())));
                        continue;
                    }
                    String value = valueOpt.get();

                    // 3. Check value exists in enum
                    Shape fieldTarget = model.expectShape(fieldOpt.get().getTarget());
                    if (fieldTarget.isEnumShape()) {
                        Set<String> enumValues = fieldTarget.asEnumShape().get().getAllMembers().values().stream()
                                .map(m -> m.expectTrait(EnumValueTrait.class).expectStringValue())
                                .collect(Collectors.toSet());
                        if (!enumValues.contains(value)) {
                            events.add(error(struct, String.format(
                                    "@discriminatorValue(\"%s\") on `%s` is not a valid value of enum `%s`. Valid values: %s",
                                    value, struct.getId().getName(), fieldTarget.getId().getName(), enumValues)));
                        }
                    }

                    // 4. Check for duplicates
                    if (!usedValues.add(value)) {
                        events.add(error(struct, String.format(
                                "Duplicate @discriminatorValue(\"%s\") in union `%s`.",
                                value, union.getId().getName())));
                    }
                }

                // 5. Check all enum values are covered by union members
                // Find the enum shape from the discriminator field of any member
                union.getAllMembers().values().stream()
                        .map(m -> model.expectShape(m.getTarget()))
                        .filter(Shape::isStructureShape)
                        .map(s -> s.asStructureShape().get())
                        .flatMap(s -> s.getMember(fieldName).stream())
                        .map(m -> model.expectShape(m.getTarget()))
                        .filter(Shape::isEnumShape)
                        .findFirst()
                        .ifPresent(enumShape -> {
                            Set<String> allEnumValues = enumShape.asEnumShape().get().getAllMembers().values().stream()
                                    .map(m -> m.expectTrait(EnumValueTrait.class).expectStringValue())
                                    .collect(Collectors.toSet());
                            Set<String> uncovered = new HashSet<>(allEnumValues);
                            uncovered.removeAll(usedValues);
                            if (!uncovered.isEmpty()) {
                                events.add(error(union, String.format(
                                        "Union `%s` does not cover all values of enum `%s`. Missing: %s",
                                        union.getId().getName(), enumShape.getId().getName(), uncovered)));
                            }
                        });
            });
        }

        return events;
    }
}
