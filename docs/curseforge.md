# 公開・配布手順

## 配布方針

GitHub Releases への MOD JAR 添付は初回の `v0.1.0` に限定します。
CurseForge の審査通過後は CurseForge で MOD ファイルを配布し、以後の GitHub Releases には変更履歴・対応環境・CurseForge の配布リンクを掲載します。
GitHub Releases の後続リリースには MOD JAR を添付しません。GitHub が自動表示するソースコードアーカイブと、開発用の Actions 成果物は通常どおり利用できます。

CurseForge のプロジェクト ID は `1793647`、配布先は [Singulo Arsenal](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal) です。

## 0.2.0 の公開準備

Minecraft 1.21.1 / NeoForge / Java 21 向けです。`gradle.properties` の `mod_version` と CI の成果物は `0.2.0`、配布ファイルは `build/libs/Singulo-Arsenal-0.2.0-1.21.1N.jar` です。必須依存は Singulo 0.16.x です。

PR のレビュー、build / integration、実クライアントの確認結果を判断してからマージします。公開用の変更履歴は [`changelog-0.2.0.md`](curseforge/changelog-0.2.0.md) です。GitHub の `v0.2.0` には変更履歴と配布リンクを掲載し、MOD JAR は添付しません。

## Actions から CurseForge に公開する

`.github/workflows/publish-curseforge.yml` の **Publish CurseForge** を手動実行します。push や PR のマージだけでは公開されません。GitHub の仕様上、最初にこのワークフローを既定ブランチ `1.21.1` にマージする必要があります。

GitHub Environment **`curseforge`** に以下を設定します。2026-11-10 時点で、両方の Secret 名が登録済みであることを確認しています。値はログやリポジトリに保存しません。

| Environment secret | 設定内容 |
| --- | --- |
| `CURSEFORGE_PROJECT_ID` | このアドオンのプロジェクト ID `1793647` |
| `CURSEFORGE_TOKEN` | このプロジェクトへのファイルアップロード権限を持つ CurseForge API token |

`CURSEFORGE_PROJECT_ID` は同名の Environment variable でも指定できます。Secret があればそちらを優先します。

1. Actions → **Publish CurseForge** → **Run workflow** を開き、ブランチを `1.21.1` にします。
2. 最初は `dry_run=true`（既定値）、`release_type=beta`（既定値）で実行します。build、integration、公開用 JAR の検査、設定済みトークンを使った CurseForge バージョン一覧の GET を確認します。このモードはアップロードしません。GET の成功だけでは対象プロジェクトのアップロード権限や審査状態までは確認できません。
3. 公開するときは同じブランチで `dry_run=false` にして実行します。リリース種別は `alpha` / `beta` / `release` から選べます。アップロードは `1.21.1` ブランチに限定します。
4. 実行ごとに選択時のコミットをビルドし、通常 CI と同じ build / integration（本体素材 ID 照合・55 GameTest）を通します。同じ実行で生成・検査した JAR だけをアップロードします。確認モードと公開の間にコミットが進んでいれば、公開時のコミットで再検証されます。
5. Actions の Summary でコミット、ファイル名、SHA-256 と公開ファイル URL を確認します。CurseForge の審査・公開状態を確認してから README や GitHub リリースノートから案内します。

公開ファイルには Minecraft `1.21.1`、NeoForge、Java `21`、必須依存 `singulo` を設定します。`gradle.properties` のバージョンと一致する JAR・変更履歴が必要で、JAR 内の依存関係とテスト用クラスの除外も検査します。次回リリースでは `mod_version` と `docs/curseforge/changelog-<version>.md` を更新してください。

アップロードは自動再試行しません。通信が途切れても CurseForge 側では受理されている場合があるため、失敗後に再実行する前に Files 一覧を確認してください。同時公開は concurrency で直列化します。GitHub のタグ・Release はこのワークフローでは作成しません。

`tools/prepare_release.py` は [CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api) に従って送信します。バージョン一覧とカテゴリから正しい `gameVersions` の数値 ID を解決し、必須依存が拒否された場合も依存指定を外すフォールバックは行いません。

## 初回 0.1.0 の公開情報

| 項目 | 値 |
| --- | --- |
| MOD 名 | Singulo Arsenal |
| ファイル・表示名 | Singulo-Arsenal-0.1.0-1.21.1N |
| ファイル | build/libs/Singulo-Arsenal-0.1.0-1.21.1N.jar |
| Minecraft | 1.21.1 |
| ローダー | NeoForge |
| Java | 21 |
| 必須依存 | Singulo（申請後、実際の CF プロジェクトを指定） |
| リリース種別 | Beta（GitHub では Pre-release） |
| GitHub タグ | `v0.1.0` |
| GitHub リリース名 | `Singulo Arsenal 0.1.0 — Minecraft 1.21.1 / NeoForge (Beta)` |
| 言語 | 日本語・英語 |
| アイコン | `docs/curseforge/logo.png`（512×512）、`logo_400.png`（400×400） |

アイコンはM案（極ジェット付きのブラックホール）を使用します。
本体と共通の光線追跡式に、解析式で表したジェットの発光を加え、光の分布を色面へ整理しています。
数値設定は `docs/branding/logo-settings.json`、再生成は `python tools/arsenal_logo.py`（NumPy・Pillowが必要）。
MOD一覧用の画像は `src/main/resources/singulo_arsenal_logo.png` に同時出力します。
K・L案も同じ設定ファイルに保存しています。`python tools/arsenal_logo.py --variant K`、
`python tools/arsenal_logo.py --variant L` で `docs/branding/variants/` に512・400・64px版を出力できます。

## GitHub 初回リリース

1. README・装備仕様・公開手順を更新し、PR のレビューと CI の build / integration の成功を確認します。
2. PR を `1.21.1` にマージし、マージ後のコミットでも CI の成功を確認します。
3. そのコミットの build ジョブが生成した `Singulo-Arsenal-0.1.0-1.21.1N.jar` を取得します。
4. JAR の MOD バージョン・依存関係・ロゴを確認します。配布対象はアドオン本体で、検証用クラスと Singulo 本体はビルド設定で除外しています。
5. マージコミットに `v0.1.0` を付け、Beta の Pre-release を作成します。JAR を添付し、リリースノートへ対応環境、導入手順、変更内容、SHA-256 を記載して公開します。

## CurseForge 審査通過後

1. 実際のプロジェクト ID・公開 URL と、必須依存である Singulo 本体のプロジェクトを確定します。
2. Minecraft 1.21.1 / NeoForge / Java 21 向けのファイルとして公開し、README と GitHub リリースノートから案内します。
3. 次回以降の GitHub リリースでは、タグと変更履歴を公開し、CurseForge の該当バージョンへのリンクを記載します。添付アセットへ MOD JAR を追加しない運用とします。

通常の CI は検証と開発用成果物の保存を行います。CurseForge への公開は上記の手動ワークフローで行います。GitHub Releases への JAR 添付を初回限定とする方針は変わりません。

## 0.2.1 compatibility build — unpublished

Singulo 0.16.x / 0.17.x に対応する検証用ビルドです。この作業ではタグ・GitHub Release・CurseForge・Modrinth の公開は行いません。[互換性記録](development/singulo-0.17-compatibility.md)と[変更履歴](curseforge/changelog-0.2.1.md)を参照してください。
