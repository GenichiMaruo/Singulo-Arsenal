# 公開・配布手順

## 配布方針

GitHub Releases への MOD JAR 添付は初回の `v0.1.0` に限定します。
CurseForge の審査通過後は CurseForge で MOD ファイルを配布し、以後の GitHub Releases には変更履歴・対応環境・CurseForge の配布リンクを掲載します。
GitHub Releases の後続リリースには MOD JAR を添付しません。GitHub が自動表示するソースコードアーカイブと、開発用の Actions 成果物は通常どおり利用できます。

CurseForge のプロジェクト ID は `1793647`、[プロジェクトページ](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal) は作成済みです。初回の1.21.1版はファイルID `9523647` として提出済みです。

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

現在の CI は検証と開発用成果物の保存を行います。公開は手動操作で実施します。将来自動化するときも、GitHub Releases への JAR 添付を初回限定とする方針を維持してください。

## 1.20.1 Beta の追加

NeoForge 47.1.106 と Forge 47.4.26 に対応し、Java 17 を使用します。同じプロジェクトに次の2ファイルをBetaとして追加します。

| ローダー | ファイル | 対応する本体 |
| --- | --- | --- |
| NeoForge | `Singulo-Arsenal-0.1.0-1.20.1N.jar` | `Singulo-0.16.0-1.20.1N.jar` |
| Forge | `Singulo-Arsenal-0.1.0-1.20.1F.jar` | `Singulo-0.16.0-1.20.1F.jar` |

プロジェクトの概要・説明・変更履歴は英語で登録します。各ファイルにはMinecraft 1.20.1、Java 17、該当ローダー、Client / Server、SinguloのRequired Dependencyを指定し、承認後の自動公開を選択します。
マージ後のCI成果物を使用し、GitHub ReleasesへのJAR追加は行いません。
