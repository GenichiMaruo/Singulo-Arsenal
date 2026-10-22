#!/usr/bin/env python3
"""Original railgun assets. JSON/PNG regeneration needs only Python; --sounds also needs ffmpeg."""
import json
import math
from pathlib import Path
import random
import struct
import subprocess
import sys
import tempfile
import wave
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
ASSETS = ROOT / 'assets/singulo_arsenal'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def generate():
    write(ASSETS / 'models/item/ciel_railgun.json', {
        'parent': 'minecraft:builtin/entity',
        'textures': {'particle': 'minecraft:block/white_concrete'},
        'display': {
            'gui': {'rotation': [20, -35, -15], 'translation': [-2, 0, 0], 'scale': [.65, .65, .65]},
            'firstperson_righthand': {'rotation': [0, 90, -4], 'translation': [0, 1, 1], 'scale': [1, 1, 1]},
            'firstperson_lefthand': {'rotation': [0, -90, 4], 'translation': [0, 1, 1], 'scale': [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 1], 'scale': [.9, .9, .9]},
            'thirdperson_lefthand': {'rotation': [0, -90, 0], 'translation': [0, 2, 1], 'scale': [.9, .9, .9]},
            'ground': {'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [-2, 0, 0], 'scale': [.7, .7, .7]}
        }
    })
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    path = ASSETS / 'textures/misc/white.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b'\x00\xff\xff\xff\xff')) + chunk(b'IEND', b''))
    write(ROOT / 'data/singulo_arsenal/damage_type/railgun.json', {
        'message_id': 'singulo_arsenal.railgun', 'scaling': 'never', 'exhaustion': .1, 'effects': 'hurt'
    })
    translations = {
        'item.singulo_arsenal.ciel_railgun': ['シエル式試作レールガン', 'Ciel Prototype Railgun'],
        'railgun.singulo_arsenal.prototype': ['クリエイティブ専用 · 電磁加速兵装', 'Creative prototype · Electromagnetic accelerator'],
        'railgun.singulo_arsenal.controls': ['メインハンドで右クリック長押し → 充電完了後に離して発射', 'Main hand: hold use to charge, release when ready to fire'],
        'railgun.singulo_arsenal.stats': ['充電 1.6秒 / 冷却 1.2秒 / 射程 96m / 最大4体を貫通', 'Charge 1.6s / Cooldown 1.2s / Range 96m / Pierces up to 4 targets'],
        'railgun.singulo_arsenal.creative_only': ['この試作兵装はクリエイティブモード専用です', 'This prototype requires Creative mode'],
        'railgun.singulo_arsenal.charging': ['磁場収束中', 'FIELD CHARGING'],
        'railgun.singulo_arsenal.locked': ['充電完了 — 離して発射', 'READY — RELEASE TO FIRE'],
        'railgun.singulo_arsenal.cooling': ['コイル冷却中', 'COILS COOLING'],
        'subtitles.singulo_arsenal.railgun_charge': ['レールガンが充電される', 'Railgun charging'],
        'subtitles.singulo_arsenal.railgun_ready': ['レールガンの充電が完了する', 'Railgun ready'],
        'subtitles.singulo_arsenal.railgun_fire': ['レールガンが轟く', 'Railgun fires'],
        'subtitles.singulo_arsenal.railgun_vent': ['レールガンが放電する', 'Railgun discharges'],
        'death.attack.singulo_arsenal.railgun': ['%1$sは%2$sの電磁加速弾に撃ち抜かれた', '%1$s was pierced by %2$s’s railgun'],
        'death.attack.singulo_arsenal.railgun.player': ['%1$sは%2$sの電磁加速弾に撃ち抜かれた', '%1$s was pierced by %2$s’s railgun'],
    }
    for index, locale in enumerate(['ja_jp', 'en_us']):
        path = ASSETS / f'lang/{locale}.json'
        data = json.loads(path.read_text())
        data.update({key: values[index] for key, values in translations.items()})
        write(path, data)
    write(ASSETS / 'sounds.json', {
        f'railgun_{name}': {'subtitle': f'subtitles.singulo_arsenal.railgun_{name}', 'sounds': [f'singulo_arsenal:railgun/{name}']}
        for name in ['charge', 'ready', 'fire', 'vent']
    })


def sounds():
    rate = 44100
    rng = random.Random(10421)
    out = ASSETS / 'sounds/railgun'
    out.mkdir(parents=True, exist_ok=True)
    for name, duration in [('charge', 1), ('ready', .24), ('fire', 1.2), ('vent', .35)]:
        data = []
        for i in range(int(rate * duration)):
            t = i / rate
            noise = rng.uniform(-1, 1)
            if name == 'charge':
                # Integer frequencies and periodic modulation give a seamless mono loop.
                value = (.24*math.sin(2*math.pi*80*t) + .14*math.sin(2*math.pi*160*t) + .07*math.sin(2*math.pi*320*t)) * (.8+.2*math.cos(2*math.pi*4*t))
            elif name == 'fire':
                attack = min(1, t/.0015)
                value = attack*(.53*noise*math.exp(-t*24) + .62*math.sin(2*math.pi*(70*t+2*(1-math.exp(-t*20))))*math.exp(-t*7) + .22*math.sin(2*math.pi*(1300*t-300*t*t))*math.exp(-t*14))
            elif name == 'ready':
                value = .3*(math.sin(2*math.pi*880*t)+.5*math.sin(2*math.pi*1320*t))*math.sin(math.pi*t/duration)**2
            else:
                value = .28*noise*min(1,t/.01)*math.exp(-t*13) + .12*math.sin(2*math.pi*(400*t-300*t*t))*math.sin(math.pi*t/duration)**2
            data.append(struct.pack('<h', int(max(-.96, min(.96, value))*32767)))
        with tempfile.TemporaryDirectory() as temp:
            wav = Path(temp) / f'{name}.wav'
            with wave.open(str(wav), 'wb') as stream:
                stream.setparams((1, 2, rate, 0, 'NONE', 'not compressed'))
                stream.writeframes(b''.join(data))
            subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(wav), '-c:a', 'libvorbis', '-q:a', '5', str(out/f'{name}.ogg')], check=True)


if __name__ == '__main__':
    generate()
    if '--sounds' in sys.argv:
        sounds()
