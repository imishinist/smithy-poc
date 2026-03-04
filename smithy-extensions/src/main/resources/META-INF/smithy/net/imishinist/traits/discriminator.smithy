$version: "2"

namespace net.imishinist.traits

/// Specifies the discriminator value for a structure used in a union.
@trait(selector: "structure")
string discriminatorValue

/// Specifies which field to use as the discriminator in a union.
@trait(selector: "union")
string discriminatorField

/// Specifies a JSON example for a structure, used to populate OpenAPI `example`.
@trait(selector: "structure")
document jsonExample

/// Specifies the CSV column number that this member maps to.
@trait(selector: "structure > member")
integer csvColumn

/// Specifies the Protocol Buffers field number for this member.
@trait(selector: "structure > member")
integer protoField

/// Defines complex cross-field validation rules for a structure.
@trait(selector: "structure")
list complexValidation {
    member: ValidationRule
}

@private
structure ValidationRule {
    /// Unique rule identifier
    @required
    id: String

    /// Human-readable description of the rule
    @required
    description: String

    /// Condition that triggers the rule
    @required
    condition: ValidationCondition

    /// Effect to apply when condition is met
    @required
    effect: ValidationEffect
}

@private
structure ValidationCondition {
    /// Single condition: JSON path to evaluate
    path: String

    /// Comparison operator: eq, ne, not_empty, empty, in
    operator: String

    /// Value to compare against (for eq, ne, in)
    value: Document

    /// All conditions must be true (AND)
    all: ValidationConditionList

    /// Any condition must be true (OR)
    any: ValidationConditionList
}

@private
list ValidationConditionList {
    member: ValidationCondition
}

@private
structure ValidationEffect {
    /// JSON path of the target field
    @required
    target: String

    /// Constraint type: required, range, pattern
    @required
    constraint: String

    /// Constraint parameters (e.g., min/max for range, regex for pattern)
    params: Document
}
