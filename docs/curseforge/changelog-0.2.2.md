# Singulo Arsenal 0.2.2 — Minecraft 1.21.1

Compatibility update. Requires the matching Singulo 0.16.x, 0.17.x or 0.18.x build.

## English

- Allow Singulo 0.18.x; Arsenal 0.2.1 rejected 0.18.0 through its dependency upper bound.
- Pin integration validation to Singulo 0.18.0. Equipment mechanics, recipes and saved item data are unchanged.
- Validate actual Arsenal gear in the new Singulo SMES charging slots, including FE accounting, full-charge output, blocked output and installed modules.
- Keep Java sources in LF on every platform so compiler-generated debug metadata does not depend on checkout line endings.

## 日本語

- Singulo 0.18.x に対応しました。Arsenal 0.2.1 では依存関係の上限により 0.18.0 が読み込めませんでした。
- 連携検証を Singulo 0.18.0 のコミットへ固定しました。装備の動作・レシピ・保存データの形式は変えていません。
- 新しい SMES 充電枠で実装備を検証し、電力収支、満充電後の出力、出力詰まり、装着モジュールの保持を確認しました。
- Java ソースの改行を LF に統一し、コンパイラー生成のデバッグ情報が環境の改行設定に依存する問題を抑えました。
