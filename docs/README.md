# ドキュメント案内

この版は **0.2.1 / Singulo 0.17.0 互換性対応**。対応範囲と検証結果は [互換性記録](development/singulo-0.17-compatibility.md) を参照してください。

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
