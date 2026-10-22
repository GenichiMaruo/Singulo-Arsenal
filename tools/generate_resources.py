#!/usr/bin/env python3
"""Deterministic data and pixel-art generator; Python 3 stdlib only. Run from any directory."""
import gzip
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
MOD = 'singulo_arsenal'
TIERS = ['explorer', 'catalytic', 'inertial', 'boundary']
PARTS = ['helmet', 'chestplate', 'leggings', 'boots']
EXTRAS = ['circulation_cartridge', 'inertial_upgrade', 'boundary_upgrade']
PALETTES = {
    'explorer': ('#525963', '#92999d', '#d6b879'),
    'catalytic': ('#56646a', '#d6e1d9', '#64cfaa'),
    'inertial': ('#253449', '#7c96b1', '#56d9ef'),
    'boundary': ('#221f35', '#544e72', '#ce9bf7'),
}

def write(path, value):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def item(id):
    return {'item': id}

def recipe(name, data, unlock):
    data['category'] = 'equipment' if data['type'] == 'minecraft:crafting_shaped' else data.get('category', 'misc')
    if data['type'].startswith('minecraft:smithing_'):
        data.pop('category', None)
    write(f'data/{MOD}/recipe/{name}.json', data)
    write(f'data/{MOD}/advancement/recipes/{name}.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed',
                    'conditions': {'items': [{'items': [unlock]}]}},
                    'has_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': f'{MOD}:{name}'}}},
        'requirements': [['has_material', 'has_recipe']],
        'rewards': {'recipes': [f'{MOD}:{name}']}})

# Both early suits can be crafted independently. Later suits improve existing equipment at a smithing table.
patterns = {
    'helmet': ['PSP', 'P P'], 'chestplate': ['P P', 'PSP', 'PPP'],
    'leggings': ['PSP', 'P P', 'P P'], 'boots': ['P P', 'S S']}
for tier in TIERS[:2]:
    for part in PARTS:
        key = {'P': item('singulo:steel_plate'), 'S': item('singulo:white_ceramic_composite')}
        pattern = patterns[part]
        if tier == 'catalytic':
            key = {'P': item('singulo:superconducting_coil'), 'S': item('singulo:muon_bundle')}
        recipe(f'{tier}_{part}', {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': key,
               'result': {'id': f'{MOD}:{tier}_{part}', 'count': 1}}, key['S']['item'])

recipe('circulation_cartridge', {'type': 'minecraft:crafting_shapeless',
       'ingredients': [item('singulo:spent_muon_catalyst'), item('singulo:white_ceramic_composite'), item('singulo:steel_plate')],
       'result': {'id': f'{MOD}:circulation_cartridge', 'count': 1}}, 'singulo:spent_muon_catalyst')
recipe('inertial_upgrade', {'type': 'minecraft:crafting_shaped', 'pattern': [' C ', 'QEQ', ' C '],
       'key': {'C': item('singulo:superconducting_coil'), 'Q': item('singulo:quantum_computing_module'), 'E': item('singulo:entangled_element')},
       'result': {'id': f'{MOD}:inertial_upgrade', 'count': 1}}, 'singulo:quantum_computing_module')
recipe('boundary_upgrade', {'type': 'minecraft:crafting_shaped', 'pattern': [' D ', 'GHG', ' D '],
       'key': {'D': item('singulo:degenerate_matter_shell'), 'G': item('singulo:gravity_field_stabilizer'), 'H': item('singulo:hawking_condensate')},
       'result': {'id': f'{MOD}:boundary_upgrade', 'count': 1}}, 'singulo:hawking_condensate')
reset = {f'{MOD}:{key}': 0 for key in ['energy', 'impact', 'repair', 'shield', 'active_until', 'ready_at', 'overloaded_until', 'collect_at']}
for tier, base, addition in [('inertial', 'catalytic', 'quantum_computing_module'), ('boundary', 'inertial', 'gravity_field_stabilizer')]:
    for part in PARTS:
        recipe(f'{tier}_{part}', {'type': 'minecraft:smithing_transform',
               'template': item(f'{MOD}:{tier}_upgrade'), 'base': item(f'{MOD}:{base}_{part}'),
               'addition': item(f'singulo:{addition}'),
               'result': {'id': f'{MOD}:{tier}_{part}', 'components': reset}}, f'{MOD}:{tier}_upgrade')

ja, en = {}, {}
ja_names = {
 'explorer': ['遺構探査面', '補強探索服', '踏破用作業衣', '緩衝作業靴'],
 'catalytic': ['循環監視面', '触媒循環装甲', '補助循環装甲', '耐熱作業靴'],
 'inertial': ['慣性観測器', '慣性制御装甲', '姿勢制御装甲', '衝撃回収靴'],
 'boundary': ['境界観測面', '事象境界装甲', '境界安定装甲', '空間固定靴']}
en_names = {
 'explorer': ['Ruin Survey Visor', 'Reinforced Exploration Suit', 'Expedition Trousers', 'Cushioned Work Boots'],
 'catalytic': ['Circulation Monitor', 'Catalytic Circulation Armor', 'Auxiliary Circulation Leggings', 'Heatproof Work Boots'],
 'inertial': ['Inertial Observer', 'Inertial Control Armor', 'Attitude Control Leggings', 'Impact Recovery Boots'],
 'boundary': ['Boundary Observer', 'Event Boundary Armor', 'Boundary Stabilization Leggings', 'Spatial Anchor Boots']}
ja_tips = {
 'explorer': ['暗視キー（既定 V）：暗視10秒／再使用30秒。', '鉄相当の防御。鋼板で修理できる高耐久の作業服。', '着用中は凍結の蓄積を抑える。', '粉雪の上を歩ける。落下距離を3ブロック分軽減。'],
 'catalytic': ['対応する胴の補修残量を HUD に表示。', '非戦闘10秒後、1秒につき耐久1回復。持ち物の循環カートリッジ1個で256補修。', '対応する胴の自己修復を、装備中の触媒循環防具全体に拡張。', '溶岩の外では残り炎上時間を最大2秒に短縮。溶岩耐性はない。'],
 'inertial': ['衝撃蓄積・過負荷・蓄積装備の電力を HUD に表示。', '100 FE でノックバック60%軽減。被弾から衝撃を回収（上限100）。満杯で5秒停止。', '回避キー（既定 Z）：衝撃20＋2,000 FE。地上のみ／再使用1.5秒。単品でも衝撃を回収（上限40）。', '200 FE で落下ダメージ50%軽減。6ブロック以上の落下から衝撃回収。'],
 'boundary': ['防壁残量・持続時間・再使用待ち・胴の電力を HUD に表示。', '防壁キー（既定 B）：20,000 FE＋100 FE/t。ダメージ16点を吸収／3秒／再使用60秒。', '対応する胴の防壁の持続時間を5秒に延長。', '対応する防壁の作動中、落下ダメージとノックバックを75%軽減。']}
en_tips = {
 'explorer': ['Vision key (default V): 10s night vision, 30s cooldown.', 'Iron-level protection, high durability; repair with Steel Plates.', 'Prevents freezing from building up while worn.', 'Walk on powder snow; subtract 3 blocks from fall distance.'],
 'catalytic': ['Displays the matching chestplate\'s repair reserve.', 'After 10s out of combat, repairs 1 durability/s. A carried cartridge supplies 256 points.', 'Extends the matching chestplate\'s repair to all worn catalytic armor.', 'Outside lava, caps remaining burn time at 2 seconds. No lava immunity.'],
 'inertial': ['Displays impact, overload, and the reservoir armor\'s FE.', '100 FE reduces knockback by 60%. Collects impact from hits (max 100); overload disables for 5s.', 'Dodge key (default Z): 20 impact + 2,000 FE, grounded only, 1.5s cooldown. Collects impact alone (max 40).', '200 FE halves fall damage; recovers impact from falls of at least 6 blocks.'],
 'boundary': ['Displays shield points, duration, cooldown, and chestplate FE.', 'Barrier key (default B): 20,000 FE + 100 FE/t, absorbs 16 damage for 3s, 60s cooldown.', 'Extends the matching chestplate\'s barrier duration to 5 seconds.', 'While the matching barrier is active, reduces fall damage and knockback by 75%.']}
for tier in TIERS:
    for n, part in enumerate(PARTS):
        ja[f'item.{MOD}.{tier}_{part}'] = ja_names[tier][n]
        en[f'item.{MOD}.{tier}_{part}'] = en_names[tier][n]
        ja[f'tooltip.{MOD}.{tier}.{part}'] = ja_tips[tier][n]
        en[f'tooltip.{MOD}.{tier}.{part}'] = en_tips[tier][n]
for id, j, e in [('circulation_cartridge', '循環カートリッジ', 'Circulation Cartridge'),
                 ('inertial_upgrade', '慣性制御改修キット', 'Inertial Upgrade Kit'),
                 ('boundary_upgrade', '事象境界改修キット', 'Boundary Upgrade Kit')]:
    ja[f'item.{MOD}.{id}'] = j
    en[f'item.{MOD}.{id}'] = e
common = {
 'itemGroup.singulo_arsenal': ('Singulo Arsenal', 'Singulo Arsenal'),
 'key.categories.singulo_arsenal': ('Singulo Arsenal', 'Singulo Arsenal'),
 'key.singulo_arsenal.vision': ('遺構探査面：暗視', 'Survey visor: Night vision'),
 'key.singulo_arsenal.dodge': ('慣性装甲：地上回避', 'Inertial armor: Ground dodge'),
 'key.singulo_arsenal.barrier': ('境界装甲：防壁展開', 'Boundary armor: Deploy barrier'),
 'tooltip.singulo_arsenal.stage': ('Singulo 進行段階 %s', 'Singulo progression stage %s'),
 'tooltip.singulo_arsenal.energy': ('電力：%s / %s FE', 'Energy: %s / %s FE'),
 'tooltip.singulo_arsenal.charge': ('充電：手に持ち、スニークして電源の出力面に使用。', 'Charge: hold and sneak-use an energy output face.'),
 'tooltip.singulo_arsenal.repair': ('補修残量：%s / %s', 'Repair reserve: %s / %s'),
 'message.singulo_arsenal.charged': ('%s FE 充電しました。', 'Charged %s FE.'),
 'message.singulo_arsenal.need_vision': ('遺構探査面を装備してください。', 'Equip a Ruin Survey Visor.'),
 'message.singulo_arsenal.need_dodge': ('姿勢制御装甲を装備してください。', 'Equip Attitude Control Leggings.'),
 'message.singulo_arsenal.need_barrier': ('事象境界装甲を装備してください。', 'Equip Event Boundary Armor.'),
 'message.singulo_arsenal.cooling': ('再使用まで待機中です。', 'Ability is cooling down.'),
 'message.singulo_arsenal.no_energy': ('電力が不足しています。', 'Not enough energy.'),
 'message.singulo_arsenal.overloaded': ('慣性装甲が過負荷：5秒間、衝撃制御が停止します。', 'Inertial overload: impact control disabled for 5 seconds.'),
 'message.singulo_arsenal.dodge_unavailable': ('回避には接地・衝撃20・脚の2,000 FEが必要です。過負荷や再使用待ちの間は使用できません。', 'Dodge needs ground, 20 impact and 2,000 leg FE; unavailable during overload or cooldown.'),
 'hud.singulo_arsenal.repair': ('循環装甲 | 補修残量 %s', 'Circulation | Repair reserve %s'),
 'hud.singulo_arsenal.overload': ('慣性装甲 | 過負荷 あと%s秒', 'Inertial | Overload: %ss'),
 'hud.singulo_arsenal.impact': ('慣性装甲 | 衝撃 %s | 回避 %s FE | 安定化 %s FE', 'Inertial | Impact %s | Dodge %s FE | Stabilizer %s FE'),
 'hud.singulo_arsenal.shield': ('境界場 | 防壁 %s | あと%s秒', 'Boundary | Shield %s | %ss'),
 'hud.singulo_arsenal.boundary': ('境界装甲 | %s FE | 再展開 %s秒', 'Boundary | %s FE | Cooldown %ss'),
}
for key, (j, e) in common.items():
    ja[key], en[key] = j, e
for locale, data in [('ja_jp', ja), ('en_us', en)]:
    write(f'assets/{MOD}/lang/{locale}.json', data)
write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Singulo Arsenal equipment resources'}})
write('data/minecraft/tags/item/freeze_immune_wearables.json', {
    'replace': False, 'values': [f'{MOD}:explorer_leggings']})

# Minimal PNG writer: no image tools/dependencies needed to regenerate the checked-in art.
def rgb(code):
    return tuple(bytes.fromhex(code.removeprefix('#'))) + (255,)

def canvas(w, h):
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]

def rect(pixels, x, y, w, h, color):
    for py in range(y, min(y+h, len(pixels))):
        for px in range(x, min(x+w, len(pixels[0]))):
            if px >= 0 and py >= 0:
                pixels[py][px] = color

def png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = b''.join(b'\x00' + b''.join(bytes(p) for p in row) for row in pixels)
    def chunk(tag, value):
        return struct.pack('>I', len(value)) + tag + value + struct.pack('>I', zlib.crc32(tag + value) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)

for tier in TIERS:
    dark, panel, glow = map(rgb, PALETTES[tier])
    for part in PARTS:
        pixels = canvas(16, 16)
        if part == 'helmet':
            rect(pixels, 3, 3, 10, 10, dark); rect(pixels, 4, 3, 8, 3, panel)
            rect(pixels, 4, 7, 8, 2, glow); rect(pixels, 6, 10, 4, 2, panel)
        elif part == 'chestplate':
            rect(pixels, 2, 3, 12, 4, dark); rect(pixels, 4, 5, 8, 9, dark)
            rect(pixels, 5, 5, 6, 7, panel); rect(pixels, 7, 6, 2, 3, glow)
            rect(pixels, 2, 3, 3, 2, panel); rect(pixels, 11, 3, 3, 2, panel)
            rect(pixels, 4, 12, 8, 1, glow)
        elif part == 'leggings':
            rect(pixels, 3, 2, 10, 4, dark); rect(pixels, 3, 6, 4, 8, dark); rect(pixels, 9, 6, 4, 8, dark)
            rect(pixels, 4, 6, 2, 6, panel); rect(pixels, 10, 6, 2, 6, panel)
            rect(pixels, 4, 3, 8, 1, glow); rect(pixels, 4, 9, 2, 1, glow); rect(pixels, 10, 9, 2, 1, glow)
        else:
            for x in (2, 9):
                rect(pixels, x, 4, 4, 9, dark); rect(pixels, x-1, 10, 5, 3, dark)
                rect(pixels, x+1, 5, 2, 6, panel); rect(pixels, x, 8, 3, 1, glow)
        id = f'{tier}_{part}'
        png(f'assets/{MOD}/textures/item/{id}.png', pixels)
        write(f'assets/{MOD}/models/item/{id}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/{id}'}})
    # Vanilla biped armor UV atlas (64x32): head, body, legs, arms. Layer 2 is used by leggings.
    for layer in (1, 2):
        pixels = canvas(64, 32)
        for x, y, w, h in [(0, 0, 32, 16), (0, 16, 16, 16), (16, 16, 24, 16), (40, 16, 24, 16)]:
            rect(pixels, x, y, w, h, dark)
            for py in range(y+1, y+h-1):
                for px in range(x+1, x+w-1):
                    if px % 8 not in (0, 7) and py % 8 not in (0, 7): pixels[py][px] = panel
            rect(pixels, x+1, y+5, w-2, 1, glow)
        # Narrow dark visor with illuminated edges on the front of the head.
        rect(pixels, 8, 10, 8, 3, dark); rect(pixels, 8, 10, 8, 1, glow)
        png(f'assets/{MOD}/textures/models/armor/{tier}_layer_{layer}.png', pixels)
for n, extra in enumerate(EXTRAS):
    pixels = canvas(16, 16)
    rect(pixels, 4, 2, 8, 12, rgb('#283442'))
    rect(pixels, 5, 3, 6, 10, rgb('#a8b8bb'))
    color = rgb(['#64cfaa', '#56d9ef', '#ce9bf7'][n])
    rect(pixels, 6, 5, 4, 6, color); rect(pixels, 6, 2, 4, 1, rgb('#d6b879'))
    png(f'assets/{MOD}/textures/item/{extra}.png', pixels)
    write(f'assets/{MOD}/models/item/{extra}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/{extra}'}})

# GameTest structure: 5x5 stone floor inside an otherwise empty arena, deterministic gzip timestamp.
def utf(s):
    b = s.encode(); return struct.pack('>H', len(b)) + b

def tag(t, name, data):
    return bytes([t]) + utf(name) + data

def integer(i):
    return struct.pack('>i', i)

def ints(values):
    return b'\x03' + integer(len(values)) + b''.join(integer(v) for v in values)

palette = b'\x0a' + integer(1) + tag(8, 'Name', utf('minecraft:stone')) + b'\x00'
blocks = b'\x0a' + integer(25) + b''.join(tag(9, 'pos', ints([x, 0, z])) + tag(3, 'state', integer(0)) + b'\x00' for x in range(5) for z in range(5))
nbt = b'\x0a\x00\x00' + tag(3, 'DataVersion', integer(3955)) + tag(9, 'size', ints([5, 5, 5])) + tag(9, 'palette', palette) + tag(9, 'blocks', blocks) + tag(9, 'entities', b'\x0a' + integer(0)) + b'\x00'
path = ROOT / f'data/{MOD}/structure/empty.nbt'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(nbt, mtime=0))
print('Generated 19 items, 8 armor textures, 19 recipes, recipe unlocks, ja/en translations and GameTest arena.')

# Keep weapon translations alongside the armor catalog during deterministic regeneration.
from generate_railgun_resources import generate as generate_railgun

generate_railgun()
