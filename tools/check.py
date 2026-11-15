#!/usr/bin/env python3
"""Offline regression suite: balance arithmetic, recipes against the real Singulo item list, resources, translations, determinism."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
SETS = ['meissner', 'degeneracy', 'collapsar']
PARTS = ['helmet', 'chestplate', 'leggings', 'boots']
WEAPONS = ['quench_saber', 'casimir_blade', 'tidal_reaver', 'jet_cannon']
TOOLS = ['squid_pick', 'degeneracy_breaker', 'shear_saw', 'accretion_drill']
MATERIALS = ['flux_weave', 'pressure_lattice', 'collapsar_lamina', 'jet_collimator', 'tidal_edge']
MODULES = ['bit_drone_module', 'capacitor_module', 'plating_module', 'thruster_module', 'coolant_module', 'amplifier_module', 'metric_drive_module', 'meltdown_module', 'overclock_module', 'smelter_module', 'induction_module', 'collector_module']
ARMOR = [f'{s}_{p}' for s in SETS for p in PARTS]
GEAR = ARMOR + WEAPONS + TOOLS + ['module_integrator']
CONTAINERS = ['pellet_canister']
NIHONIUM = [f'nihonium_{p}' for p in ['sword', 'pickaxe', 'axe', 'shovel'] + PARTS]
ITEMS = GEAR + MATERIALS + MODULES + CONTAINERS + NIHONIUM

javac = str(Path(os.environ['JAVA_HOME']) / 'bin/javac') if os.environ.get('JAVA_HOME') else shutil.which('javac')
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')
assert javac and java, 'A JDK 25 is required'
with tempfile.TemporaryDirectory(prefix='arsenal-tests-') as directory:
    subprocess.run([javac, '--release', '25', '-d', directory,
                    str(ROOT / 'src/main/java/io/github/genichimaruo/singuloarsenal/core/Rules.java'),
                    str(ROOT / 'tests/RulesTest.java')], check=True)
    subprocess.run([java, '-cp', directory, 'RulesTest'], check=True)

recipes = RES / 'data/singulo_arsenal/recipe'
recipe_name = lambda item: 'jet_lance' if item == 'jet_cannon' else item
assert {p.stem for p in recipes.glob('*.json')} == {recipe_name(item) for item in ITEMS}, 'missing or extra recipes'
locales = [json.loads((RES / f'assets/singulo_arsenal/lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['ja_jp', 'en_us']]
assert locales[0].keys() == locales[1].keys(), 'locale key mismatch'
# Public display is unchanged; the old registry ID migrates through the NeoForge alias.
assert locales[1]['item.singulo_arsenal.jet_cannon'] == 'Accretion Jet Cannon'
for locale in locales:
    for key, value in locale.items():
        if '.jet_cannon' in key:
            assert locale[key.replace('.jet_cannon', '.jet_lance')] == value, 'legacy text key lost'

for key in locales[0]:
    for token in ('%s', '%1$s', '%2$s'):
        assert locales[0][key].count(token) == locales[1][key].count(token), f'format placeholders differ: {key}'

for item in ITEMS:
    assert f'item.singulo_arsenal.{item}' in locales[0] or f'block.singulo_arsenal.{item}' in locales[0], f'missing name {item}'
    model = json.loads((RES / f'assets/singulo_arsenal/models/item/{item}.json').read_text(encoding='utf-8'))
    # Armor items are flat icons like vanilla armor; worn armor is drawn by SuitLayer, not by these models.
    if item in GEAR and item not in ARMOR:
        assert 'parent' not in model, f'{item} special model must not inherit removed builtin/entity'
        definition = json.loads((RES / f'assets/singulo_arsenal/items/{item}.json').read_text(encoding='utf-8'))
        assert definition['model']['type'] == 'minecraft:select', f'{item} must dispatch special rendering by display context'
        assert all(context in model['display'] for context in ['gui', 'ground', 'fixed', 'firstperson_righthand', 'firstperson_lefthand',
                                                                'thirdperson_righthand', 'thirdperson_lefthand']), item
    else:
        handheld = item in NIHONIUM[:4]
        assert model['parent'] == ('minecraft:item/handheld' if handheld else 'minecraft:item/generated'), item
        assert model['textures']['layer0'] == f'singulo_arsenal:item/{item}'
        data = (RES / f'assets/singulo_arsenal/textures/item/{item}.png').read_bytes()
        width, height = struct.unpack('>II', data[16:24])
        assert data[1:4] == b'PNG' and width == 16 and height % 16 == 0, f'bad icon {item}'
    recipe = json.loads((recipes / f'{recipe_name(item)}.json').read_text(encoding='utf-8'))
    assert recipe['result']['id'] == f'singulo_arsenal:{item}', item
    advancement = json.loads((RES / f'data/singulo_arsenal/advancement/recipes/{recipe_name(item)}.json').read_text(encoding='utf-8'))
    assert advancement['rewards']['recipes'] == [f'singulo_arsenal:{recipe_name(item)}'], item
    if recipe['type'] == 'minecraft:crafting_shaped':
        symbols = set(''.join(recipe['pattern'])) - {' '}
        assert symbols == recipe['key'].keys(), f'bad crafting key: {item}'
        assert len(recipe['pattern']) <= 3 and max(map(len, recipe['pattern'])) <= 3, item
    else:
        assert recipe['type'] == 'singulo:machine', item
        limit = {'accretion_jet_forge': 3, 'degenerate_compactor': 3, 'tidal_spindle': 2, 'precision_assembler': 6}[recipe['station']]
        assert len(recipe['ingredients']) <= limit, f'{item}: too many inputs for {recipe["station"]}'

for module in MODULES:
    assert f'tooltip.singulo_arsenal.{module}.detail' in locales[0], module
# The default tooltip stays one short line; anything longer belongs behind Shift.
for key, text in locales[0].items():
    if key.endswith('.summary') or re.fullmatch(r'tooltip\.singulo_arsenal\.[a-z_]+_module', key):
        assert len(text) <= 20, f'summary too long: {key}'
for suit in SETS:
    for part in PARTS:
        assert f'tooltip.singulo_arsenal.{suit}.{part}' in locales[0], f'{suit}.{part}'
        assert f'tooltip.singulo_arsenal.{suit}.{part}.summary' in locales[0], f'{suit}.{part} summary'
    assert f'tooltip.singulo_arsenal.full_set.{suit}' in locales[0], suit
for weapon in WEAPONS + TOOLS:
    for line in (1, 2, 3, 'summary'):
        assert f'tooltip.singulo_arsenal.{weapon}.{line}' in locales[0], weapon

# Nihonium armor is drawn by the vanilla armor layer from the material's texture pair on the 64x32 player net.
for n in (1, 2):
    data = (RES / f'assets/singulo_arsenal/textures/models/armor/nihonium_layer_{n}.png').read_bytes()
    assert data[1:4] == b'PNG' and struct.unpack('>II', data[16:24]) == (64, 32), f'bad nihonium armor layer {n}'

# Materials tile at one texel per model pixel; Canvas.MATERIAL_SIZE assumes 32.
for suit in SETS:
    data = (RES / f'assets/singulo_arsenal/textures/misc/material_{suit}.png').read_bytes()
    assert data[1:4] == b'PNG' and struct.unpack('>II', data[16:24]) == (32, 32), f'bad material {suit}'

damage_dir = RES / 'data/singulo_arsenal/damage_type'
for path in damage_dir.glob('*.json'):
    message = json.loads(path.read_text(encoding='utf-8'))['message_id']
    assert f'death.attack.{message}' in locales[0], f'missing death message {message}'

# The Jet Forge materials must really be Jet Forge materials: endgame gear cannot slip onto a cheaper path.
jet_forged = {'collapsar_lamina'}
for path in recipes.glob('*.json'):
    recipe = json.loads(path.read_text(encoding='utf-8'))
    text = path.read_text(encoding='utf-8')
    if path.stem.startswith('collapsar_') and path.stem != 'collapsar_lamina':
        assert 'singulo_arsenal:collapsar_lamina' in text, f'{path.stem} skips the Jet Forge'
    if path.stem in jet_forged:
        assert recipe['station'] == 'accretion_jet_forge', path.stem
assert 'singulo_arsenal:collapsar_lamina' in (recipes / 'tidal_reaver.json').read_text(encoding='utf-8')
assert 'singulo_arsenal:collapsar_lamina' in (recipes / 'jet_lance.json').read_text(encoding='utf-8')

# With a Singulo checkout, every ingredient is checked against the real generated recipe/item data.
if os.environ.get('SINGULO_SOURCE'):
    singulo = Path(os.environ['SINGULO_SOURCE']) / 'src/generated/resources'
    known = {p.stem for p in (singulo / 'assets/singulo/items').glob('*.json')}
    assert known, 'Singulo checkout has no 26.x item definitions'
    fluids = {m for p in (singulo / 'data/singulo/recipe').rglob('*.json')
              for m in re.findall(r'"fluid": "singulo:([a-z0-9_]+)"', p.read_text(encoding='utf-8'))}
    used = 0
    for path in recipes.glob('*.json'):
        text = path.read_text(encoding='utf-8')
        recipe = json.loads(text)
        ingredient_data = recipe.get('key', {}) if recipe['type'] == 'minecraft:crafting_shaped' else recipe.get('ingredients', [])
        for ref in re.findall(r'"singulo:([a-z0-9_]+)"', json.dumps(ingredient_data)):
            assert ref in known, f'unknown Singulo item {ref} in {path.name}'
            used += 1
        for ref in re.findall(r'"fluid": "singulo:([a-z0-9_]+)"', text):
            assert ref in fluids, f'unknown Singulo fluid {ref} in {path.name}'
    assert used > 0, 'No Singulo ingredient references were checked'
    print(f'{used} Singulo ingredient references resolved against the checkout')


def snapshot():
    return {str(p.relative_to(RES)): hashlib.sha256(p.read_bytes()).hexdigest() for p in RES.rglob('*') if p.is_file() and 'structure' not in p.parts}


before = snapshot()
subprocess.run([sys.executable, str(ROOT / 'tools/generate_resources.py')], check=True, stdout=subprocess.DEVNULL)
assert snapshot() == before, 'resource generation is not deterministic'
print(f'Resources: {len(ITEMS)} items, recipes, models, 2 locales and regeneration checked')
