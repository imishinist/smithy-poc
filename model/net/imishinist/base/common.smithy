$version: "2"

namespace net.imishinist.base

use net.imishinist.traits#jsonExample

@jsonExample({ latitude: 35.6812, longitude: 139.7671 })
structure Location {
    @required
    latitude: Float

    @required
    longitude: Float
}

@mixin
structure GeoMixin {
    @required
    location: Location
}

@jsonExample({
    location: { latitude: 35.6812, longitude: 139.7671 }
})
structure Geo with [GeoMixin] {}

enum RealEstateType {
    TYPE_11XX = "11xx"
    TYPE_12XX = "12xx"
    TYPE_3201 = "3201"
}
