# ドキュメント案内

このブランチは **0.2.0 / Minecraft 1.20.1 / NeoForge・Forge** 向けです。目的別に次を読んでください。

| 目的 | 読むもの |
| --- | --- |
| 遊び方 | [日本語](player/ja.md) · [English](player/en.md) |
| 数値・レシピの配置・製作コスト | [装備仕様](arsenal.md)。数値の正本は `core/Rules.java` |
| コードを変更する | [開発ガイド](development/README.md) → [テスト手順](development/testing.md) |
| 配布 | [公開・配布手順](curseforge.md)、ロゴ素材は [curseforge/](curseforge/) と [branding/](branding/) |

## 配置と維持

- `player/`：プレイヤー向けの説明。機能を変えたら日本語と英語を両方直す。
- `arsenal.md`：装備仕様。`Rules.java` と `tools/generate_resources.py` から書き起こしている。数値やレシピを変えたら合わせて直す。
- `development/`：ビルド・生成・検査の現行手順。
- `curseforge.md`、`curseforge/`、`branding/`：公開用の手順・説明・ロゴ。
