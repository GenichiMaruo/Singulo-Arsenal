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
WEAPONS = ['quench_saber', 'casimir_blade', 'tidal_reaver', 'jet_lance']
MATERIALS = ['flux_weave', 'pressure_lattice', 'collapsar_lamina', 'jet_collimator', 'tidal_edge']
GEAR = [f'{s}_{p}' for s in SETS for p in PARTS] + WEAPONS
ITEMS = GEAR + MATERIALS

javac = str(Path(os.environ['JAVA_HOME']) / 'bin/javac') if os.environ.get('JAVA_HOME') else shutil.which('javac')
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')
assert javac and java, 'A JDK 21 is required'
with tempfile.TemporaryDirectory(prefix='arsenal-tests-') as directory:
    subprocess.run([javac, '--release', '21', '-d', directory,
                    str(ROOT / 'src/main/java/io/github/genichimaruo/singuloarsenal/core/Rules.java'),
                    str(ROOT / 'tests/RulesTest.java')], check=True)
    subprocess.run([java, '-cp', directory, 'RulesTest'], check=True)

recipes = RES / 'data/singulo_arsenal/recipe'
assert {p.stem for p in recipes.glob('*.json')} == set(ITEMS), 'missing or extra recipes'
locales = [json.loads((RES / f'assets/singulo_arsenal/lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['ja_jp', 'en_us']]
assert locales[0].keys() == locales[1].keys(), 'locale key mismatch'
for key in locales[0]:
    for token in ('%s', '%1$s', '%2$s'):
        assert locales[0][key].count(token) == locales[1][key].count(token), f'format placeholders differ: {key}'

for item in ITEMS:
    assert f'item.singulo_arsenal.{item}' in locales[0], f'missing name {item}'
    model = json.loads((RES / f'assets/singulo_arsenal/models/item/{item}.json').read_text(encoding='utf-8'))
    if item in GEAR:
        assert model['parent'] == 'minecraft:builtin/entity', f'{item} must use the gear renderer'
        assert all(context in model['display'] for context in ['gui', 'ground', 'fixed', 'firstperson_righthand', 'firstperson_lefthand',
                                                                'thirdperson_righthand', 'thirdperson_lefthand']), item
    else:
        assert model['textures']['layer0'] == f'singulo_arsenal:item/{item}'
        data = (RES / f'assets/singulo_arsenal/textures/item/{item}.png').read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (16, 16), f'bad icon {item}'
    recipe = json.loads((recipes / f'{item}.json').read_text(encoding='utf-8'))
    assert recipe['result']['id'] == f'singulo_arsenal:{item}', item
    advancement = json.loads((RES / f'data/singulo_arsenal/advancement/recipes/{item}.json').read_text(encoding='utf-8'))
    assert advancement['rewards']['recipes'] == [f'singulo_arsenal:{item}'], item
    if recipe['type'] == 'minecraft:crafting_shaped':
        symbols = set(''.join(recipe['pattern'])) - {' '}
        assert symbols == recipe['key'].keys(), f'bad crafting key: {item}'
        assert len(recipe['pattern']) <= 3 and max(map(len, recipe['pattern'])) <= 3, item
    else:
        assert recipe['type'] == 'singulo:machine', item
        limit = {'accretion_jet_forge': 3, 'degenerate_compactor': 3, 'tidal_spindle': 2, 'precision_assembler': 6}[recipe['station']]
        assert len(recipe['ingredients']) <= limit, f'{item}: too many inputs for {recipe["station"]}'

for suit in SETS:
    for part in PARTS:
        assert f'tooltip.singulo_arsenal.{suit}.{part}' in locales[0], f'{suit}.{part}'
    assert f'tooltip.singulo_arsenal.full_set.{suit}' in locales[0], suit
for weapon in WEAPONS:
    for line in (1, 2, 3):
        assert f'tooltip.singulo_arsenal.{weapon}.{line}' in locales[0], weapon

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
    known = {p.stem for p in (singulo / 'assets/singulo/models/item').glob('*.json')}
    fluids = {m for p in (singulo / 'data/singulo/recipe').rglob('*.json')
              for m in re.findall(r'"fluid": "singulo:([a-z0-9_]+)"', p.read_text(encoding='utf-8'))}
    used = 0
    for path in recipes.glob('*.json'):
        text = path.read_text(encoding='utf-8')
        for ref in re.findall(r'"item": "singulo:([a-z0-9_]+)"', text):
            assert ref in known, f'unknown Singulo item {ref} in {path.name}'
            used += 1
        for ref in re.findall(r'"fluid": "singulo:([a-z0-9_]+)"', text):
            assert ref in fluids, f'unknown Singulo fluid {ref} in {path.name}'
    print(f'{used} Singulo ingredient references resolved against the checkout')


def snapshot():
    return {str(p.relative_to(RES)): hashlib.sha256(p.read_bytes()).hexdigest() for p in RES.rglob('*') if p.is_file() and 'structure' not in p.parts}


before = snapshot()
subprocess.run([sys.executable, str(ROOT / 'tools/generate_resources.py')], check=True, stdout=subprocess.DEVNULL)
assert snapshot() == before, 'resource generation is not deterministic'
print(f'Resources: {len(ITEMS)} items, recipes, models, 2 locales and regeneration checked')
