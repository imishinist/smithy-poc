$version: "2"

namespace net.imishinist.type11xx

use net.imishinist.base#Geo
use net.imishinist.base#RealEstateType
use net.imishinist.traits#discriminatorValue
use net.imishinist.traits#jsonExample

@jsonExample({ type: "11xx", id: "RE-11XX-00001", modifiedAt: "2026-03-04T10:30:00Z" })
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
