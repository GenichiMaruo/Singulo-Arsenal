# Singulo Arsenal 0.3.0 (development)

- Fix cannon aiming, supporting arms and muzzle alignment across vanilla and shader hand-rendering coordinate systems.
- A main-hand cannon uses both arms and hides the offhand item in first person. Holding the cannon in the offhand no longer adds two extra arms.
- Use a shader-pack-compatible beam fallback instead of an invisible custom shader.
- Center all four weapons and four powered tools in item frames.
- Rename the internal item ID to jet_cannon and migrate legacy jet_lance stacks through a NeoForge alias. Preserve recipe unlocks, item components and the existing Accretion Jet Cannon display name.

This is an unpublished development branch, not a release announcement.

## 日本語（開発中・未公開）

- shader 環境で銃の向き、腕、銃口とビームの座標系がずれる問題を修正。
- 主手の銃は両手持ちにし、一人称のオフハンド物を非表示にします。オフハンドの銃には余分な両腕を描きません。装備や三人称表示は変更しません。
- shader pack が専用シェーダーを抑止する環境では互換ビーム描画を使用。
- 4 武器と 4 ツールを額縁の中央へ配置。
- 内部アイテムIDを jet_cannon に変更し、旧 jet_lance は NeoForge の別名登録で引き継ぎます。レシピ解放・保存コンポーネント・「降着ジェット砲 / Accretion Jet Cannon」の表示名は維持します。
