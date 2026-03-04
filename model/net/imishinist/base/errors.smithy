$version: "2"

namespace net.imishinist.base

use net.imishinist.traits#jsonExample

/// RFC 9457 Problem Details for HTTP APIs
@jsonExample({
    type: "about:blank"
    title: "Internal Server Error"
    status: 500
    detail: "予期しないエラーが発生しました"
    instance: "/v1/member/member-001/real_estates"
})
structure ProblemDetails {
    /// 問題の種別を識別する URI
    @required
    type: String

    /// 問題の概要（人間向け）
    @required
    title: String

    /// HTTP ステータスコード
    @required
    status: Integer

    /// 問題の詳細説明
    detail: String

    /// 問題が発生したリソースの URI
    instance: String
}

/// バリデーションエラーの個別項目
@jsonExample({ field: "$.real_estates[0].type", message: "不正な物件種別です" })
structure ValidationError {
    /// エラーが発生したフィールドのパス
    @required
    field: String

    /// エラーメッセージ
    @required
    message: String
}

list ValidationErrorList {
    member: ValidationError
}

/// バリデーションエラー用の Problem Details（RFC 9457 拡張）
@jsonExample({
    type: "about:blank"
    title: "Bad Request"
    status: 400
    detail: "リクエストの入力値に問題があります"
    errors: [
        {
            field: "$.real_estates[0].type"
            message: "不正な物件種別です"
        }
        {
            field: "$.real_estates[1].id"
            message: "物件IDは必須です"
        }
    ]
})
structure ValidationProblemDetails {
    @required
    type: String

    @required
    title: String

    @required
    status: Integer

    detail: String

    instance: String

    /// バリデーションエラーの一覧
    @required
    errors: ValidationErrorList
}
