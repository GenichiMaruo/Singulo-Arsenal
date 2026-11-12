# 開発ガイド

Minecraft 1.21.1 / NeoForge 21.1.256 / Java 21。MOD ID は `singulo_arsenal`。作業対象のブランチと `gradle.properties` を最初に確認してください。
メインブランチは `1.21.1`、0.2.1 は Singulo 0.17.0 互換性対応版です。1.20.1 版は `1.20.1` ブランチを基点としています。

Singulo 本体はコンパイル時の依存にしていません。本体の ID・タグ・データコンポーネントは名前で参照し、実行時にだけ本体 JAR を読み込みます（`[0.16.0,0.18.0)`）。本体を Arsenal の JAR に同梱・再配布しません。

## 正となる生成元

| 内容 | 編集する場所 |
| --- | --- |
| 能力の数値・電力・モジュールの効果量 | `src/main/java/.../core/Rules.java`。オフライン検査 `tests/RulesTest.java` も合わせる |
| レシピ・タグ・アイテムモデル・翻訳・2D アイコン | `tools/generate_resources.py` |
| 防具・武器・工具の立体の形 | `client/art/`（防具は `SuitArt`、武器は `WeaponArt`、工具は `ToolArt`、統合台とビットは `MachineArt`）。描画の土台は `Canvas` |
| 統合台の画面 | `client/GuiKit.java`、`client/IntegratorScreen.java`。本体の GUI 規則に合わせる |
| 実機での検査 | `gametest/ArsenalGameTests.java` |

`src/main/resources/` のレシピ・翻訳・モデル・アイコンは生成物です。直接直さず、`tools/generate_resources.py` を直して再生成してください。外部の Python パッケージは不要です。

```sh
python3 tools/generate_resources.py
```

`tools/check.py` は再生成しても差分が出ないことまで確認します。

## 実装上の条件

- 判定はサーバーで行い、クライアントは表示だけにする。装備の状態は ItemStack のデータコンポーネントに持たせ、他のプレイヤーにも同期させる。
- 装備は FE を受け取るだけで、取り出せない。機械の電池にしない。
- 地形や他のブロックを壊す処理は、ブロックエンティティ、Singulo のブロック、保護 MOD で禁止された場所を巻き込まない。
- 立体の形は半ピクセル単位で作る。重なる面は 1/8 ピクセル以上ずらし、同じ平面に 2 枚の面を置かない（Z ファイティングでちらつく）。
- 本体の重力レンズは `client/Lensing` から名前で呼ぶ。本体の描画より先に登録する。

## ビルド

```sh
./gradlew --no-daemon build
```

Windows は `gradlew.bat build`。`build` は `tools/check.py` も実行するため Python 3 が必要です。Python は `-PpythonExecutable=C:/path/to/python.exe` でも指定できます。
出力は `build/libs/Singulo-Arsenal-<版>-1.21.1N.jar`。命名は `<MOD名>-<MODバージョン>-<Minecraftバージョン><ローダー記号>.jar` で、版は `gradle.properties` の `mod_version` です。1.21.1 に Forge 成果物は作りません。

## 開発起動

同じ Minecraft・ローダー向けの Singulo 本体 JAR を指定します。未指定や存在しないパスはエラーになります。

```sh
./gradlew runClient -PsinguloJar=/absolute/path/Singulo-0.17.0-1.21.1N.jar
./gradlew runServer -PsinguloJar=/absolute/path/Singulo-0.17.0-1.21.1N.jar
```

クリエイティブモードのワールドを作ると、専用タブから充電済みの装備を試せます。起動ログの `Singulo Arsenal initialized` と MOD 一覧で、本体と同時に読み込めたことを確認してください。
専用サーバーは Minecraft EULA を確認し、同意する場合にのみ `run/eula.txt` を設定して再実行してください。クライアント起動には画面表示環境が必要です。

### Windows のローカル設定

`start-client.cmd` をダブルクリックすると、本体とアドオンを読み込んだ開発用 Minecraft が起動します。`build-local.cmd` は Python の検査を含めてビルドします。
PowerShell からは `.\gradlew runClient` でも起動できます。Windows の Gradle ラッパーが `.local/environment.cmd` を読み込み、Java 21・本体 JAR・Python を設定します。明示した `-P` オプションのほうが優先されます。

PC ごとの設定は Git 管理外の `.local/environment.cmd` に置きます。別の PC では、パスを実際の配置に合わせて作ってください。

```bat
@echo off
set "JAVA_HOME=C:\path\to\jdk-21"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "ARSENAL_PYTHON=C:\path\to\python.exe"
set "ARSENAL_SINGULO=%~dp0..\libs\Singulo-0.17.0-1.21.1N.jar"
```

Java や Python のシステム全体の設定は変えず、これらのスクリプトの中で指定します。
