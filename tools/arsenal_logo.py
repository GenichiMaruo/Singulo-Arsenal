"""Regenerate the adopted icon, or a preserved K/L/M concept.

Run python tools/arsenal_logo.py from any directory. Requires NumPy and Pillow.
The physical renderer and stylization function reproduce the saved variants.
"""
import argparse
import json
from pathlib import Path
import subprocess
import sys

from PIL import Image
from stylize_physical_icons import stylize

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--variant', choices=['K', 'L', 'M'], help='Save a concept under docs/branding/variants')
    args = parser.parse_args()
    config = json.loads((ROOT / 'docs/branding/logo-settings.json').read_text(encoding='utf-8'))
    variant = args.variant or config['selected']
    settings = config['variants'][variant]
    render = ROOT / 'build/branding' / (settings['concept'] + '.png')
    command = [sys.executable, str(ROOT / 'tools/render_physical_icon.py'), str(render)]
    for flag, key in [('elevation', 'elevation_degrees'), ('fov', 'fov_degrees'),
                      ('roll', 'roll_degrees'), ('outer-radius', 'disk_outer_radius'),
                      ('resolution', 'resolution')]:
        command.extend([f'--{flag}', str(settings[key])])
    if settings['analytic_jets']:
        command.append('--jets')
    subprocess.run(command, check=True)
    image = stylize(render)
    if args.variant:
        destinations = [(ROOT / 'docs/branding/variants' / f'{variant}-{size}.png', size)
                        for size in (512, 400, 64)]
    else:
        destinations = [
            (ROOT / 'docs/curseforge/logo.png', 512),
            (ROOT / 'docs/curseforge/logo_400.png', 400),
            (ROOT / 'src/main/resources/singulo_arsenal_logo.png', 512),
        ]
    for path, size in destinations:
        path.parent.mkdir(parents=True, exist_ok=True)
        image.resize((size, size), Image.Resampling.LANCZOS).save(path)
        print(path)


if __name__ == '__main__':
    main()
