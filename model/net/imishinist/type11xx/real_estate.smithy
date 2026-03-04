$version: "2"

namespace net.imishinist.type11xx

use net.imishinist.base#Geo
use net.imishinist.base#RealEstateType
use net.imishinist.traits#csvColumn
use net.imishinist.traits#discriminatorValue
use net.imishinist.traits#jsonExample

/// マンション物件
@jsonExample({ type: "11xx", id: "RE-11XX-00001", modifiedAt: "2026-03-04T10:30:00Z" })
@discriminatorValue("11xx")
structure RealEstate11xx {
    /// 物件種別
    @required
    @csvColumn(1)
    type: RealEstateType

    /// 物件ID
    @required
    @csvColumn(2)
    id: String

    /// 最終更新日時
    @required
    @csvColumn(3)
    modifiedAt: Timestamp

    /// 位置情報
    @required
    geo: Geo
}
