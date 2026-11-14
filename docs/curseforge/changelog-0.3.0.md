# Singulo Arsenal 0.3.0 — Minecraft 1.20.1 Forge / NeoForge

- Fix cannon aiming, supporting arms and muzzle alignment across vanilla and shader hand-rendering coordinate systems.
- A main-hand cannon uses both arms and hides the offhand item in first person. Holding the cannon in the offhand no longer adds two extra arms.
- Use a shader-pack-compatible beam fallback instead of an invisible custom shader.
- Center all four weapons and four powered tools in item frames.
- Rename the internal item ID to jet_cannon and migrate legacy jet_lance stacks through a Forge registry alias. Preserve recipe unlocks, item NBT and the existing Accretion Jet Cannon display name.

- Requires Singulo 0.19.x ([0.19.0,0.20.0)). Older and untested future minor releases are not supported.
- Add plain nihonium tools and armor with vanilla durability, repair, enchantments and armor trims. These items do not use FE or modules.

## 日本語

- shader 環境で銃の向き、腕、銃口とビームの座標系がずれる問題を修正。
- 主手の銃は両手持ちにし、一人称のオフハンド物を非表示にします。オフハンドの銃には余分な両腕を描きません。装備や三人称表示は変更しません。
- shader pack が専用シェーダーを抑止する環境では互換ビーム描画を使用。
- 4 武器と 4 ツールを額縁の中央へ配置。
- 内部アイテムIDを jet_cannon に変更し、旧 jet_lance は Forge registry の別名登録で引き継ぎます。レシピ解放・保存NBT・「降着ジェット砲 / Accretion Jet Cannon」の表示名は維持します。

- Singulo 0.19.x 必須（[0.19.0,0.20.0)）。旧版と互換性未検証の将来minor版は対象外です。
- 通常の耐久・修理・エンチャント・防具装飾に対応するニホニウムのツールと防具を追加。FEやモジュールを使わない通常装備です。
