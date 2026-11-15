#!/usr/bin/env python3
"""Click-to-paint editor for the item sprites drawn in tools/generate_resources.py. Python 3 stdlib only.

    python tools/icon_editor.py [--port 8765] [--no-browser]

Every sprite('name', [16 rows], palette, ...) call in the generator is listed. Saving writes that sprite's rows back into the
generator, in place, and reruns it, so the preview is the real texture with its outline, shading and glow. Only the
art changes: palettes and options stay in the generator. Serves 127.0.0.1 only.
"""
import argparse
import ast
import json
import subprocess
import sys
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GENERATOR = ROOT / 'tools/generate_resources.py'
TEXTURES = ROOT / 'src/main/resources/assets/singulo_arsenal/textures/item'
LOCK = threading.Lock()


def read_sprites():
    """(sprites, source) where each sprite has its name, rows, palette, glow letters, options and the source span of its rows."""
    source = GENERATOR.read_text(encoding='utf-8')
    tree = ast.parse(source)
    named, glows = {}, {}
    for node in tree.body:
        if isinstance(node, ast.Assign) and len(node.targets) == 1 and isinstance(node.targets[0], ast.Name):
            try:
                value = ast.literal_eval(node.value)
            except ValueError:
                continue
            named[node.targets[0].id] = value
            if node.targets[0].id == 'GLOWS':
                glows.update(value)
        elif isinstance(node, ast.Expr) and isinstance(node.value, ast.Call):
            f = node.value.func
            if isinstance(f, ast.Attribute) and isinstance(f.value, ast.Name) and f.value.id == 'GLOWS' and f.attr == 'update':
                glows.update(ast.literal_eval(node.value.args[0]))
    sprites = []
    for node in tree.body:
        if not (isinstance(node, ast.Expr) and isinstance(node.value, ast.Call) and isinstance(node.value.func, ast.Name)
                and node.value.func.id == 'sprite'):
            continue
        call = node.value
        name = ast.literal_eval(call.args[0])
        rows_node = call.args[1]
        pal_node = call.args[2]
        palette = named[pal_node.id] if isinstance(pal_node, ast.Name) else ast.literal_eval(pal_node)
        options = {k.arg: ast.literal_eval(k.value) for k in call.keywords}
        sprites.append({
            'name': name,
            'rows': ast.literal_eval(rows_node),
            'palette': {k: f'#{v:06x}' for k, v in palette.items()},
            'paletteName': pal_node.id if isinstance(pal_node, ast.Name) else None,
            'glow': glows.get(name, ''),
            'options': options,
            'span': [rows_node.lineno, rows_node.col_offset, rows_node.end_lineno, rows_node.end_col_offset],
        })
    return sprites, source


def write_rows(name, rows):
    sprites, source = read_sprites()
    sprite = next((s for s in sprites if s['name'] == name), None)
    if sprite is None:
        raise ValueError(f'no sprite {name}')
    if len(rows) != 16 or any(len(r) != 16 for r in rows):
        raise ValueError('art must be 16 rows of 16')
    allowed = set(sprite['palette']) | {'.'}
    bad = {c for r in rows for c in r} - allowed
    if bad:
        raise ValueError(f'letters not in the palette: {"".join(sorted(bad))}')
    lines = source.split('\n')
    l0, c0, l1, c1 = sprite['span']
    # Offsets are in UTF-8 bytes; the rows' own lines are ASCII, and the text before '[' is "sprite('name', ".
    head = lines[l0 - 1].encode('utf-8')[:c0].decode('utf-8')
    tail = lines[l1 - 1].encode('utf-8')[c1:].decode('utf-8')
    block = '[\n' + ''.join(f"    '{r}',\n" for r in rows) + ']'
    new = '\n'.join(lines[:l0 - 1]) + '\n' + head + block + tail + '\n' + '\n'.join(lines[l1:])
    GENERATOR.write_text(new, encoding='utf-8', newline='\n')


def regenerate():
    result = subprocess.run([sys.executable, str(GENERATOR)], capture_output=True, text=True, cwd=ROOT)
    return result.returncode == 0, (result.stdout + result.stderr)[-4000:]


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def send(self, code, body, kind):
        data = body if isinstance(body, bytes) else body.encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', kind)
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = self.path.split('?')[0]
        if path == '/':
            self.send(200, PAGE, 'text/html; charset=utf-8')
        elif path == '/api/sprites':
            with LOCK:
                sprites, _ = read_sprites()
            self.send(200, json.dumps(sprites), 'application/json')
        elif path.startswith('/texture/') and path.endswith('.png'):
            name = path[len('/texture/'):-4]
            target = (TEXTURES / f'{name}.png').resolve()
            if target.parent != TEXTURES.resolve() or not target.is_file():
                self.send(404, 'not found', 'text/plain')
            else:
                self.send(200, target.read_bytes(), 'image/png')
        else:
            self.send(404, 'not found', 'text/plain')

    def do_POST(self):
        if self.path != '/api/save':
            self.send(404, 'not found', 'text/plain')
            return
        body = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        with LOCK:
            backup = GENERATOR.read_text(encoding='utf-8')
            try:
                write_rows(body['name'], body['rows'])
            except ValueError as e:
                self.send(400, json.dumps({'ok': False, 'log': str(e)}), 'application/json')
                return
            ok, log = regenerate()
            if not ok:
                # A sprite the generator rejects (for example one touching the border) is not kept.
                GENERATOR.write_text(backup, encoding='utf-8', newline='\n')
                regenerate()
        self.send(200, json.dumps({'ok': ok, 'log': log}), 'application/json')


PAGE = r'''<!doctype html>
<html lang="ja">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Icon Editor</title>
<style>
:root { --bg:#f4f5f7; --panel:#fff; --ink:#1d2330; --muted:#6b7385; --line:#d9dde4; --accent:#2f7de1; --bad:#c43c3c; --ok:#2b8a4b; }
@media (prefers-color-scheme: dark) { :root { --bg:#16191f; --panel:#1f232b; --ink:#e6e9ef; --muted:#9aa3b5; --line:#343a46; --accent:#5aa0ff; --bad:#ff7070; --ok:#5ccf86; } }
* { box-sizing:border-box; }
body { margin:0; background:var(--bg); color:var(--ink); font:14px/1.45 system-ui, "Segoe UI", "Yu Gothic UI", sans-serif; }
main { display:grid; grid-template-columns:220px auto 1fr; gap:16px; padding:16px; height:100vh; }
section { background:var(--panel); border:1px solid var(--line); border-radius:8px; padding:12px; overflow:auto; }
h2 { font-size:13px; font-weight:600; margin:0 0 8px; color:var(--muted); }
#list button { display:flex; align-items:center; gap:8px; width:100%; text-align:left; background:none; border:0; border-radius:6px; padding:4px 6px; color:inherit; font:inherit; cursor:pointer; }
#list button:hover { background:color-mix(in srgb, var(--accent) 10%, transparent); }
#list button.on { background:color-mix(in srgb, var(--accent) 20%, transparent); }
#list canvas { width:32px; height:32px; image-rendering:pixelated; background:#8b8b8b; border-radius:3px; }
#filter { width:100%; padding:6px 8px; margin-bottom:8px; border:1px solid var(--line); border-radius:6px; background:var(--bg); color:inherit; }
#grid { display:block; image-rendering:pixelated; cursor:crosshair; touch-action:none; }
.row { display:flex; gap:8px; flex-wrap:wrap; align-items:center; margin-top:10px; }
.btn { padding:6px 12px; border:1px solid var(--line); border-radius:6px; background:var(--bg); color:inherit; font:inherit; cursor:pointer; }
.btn.on, .btn.primary { background:var(--accent); border-color:var(--accent); color:#fff; }
.btn:disabled { opacity:.5; cursor:default; }
#palette { display:grid; grid-template-columns:repeat(auto-fill, minmax(64px, 1fr)); gap:6px; }
.sw { display:flex; align-items:center; gap:6px; padding:4px; border:2px solid transparent; border-radius:6px; cursor:pointer; background:var(--bg); }
.sw.on { border-color:var(--accent); }
.sw i { width:22px; height:22px; border-radius:4px; border:1px solid var(--line); flex:none; }
.sw b { font-family:ui-monospace, Consolas, monospace; }
.sw small { color:var(--muted); font-size:11px; }
.previews { display:flex; gap:16px; align-items:flex-end; flex-wrap:wrap; }
.slot { background:#8b8b8b; border:2px solid; border-color:#373737 #fff #fff #373737; display:inline-block; line-height:0; }
.slot canvas { image-rendering:pixelated; }
.caption { font-size:12px; color:var(--muted); margin-top:4px; }
#status { margin-top:10px; font-size:13px; white-space:pre-wrap; }
#status.bad { color:var(--bad); } #status.ok { color:var(--ok); }
kbd { font-family:ui-monospace, Consolas, monospace; border:1px solid var(--line); border-bottom-width:2px; border-radius:4px; padding:0 4px; font-size:12px; }
.help { color:var(--muted); font-size:12px; margin-top:12px; line-height:1.8; }
@media (max-width: 900px) { main { grid-template-columns:1fr; height:auto; } }
</style>
</head>
<body>
<main>
  <section>
    <h2>スプライト</h2>
    <input id="filter" placeholder="絞り込み（例: nihonium）">
    <div id="list"></div>
  </section>
  <section>
    <h2 id="title">—</h2>
    <canvas id="grid" width="416" height="416"></canvas>
    <div class="row">
      <button class="btn on" data-tool="pen">ペン</button>
      <button class="btn" data-tool="fill">塗りつぶし</button>
      <button class="btn" data-tool="pick">スポイト</button>
      <label><input type="checkbox" id="mirror"> 左右対称</label>
    </div>
    <div class="row">
      <button class="btn" id="undo">元に戻す</button>
      <button class="btn" id="redo">やり直す</button>
      <button class="btn" id="revert">保存前に戻す</button>
      <button class="btn primary" id="save">保存して生成</button>
    </div>
    <div id="status"></div>
    <div class="help">
      左ドラッグ: 塗る / 右ドラッグ: 消す / <kbd>Alt</kbd>+クリック: スポイト<br>
      <kbd>Ctrl</kbd>+<kbd>Z</kbd> 元に戻す / <kbd>Ctrl</kbd>+<kbd>Y</kbd> やり直す / <kbd>Ctrl</kbd>+<kbd>S</kbd> 保存<br>
      青い枠の内側が塗れる範囲です（枠のないもの＝剣は 16×16 全体）。縁取りと陰影は生成時に付きます。
    </div>
  </section>
  <section>
    <h2>色</h2>
    <div id="palette"></div>
    <h2 style="margin-top:20px">生成結果（保存後に更新）</h2>
    <div class="previews">
      <div><div class="slot"><canvas id="big" width="256" height="256"></canvas></div><div class="caption">16倍</div></div>
      <div><div class="slot"><canvas id="p3" width="48" height="48"></canvas></div><div class="caption">GUI 3倍</div></div>
      <div><div class="slot"><canvas id="p2" width="32" height="32"></canvas></div><div class="caption">GUI 2倍</div></div>
      <div><div class="slot"><canvas id="p1" width="16" height="16"></canvas></div><div class="caption">等倍</div></div>
    </div>
    <div class="caption" id="opts" style="margin-top:12px"></div>
  </section>
</main>
<script>
const CELL = 26, N = 16;
let sprites = [], cur = null, art = null, saved = null, sel = '.', tool = 'pen';
let undo = [], redo = [], painting = null;
const $ = id => document.getElementById(id);
const grid = $('grid'), g = grid.getContext('2d');

async function load(keep) {
  sprites = await (await fetch('/api/sprites')).json();
  renderList();
  const name = keep || (cur && cur.name) || sprites[0].name;
  open(name, !!keep);
}

function renderList() {
  const q = $('filter').value.trim();
  const list = $('list'); list.innerHTML = '';
  for (const s of sprites) {
    if (q && !s.name.includes(q)) continue;
    const b = document.createElement('button');
    b.className = cur && cur.name === s.name ? 'on' : '';
    const c = document.createElement('canvas'); c.width = 16; c.height = 16;
    texture(s.name, c);
    b.append(c, document.createTextNode(s.name));
    b.onclick = () => {
      if (dirty() && !confirm('保存していない変更を破棄しますか？')) return;
      open(s.name);
    };
    list.append(b);
  }
}

function open(name, keepEdits) {
  cur = sprites.find(s => s.name === name);
  saved = cur.rows.map(r => r.split(''));
  if (!keepEdits || !art) art = saved.map(r => r.slice());
  undo = []; redo = [];
  sel = Object.keys(cur.palette)[0];
  $('title').textContent = cur.name;
  const o = Object.entries(cur.options).map(([k, v]) => k + '=' + v).join(', ');
  $('opts').textContent = 'パレット: ' + (cur.paletteName || '個別') + (cur.glow ? ' / 発光: ' + cur.glow : '') + (o ? ' / ' + o : '');
  renderPalette(); draw(); previews(); renderList(); status('');
}

function renderPalette() {
  const p = $('palette'); p.innerHTML = '';
  const entries = [['.', null], ...Object.entries(cur.palette)];
  for (const [k, color] of entries) {
    const d = document.createElement('div');
    d.className = 'sw' + (k === sel ? ' on' : '');
    const i = document.createElement('i');
    i.style.background = color || 'repeating-conic-gradient(#bbb 0 25%, #fff 0 50%) 0 0/8px 8px';
    const label = document.createElement('div');
    label.innerHTML = '<b>' + (k === '.' ? '消' : k) + '</b>' + (cur.glow.includes(k) ? '<br><small>発光</small>' : '');
    d.append(i, label);
    d.onclick = () => { sel = k; renderPalette(); };
    p.append(d);
  }
}

function draw() {
  for (let y = 0; y < N; y++) for (let x = 0; x < N; x++) {
    const c = art[y][x];
    if (c === '.') { g.fillStyle = (x + y) % 2 ? '#cfcfcf' : '#e6e6e6'; }
    else g.fillStyle = cur.palette[c];
    g.fillRect(x * CELL, y * CELL, CELL, CELL);
    if (c !== '.' && cur.glow.includes(c)) { g.fillStyle = 'rgba(255,255,255,.55)'; g.fillRect(x * CELL + CELL / 2 - 2, y * CELL + CELL / 2 - 2, 4, 4); }
  }
  g.strokeStyle = 'rgba(0,0,0,.18)'; g.lineWidth = 1;
  for (let i = 0; i <= N; i++) {
    g.beginPath(); g.moveTo(i * CELL + .5, 0); g.lineTo(i * CELL + .5, N * CELL); g.stroke();
    g.beginPath(); g.moveTo(0, i * CELL + .5); g.lineTo(N * CELL, i * CELL + .5); g.stroke();
  }
  // The generator keeps sprites off the 16x16 border; outlined ones also leave room for their outline.
  // Full-size art (the sword) may use all 16x16, so it gets no frame.
  const m = cur.options.full ? 0 : cur.options.outlined ? 2 : 1;
  if (m) {
    g.strokeStyle = 'rgba(47,125,225,.9)'; g.lineWidth = 2;
    g.strokeRect(m * CELL, m * CELL, (N - 2 * m) * CELL, (N - 2 * m) * CELL);
  }
  $('save').textContent = dirty() ? '保存して生成 *' : '保存して生成';
}

const dirty = () => art && saved && art.some((r, y) => r.some((c, x) => c !== saved[y][x]));
const snapshot = () => art.map(r => r.slice());
function cellAt(e) {
  const r = grid.getBoundingClientRect();
  const x = Math.floor((e.clientX - r.left) / r.width * N), y = Math.floor((e.clientY - r.top) / r.height * N);
  return x >= 0 && y >= 0 && x < N && y < N ? [x, y] : null;
}
function set(x, y, c) {
  art[y][x] = c;
  if ($('mirror').checked) art[y][N - 1 - x] = c;
}
function fill(x, y, c) {
  const from = art[y][x]; if (from === c) return;
  const stack = [[x, y]];
  while (stack.length) {
    const [px, py] = stack.pop();
    if (px < 0 || py < 0 || px >= N || py >= N || art[py][px] !== from) continue;
    set(px, py, c);
    stack.push([px + 1, py], [px - 1, py], [px, py + 1], [px, py - 1]);
  }
}
grid.addEventListener('contextmenu', e => e.preventDefault());
grid.addEventListener('pointerdown', e => {
  const p = cellAt(e); if (!p) return;
  const [x, y] = p;
  if (e.altKey || tool === 'pick') { sel = art[y][x]; renderPalette(); return; }
  undo.push(snapshot()); redo = [];
  const c = e.button === 2 ? '.' : sel;
  if (tool === 'fill') { fill(x, y, c); draw(); return; }
  painting = c; grid.setPointerCapture(e.pointerId); set(x, y, c); draw();
});
grid.addEventListener('pointermove', e => {
  if (painting === null) return;
  const p = cellAt(e); if (!p) return;
  set(p[0], p[1], painting); draw();
});
grid.addEventListener('pointerup', () => { painting = null; });

function doUndo() { if (!undo.length) return; redo.push(snapshot()); art = undo.pop(); draw(); }
function doRedo() { if (!redo.length) return; undo.push(snapshot()); art = redo.pop(); draw(); }
$('undo').onclick = doUndo; $('redo').onclick = doRedo;
$('revert').onclick = () => { undo.push(snapshot()); art = saved.map(r => r.slice()); draw(); };
document.querySelectorAll('[data-tool]').forEach(b => b.onclick = () => {
  tool = b.dataset.tool;
  document.querySelectorAll('[data-tool]').forEach(o => o.classList.toggle('on', o === b));
});
$('filter').oninput = renderList;
document.addEventListener('keydown', e => {
  if (!(e.ctrlKey || e.metaKey)) return;
  const k = e.key.toLowerCase();
  if (k === 'z') { e.preventDefault(); e.shiftKey ? doRedo() : doUndo(); }
  else if (k === 'y') { e.preventDefault(); doRedo(); }
  else if (k === 's') { e.preventDefault(); save(); }
});

async function save() {
  status('生成中…');
  $('save').disabled = true;
  try {
    const r = await (await fetch('/api/save', { method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: cur.name, rows: art.map(r => r.join('')) }) })).json();
    if (r.ok) { status('保存して生成しました。', 'ok'); await load(cur.name); }
    else status('生成に失敗したので保存を取り消しました:\n' + r.log, 'bad');
  } catch (err) { status('サーバーに接続できません: ' + err, 'bad'); }
  $('save').disabled = false;
  draw();
}
$('save').onclick = save;
function status(text, kind) { const s = $('status'); s.textContent = text; s.className = kind || ''; }

function texture(name, canvas, scale) {
  const img = new Image();
  img.onload = () => {
    const c = canvas.getContext('2d'); c.imageSmoothingEnabled = false;
    c.clearRect(0, 0, canvas.width, canvas.height);
    c.drawImage(img, 0, 0, 16, 16, 0, 0, canvas.width, canvas.height);  // first frame of animated strips
  };
  img.src = '/texture/' + name + '.png?t=' + Date.now();
}
function previews() { for (const id of ['big', 'p3', 'p2', 'p1']) texture(cur.name, $(id)); }
window.addEventListener('beforeunload', e => { if (dirty()) { e.preventDefault(); e.returnValue = ''; } });
load();
</script>
</body>
</html>
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--no-browser', action='store_true')
    args = parser.parse_args()
    server = ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    url = f'http://127.0.0.1:{args.port}/'
    print(f'Icon editor: {url}  (Ctrl+C to stop)', flush=True)
    if not args.no_browser:
        webbrowser.open(url)
    server.serve_forever()


if __name__ == '__main__':
    main()
