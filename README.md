# smithy-poc

Smithy モデルから OpenAPI 定義・Protocol Buffers・バリデーションルールを一元生成し、Go の型コードと Stoplight Elements による API ドキュメントを自動生成するパイプラインの PoC。

## ディレクトリ構成

```
.
├── model/                          # Smithy モデル定義
│   └── net/imishinist/
│       ├── main.smithy             # サービス定義・オペレーション・エラー
│       ├── real_estate.smithy      # PutRealEstate union（discriminated union）
│       ├── base/
│       │   ├── common.smithy       # 共通型（Location, Geo, GeoMixin, RealEstateType）
│       │   └── errors.smithy       # RFC 9457 Problem Details エラー型
│       ├── type11xx/
│       │   └── real_estate.smithy  # マンション（1101, 1102, 1103）
│       ├── type12xx/
│       │   └── real_estate.smithy  # 土地（1201, 1202, 1203）
│       └── type3201/
│           └── real_estate.smithy  # 戸建（3201）
├── smithy-extensions/              # Smithy 拡張プラグイン (Java)
│   └── src/main/
│       ├── java/.../extensions/
│       │   ├── UnionDiscriminatorMapper.java      # oneOf + discriminator 変換
│       │   ├── UnionDiscriminatorExtension.java   # Mapper の SPI 登録
│       │   ├── JsonExampleMapper.java             # @jsonExample → OpenAPI examples
│       │   ├── CsvColumnMapper.java               # @csvColumn → x-csv-column
│       │   ├── UnionExampleMapper.java            # union example のフラット化
│       │   ├── DiscriminatorValidator.java        # discriminator バリデーション
│       │   ├── ComplexValidationValidator.java    # @complexValidation パス検証
│       │   ├── ProtoGeneratorPlugin.java          # .proto ファイル生成
│       │   └── ValidationRulesPlugin.java         # validation-rules.json 生成
│       └── resources/META-INF/
│           ├── services/           # SPI 登録
│           └── smithy/net/imishinist/traits/
│               └── discriminator.smithy           # カスタムトレイト定義
├── gen/                            # Go コード生成
│   ├── api/
│   │   └── types.gen.go            # oapi-codegen で生成された型定義
│   ├── tools.go                    # oapi-codegen の tool dependency
│   └── go.mod
├── docs/
│   └── index.html                  # Stoplight Elements による API ドキュメント
├── scripts/
│   └── generate-docs.sh            # Stoplight HTML 生成スクリプト
├── smithy-build.json               # Smithy ビルド設定（openapi / proto / validation）
├── build.gradle.kts                # ルートプロジェクト
├── settings.gradle.kts
└── Makefile                        # ビルドパイプライン
```

## 前提条件

- Java 17+
- Gradle 8.13+
- Go 1.24+

## ビルドパイプライン

```
                    ┌─→ OpenAPI JSON ─→ oapi-codegen ─→ Go 型コード
                    │                 └─→ generate-docs.sh ─→ Stoplight HTML
Smithy モデル ──────┼─→ model.proto（Protocol Buffers）
                    └─→ validation-rules.json（クライアント配布用）
```

### コマンド

```bash
# 全体を実行（Go コード生成 + ドキュメント生成）
make

# 個別実行
make smithy      # Smithy ビルド（バリデーション + 全 projection 生成）
make generate    # Smithy ビルド + Go 型コード生成
make docs        # Smithy ビルド + Stoplight HTML 生成

# クリーン
make clean
```

## カスタムトレイト

`smithy-extensions` モジュールで定義：

| トレイト | 対象 | 説明 |
|---|---|---|
| `@discriminatorField` | union | discriminator として使うフィールド名を指定 |
| `@discriminatorValue` | structure | discriminator 値のリスト（例: `["1101", "1102", "1103"]`） |
| `@jsonExample` | structure | OpenAPI の schema-level example を生成 |
| `@csvColumn` | member | CSV カラム番号のメタデータ（`x-csv-column` として出力） |
| `@protoField` | member | Proto field number を明示指定 |
| `@complexValidation` | structure | 条件付きバリデーションルール（condition/effect） |

### 使用例

```smithy
@discriminatorField("type")
union PutRealEstate {
    type11xx: RealEstate11xx
    type12xx: RealEstate12xx
    type3201: RealEstate3201
}

@discriminatorValue(["1101", "1102", "1103"])
structure RealEstate11xx {
    @required
    type: RealEstateType

    @csvColumn(1)
    @protoField(1)
    id: String
}
```

## ビルド時バリデーション

### DiscriminatorValidator

- union メンバーが structure であること
- 各 structure に discriminator フィールドが存在すること
- `@discriminatorValue` トレイトが付与されていること
- discriminator 値が対応する enum に含まれていること
- discriminator 値の重複がないこと
- enum の全値が union メンバーでカバーされていること

### ComplexValidationValidator

- `@complexValidation` の condition/effect に記述されたフィールドパス（`$.field`）が実際の structure に存在すること

## 生成物

### OpenAPI（`build/smithyprojections/smithy-poc/openapi/`）

- `oneOf` + `discriminator` による polymorphic スキーマ
- `@jsonExample` による schema-level examples
- `x-csv-column` 拡張フィールド

### Protocol Buffers（`build/smithyprojections/smithy-poc/proto/`）

- `model.proto` - message, enum, oneof 定義
- `optional` キーワード対応（非 required / 非 repeated フィールド）

### バリデーションルール（`build/smithyprojections/smithy-poc/validation/`）

- `validation-rules.json` - `@complexValidation` のルールをフラット化して出力（クライアント配布用）

## エラーレスポンス

RFC 9457 (Problem Details for HTTP APIs) に準拠：

| HTTP Status | エラー型 | レスポンスボディ |
|---|---|---|
| 400 | `BadRequestError` | `ValidationProblemDetails`（`errors` フィールド付き） |
| 404 | `NotFoundError` | `ProblemDetails` |
| 409 | `ConflictError` | `ProblemDetails` |
| 500 | `InternalServerError` | `ProblemDetails` |

## API ドキュメント

```bash
open docs/index.html
```
