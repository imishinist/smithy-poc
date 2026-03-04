$version: "2"

namespace net.imishinist

use net.imishinist.traits#discriminatorField
use net.imishinist.type11xx#RealEstate11xx
use net.imishinist.type12xx#RealEstate12xx
use net.imishinist.type3201#RealEstate3201

@discriminatorField("type")
union PutRealEstate {
    type11xx: RealEstate11xx
    type12xx: RealEstate12xx
    type3201: RealEstate3201
}
