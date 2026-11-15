"""The optional GUI preparation must never overwrite an earlier test world."""
import pathlib
import subprocess
import sys
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "tools/review/prepare_gui.py"

class ReviewPreparationTests(unittest.TestCase):
    def run_prepare(self, root):
        return subprocess.run([sys.executable, str(SCRIPT)], cwd=root, capture_output=True, text=True)

    def source(self, root):
        world = pathlib.Path(root) / "build/gametest/world"
        world.mkdir(parents=True)
        (world / "level.dat").write_bytes(b"synthetic saved-world fixture")
        return world

    def test_copy_is_disposable_and_only_music_is_muted(self):
        with tempfile.TemporaryDirectory() as root:
            source = self.source(root)
            result = self.run_prepare(root)
            self.assertEqual(result.returncode, 0, result.stderr)
            game = pathlib.Path(root) / "build/gui-review"
            self.assertEqual((game / "saves/Review/level.dat").read_bytes(), (source / "level.dat").read_bytes())
            sound = [x for x in (game / "options.txt").read_text().splitlines() if x.startswith("soundCategory_")]
            self.assertEqual(sound, ["soundCategory_music:0.0"])

    def test_existing_world_and_evidence_are_preserved(self):
        with tempfile.TemporaryDirectory() as root:
            self.source(root)
            self.assertEqual(self.run_prepare(root).returncode, 0)
            target = pathlib.Path(root) / "build/gui-review/saves/Review/level.dat"
            target.write_bytes(b"previous run evidence")
            result = self.run_prepare(root)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("Archive the previous", result.stderr)
            self.assertEqual(target.read_bytes(), b"previous run evidence")

    def test_missing_source_fails_without_creating_destination(self):
        with tempfile.TemporaryDirectory() as root:
            self.assertNotEqual(self.run_prepare(root).returncode, 0)
            self.assertFalse((pathlib.Path(root) / "build/gui-review").exists())

if __name__ == "__main__":
    unittest.main()
