# Singulo Arsenal 0.3.0 for Minecraft 26.1.2 NeoForge

- Port to Minecraft 26.1.2, NeoForge 26.1.2.126 or newer, and Java 25.
- Requires the matching Minecraft 26.1.2 build of Singulo 0.19.0 or newer.
- Update transactional energy, persistence, item definitions, armor and render pipelines.
- Verified with 65 addon GameTests and vanilla client visual review.
- Known limitation: shader support is not verified on this port. Iris 1.11.3/1.11.4 with Complementary can fail development GL validation with `Missing sampler Sampler1`, also reproduced without Singulo or Arsenal. Use the normal renderer if affected. See Iris issue #3347. This does not establish that the same exception occurs in production installations.

## Features retained from 0.3.0

- Add ordinary nihonium tools and armor with durability, repair and vanilla enchantments, but no FE or special abilities.
- Refresh armor inventory sprites and fit the cannon icon diagonally.
- Fix cannon aiming, supporting arms and muzzle alignment across vanilla and shader hand-rendering coordinate systems.
- A main-hand cannon uses both arms and hides the offhand item in first person. Holding the cannon in the offhand no longer adds two extra arms.
- Use a shader-pack-compatible beam fallback instead of an invisible custom shader.
- Center all four weapons and four powered tools in item frames.
- Rename the internal item ID to jet_cannon and migrate legacy jet_lance stacks through a NeoForge alias. Preserve recipe unlocks, item components and the existing Accretion Jet Cannon display name.

Requires Singulo 0.19.0 or newer, without an upper bound. Tested with 0.19.0; compatibility with 0.20 and later is unverified. Back up worlds before upgrading; downgrading canonical jet_cannon saves to 0.2.2 is not supported.

## 日本語

- Singulo 0.19.0 以上を必須化（上限なし）。0.19.0 で検証済み、0.20 以降の互換性は未検証です。更新前にワールドをバックアップしてください。
- FEや特殊能力を持たないニホニウム製の工具・防具を追加。通常の修理・耐久・エンチャントに対応。
- 防具のアイコンを刷新し、ジェット砲のアイコンを対角に配置。
- shader 環境で銃の向き、腕、銃口とビームの座標系がずれる問題を修正。
- 主手の銃は両手持ちにし、一人称のオフハンド物を非表示にします。オフハンドの銃には余分な両腕を描きません。装備や三人称表示は変更しません。
- shader pack が専用シェーダーを抑止する環境では互換ビーム描画を使用。
- 4 武器と 4 ツールを額縁の中央へ配置。
- 内部アイテムIDを jet_cannon に変更し、旧 jet_lance は NeoForge の別名登録で引き継ぎます。レシピ解放・保存コンポーネント・「降着ジェット砲 / Accretion Jet Cannon」の表示名は維持します。
