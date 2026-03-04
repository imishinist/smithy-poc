$version: "2"

namespace net.imishinist

use aws.protocols#restJson1

@restJson1
service MyService {
    version: "1"
    operations: [
        PutRealEstates
    ]
}

@http(method: "POST", uri: "/v1/member/{memberId}/real_estates")
@examples([
    {
        title: "マンション(1101)の登録"
        documentation: "マンション物件を1件登録する例"
        input: {
            memberId: "member-001"
            body: {
                real_estates: [
                    {
                        type11xx: {
                            type: "1101"
                            id: "RE-1101-00001"
                            modifiedAt: "2026-03-04T10:30:00Z"
                            geo: {
                                location: { latitude: 35.6812, longitude: 139.7671 }
                            }
                        }
                    }
                ]
            }
        }
        output: {}
    }
    {
        title: "複数種別の物件を一括登録"
        documentation: "マンション(1101)・土地(1201)・戸建(3201)を一括登録する例"
        input: {
            memberId: "member-002"
            body: {
                real_estates: [
                    {
                        type11xx: {
                            type: "1101"
                            id: "RE-1101-00042"
                            modifiedAt: "2026-03-01T09:00:00Z"
                            geo: {
                                location: { latitude: 34.6937, longitude: 135.5023 }
                            }
                        }
                    }
                    {
                        type12xx: {
                            type: "1201"
                            id: "RE-1201-00015"
                            geo: {
                                location: { latitude: 35.0116, longitude: 135.7681 }
                                address: "京都府京都市中京区河原町通三条上ル"
                            }
                        }
                    }
                    {
                        type3201: {
                            type: "3201"
                            id: "RE-3201-00008"
                            geo: {
                                location: { latitude: 43.0621, longitude: 141.3544 }
                            }
                        }
                    }
                ]
            }
        }
        output: {}
    }
])
operation PutRealEstates {
    input: PutRealEstatesInput
    output: PutRealEstatesOutput
}

structure PutRealEstatesInput {
    @required
    @httpLabel
    memberId: String

    @required
    @httpPayload
    body: PutRealEstatesInputBody
}

structure PutRealEstatesInputBody {
    real_estates: PutRealEstatesList
}

list PutRealEstatesList {
    member: PutRealEstate
}

structure PutRealEstatesOutput {}
