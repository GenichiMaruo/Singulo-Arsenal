# Singulo Arsenal

## 0.3.2 compatibility update

Singulo 0.21.0 integration update. See [verification notes](docs/development/singulo-0.21-compatibility.md). Historical release notes below describe earlier verification.

## 0.3.1 compatibility update

Singulo 0.20.0 integration update. See [verification notes](docs/development/singulo-0.20-compatibility.md). Historical 0.3.0 notes below describe original verification; this update retains the Singulo >=0.19.0 dependency policy.

[日本語](docs/player/ja.md) · [English](docs/player/en.md) · [装備仕様・レシピ](docs/arsenal.md)

**超伝導、縮退圧、ブラックホール。Singulo の物理を、そのまま身に着ける装備にする公式アドオンです。**
Minecraft 1.21.1（NeoForge）、1.20.1（Forge / NeoForge）、26.1.2（NeoForge）向け。Singulo の工場で作った素材から防具・武器・工具を作ります。どの装備も本体で扱う物理現象をひとつ題材にしていて、その現象が性能にも見た目にも出ます。

## 対応バージョン / Minecraft versions

Arsenal 0.3.2 は、下の4構成で Singulo 0.21.0 と動作確認済みです。依存関係の宣言は Singulo 0.19.0 以上ですが、導入時は同じ行の 0.21.0 を推奨します。

Arsenal 0.3.2 was tested with Singulo 0.21.0 on all four configurations below. The declared dependency accepts 0.19.0 and later; use the matching 0.21.0 build for the tested setup.

| Minecraft | ローダー / Loader | Java | Singulo 0.21.0 | Arsenal 0.3.2 |
| --- | --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.256+ | 21 | [本体 / Core](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9709614) | [1.21.1N](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files/9709876) |
| 1.20.1 | Forge 47.4.26+ | 17 | [本体 / Core](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9709620) | [1.20.1F](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files/9709929) |
| 1.20.1 | NeoForge 47.1.106+ | 17 | [本体 / Core](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9709627) | [1.20.1N](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files/9709956) |
| 26.1.2 | NeoForge 26.1.2.126+ | 25 | [本体 / Core](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9709632) | [26.1.2N](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files/9709976) |

ファイル名の末尾は `N` が NeoForge、`F` が Forge です。マルチプレイではクライアントとサーバーの両方に導入してください。

File names end in `N` for NeoForge or `F` for Forge. Install both mods on the client and the server for multiplayer.

初回の 0.1.0 は GitHub Releases で配布しています。以後の版は [CurseForge](https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files) を参照してください。

## 何ができる？

- **3セットの防具を着る** — 磁場を押し出して浮かぶマイスナー防護服、受けた衝撃を圧力として溜める縮退圧装甲、背中にブラックホールを背負い、核パスタの相で性質が変わるコラプサー装甲。
- **物理で戦う** — 超伝導が破れて爆ぜるクエンチ・セイバー、真空の刃で防具を無視するカシミール刃、ロッシュ焦点で敵を引き伸ばす潮汐断裁刃、相対論的ジェットを撃ち出す降着ジェット砲。
- **電力で掘る**（0.2.0） — 鉱石を壁越しに探す SQUID 探査ピック、3×3 で崩す縮退圧ブレーカー、木を丸ごと倒す剪断ソー、鉱脈ごと吸い込む降着ドリル。どれも耐久値がなく、電力で動きます。
- **装備を組み替える**（0.2.0） — モジュール統合台で、ビットドローン、電力だけでの飛行、融解起爆、自動精錬、誘導給電、アイテム回収などの拡張モジュールを、装備の段階に応じて 1／3／5 個組み込めます。
- **本体の電力で動かす** — 装備は標準 FE で充電します。コラプサー級は質量ペレットを直接電力に変え、ペレットは質量ペレット缶にまとめて持ち歩けます。

**Singulo Arsenal** is the official equipment add-on for [Singulo](https://github.com/GenichiMaruo/Singulo) on Minecraft 1.21.1 (NeoForge), 1.20.1 (Forge or NeoForge), and 26.1.2 (NeoForge). It adds three armor sets, four weapons and, from 0.2.0, four powered tools, each built around one piece of physics from the base mod: superconductivity, degeneracy pressure, tidal forces, relativistic jets. Gear runs on FE, takes upgrade modules at the Module Integrator, and the top tier feeds on mass pellets. Start with the [English guide](docs/player/en.md).

## ドキュメント

- [遊び方・開発資料の入口](docs/README.md)
- [日本語](docs/player/ja.md) · [English](docs/player/en.md)
- [装備仕様・レシピ](docs/arsenal.md)：数値、レシピの配置、製作コスト
- [公開・配布手順](docs/curseforge.md)

## 開発するには

Java 21 の JDK と Python 3 を用意し、Singulo 本体の JAR を指定して起動します。0.3.2 の開発・互換性情報は [検証記録](docs/development/singulo-0.21-compatibility.md) を参照してください。

```sh
./gradlew --no-daemon build
./gradlew runClient -PsinguloJar=/absolute/path/Singulo-0.21.0-1.21.1N.jar
```

生成元と実装時の条件は[開発ガイド](docs/development/README.md)、検査と GameTest は[テスト手順](docs/development/testing.md)、配布は[公開・配布手順](docs/curseforge.md)を参照してください。

ライセンス: All Rights Reserved（Singulo 本体の表記に準拠）。

## 0.2.1 compatibility update / 互換性更新

Singulo 0.17.0 に対応しました。依存範囲を 0.16.x–0.17.x に広げ、CI の本体をレビュー済み 0.17.0 コミットへ固定しました。

The 0.2.1 compatibility build accepts Singulo 0.16.x and 0.17.x and validates integration against the released 0.17.0 source. See [verification notes](docs/development/singulo-0.17-compatibility.md).

## 0.2.2 compatibility update / 互換性更新

Singulo 0.18.x に対応する更新です。装備の動作・レシピ・保存データの形式は変更しません。

The 0.2.2 compatibility build accepts Singulo 0.16.x, 0.17.x and 0.18.x. See [verification notes](docs/development/singulo-0.18-compatibility.md).

## 0.3.0

- Singulo 0.19.0 以上が必須（上限なし）。0.20 以降も導入を許可しますが、将来版の互換性は未検証です。
- 1.20.1 では、外部データパックの item tag にある旧 `singulo_arsenal:jet_lance` 要素は `singulo_arsenal:jet_cannon` へ更新が必要です。タグ名・旧レシピIDは変えません。所持品などの保存状態は自動移行します。
- 能力・FE を持たないニホニウムの剣・工具・防具を追加。防具アイコンを刷新。
- ジェット砲の shader / オフハンド表示と額縁配置を修正。
- 内部名を `jet_cannon` に整理し、旧 `jet_lance` の保存アイテムを引き継ぎます。

Back up worlds before upgrading. Legacy cannon stacks migrate forward; downgrading
new saves to 0.2.2 is not supported. See [migration details](docs/development/cannon-migration-0.3.0.md).
