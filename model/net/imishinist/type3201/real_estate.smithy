$version: "2"

namespace net.imishinist.type3201

use net.imishinist.base#Geo
use net.imishinist.base#RealEstateType
use net.imishinist.traits#csvColumn
use net.imishinist.traits#discriminatorValue
use net.imishinist.traits#jsonExample
use net.imishinist.traits#protoField

/// 戸建物件
@jsonExample({ type: "3201", id: "RE-3201-00008" })
@discriminatorValue(["3201"])
structure RealEstate3201 {
    /// 物件種別
    @required
    @csvColumn(1)
    @protoField(1)
    type: RealEstateType

    /// 物件ID
    @required
    @csvColumn(2)
    @protoField(2)
    id: String

    /// 位置情報
    @protoField(3)
    geo: Geo
}
