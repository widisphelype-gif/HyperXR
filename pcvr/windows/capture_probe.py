"""One-frame Windows desktop-capture diagnostic for HyperXR.

This validates FFmpeg's gdigrab input independently from the future VR stream.
It does not capture protected content or transmit the desktop.
"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


def build_command(ffmpeg: str, output_path: str) -> list[str]:
    return [
        ffmpeg, "-hide_banner", "-loglevel", "error",
        "-f", "gdigrab", "-draw_mouse", "1", "-framerate", "1",
        "-i", "desktop", "-frames:v", "1", "-y", output_path,
    ]


def probe_capture(ffmpeg: str | None = None) -> tuple[bool, str]:
    executable = ffmpeg or shutil.which("ffmpeg")
    if not executable:
        return False, "FFmpeg was not found. Install FFmpeg and add ffmpeg.exe to PATH."
    if sys.platform != "win32":
        return False, "Desktop capture probe is supported on Windows only."
    try:
        with tempfile.TemporaryDirectory(prefix="hyperxr-capture-") as folder:
            output = str(Path(folder) / "capture-test.png")
            result = subprocess.run(
                build_command(executable, output),
                capture_output=True, text=True, timeout=15, check=False,
            )
            if result.returncode != 0:
                detail = (result.stderr or "FFmpeg could not capture the desktop.").strip()
                return False, detail[-1200:]
            if not Path(output).is_file() or Path(output).stat().st_size == 0:
                return False, "FFmpeg returned success but did not create a frame."
        return True, "Windows desktop capture succeeded (one diagnostic frame)."
    except subprocess.TimeoutExpired:
        return False, "Desktop capture timed out after 15 seconds."
    except OSError as exc:
        return False, str(exc)


def main() -> int:
    ok, message = probe_capture()
    print(f"[capture] {'OK' if ok else 'FAILED'}: {message}")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
