# テスト手順

変更したら、少なくとも `build`（オフライン検査を含む）と GameTest を通してください。見た目を変えたときは実クライアントでの撮影も行います。

## オフライン検査

ゲームを起動せずに実行できます。`build` からも自動で呼ばれます。

```sh
python3 tools/check.py
# 本体のソースがあれば、レシピに使う本体の素材 ID も照合する
SINGULO_SOURCE=/absolute/path/to/Singulo python3 tools/check.py
```

- `tests/RulesTest.java`：`Rules.java` の能力計算（現在 5,331 チェック）。
- レシピ・モデル・アイコン・翻訳キーの揃い、日英の書式記号の一致、要約の長さ。
- `generate_resources.py` を再実行しても差分が出ないこと（生成物の直接編集を検出する）。

## GameTest

本体 JAR と同時に読み込んだ実サーバーで、装備の能力を実際に動かして確かめます。現在は必須テスト 57 件（各ローダー）です。

```sh
./gradlew runGameTestServer -PsinguloJar=/absolute/path/Singulo-0.18.0-1.20.1N.jar
```

Forge は `-Ploader=forge` と Forge 用の本体 JAR を指定します。同じ作業ディレクトリで異なるローダーを同時に起動しないでください。

電力、保存と読み込み、降着と質量ペレット缶、飛び道具の反発、チャンドラセカール限界、核パスタ相、事象の地平面、ジェット噴射、電力シールド、武器の能力、地形破壊、工具の範囲採掘・鉱石探査と感度、モジュール統合と各モジュールの効果、容量の違う装備の電力分配、武器とドローンの効果分離、全レシピの読み込みを検証します。
テストクラスとテスト用構造物は配布 JAR から除外しています。
採掘キャンセル後の周辺破壊・FE 消費、持ち替えや終了 tick の欠落によるリロード回避、クリックと Shift クリックでの探査感度継承、精錬結果のスタック分割、0.1.0 の統合台保存データの移行も検証します。

テストを足すときの注意：
- モックプレイヤーはネットワーク経由で tick されません。tick に依存する処理は、`SuitEvents.upkeep` のように公開した入口を直接呼びます。
- 対象のブロックやアイテムはテスト用構造物（アリーナ）の内側に置きます。外側は読み込まれていないことがあります。
- 他のテストと干渉する重い処理（地形を大きく壊すものなど）は別の `batch` に分けます。

## 実クライアントでの撮影

`tools/review` は、開発クライアントを起動して全装備・全状態を撮影するハーネスです。アイコンの大きさ、構えたときの位置、面の重なり（Z ファイティング）も検査します。

```sh
python3 tools/review/prepare_gui.py --build-dir build/neoforge
./gradlew --no-daemon -I tools/review/gui.gradle runClient -PsinguloJar=/absolute/path/Singulo-0.18.0-1.20.1N.jar
```

撮影結果は `build/<loader>/gui-review/screenshots/` に出ます。面の重なりの報告は `zfight.txt`、アイコン単体の撮影は `icons-bare.png` です。静止画なので、動いているときのちらつきは実際に操作して確認してください。

## CI

GitHub Actions（`.github/workflows/build.yml`）は `1.20.1` と `feature/**` への push、`1.20.1` への pull request で動きます。

- **build (neoforge / forge)**：`build`（オフライン検査を含む）、公開検査スクリプトの回帰テスト、JAR・変更履歴の検査を実行し、固定した本体コミットとの素材 ID 照合・GameTest も実行する。JAR とログを成果物として保存する。

Gradle 配布ファイルの取得は、失敗しても最大 5 回まで再試行します。

公開ワークフローも同じ CI を呼び出し、Forge / NeoForge の両方が成功したときだけ次に進みます。公開検査だけをローカルで確認するには、Python 3.12 で以下を実行します（秘密情報・ネットワークは不要）。

```sh
python3 -m unittest discover -s tests -p 'test_release.py' -v
python3 tools/prepare_release.py
```

公開処理の回帰テストは25件です。古い JAR の取り違え、バージョン・依存関係の不一致、テスト資材の混入、変更履歴の欠落、誤った公開先を拒否することを検証します。API 確認が GET のみであること、アップロード時の multipart が正しいこと、失敗時に再送や必須依存の除去を行わないこともモックで確認します。実際の API 設定確認と公開手順は [公開・配布手順](../curseforge.md) を参照してください。

0.2.2 adds a Singulo 0.18 SMES interoperability test: partial charging, exact FE debit, full-charge transfer, blocked output and installed-module retention. The full suite requires Singulo 0.18.0; production metadata still permits the previously supported 0.16.x / 0.17.x releases.
