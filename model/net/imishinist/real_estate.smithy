$version: "2"

namespace net.imishinist

use net.imishinist.traits#discriminatorField
use net.imishinist.traits#discriminatorValue

structure Location {
    @required
    latitude: Float

    @required
    longitude: Float
}

structure Geo {
    @required
    location: Location
}

enum RealEstateType {
    TYPE_11XX = "11xx"
    TYPE_12XX = "12xx"
}

@discriminatorValue("11xx")
structure RealEstate11xx {
    @required
    type: RealEstateType

    @required
    id: String

    @required
    modifiedAt: Timestamp

    @required
    geo: Geo
}

@discriminatorValue("12xx")
structure RealEstate12xx {
    @required
    type: RealEstateType

    @required
    id: String

    geo: Geo
}

@discriminatorField("type")
union PutRealEstate {
    type11xx: RealEstate11xx
    type12xx: RealEstate12xx
}
