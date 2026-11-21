import importlib.util
import unittest
from pathlib import Path
from unittest.mock import patch

MODULE = Path(__file__).with_name("capture_probe.py")
SPEC = importlib.util.spec_from_file_location("hyperxr_capture_probe", MODULE)
PROBE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROBE)


class CaptureProbeTests(unittest.TestCase):
    def test_command_uses_windows_desktop_input_and_one_frame(self):
        command = PROBE.build_command("ffmpeg.exe", "frame.png")
        self.assertEqual(command[0], "ffmpeg.exe")
        self.assertIn("gdigrab", command)
        self.assertIn("desktop", command)
        self.assertIn("-frames:v", command)
        self.assertEqual(command[-1], "frame.png")

    def test_missing_ffmpeg_is_reported(self):
        with patch.object(PROBE.shutil, "which", return_value=None):
            ok, message = PROBE.probe_capture()
        self.assertFalse(ok)
        self.assertIn("FFmpeg was not found", message)


if __name__ == "__main__":
    unittest.main()
