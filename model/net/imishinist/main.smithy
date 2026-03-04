$version: "2"

namespace net.imishinist

use aws.protocols#restJson1

@restJson1
service MyService {
    version: "2026-03-04"
    operations: [
        PutRealEstates
    ]
}

@http(method: "POST", uri: "/v1/member/{memberId}/real_estates")
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
