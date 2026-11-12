# Singulo Arsenal

[日本語](docs/player/ja.md) · [English](docs/player/en.md) · [装備仕様・レシピ](docs/arsenal.md)

**超伝導、縮退圧、ブラックホール。Singulo の物理を、そのまま身に着ける装備にする公式アドオンです。**
Minecraft 1.21.1（NeoForge）向け。Singulo の工場で作った素材から防具・武器・工具を作ります。どの装備も本体で扱う物理現象をひとつ題材にしていて、その現象が性能にも見た目にも出ます。

## 対応バージョン / Minecraft versions

Singulo 本体が必須です。Minecraft・ローダー・Singulo の版が合うファイルを選んでください。
Singulo is required. Choose the file that matches your Minecraft version, loader and Singulo version.

以下は未公開の **0.2.1 対応版**の範囲です。既存の 0.2.0 は Singulo 0.16.x 専用です。
The table describes the unpublished **0.2.1 compatibility build**. Existing 0.2.0 files require Singulo 0.16.x.

| Minecraft | ローダー / Loader | Java | Singulo | ソース / Source | ダウンロード / Download |
| --- | --- | --- | --- | --- | --- |
| **1.21.1** | **NeoForge** 21.1.256+ | **21** | 0.16.x / 0.17.x | [1.21.1](https://github.com/GenichiMaruo/Singulo-Arsenal/tree/1.21.1) | 未公開 / Not published |
| 1.20.1 | Forge / NeoForge | 17 | 0.16.x / 0.17.x | [1.20.1](https://github.com/GenichiMaruo/Singulo-Arsenal/tree/1.20.1) | 未公開 / Not published |

ファイル名は `Singulo-Arsenal-<版>-<Minecraft><ローダー>` で、末尾の `N` は NeoForge です。マルチプレイではクライアントとサーバーの両方に導入してください。
初回の 0.1.0 は GitHub Releases で配布しています。以後の版の配布先は [CurseForge](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal) です。0.2.0 は Minecraft 1.21.1 と 1.20.1 に対応しています。
File names follow `Singulo-Arsenal-<version>-<Minecraft><loader>`, where `N` means NeoForge. Install it on both the client and the server.

## 何ができる？

- **3セットの防具を着る** — 磁場を押し出して浮かぶマイスナー防護服、受けた衝撃を圧力として溜める縮退圧装甲、背中にブラックホールを背負い、核パスタの相で性質が変わるコラプサー装甲。
- **物理で戦う** — 超伝導が破れて爆ぜるクエンチ・セイバー、真空の刃で防具を無視するカシミール刃、ロッシュ焦点で敵を引き伸ばす潮汐断裁刃、相対論的ジェットを撃ち出す降着ジェット砲。
- **電力で掘る**（0.2.0） — 鉱石を壁越しに探す SQUID 探査ピック、3×3 で崩す縮退圧ブレーカー、木を丸ごと倒す剪断ソー、鉱脈ごと吸い込む降着ドリル。どれも耐久値がなく、電力で動きます。
- **装備を組み替える**（0.2.0） — モジュール統合台で、ビットドローン、電力だけでの飛行、融解起爆、自動精錬、誘導給電、アイテム回収などの拡張モジュールを、装備の段階に応じて 1／3／5 個組み込めます。
- **本体の電力で動かす** — 装備は標準 FE で充電します。コラプサー級は質量ペレットを直接電力に変え、ペレットは質量ペレット缶にまとめて持ち歩けます。

**Singulo Arsenal** is the official equipment add-on for [Singulo](https://github.com/GenichiMaruo/Singulo) on Minecraft 1.21.1 (NeoForge). It adds three armor sets, four weapons and, from 0.2.0, four powered tools, each built around one piece of physics from the base mod: superconductivity, degeneracy pressure, tidal forces, relativistic jets. Gear runs on FE, takes upgrade modules at the Module Integrator, and the top tier feeds on mass pellets. Start with the [English guide](docs/player/en.md).

## ドキュメント

- [遊び方・開発資料の入口](docs/README.md)
- [日本語](docs/player/ja.md) · [English](docs/player/en.md)
- [装備仕様・レシピ](docs/arsenal.md)：数値、レシピの配置、製作コスト
- [公開・配布手順](docs/curseforge.md)

## 開発するには

Java 21 の JDK と Python 3 を用意し、Singulo 本体の JAR を指定して起動します。0.2.0 はブランチ `feature/0.2.0` で開発しています。

```sh
./gradlew --no-daemon build
./gradlew runClient -PsinguloJar=/absolute/path/Singulo-0.17.0-1.21.1N.jar
```

生成元と実装時の条件は[開発ガイド](docs/development/README.md)、検査と GameTest は[テスト手順](docs/development/testing.md)、配布は[公開・配布手順](docs/curseforge.md)を参照してください。

ライセンス: All Rights Reserved（Singulo 本体の表記に準拠）。

## 0.2.1 compatibility update / 互換性更新

Singulo 0.17.0 対応版を準備しています。依存範囲を 0.16.x–0.17.x に広げ、CI の本体をレビュー済み 0.17.0 コミットへ固定しました。この対応版は未公開です。

The 0.2.1 compatibility build accepts Singulo 0.16.x and 0.17.x and validates integration against the released 0.17.0 source. It has not been published. See [verification notes](docs/development/singulo-0.17-compatibility.md).
