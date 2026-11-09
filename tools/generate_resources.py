#!/usr/bin/env python3
"""Deterministic generator for every data file, translation and icon. Python 3 stdlib only; run from any directory."""
import json
import math
from pathlib import Path
import random
import shutil
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
MOD = 'singulo_arsenal'
SETS = ['meissner', 'degeneracy', 'collapsar']
PARTS = ['helmet', 'chestplate', 'leggings', 'boots']
WEAPONS = ['quench_saber', 'casimir_blade', 'tidal_reaver', 'jet_lance']
MATERIALS = ['flux_weave', 'pressure_lattice', 'collapsar_lamina', 'jet_collimator', 'tidal_edge']
MODULES = ['bit_drone_module', 'capacitor_module', 'plating_module', 'thruster_module', 'coolant_module', 'amplifier_module']
DAMAGE = ['quench', 'collapse', 'jet', 'shear', 'horizon', 'bit']
MATERIAL_SIZE = 32
FINE_SIZE = 64


def write(path, value):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')


def s(name):
    return f'singulo:{name}'


def own(name):
    return f'{MOD}:{name}'


def ingredient(ref):
    return {'tag': ref[1:]} if ref.startswith('#') else {'item': ref}


def unlock_advancement(name, unlock):
    write(f'data/{MOD}/advancement/recipes/{name}.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': [unlock]}]}},
            'has_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': own(name)}},
        },
        'requirements': [['has_material', 'has_recipe']],
        'rewards': {'recipes': [own(name)]},
    })


def shaped(name, pattern, key, unlock):
    """A crafting-table recipe. Every pattern below is a drawing of the thing being built; see docs/arsenal.md."""
    write(f'data/{MOD}/recipe/{name}.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern,
        'key': {symbol: ingredient(ref) for symbol, ref in key.items()},
        'result': {'id': own(name), 'count': 1},
    })
    unlock_advancement(name, unlock)


def machine(name, station, stage, time, energy, ingredients, unlock, fluids=(), count=1):
    """A Singulo machine recipe. The base mod reads these by station id, so they appear in its machines and JEI pages."""
    data = {
        'type': 'singulo:machine', 'station': station, 'stage': stage, 'time': time, 'energy': energy,
        'ingredients': [{'ingredient': ingredient(ref), 'count': n} for ref, n in ingredients],
    }
    if fluids:
        data['fluid_ingredients'] = [{'fluid': fluid, 'amount': amount} for fluid, amount in fluids]
    data['result'] = {'id': own(name), 'count': count}
    write(f'data/{MOD}/recipe/{name}.json', data)
    unlock_advancement(name, unlock)


# ---- Components -------------------------------------------------------------------------------------------------

HELIUM = s('liquid_helium')
machine('flux_weave', 'precision_assembler', 2, 120, 200,
        [(s('superconducting_wire'), 4), (s('white_ceramic_composite'), 2), (s('coolant_cartridge'), 1)],
        s('superconducting_wire'), count=2)
machine('pressure_lattice', 'degenerate_compactor', 4, 400, 50_000,
        [(s('degenerate_matter_shell'), 1), (s('nihonium_plate'), 4), ('#c:plates/steel', 4)],
        s('degenerate_matter_shell'), fluids=[(s('liquid_nitrogen'), 400)], count=4)
# One lamina is one full Jet Forge firing on top of the firing that made its ingot: about 316 GFE and 78 seconds each.
machine('collapsar_lamina', 'accretion_jet_forge', 5, 1200, 250_000_000,
        [(s('collapsar_ingot'), 1), (s('nuclear_pasta'), 1), (s('mass_pellet'), 32)],
        s('collapsar_ingot'), fluids=[(HELIUM, 2000)])
machine('jet_collimator', 'precision_assembler', 5, 400, 200_000,
        [(s('confinement_ring'), 2), (s('optical_lattice_substrate'), 2), (s('jet_condensate'), 4), (s('collapsar_ingot'), 1)],
        s('jet_condensate'))
machine('tidal_edge', 'tidal_spindle', 5, 1200, 20_000_000,
        [(s('collapsar_ingot'), 2), (s('tidal_filament'), 8)], s('tidal_filament'), fluids=[(HELIUM, 500)])

# ---- Suits: every shape puts the working part where it sits on the body ----------------------------------------------

meissner = {'C': s('white_ceramic_composite'), 'W': own('flux_weave'), 'S': s('superconducting_coil'), 'K': s('coolant_cartridge')}
for name, pattern, used in [
    ('meissner_helmet', ['CSC', 'W W'], 'CSW'),       # sensing coil at the crown
    ('meissner_chestplate', ['W W', 'CKC', 'CWC'], 'WCK'),  # cryostat over the heart
    ('meissner_leggings', ['CSC', 'W W', 'C C'], 'CSW'),    # persistent-current loop at the waist
    ('meissner_boots', ['W W', 'S S'], 'WS'),         # levitation coils in the soles
]:
    shaped(name, pattern, {k: meissner[k] for k in used}, own('flux_weave'))

degeneracy = {'G': own('pressure_lattice'), 'D': s('degenerate_matter_shell'), 'N': s('nihonium_plate'), 'Q': s('quantum_computing_module')}
for name, pattern, used in [
    ('degeneracy_helmet', ['GQG', 'G G'], 'GQ'),      # telemetry at the crown
    ('degeneracy_chestplate', ['G G', 'GDG', 'NGN'], 'GDN'),  # shell at the sternum, carrying the load
    ('degeneracy_leggings', ['GNG', 'G G', 'N N'], 'GN'),     # braced nihonium hip and shin
    ('degeneracy_boots', ['N N', 'D D'], 'ND'),       # degenerate shells for soles: what the slam lands on
]:
    shaped(name, pattern, {k: degeneracy[k] for k in used}, own('pressure_lattice'))

collapsar = {'L': own('collapsar_lamina'), 'B': s('micro_black_hole'), 'R': s('confinement_ring'), 'F': s('tidal_filament'),
             'Q': s('quantum_computing_module'), 'O': s('optical_lattice_substrate'), 'G': s('gravity_field_stabilizer'), 'J': s('jet_condensate')}
for name, pattern, used in [
    ('collapsar_helmet', ['LQL', 'FOF'], 'LQFO'),     # optical lattice over the eyes
    ('collapsar_chestplate', ['L L', 'RBR', 'LFL'], 'LRBF'),  # a contained hole between its confinement rings
    ('collapsar_leggings', ['LGL', 'F F', 'L L'], 'LGF'),     # field stabilizer at the waist, filament down the legs
    ('collapsar_boots', ['L L', 'J J'], 'LJ'),        # jet condensate at the soles, where Jet Burst exits
]:
    shaped(name, pattern, {k: collapsar[k] for k in used}, own('collapsar_lamina'))

# ---- Weapons -----------------------------------------------------------------------------------------------------

shaped('quench_saber', [' S ', 'WKW', ' P '],
       {'S': s('superconducting_coil'), 'W': own('flux_weave'), 'K': s('coolant_cartridge'), 'P': s('steel_plate')}, own('flux_weave'))
# Mirrors either side of exotic matter: the gap in the top row is the blade.
shaped('casimir_blade', ['M M', 'MEM', ' F '],
       {'M': s('mirror_plate'), 'E': s('exotic_matter'), 'F': s('nihonium_frame')}, s('mirror_plate'))
shaped('tidal_reaver', [' T ', 'LGL', ' F '],
       {'T': own('tidal_edge'), 'L': own('collapsar_lamina'), 'G': s('gravity_field_stabilizer'), 'F': s('tidal_filament')}, own('tidal_edge'))
# The hole in the middle, a collimator on the firing pole, and a confinement ring sealing the other.
shaped('jet_lance', [' J ', 'LBL', ' R '],
       {'J': own('jet_collimator'), 'L': own('collapsar_lamina'), 'B': s('micro_black_hole'), 'R': s('confinement_ring')}, own('jet_collimator'))

# ---- Module Integrator and modules --------------------------------------------------------------------------------

# Tool arms over a work lens, a control pair either side of the frame, on a plate base: the machine as it stands.
shaped('module_integrator', ['ATA', 'QFQ', 'SSS'],
       {'A': s('precision_actuator'), 'T': s('optical_lattice_substrate'), 'Q': s('quantum_computing_module'), 'F': s('nihonium_frame'), 'S': s('steel_plate')},
       s('nihonium_frame'))
ASSEMBLER = 'precision_assembler'
machine('bit_drone_module', ASSEMBLER, 3, 300, 400, [(s('android_memory_bit'), 1), (s('entangled_element'), 2), (s('superconducting_coil'), 2), (s('nihonium_plate'), 2)], s('android_memory_bit'))
machine('capacitor_module', ASSEMBLER, 3, 200, 400, [(s('superconducting_wire'), 4), (s('ceramic_substrate'), 2), (s('nihonium_plate'), 1)], s('superconducting_wire'))
machine('plating_module', ASSEMBLER, 3, 200, 400, [(s('steel_plate'), 4), (s('white_ceramic_composite'), 2), (s('nihonium_plate'), 2)], s('nihonium_plate'))
machine('thruster_module', ASSEMBLER, 3, 200, 400, [(s('precision_actuator'), 1), (s('superconducting_coil'), 2), (s('muon_bundle'), 2), (s('steel_plate'), 2)], s('muon_bundle'))
machine('coolant_module', ASSEMBLER, 3, 200, 400, [(s('coolant_cartridge'), 2), (s('heat_exchange_core'), 1), (s('superconducting_wire'), 2)], s('coolant_cartridge'))
machine('amplifier_module', ASSEMBLER, 3, 200, 400, [(s('quantum_computing_module'), 1), (s('entangled_element'), 2), (s('optical_lattice_substrate'), 1)], s('quantum_computing_module'))
write(f'data/{MOD}/loot_table/blocks/module_integrator.json', {
    'type': 'minecraft:block',
    'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': own('module_integrator')}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [own('module_integrator')]})
write('data/minecraft/tags/block/needs_iron_tool.json', {'replace': False, 'values': [own('module_integrator')]})
write(f'assets/{MOD}/blockstates/module_integrator.json', {'variants': {'': {'model': f'{MOD}:block/module_integrator'}}})
write(f'assets/{MOD}/models/block/module_integrator.json', {'textures': {'particle': 'minecraft:block/gray_concrete'}})

# ---- Damage types and tags ---------------------------------------------------------------------------------------

for name in DAMAGE:
    write(f'data/{MOD}/damage_type/{name}.json', {'message_id': f'{MOD}.{name}', 'scaling': 'never', 'exhaustion': 0.1, 'effects': 'hurt'})
write('data/minecraft/tags/damage_type/bypasses_cooldown.json', {'replace': False, 'values': [own('jet'), own('shear'), own('bit')]})
for tag, part in [('head_armor', 'helmet'), ('chest_armor', 'chestplate'), ('leg_armor', 'leggings'), ('foot_armor', 'boots')]:
    write(f'data/minecraft/tags/item/{tag}.json', {'replace': False, 'values': [own(f'{t}_{part}') for t in SETS]})
write('data/minecraft/tags/item/swords.json', {'replace': False, 'values': [own(w) for w in WEAPONS[:3]]})
write('data/minecraft/tags/item/enchantable/durability.json', {'replace': False, 'values': [own('jet_lance')]})

# ---- Item models ---------------------------------------------------------------------------------------------------

BUILTIN = {'parent': 'minecraft:builtin/entity', 'textures': {'particle': 'minecraft:block/gray_concrete'}}
SWORD_DISPLAY = {
    'gui': {'rotation': [14, -26, -45], 'translation': [-0.8, -0.8, 0], 'scale': [0.5, 0.5, 0.5]},
    'ground': {'translation': [0, 2, 0], 'scale': [0.38, 0.38, 0.38]},
    'fixed': {'rotation': [0, 0, -45], 'scale': [0.7, 0.7, 0.7]},
    # Vanilla item/handheld, with 45 degrees taken out of the Z rotation: vanilla's blade runs up the sprite's diagonal, this one up +Y.
    'firstperson_righthand': {'rotation': [0, -90, -20], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, 20], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'thirdperson_righthand': {'rotation': [0, -90, 10], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, -10], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
}
# Carried at the ready in both hands: +Y to the muzzle, +Z up. The translation seats the pistol grip in the fist.
LANCE_DISPLAY = {
    'gui': {'rotation': [-74, 0, -90], 'translation': [-1.9, 0.2, 0], 'scale': [0.36, 0.36, 0.36]},
    'ground': {'rotation': [-90, 0, -90], 'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
    'fixed': {'rotation': [-90, 0, -90], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_righthand': {'rotation': [-90, 0, 0], 'translation': [-1.6, -0.6, -2.1], 'scale': [1, 1, 1]},
    'firstperson_lefthand': {'rotation': [-90, 0, 0], 'translation': [-1.6, -0.6, -2.1], 'scale': [1, 1, 1]},
    'thirdperson_righthand': {'rotation': [-90, 0, 0], 'translation': [0, 2.1, 0], 'scale': [0.62, 0.62, 0.62]},
    'thirdperson_lefthand': {'rotation': [-90, 0, 0], 'translation': [0, 2.1, 0], 'scale': [0.62, 0.62, 0.62]},
}
ARMOR_DISPLAY = {
    'gui': {'rotation': [24, 216, 0], 'scale': [0.72, 0.72, 0.72]},
    'ground': {'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'scale': [1, 1, 1]},
    'firstperson_righthand': {'rotation': [0, 150, 0], 'translation': [1.5, 3, 0], 'scale': [0.38, 0.38, 0.38]},
    'firstperson_lefthand': {'rotation': [0, -150, 0], 'translation': [1.5, 3, 0], 'scale': [0.38, 0.38, 0.38]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.34, 0.34, 0.34]},
    'thirdperson_lefthand': {'rotation': [75, 225, 0], 'translation': [0, 2.5, 0], 'scale': [0.34, 0.34, 0.34]},
}
# The reaver is far broader than a vanilla sword: same angle and position, drawn a little smaller so it does not fill the first-person view.
REAVER_DISPLAY = {**SWORD_DISPLAY,
                  'firstperson_righthand': {**SWORD_DISPLAY['firstperson_righthand'], 'scale': [0.54, 0.54, 0.54]},
                  'firstperson_lefthand': {**SWORD_DISPLAY['firstperson_lefthand'], 'scale': [0.54, 0.54, 0.54]}}
DISPLAYS = {'jet_lance': LANCE_DISPLAY, 'tidal_reaver': REAVER_DISPLAY}
for name in WEAPONS:
    write(f'assets/{MOD}/models/item/{name}.json', {**BUILTIN, 'display': DISPLAYS.get(name, SWORD_DISPLAY)})
for t in SETS:
    for part in PARTS:
        write(f'assets/{MOD}/models/item/{t}_{part}.json', {**BUILTIN, 'display': ARMOR_DISPLAY})
write(f'assets/{MOD}/models/item/module_integrator.json', {**BUILTIN, 'display': {
    'gui': {'rotation': [28, 225, 0], 'scale': [0.56, 0.56, 0.56]},
    'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'scale': [0.5, 0.5, 0.5]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
}})
for name in MATERIALS + MODULES:
    write(f'assets/{MOD}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/{name}'}})

# ---- Translations --------------------------------------------------------------------------------------------------

ja, en = {}, {}
names = {
    'meissner': (['マイスナー観測帽', 'マイスナー胸殻', 'マイスナー脚衣', 'マイスナー浮上靴'],
                 ['Meissner Observer', 'Meissner Carapace', 'Meissner Greaves', 'Meissner Levitators']),
    'degeneracy': (['縮退圧兜', '縮退圧胸甲', '縮退圧脚甲', '縮退圧鉄靴'],
                   ['Degeneracy Helm', 'Degeneracy Cuirass', 'Degeneracy Tassets', 'Degeneracy Sabatons']),
    'collapsar': (['コラプサー観測冠', 'コラプサー炉心甲', 'コラプサー潮汐脚甲', 'コラプサー噴流靴'],
                  ['Collapsar Corona', 'Collapsar Corecase', 'Collapsar Tidal Greaves', 'Collapsar Jet Boots']),
}
tips = {
    'meissner': (
        ['SQUID磁力計：敵対的なものの位置を壁越しに表示する。', '飛び道具を磁場排除で弾き返す（1,500 FE／発）。', '永久電流：着用中の各部位の電力を均等化する。歩行速度+7%。', '空中でスニーク：磁束ピン止めでゆっくり降下する（30 FE/t）。'],
        ['SQUID magnetometer: marks hostile creatures through walls.', 'Expels projectiles with the Meissner effect (1,500 FE each).', 'Persistent current: worn pieces share FE evenly. +7% walking speed.', 'Sneak in midair: flux pinning lowers you slowly (30 FE/t).']),
    'degeneracy': (
        ['毒とウィザーを濾過する（2,000 FE）。', 'チャンドラセカール限界：一撃の被ダメージ8を超える分を圧力に変換する。圧力60で崩壊して衝撃波を放ち、30秒間停止する。能力キーで手動で放出できる（圧力15以上）。', '接地中にスニークすると踏ん張る：被ダメージ-30%、ノックバック無効。', '4ブロック以上の落下を衝撃波に変える（500 FE／ブロック）。'],
        ['Filters poison and wither (2,000 FE).', 'Chandrasekhar limit: damage over 8 per hit becomes pressure. At 60 it collapses in a shockwave and locks for 30s. The ability key vents it early (15+ pressure).', 'Sneak on the ground to brace: -30% damage, no knockback.', 'Turns falls of 4+ blocks into a shockwave (500 FE per block).']),
    'collapsar': (
        ['無毛定理：有害な効果を消し去る（10,000 FE）。', '核パスタの裏地：受けた衝撃で ニョッキ（速度+15%）→ スパゲッティ（間合い+1、ノックバック軽減）→ ラザニア（被ダメージ-30%、速度-10%）と相転移する。脚甲が必要。能力キー：ジェット噴射（靴が必要）。', '胴の核パスタ相転移を駆動する。', '落下ダメージ無効。胴とそろえるとジェット噴射が使える。'],
        ['No-hair theorem: erases harmful effects (10,000 FE).', 'Nuclear-pasta lining: damage drives it through gnocchi (+15% speed), spaghetti (+1 reach, knockback resistance), lasagna (-30% damage, -10% speed). Needs the greaves. Ability key: Jet Burst (needs the boots).', 'Drives the corecase\'s pasta phase transitions.', 'No fall damage. With the corecase, enables Jet Burst.']),
}
for t in SETS:
    for n, part in enumerate(PARTS):
        ja[f'item.{MOD}.{t}_{part}'], en[f'item.{MOD}.{t}_{part}'] = names[t][0][n], names[t][1][n]
        ja[f'tooltip.{MOD}.{t}.{part}'], en[f'tooltip.{MOD}.{t}.{part}'] = tips[t][0][n], tips[t][1][n]

weapon_text = {
    'quench_saber': ('クエンチ・セイバー', 'Quench Saber',
                     ['攻撃9／攻撃速度1.6。', '命中で電流が溜まり、5発目で超伝導が破れてクエンチする。', '周囲に10ダメージと炎上（8,000 FE）。'],
                     ['9 damage, 1.6 attack speed.', 'Hits build current; the fifth quenches the superconductor.', 'Dumps 10 damage and fire around the target (8,000 FE).']),
    'casimir_blade': ('カシミール刃', 'Casimir Blade',
                      ['攻撃13／攻撃速度1.6。刃は鏡の間の真空で、防具の効果を35%無視する。', '使用長押し（0.8秒）で鏡を閉じる（40,000 FE）。', '次の一撃は防具を完全に無視し、ダメージ1.5倍。'],
                      ['13 damage, 1.6 attack speed. The blade is the vacuum between its mirrors; ignores 35% of armor.', 'Hold use for 0.8s to close the mirrors (40,000 FE).', 'The next hit ignores armor entirely and deals 1.5x.']),
    'tidal_reaver': ('潮汐断裁刃', 'Tidal Reaver',
                     ['攻撃22／攻撃速度1.0。命中で潮汐応力を付与（最大5、被ダメージ+8%／層）。', '使用長押し：ロッシュ焦点が周囲の敵を引き寄せる（300,000 FE/秒）。', '離して断裁：応力に応じて16〜66ダメージを与え、応力を解放する。'],
                     ['22 damage, 1.0 attack speed. Hits apply tidal stress (up to 5, +8% damage taken each).', 'Hold use: a Roche focus drags nearby enemies in (300,000 FE/s).', 'Release to shear for 16-66 damage by stress, spending it.']),
    'jet_lance': ('降着ジェット砲', 'Accretion Jet Cannon',
                  ['使用長押し：点火→3段の収束→照射（最長6秒）。', '前方80ブロックを貫通し、防具の6割を無視して56 DPS（500,000 FE/秒）。', '磁気ミラーが後極を塞いでいて、ジェットは銃身からのみ出る。'],
                  ['Hold use: ignition, three-ring collimation, then irradiation (up to 6s).', 'Pierces 80 blocks ahead for 56 DPS, ignoring 60% of armor (500,000 FE/s).', 'A magnetic mirror seals the rear pole, so the jet only leaves through the barrel.']),
}
for name, (j, e, tj, te) in weapon_text.items():
    ja[f'item.{MOD}.{name}'], en[f'item.{MOD}.{name}'] = j, e
    for n in range(3):
        ja[f'tooltip.{MOD}.{name}.{n + 1}'], en[f'tooltip.{MOD}.{name}.{n + 1}'] = tj[n], te[n]

for name, j, e in [('flux_weave', '磁束織布', 'Flux Weave'), ('pressure_lattice', '縮退格子板', 'Pressure Lattice'),
                   ('collapsar_lamina', 'コラプサー積層板', 'Collapsar Lamina'), ('jet_collimator', 'ジェット収束器', 'Jet Collimator'),
                   ('tidal_edge', '潮汐鍛造刃', 'Tidal Edge')]:
    ja[f'item.{MOD}.{name}'], en[f'item.{MOD}.{name}'] = j, e

for name, j, e, tj, te in [
    ('bit_drone_module', 'ビットドローン・モジュール', 'Bit Drone Module', '胴用。ビット2機が周囲を巡回し、敵を毎秒自動で撃つ（5ダメージ、1,500 FE／発）。', 'Chestpieces. Two bits orbit you and shoot hostile mobs each second (5 damage, 1,500 FE per shot).'),
    ('capacitor_module', '拡張コンデンサ', 'Capacitor Module', '防具・武器用。電力容量 +50%。', 'Any gear. +50% energy capacity.'),
    ('plating_module', '追加装甲板', 'Plating Module', '防具用。防御力 +2、防具強度 +1。', 'Armor. +2 armor, +1 toughness.'),
    ('thruster_module', '慣性スラスター', 'Inertial Thruster', '靴用。ジャンプ力と段差の乗り越えを強化する。', 'Boots. Higher jumps and step height.'),
    ('coolant_module', '冷却モジュール', 'Coolant Module', '武器用。再使用時間 -25%（重複可）。', 'Weapons. -25% cooldown (stacks).'),
    ('amplifier_module', '出力増幅器', 'Output Amplifier', '武器用。与えるダメージ +15%（重複可）。', 'Weapons. +15% damage dealt (stacks).'),
]:
    ja[f'item.{MOD}.{name}'], en[f'item.{MOD}.{name}'] = j, e
    ja[f'tooltip.{MOD}.{name}'], en[f'tooltip.{MOD}.{name}'] = tj, te

common = {
    'block.singulo_arsenal.module_integrator': ('モジュール統合台', 'Module Integrator'),
    'screen.singulo_arsenal.integrate': ('統合', 'Integrate'),
    'tooltip.singulo_arsenal.modules': ('モジュール：%s / %s', 'Modules: %s / %s'),
    'tooltip.singulo_arsenal.module_hint': ('モジュール統合台で装備に組み込む。', 'Install on gear at a Module Integrator.'),
    'death.attack.singulo_arsenal.bit': ('%1$s は %2$s のビットに撃たれた', '%1$s was shot by %2$s\'s bit drone'),
    'itemGroup.singulo_arsenal': ('Singulo Arsenal', 'Singulo Arsenal'),
    'key.categories.singulo_arsenal': ('Singulo Arsenal', 'Singulo Arsenal'),
    'key.singulo_arsenal.ability': ('装甲の起動機能', 'Armor ability'),
    'tooltip.singulo_arsenal.stage': ('Singulo 進行段階 %s', 'Singulo progression stage %s'),
    'tooltip.singulo_arsenal.energy': ('電力：%s / %s FE', 'Energy: %s / %s FE'),
    'tooltip.singulo_arsenal.charge': ('充電：手に持ち、スニークして電源の出力面に使用。', 'Charge: hold and sneak-use an energy output face.'),
    'tooltip.singulo_arsenal.accretion': ('降着：持ち物の質量ペレットを自動で電力に変える（1個で500,000 FE）。', 'Accretion: burns mass pellets from your inventory for power (500,000 FE each).'),
    'tooltip.singulo_arsenal.full_set.meissner': ('全身：レンツの法則で攻撃者を弾き飛ばす。', 'Full set: Lenz\'s law throws melee attackers back.'),
    'tooltip.singulo_arsenal.full_set.degeneracy': ('各部位の機能は単体で働く。', 'Each piece works on its own.'),
    'tooltip.singulo_arsenal.full_set.collapsar': ('全身：致死の一撃を事象の地平面で止める。電力8,000,000 FE、再使用10分。', 'Full set: an event horizon stops a lethal hit. 8,000,000 FE, 10-minute cooldown.'),
    'message.singulo_arsenal.charged': ('%s FE を充電（残量 %s FE）', 'Charged %s FE (%s FE stored)'),
    'message.singulo_arsenal.no_energy': ('電力が不足しています。', 'Not enough energy.'),
    'message.singulo_arsenal.no_ability': ('この胴装備に起動機能はありません。', 'This chestpiece has no ability.'),
    'message.singulo_arsenal.need_boots': ('ジェット噴射にはコラプサー噴流靴が必要です。', 'Jet Burst needs Collapsar Jet Boots.'),
    'message.singulo_arsenal.vent_unavailable': ('放出には圧力 %s 以上が必要です。', 'Venting needs at least %s pressure.'),
    'message.singulo_arsenal.collapse': ('チャンドラセカール限界を超過：装甲が崩壊した。', 'Chandrasekhar limit exceeded: the plating collapsed.'),
    'hud.singulo_arsenal.meissner': ('マイスナー', 'Meissner'),
    'hud.singulo_arsenal.degeneracy': ('縮退圧', 'Degeneracy'),
    'hud.singulo_arsenal.collapsar': ('コラプサー', 'Collapsar'),
    'hud.singulo_arsenal.pressure': ('圧力 %s / %s', 'Pressure %s / %s'),
    'hud.singulo_arsenal.collapsed': ('崩壊中：あと %s 秒', 'Collapsed: %ss'),
    'hud.singulo_arsenal.phase.0': ('ニョッキ相 %s / %s', 'Gnocchi phase %s / %s'),
    'hud.singulo_arsenal.phase.1': ('スパゲッティ相 %s / %s', 'Spaghetti phase %s / %s'),
    'hud.singulo_arsenal.phase.2': ('ラザニア相 %s / %s', 'Lasagna phase %s / %s'),
    'hud.singulo_arsenal.horizon_cooling': ('事象の地平面：再使用まで %s 秒', 'Horizon: ready in %ss'),
    'death.attack.singulo_arsenal.quench': ('%1$s は %2$s のクエンチに巻き込まれた', '%1$s was caught in %2$s\'s quench'),
    'death.attack.singulo_arsenal.collapse': ('%1$s は %2$s の縮退圧に押しつぶされた', '%1$s was crushed by %2$s\'s degeneracy pressure'),
    'death.attack.singulo_arsenal.jet': ('%1$s は %2$s の相対論的ジェットに焼かれた', '%1$s was burned by %2$s\'s relativistic jet'),
    'death.attack.singulo_arsenal.shear': ('%1$s は %2$s の潮汐力で引き裂かれた', '%1$s was torn apart by %2$s\'s tidal force'),
    'death.attack.singulo_arsenal.horizon': ('%1$s は %2$s の事象の地平面に触れた', '%1$s touched %2$s\'s event horizon'),
}
for key, (j, e) in common.items():
    ja[key], en[key] = j, e
for locale, data in [('ja_jp', ja), ('en_us', en)]:
    write(f'assets/{MOD}/lang/{locale}.json', dict(sorted(data.items())))
write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Singulo Arsenal equipment resources'}})


# ---- Icons ---------------------------------------------------------------------------------------------------------

def png(path, width, height, pixels):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    rows = b''.join(b'\x00' + bytes(v for px in pixels[y * width:(y + 1) * width] for v in px) for y in range(height))
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                       + chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b''))


def rgb(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, 255)


GLOWS = {'flux_weave': 'c', 'pressure_lattice': 'a', 'collapsar_lamina': 'oy', 'jet_collimator': 'cW', 'tidal_edge': 'sy',
         'bit_drone_module': 'cW', 'capacitor_module': 'c', 'thruster_module': 'cW', 'coolant_module': 'c', 'amplifier_module': 'c'}
FRAMES = 8


def scale_rgb(colour, factor):
    return (*[max(0, min(255, int(v * factor))) for v in colour[:3]], 255)


def shade_pixel(art, x, y, grain):
    """Light from the upper left: a bright rim where the top or left is open, a dark rim opposite, softer steps between colours."""
    c = art[y][x]
    def at(px, py):
        return art[py][px] if 0 <= px < 16 and 0 <= py < 16 else '.'
    factor = 1 + grain
    if at(x, y - 1) == '.' or at(x - 1, y) == '.':
        factor *= 1.34
    elif at(x, y + 1) == '.' or at(x + 1, y) == '.':
        factor *= 0.6
    else:
        if at(x, y - 1) != c:
            factor *= 1.14
        if at(x, y + 1) != c:
            factor *= 0.84
    return factor


def sprite(name, art, palette):
    assert len(art) == 16 and all(len(row) == 16 for row in art), name
    # Icons stay inside 14x14 so a one-pixel margin always separates them from the slot border.
    assert set(art[0] + art[15] + ''.join(row[0] + row[15] for row in art)) == {'.'}, f'{name} touches the 16x16 border'
    rng = random.Random(zlib.crc32(name.encode()))
    grain = [[rng.uniform(-0.07, 0.07) for _ in range(16)] for _ in range(16)]
    glow = GLOWS.get(name, '')
    frames = FRAMES if glow else 1
    pixels = []
    for frame in range(frames):
        for y, row in enumerate(art):
            for x, c in enumerate(row):
                if c == '.':
                    pixels.append((0, 0, 0, 0))
                elif c in glow:
                    # Light runs diagonally across the lit parts, and they are not shaded like metal.
                    wave = math.sin((frame / FRAMES + (x + y) / 14) * math.tau)
                    pixels.append(scale_rgb(rgb(palette[c]), 0.92 + 0.16 * wave))
                else:
                    pixels.append(scale_rgb(rgb(palette[c]), shade_pixel(art, x, y, grain[y][x])))
    png(f'assets/{MOD}/textures/item/{name}.png', 16, 16 * frames, pixels)
    if frames > 1:
        (ROOT / f'assets/{MOD}/textures/item/{name}.png.mcmeta').write_text(
            json.dumps({'animation': {'frametime': 3}}, indent=2) + '\n', encoding='utf-8', newline='\n')


# A bolt of the woven conductor, seen from its rolled end, with a loose thread of superconductor.
sprite('flux_weave', [
    '................',
    '................',
    '...sssWWcWWcWW..',
    '..sSSSsWcWWcWWc.',
    '.sSccSSsccccccc.',
    '.sSsSScSsWcWWcW.',
    '.sScSsSSsWcWWcW.',
    '.sSccSScscccccc.',
    '.sSSSSSSsWcWWcW.',
    '..sSSSSsWWcWWcW.',
    '..ssssWWWcWWcW..',
    '...cccccccccc...',
    '.............c..',
    '...........cc...',
    '...........c....',
    '................',
], {'s': 0x8D9AA8, 'S': 0xB9C5D1, 'W': 0xE7ECF1, 'c': 0x7CE8FF})
# A thick forged plate with the lattice cut into its face, its compressed layers showing hot along the edges.
sprite('pressure_lattice', [
    '................',
    '................',
    '.......DD.......',
    '.....DDPPDD.....',
    '...DDPaPPaPDD...',
    '.DDPaPPaaPPaPDD.',
    '.DPPaPPPPPPaPPD.',
    '.DDPPaPaaPaPPDD.',
    '.DLDDPPaPaPPDRD.',
    '.DLLLDDPPDDRRRD.',
    '.DaaLLLDDRRRaaD.',
    '..DDaaLLRRaaDD..',
    '....DDaLRaDD....',
    '......DaaD......',
    '.......DD.......',
    '................',
], {'D': 0x262220, 'P': 0x59524A, 'L': 0x3E3936, 'R': 0x2E2A27, 'a': 0xE08A2E})
# A slab of collapsar metal in an ivory-edged frame; its cut edge shows the nuclear-pasta strata it was forged around.
sprite('collapsar_lamina', [
    '................',
    '................',
    '.......II.......',
    '.....IIVVII.....',
    '...IIVVVVVVII...',
    '.IIVVVEEEEVVVII.',
    '.IVVVEEEEEEVVVI.',
    '.IIIVVEEEEVVIII.',
    '.IDDIIVVVVIIWWI.',
    '.IooDDIIIIWWooI.',
    '.IDDooDIIWooWWI.',
    '.IyyDDoDWoWWyyI.',
    '..IIyyDDWWyyII..',
    '....IIyDWyII....',
    '......IDWI......',
    '................',
], {'I': 0xE7D8B0, 'V': 0x1E1C26, 'E': 0x3A3648, 'D': 0x14121C, 'W': 0x24212E, 'o': 0xFFB061, 'y': 0xFFE2A0})
# Three focusing rings stepping down in a line, as on the Jet Forge, with the jet narrowing through their bore.
sprite('jet_collimator', [
    '................',
    '..II............',
    '.IVVI...II......',
    '.IVVI..IVVI..II.',
    '.IVVIVVIVVIVIVI.',
    '.IVVIEEIVVIEIVI.',
    '.cIccIccIccIcIc.',
    '.WIWWIWWIWWIWIW.',
    '.cIccIccIccIcIc.',
    '.IVVIEEIVVIEIVI.',
    '.IVVIVVIVVIVIVI.',
    '.IVVI..IVVI..II.',
    '.IVVI...II......',
    '..II............',
    '................',
    '................',
], {'I': 0xE7D8B0, 'V': 0x24212E, 'E': 0x0E0D14, 'c': 0x8CD2FF, 'W': 0xF4FBFF})
# The reaver blade blank before it is hilted: broad cleaver head, ivory spine, spun filament binding it, bare tang at the foot.
sprite('tidal_edge', [
    '................',
    '..ISSVVVVVy.....',
    '..ISVVVVVVVy....',
    '..ISVVEEVVVVy...',
    '..IsssssssVVy...',
    '..ISVVVVVVVVy...',
    '..ISVVVVVVVy....',
    '..ISVVVVVVy.....',
    '..ISsssssVy.....',
    '..ISVVVVVy......',
    '..ISVVVVy.......',
    '..ISVVVy........',
    '..ISVVy.........',
    '..IIIII.........',
    '...TT...........',
    '................',
], {'I': 0xE7D8B0, 'S': 0x14121C, 'V': 0x24212E, 'E': 0x3A3648, 's': 0xFFE2A0, 'y': 0xFFD28A, 'T': 0x3A3648})
# A bit drone: a diamond hull, its sensor eye and two stabilisers.
sprite('bit_drone_module', [
    '................',
    '.......II.......',
    '......IVVI......',
    '.....IVVVVI.....',
    '..F.IVVVVVVI.F..',
    '.FFIVVccccVVIFF.',
    '..FIVcWWWWcVIF..',
    '..FIVcWEEWcVIF..',
    '..FIVcWEEWcVIF..',
    '..FIVcWWWWcVIF..',
    '.FFIVVccccVVIFF.',
    '..F.IVVVVVVI.F..',
    '.....IVVVVI.....',
    '......IVVI......',
    '.......II.......',
    '................',
], {'I': 0xE7D8B0, 'V': 0x3A3648, 'F': 0x8D9AA8, 'c': 0x7CE8FF, 'W': 0xF4FBFF, 'E': 0x14121C})
# A cell with its terminal on top and charge bars showing through the casing.
sprite('capacitor_module', [
    '................',
    '......IIII......',
    '......IEEI......',
    '...IIIIIIIIII...',
    '...IVVVVVVVVI...',
    '...IVccccccVI...',
    '...IVVVVVVVVI...',
    '...IVccccccVI...',
    '...IVVVVVVVVI...',
    '...IVccccccVI...',
    '...IVVVVVVVVI...',
    '...IVEEEEEEVI...',
    '...IVVVVVVVVI...',
    '...IIIIIIIIII...',
    '................',
    '................',
], {'I': 0xC2CDD9, 'V': 0x3A4656, 'E': 0x1E2530, 'c': 0x7CE8FF})
# A riveted shield-shaped armor plate with a forged boss at its centre.
sprite('plating_module', [
    '................',
    '...IIIIIIIIII...',
    '..IPPPPPPPPPPI..',
    '.IPRPPPPPPPPRPI.',
    '.IPPPPSSSSPPPPI.',
    '.IPPPSSEESSPPPI.',
    '.IPPPSEEEESPPPI.',
    '.IPPPSEEEESPPPI.',
    '..IPPSSEESSPPI..',
    '..IPPPSSSSPPPI..',
    '...IPPPPPPPPI...',
    '....IPPPPPPI....',
    '.....IPRRPI.....',
    '......IPPI......',
    '.......II.......',
    '................',
], {'I': 0xF2E4BC, 'P': 0x7A7065, 'R': 0xC2AE8C, 'S': 0x9C8F7E, 'E': 0x59524A})
# A bell nozzle with its exhaust leaving through the bottom.
sprite('thruster_module', [
    '................',
    '.....IIIIII.....',
    '.....IVVVVI.....',
    '....IIVVVVII....',
    '....IVVVVVVI....',
    '...IIVVEEVVII...',
    '...IVVVEEVVVI...',
    '..IIVVVVVVVVII..',
    '..IVVVVVVVVVVI..',
    '.IIIIIIIIIIIIII.',
    '....cWWWWWWc....',
    '.....cWWWWc.....',
    '......cWWc......',
    '.......cc.......',
    '.......c........',
    '................',
], {'I': 0xC2CDD9, 'V': 0x3A3648, 'E': 0x14121C, 'c': 0x8CD2FF, 'W': 0xF4FBFF})
# A sealed coolant cartridge with a frost crystal showing in its window.
sprite('coolant_module', [
    '................',
    '.....SSSSSS.....',
    '....IIIIIIII....',
    '...IWWWWWWWWI...',
    '...IWWcWWcWWI...',
    '...IWWWccWWWI...',
    '...IcWcWWcWcI...',
    '...IWccWWccWI...',
    '...IWccWWccWI...',
    '...IcWcWWcWcI...',
    '...IWWWccWWWI...',
    '...IWWcWWcWWI...',
    '...IWWWWWWWWI...',
    '....IIIIIIII....',
    '.....SSSSSS.....',
    '................',
], {'S': 0x8D9AA8, 'I': 0xC2CDD9, 'W': 0x2E9BC4, 'c': 0xCFF4FF})
# A pinned chip with a rising waveform across its die.
sprite('amplifier_module', [
    '................',
    '...P.P.P.P.P....',
    '..IIIIIIIIIIII..',
    '..IVVVVVVVVVVI..',
    '.PIVVVVVVVcVVIP.',
    '..IVVVVVVcVVVI..',
    '.PIVVVVVcVVVVIP.',
    '..IVVcVcVVVVVI..',
    '.PIVcVcVVVVVVIP.',
    '..IcVVVVVVVVVI..',
    '.PIVVVVVVVVVVIP.',
    '..IVVVVVVVVVVI..',
    '..IIIIIIIIIIII..',
    '...P.P.P.P.P....',
    '................',
    '................',
], {'I': 0xC2CDD9, 'V': 0x262430, 'P': 0xE7D8B0, 'c': 0x7CE8FF})
png(f'assets/{MOD}/textures/misc/white.png', 1, 1, [(255, 255, 255, 255)])


# ---- Suit materials: tiling greyscale tiles at one texel per model pixel, tinted by each plate's colour -----------------

def grey(value):
    v = max(0, min(255, int(value)))
    return (v, v, v, 255)


def material(name, seed, shade):
    rng = random.Random(seed)
    png(f'assets/{MOD}/textures/misc/material_{name}.png', MATERIAL_SIZE, MATERIAL_SIZE,
        [grey(shade(x, y, rng)) for y in range(MATERIAL_SIZE) for x in range(MATERIAL_SIZE)])


def ceramic(x, y, rng):
    # Glazed and even, with the occasional fleck.
    return 226 + rng.randint(-10, 10) - (46 if rng.random() < 0.025 else 0)


def hammered(x, y, rng):
    # Forged plate: broad dents from low-frequency waves, grit, and dark pits.
    dents = 16 * math.sin(x * math.tau / 8) * math.cos(y * math.tau / 16) + 12 * math.sin((x + y) * math.tau / 32)
    return 205 + dents + rng.randint(-24, 24) - (70 if rng.random() < 0.05 else 0)


streaks = random.Random(7714)
BRUSH = [streaks.randint(-26, 26) for _ in range(MATERIAL_SIZE)]


def brushed(x, y, rng):
    # Brushed along its length: bands per row, slight grain, a few bright glints off the surface.
    return 200 + BRUSH[y] + rng.randint(-9, 9) + (48 if rng.random() < 0.012 else 0)


material('meissner', 1101, ceramic)
material('degeneracy', 2202, hammered)
material('collapsar', 3303, brushed)

fine_rng = random.Random(9090)
FINE_BANDS = [fine_rng.randint(-16, 16) for _ in range(FINE_SIZE)]
png(f'assets/{MOD}/textures/misc/material_fine.png', FINE_SIZE, FINE_SIZE,
    [grey(226 + FINE_BANDS[x] + fine_rng.randint(-6, 6) - (34 if fine_rng.random() < 0.012 else 0))
     for y in range(FINE_SIZE) for x in range(FINE_SIZE)])
