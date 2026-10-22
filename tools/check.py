#!/usr/bin/env python3
"""Offline regression suite: actual balance rules, recipes, resources, translations, deterministic generation."""
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
sys.path.insert(0, str(ROOT / 'tools'))
TIERS = ['explorer', 'catalytic', 'inertial', 'boundary']
PARTS = ['helmet', 'chestplate', 'leggings', 'boots']
EXTRAS = ['circulation_cartridge', 'inertial_upgrade', 'boundary_upgrade']

# Compile/run the same rules used by the server; no substitute implementation or external libraries.
javac = str(Path(os.environ['JAVA_HOME']) / 'bin/javac') if os.environ.get('JAVA_HOME') else shutil.which('javac')
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')
assert javac and java, 'A JDK 21 is required'
with tempfile.TemporaryDirectory(prefix='arsenal-tests-') as directory:
    subprocess.run([javac, '--release', '21', '-d', directory,
                    str(ROOT/'src/main/java/io/github/genichimaruo/singuloarsenal/armor/ArmorRules.java'),
                    str(ROOT/'tests/ArmorRulesTest.java')], check=True)
    subprocess.run([java, '-cp', directory, 'ArmorRulesTest'], check=True)

items = {f'{t}_{p}' for t in TIERS for p in PARTS} | set(EXTRAS)
recipes = RES/'data/singulo_arsenal/recipe'
assert {p.stem for p in recipes.glob('*.json')} == items, 'missing/extra recipes'
locales = [json.loads((RES/f'assets/singulo_arsenal/lang/{lang}.json').read_text()) for lang in ['ja_jp', 'en_us']]
assert locales[0].keys() == locales[1].keys(), 'locale key mismatch'
for key in locales[0]:
    assert locales[0][key].count('%s') == locales[1][key].count('%s'), f'format placeholders differ: {key}'
for id in items:
    assert f'item.singulo_arsenal.{id}' in locales[0], f'missing name {id}'
    model = json.loads((RES/f'assets/singulo_arsenal/models/item/{id}.json').read_text())
    assert model['textures']['layer0'] == f'singulo_arsenal:item/{id}'
    data = (RES/f'assets/singulo_arsenal/textures/item/{id}.png').read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (16, 16), f'bad item PNG {id}'
    recipe = json.loads((recipes/f'{id}.json').read_text())
    assert recipe['result']['id'] == f'singulo_arsenal:{id}'
    advancement = json.loads((RES/f'data/singulo_arsenal/advancement/recipes/{id}.json').read_text())
    assert advancement['rewards']['recipes'] == [f'singulo_arsenal:{id}']
    if recipe['type'] == 'minecraft:crafting_shaped':
        symbols = set(''.join(recipe['pattern'])) - {' '}
        assert symbols == recipe['key'].keys(), f'bad crafting keys: {id}'
        assert len(recipe['pattern']) <= 3 and max(map(len, recipe['pattern'])) <= 3
    if recipe['type'] == 'minecraft:smithing_transform':
        assert all(v == 0 for v in recipe['result']['components'].values()), 'upgrades must reset resources'
        tier, part = id.split('_', 1)
        assert recipe['base']['item'] == f"singulo_arsenal:{'catalytic' if tier == 'inertial' else 'inertial'}_{part}"
for tier in TIERS:
    for part in PARTS:
        assert f'tooltip.singulo_arsenal.{tier}.{part}' in locales[0]
    for layer in (1, 2):
        data = (RES/f'assets/singulo_arsenal/textures/models/armor/{tier}_layer_{layer}.png').read_bytes()
        assert struct.unpack('>II', data[16:24]) == (64, 32)

# The prototype is deliberately obtainable only through Creative/commands.
assert not (recipes/'ciel_railgun.json').exists()
gun_model = json.loads((RES/'assets/singulo_arsenal/models/item/ciel_railgun.json').read_text())
assert gun_model['parent'] == 'minecraft:builtin/entity', 'railgun must use its 3D renderer'
assert all(context in gun_model['display'] for context in
           ['gui', 'ground', 'fixed', 'firstperson_righthand', 'firstperson_lefthand',
            'thirdperson_righthand', 'thirdperson_lefthand'])
assert 'item.singulo_arsenal.ciel_railgun' in locales[0]
damage = json.loads((RES/'data/singulo_arsenal/damage_type/railgun.json').read_text())
assert f"death.attack.{damage['message_id']}" in locales[0]
for event, definition in json.loads((RES/'assets/singulo_arsenal/sounds.json').read_text()).items():
    assert definition['subtitle'] in locales[0], f'missing subtitle {event}'
    for sound in definition['sounds']:
        namespace, path = sound.split(':')
        data = (RES/f'assets/{namespace}/sounds/{path}.ogg').read_bytes()
        assert data.startswith(b'OggS') and b'vorbis' in data[:200], f'bad sound: {sound}'

# Optional actual Singulo checkout validation; IDs are checked against source rather than invented stand-ins.
if os.environ.get('SINGULO_SOURCE'):
    sys.path.insert(0, str(Path(os.environ['SINGULO_SOURCE'])/'tools'))
    from ids import MOD
    from ids import SPENT
    from recipes import RECIPES
    valid = {v[0] for v in MOD.values() if v[2] not in ('fluid', 'structure')}
    stages = {v[0]: RECIPES[name]['stage'] for name, v in MOD.items() if name in RECIPES}
    for catalyst, spent in SPENT.items():
        stages[MOD[spent][0]] = RECIPES[catalyst]['stage']
    for path in recipes.glob('*.json'):
        tier = next((t for t in TIERS if path.stem.startswith(t + '_')), 'catalytic')
        expected_stage = {'explorer': 1, 'catalytic': 2, 'inertial': 3, 'boundary': 5}[tier]
        for id in re.findall(r'"(?:item)": "singulo:([a-z0-9_]+)"', path.read_text()):
            assert id in valid, f'unknown Singulo item {id} in {path.name}'
            assert id in stages and stages[id] <= expected_stage, f'out-of-stage material {id} in {path.name}'
    print('All recipe ingredients exist in the supplied Singulo source')

def snapshot():
    return {str(p.relative_to(RES)): hashlib.sha256(p.read_bytes()).hexdigest() for p in RES.rglob('*') if p.is_file()}
before = snapshot()
subprocess.run([sys.executable, str(ROOT/'tools/generate_resources.py')], check=True, stdout=subprocess.DEVNULL)
assert snapshot() == before, 'resource generation is not deterministic'
print(f'Resources: {len(items)} armor recipes, 20 models, 8 armor layers, railgun sounds/damage, 2 locales and regeneration checked')
