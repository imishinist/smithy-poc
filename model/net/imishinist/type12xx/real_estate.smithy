$version: "2"

namespace net.imishinist.type12xx

use net.imishinist.base#GeoMixin
use net.imishinist.base#RealEstateType
use net.imishinist.traits#csvColumn
use net.imishinist.traits#discriminatorValue
use net.imishinist.traits#jsonExample
use net.imishinist.traits#protoField

/// 土地物件用の位置情報（住所付き）
@jsonExample({
    location: { latitude: 35.0116, longitude: 135.7681 }
    address: "京都府京都市中京区河原町通三条上ル"
})
structure Geo12xx with [GeoMixin] {
    /// 所在地住所
    @required
    @csvColumn(1)
    @protoField(2)
    address: String
}

/// 土地物件
@jsonExample({ type: "12xx", id: "RE-12XX-00015" })
@discriminatorValue("12xx")
structure RealEstate12xx {
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
    geo: Geo12xx
}
