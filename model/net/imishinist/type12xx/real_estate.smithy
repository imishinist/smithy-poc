$version: "2"

namespace net.imishinist.type12xx

use net.imishinist.base#GeoMixin
use net.imishinist.base#RealEstateType
use net.imishinist.traits#discriminatorValue
use net.imishinist.traits#jsonExample

@jsonExample({
    location: { latitude: 35.0116, longitude: 135.7681 }
    address: "京都府京都市中京区河原町通三条上ル"
})
structure Geo12xx with [GeoMixin] {
    @required
    address: String
}

@jsonExample({ type: "12xx", id: "RE-12XX-00015" })
@discriminatorValue("12xx")
structure RealEstate12xx {
    @required
    type: RealEstateType

    @required
    id: String

    geo: Geo12xx
}
