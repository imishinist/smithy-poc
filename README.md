# smithy-poc

Smithy モデルから OpenAPI 定義を生成し、Go の型コードと Stoplight Elements による API ドキュメントを自動生成するパイプラインの PoC。

## ディレクトリ構成

```
.
├── model/                          # Smithy モデル定義
│   └── net/imishinist/
│       ├── main.smithy             # サービス定義・オペレーション
│       └── real_estate.smithy      # データモデル（enum, structure, union）
├── openapi-mapper/                 # Smithy → OpenAPI 変換の拡張プラグイン (Java)
│   └── src/main/
│       ├── java/.../openapi/
│       │   ├── UnionDiscriminatorMapper.java      # union を oneOf + discriminator に変換
│       │   ├── UnionDiscriminatorExtension.java   # Mapper の登録
│       │   └── DiscriminatorValidator.java        # discriminator 関連のバリデーション
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
├── smithy-build.json               # Smithy ビルド設定
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
Smithy モデル → (gradle build) → OpenAPI JSON → oapi-codegen → Go 型コード
                                              → generate-docs.sh → Stoplight HTML
```

### コマンド

```bash
# 全体を実行（Go コード生成 + ドキュメント生成）
make

# 個別実行
make smithy      # Smithy ビルド（バリデーション + OpenAPI JSON 生成）
make generate    # Smithy ビルド + Go 型コード生成
make docs        # Smithy ビルド + Stoplight HTML 生成

# クリーン
make clean
```

## カスタムトレイト

`openapi-mapper` モジュールで定義しているカスタムトレイト：

| トレイト | 対象 | 説明 |
|---|---|---|
| `@discriminatorField` | union | discriminator として使うフィールド名を指定 |
| `@discriminatorValue` | structure | その構造体の discriminator 値を指定 |

### 使用例

```smithy
@discriminatorField("type")
union PutRealEstate {
    type11xx: RealEstate11xx
    type12xx: RealEstate12xx
}

@discriminatorValue("11xx")
structure RealEstate11xx {
    @required
    type: RealEstateType
}
```

これにより OpenAPI の `oneOf` + `discriminator` に変換されます。

## バリデーション

`DiscriminatorValidator` が以下をビルド時にチェックします：

- union メンバーが structure であること
- 各 structure に discriminator フィールドが存在すること
- `@discriminatorValue` トレイトが付与されていること
- discriminator 値が対応する enum に含まれていること
- discriminator 値の重複がないこと
- enum の全値が union メンバーでカバーされていること

## API ドキュメント

`docs/index.html` を直接ブラウザで開くと Stoplight Elements による API ドキュメントを確認できます。

```bash
open docs/index.html
```
