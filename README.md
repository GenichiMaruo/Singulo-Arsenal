# Singulo Arsenal

[Singulo](https://github.com/GenichiMaruo/Singulo) の公式武器・防具アドオン。
本体の素材、失われた文明、触媒工学・重力技術の世界観を引き継ぎます。
現在は **0.1.0 の開発用ひな形**です。武器・防具・レシピはまだ登録していません。

## 対応環境

| Minecraft | ローダー | 状態 |
| --- | --- | --- |
| 1.21.1 | NeoForge (`N`) | このブランチの実装対象 |
| 1.20.1 | NeoForge (`N`) / Forge (`F`) | 将来対応方針。未実装 |

- Java 21、NeoForge 21.1.256、本体と同じ Gradle / ModDevGradle を使用。
- MOD ID: `singulo_arsenal`
- Singulo は必須。本体 `1.21.1` ブランチのバージョンに合わせ、暫定で `[0.10.0,0.11.0)` を指定。
- メインブランチ: `1.21.1`、初期開発ブランチ: `feature/0.1.0`。

## ビルド

```sh
./gradlew --no-daemon build
```

Windows は `gradlew.bat build`。生成物:

```text
build/libs/Singulo-Arsenal-0.1.0-1.21.1N.jar
```

命名規則は `<MOD名>-<MODバージョン>-<Minecraftバージョン><ローダー記号>.jar`。
`N` は NeoForge、`F` は Forge。1.21.1 に Forge 成果物は生成しません。
バージョンは `gradle.properties` で管理します。

## 開発起動

同じ Minecraft・ローダーに対応する Singulo 本体 JAR を用意してください。
本体をこの JAR に同梱・再配布しません。CF 未登録のため Maven / CF ID による自動取得は行いません。

```sh
./gradlew runClient -PsinguloJar=/absolute/path/Singulo-0.10.0-1.21.1N.jar
./gradlew runServer -PsinguloJar=/absolute/path/Singulo-0.10.0-1.21.1N.jar
```

専用サーバーは初回に表示される Minecraft EULA を確認し、同意する場合にのみ
`run/eula.txt` を設定して再実行してください。クライアント起動には画面表示環境が必要です。
`singuloJar` 未指定やファイル不存在は明示的にエラーにします。
起動時の `Singulo Arsenal initialized`、MOD 一覧、本体と同時ロードできることを確認してください。

CI は Java 21 でビルドし JAR を保存します。まだゲーム内テストはありません。
本体 JAR を必要としないビルドの成功だけでは、同時起動の検証は完了しません。

## 今後の実装

本体の `singulo:` アイテム・タグを参照してレシピを作り、素材を重複登録しない方針です。
具体的な素材 ID、装備の性能、進行段階は本体の仕様と照合してから決定します。

## 公開

CurseForge は未申請です。仮 ID や自動アップロードは設定していません。
公開時の表示名は **Singulo-Arsenal-0.1.0-1.21.1N** とします。
申請後の作業は [公開準備](docs/curseforge.md) を参照してください。

ライセンス: All Rights Reserved（Singulo 本体の表記に準拠）。
