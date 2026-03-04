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
