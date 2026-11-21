"""Launch scrcpy for HyperXR's Android-to-PC mirroring bootstrap.

This utility opens the normal scrcpy mirror window. It does not yet ingest
scrcpy frames into HyperXR's stereo renderer or SteamVR runtime.
"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys


def run(command: list[str], *, capture: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command, text=True, capture_output=capture, check=False,
        encoding="utf-8", errors="replace",
    )


def require_tool(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise SystemExit(
            f"Required tool '{name}' was not found in PATH. "
            "Install Android Platform Tools (adb) and scrcpy, then reopen the terminal."
        )
    return path


def list_devices(adb: str) -> list[tuple[str, str]]:
    result = run([adb, "devices", "-l"])
    if result.returncode != 0:
        raise SystemExit(result.stderr.strip() or "adb devices failed.")
    devices: list[tuple[str, str]] = []
    for line in result.stdout.splitlines()[1:]:
        fields = line.split()
        if len(fields) >= 2 and fields[1] == "device":
            devices.append((fields[0], line))
    return devices


def choose_device(devices: list[tuple[str, str]], transport: str, serial: str | None) -> str:
    if serial:
        matches = [item for item in devices if item[0] == serial]
    else:
        matches = [
            item for item in devices
            if (":" in item[0]) == (transport == "wifi")
        ]
    if len(matches) == 1:
        return matches[0][0]
    if not matches:
        raise SystemExit(
            f"No ready {transport.upper()} Android device found. "
            "Check USB debugging or connect the phone with --wifi IP:PORT."
        )
    choices = "\n".join(f"  {serial}: {details}" for serial, details in matches)
    raise SystemExit(
        "More than one matching device is connected. Choose one with --serial.\n"
        + choices
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Start a scrcpy Android mirror for the HyperXR PCVR workflow."
    )
    parser.add_argument("--transport", choices=("usb", "wifi"), default="usb")
    parser.add_argument(
        "--wifi", metavar="IP:PORT",
        help="ADB wireless-debugging endpoint; runs 'adb connect IP:PORT' first.",
    )
    parser.add_argument("--serial", help="Exact serial shown by 'adb devices -l'.")
    parser.add_argument("--max-size", type=int, default=1920)
    parser.add_argument("--bit-rate", default="12M")
    args = parser.parse_args()

    adb = require_tool("adb")
    scrcpy = require_tool("scrcpy")

    if args.wifi and args.transport != "wifi":
        parser.error("--wifi can only be used with --transport wifi")
    if args.wifi:
        connected = run([adb, "connect", args.wifi])
        print(connected.stdout.strip() or connected.stderr.strip())
        if connected.returncode != 0:
            return connected.returncode

    devices = list_devices(adb)
    if not devices:
        raise SystemExit(
            "No authorized Android device found. Unlock the phone and accept "
            "the USB/Wireless debugging authorization prompt."
        )

    serial = choose_device(devices, args.transport, args.serial)
    command = [
        scrcpy, "--serial", serial, "--no-audio",
        "--max-size", str(args.max_size),
        "--video-bit-rate", args.bit_rate,
        "--window-title", "HyperXR - scrcpy source",
        "--stay-awake",
    ]
    print(f"Starting scrcpy for {serial} ({args.transport.upper()})...")
    print("This opens a regular mirror window; SBS/SteamVR frame ingestion is a later milestone.")
    try:
        return subprocess.run(command, check=False).returncode
    except KeyboardInterrupt:
        return 130


if __name__ == "__main__":
    sys.exit(main())
