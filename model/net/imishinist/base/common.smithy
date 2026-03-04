$version: "2"

namespace net.imishinist.base

use net.imishinist.traits#csvColumn
use net.imishinist.traits#jsonExample
use net.imishinist.traits#protoField

/// 緯度経度を表す座標情報
@jsonExample({ latitude: 35.6812, longitude: 139.7671 })
structure Location {
    /// 緯度（十進法）
    @required
    @csvColumn(1)
    @protoField(1)
    latitude: Float

    /// 経度（十進法）
    @required
    @csvColumn(2)
    @protoField(2)
    longitude: Float
}

@mixin
structure GeoMixin {
    /// 座標情報
    @required
    @protoField(1)
    location: Location
}

/// 位置情報
@jsonExample({
    location: { latitude: 35.6812, longitude: 139.7671 }
})
structure Geo with [GeoMixin] {}

/// 物件種別
enum RealEstateType {
    /// マンション
    TYPE_11XX = "11xx"

    /// 土地
    TYPE_12XX = "12xx"

    /// 戸建
    TYPE_3201 = "3201"
}
