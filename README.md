# Singulo Arsenal

[Singulo](https://github.com/GenichiMaruo/Singulo) の公式武器・防具アドオン。
**0.1.0（初回 Beta）**は、本体 0.16 系の素材と Jet Forge を前提に、3セットの防具と4種の武器を追加します。
どの装備も本体で扱う物理現象をひとつ題材にしていて、性能と見た目の両方に出るようにしています。

## 対応環境

| Minecraft | ローダー | 状態 |
| --- | --- | --- |
| 1.20.1 | NeoForge (`N`) / Forge (`F`) | このブランチの実装対象 |
| 1.21.1 | NeoForge (`N`) | `1.21.1` ブランチで対応 |

Java 17 **JDK**、NeoForge 47.1.106 または Forge 47.4.26 を使用します。MOD ID は `singulo_arsenal`。
Singulo は必須で、`[0.16.0,0.17.0)` に対応します。
この移植版は `1.20.1` ブランチで管理します。

## 導入

1. Minecraft 1.20.1 に NeoForge 47.1.106 または Forge 47.4.26 と Java 17 を用意します。
2. 同じ Minecraft・ローダー向けの Singulo 0.16 系を導入します。
3. `Singulo-Arsenal-0.1.0-1.20.1N.jar` を `mods` フォルダーに配置します。マルチプレイではクライアントとサーバーの両方に導入します。

1.20.1版は [CurseForge](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal) で配布します。使用するローダーに合わせ、NeoForge版の `N.jar` または Forge版の `F.jar` を選んでください。

## 対応言語

日本語（`ja_jp`）と英語（`en_us`）に対応し、Minecraft の言語設定に合わせて切り替わります。
アイテム名、説明、HUD、統合台、キー設定、通知、死亡メッセージの92項目を翻訳しています。
ほかの言語を選択した場合は英語で表示します。

## 装備

| 段階 | 防具 | 題材 | 武器 |
| --- | --- | --- | --- |
| 2 | マイスナー | 超伝導の磁場排除、磁束ピン止め | クエンチ・セイバー |
| 4 | 縮退圧 | チャンドラセカール限界 | カシミール刃 |
| 5（Jet Forge） | コラプサー | 核パスタ相転移、ジェット噴射、事象の地平面 | 潮汐断裁刃・降着ジェット砲 |

能力の数値・レシピの配置の意味・素材コストは [装備仕様](docs/arsenal.md) を参照してください。
防具と武器は立体メッシュで描画します。アイテムアイコンも同じ形状を使用します。

## 操作・補給

- **V**：起動機能。縮退圧は圧力の放出、コラプサーはジェット噴射。キー設定で変更できます。
- **スニーク＋空中**：マイスナー浮上靴で磁束ピン止め。
- **右クリック長押し**：カシミール刃の鏡閉じ、潮汐断裁刃のロッシュ焦点、降着ジェット砲の照射。
- 装備は標準 FE で充電します。手に持ち、スニークして電源の出力面を右クリックすると容量の 1/8 ずつ充電します。
- コラプサー級の装備は、持ち物の質量ペレットを自動で電力に変えます（1個 500,000 FE）。
- 装備から FE を取り出すことはできません。機械用の電池にはなりません。

## ビルド・検証

```sh
./gradlew --no-daemon build
```

Windows は `gradlew.bat build`。`build` の検査には Python 3 も必要です。
出力は `build/neoforge/libs/Singulo-Arsenal-0.1.0-1.20.1N.jar` または `build/forge/libs/Singulo-Arsenal-0.1.0-1.20.1F.jar` です。
命名規則は `<MOD名>-<MODバージョン>-<Minecraftバージョン><ローダー記号>.jar`。
NeoForge版は既定値の `-Ploader=neoforge`、Forge版は `-Ploader=forge` で生成します。出力先は `build/neoforge/libs/` と `build/forge/libs/` です。バージョンは `gradle.properties` で管理します。

```sh
# ゲームなしで実行可能：能力計算、レシピ・翻訳・モデル・再生成検査
python3 tools/check.py
# 本体の素材IDも照合する場合
SINGULO_SOURCE=/absolute/path/to/Singulo python3 tools/check.py
# 本体と同時ロードする実際の GameTest（29テスト）
./gradlew runGameTestServer -PsinguloJar=/absolute/path/Singulo-0.16.0-1.20.1N.jar
```

CI は NeoForge・Forge の両方で、ビルドとオフライン検査、固定した本体コミットとのGameTestを実行します。起動失敗やテスト未実行も検出し、29件すべての成功を必要とします。
オフラインの能力計算は5,417チェックです。CI では Gradle 配布ファイルの取得失敗を最大5回まで再試行します。
GameTest は、電力、保存・読み込み、降着、飛び道具の反発、チャンドラセカール限界、核パスタ相、事象の地平面、ジェット噴射、武器の能力、モジュール統合、容量の違う装備の電力分配、武器とドローンの効果分離、レシピの読み込みを検証します。
テストクラスとテスト用構造物は配布 JAR から除外します。

## 開発起動

### Windows のローカル設定

この PC では `start-client.cmd` をダブルクリックすると、本体とアドオンを読み込んだ開発用 Minecraft が起動します。
PowerShell からは `.\gradlew runClient` でも起動できます。Windows の Gradle ラッパーが `.local/environment.cmd` を読み込み、Java 17・本体 JAR・Python の設定を適用します。明示的な `-P` オプションがあればそちらが優先されます。
クリエイティブモードのワールドを作成し、専用のクリエイティブタブから充電済みの装備を試せます。
`build-local.cmd` は Python による検査を含めたビルドを実行します。

PC ごとの設定は Git 管理外の `.local/environment.cmd` に置きます。別の PC では、以下のパスを実際の配置に合わせて作成してください。

```bat
@echo off
set "JAVA_HOME=C:\path\to\jdk-17"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "ARSENAL_PYTHON=C:\path\to\python.exe"
set "ARSENAL_SINGULO=%~dp0..\libs\Singulo-0.16.0-1.20.1N.jar"
```

Java や Python のシステム全体の設定は変更せず、これらのスクリプト内で指定します。
通常の Gradle コマンドでも `-PpythonExecutable=C:/path/to/python.exe` で Python を指定できます。

同じ Minecraft・ローダーに対応する Singulo 本体 JAR を用意してください。
本体を Arsenal の JAR に同梱・再配布しません。

```sh
./gradlew runClient -PsinguloJar=/absolute/path/Singulo-0.16.0-1.20.1N.jar
./gradlew runServer -PsinguloJar=/absolute/path/Singulo-0.16.0-1.20.1N.jar
```

専用サーバーは Minecraft EULA を確認し、同意する場合にのみ `run/eula.txt` を設定して再実行してください。
クライアント起動には画面表示環境が必要です。
`singuloJar` 未指定・ファイル不存在は明示的にエラーにします。
起動ログの `Singulo Arsenal initialized`、MOD 一覧、本体と同時ロードできることを確認してください。

リソースの再生成は `python3 tools/generate_resources.py`。外部 Python パッケージは不要です。

## 公開

GitHub Releases への MOD JAR 添付は **初回の v0.1.0 のみ**とします。
CurseForge の審査通過後は CurseForge を MOD ファイルの配布先とし、以後の GitHub Releases には変更履歴と CurseForge の配布ページへのリンクを掲載します。
公開時の表示名は **Singulo-Arsenal-0.1.0-1.20.1N** と **Singulo-Arsenal-0.1.0-1.20.1F** です。
CurseForge のプロジェクト ID は `1793647` です。公開手順と継続リリースの方針は [公開・配布手順](docs/curseforge.md) を参照してください。

ライセンス: All Rights Reserved（Singulo 本体の表記に準拠）。

1.20.1では装備の状態とモジュールを名前空間付きNBTへ保存します。通信と描画は旧Forge APIに対応し、防具貫通は通常の防御計算に合わせて適用します。
