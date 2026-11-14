"""Prepare a disposable copy of the GameTest world for a real client GUI smoke run."""
import argparse
_parser = argparse.ArgumentParser()
_parser.add_argument('--build-dir', default='build')
_build = _parser.parse_args().build_dir
import pathlib
import shutil

root = pathlib.Path(_build + "/gui-review").resolve()
source = pathlib.Path(_build + "/gametest/world").resolve()
if not (source / "level.dat").is_file():
    raise SystemExit("GameTest world was not saved; cannot start GUI review")
if root.is_relative_to(source):
    raise SystemExit("Refusing a GUI destination inside the source world")
target = root / "saves/Review"
if target.exists():
    raise SystemExit("Archive the previous gui-review directory before preparing a fresh run")
root.mkdir(parents=True, exist_ok=True)
shutil.copytree(source, target)
(root / "options.txt").write_text(
    "tutorialStep:none\nshowSubtitles:true\nlang:ja_jp\nguiScale:2\nmaxFps:30\nrenderDistance:2\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_music:0.0\n",
    encoding="utf-8",
)
