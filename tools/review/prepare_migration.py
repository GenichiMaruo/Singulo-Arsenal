#!/usr/bin/env python3
"""Prepare a new disposable migration world; refuses to overwrite any existing run."""
import argparse
import json
from pathlib import Path
import shutil

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source-world', required=True, type=Path, help='old-version world copy containing level.dat')
parser.add_argument('--build-dir', type=Path, default=Path('build'))
args = parser.parse_args()
source = args.source_world.resolve()
root = (args.build_dir / 'gui-migration').resolve()
if root.parent.name != 'build' or not (source / 'level.dat').is_file():
    parser.error('use a build/gui-migration destination and a saved source world')
if root.exists():
    parser.error('destination already exists; archive it manually rather than overwrite evidence')
root.mkdir(parents=True)
shutil.copytree(source, root / 'saves/Review')
(root / 'options.txt').write_text(
    'tutorialStep:none\nlang:en_us\nguiScale:2\nmaxFps:30\nrenderDistance:2\n'
    'simulationDistance:5\npauseOnLostFocus:false\nsoundCategory_music:0.0\n', encoding='utf-8')
(root / 'phase.txt').write_text('seed', encoding='utf-8')
pack = root / 'saves/Review/datapacks/migration-test'
files = {
    'pack.mcmeta': {'pack': {'pack_format': 48, 'description': 'Disposable old-ID migration test'}},
    'data/migration_test/recipe/legacy_cannon.json': {
        'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'singulo_arsenal:jet_lance'}],
        'result': {'id': 'singulo_arsenal:jet_lance', 'count': 1}},
    'data/migration_test/tags/item/legacy_cannons.json': {'values': ['singulo_arsenal:jet_lance']},
}
for name, data in files.items():
    target = pack / name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
print(f'Prepared {root}; run seed on 0.2.2, preserve a backup, then verify and verify-resaved on 0.3.0')
