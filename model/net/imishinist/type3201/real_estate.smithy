$version: "2"

namespace net.imishinist.type3201

use net.imishinist.base#Geo
use net.imishinist.base#RealEstateType
use net.imishinist.traits#discriminatorValue
use net.imishinist.traits#jsonExample

@jsonExample({ type: "3201", id: "RE-3201-00008" })
@discriminatorValue("3201")
structure RealEstate3201 {
    @required
    type: RealEstateType

    @required
    id: String

    geo: Geo
}
