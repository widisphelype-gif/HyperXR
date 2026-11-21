# HyperXR scrcpy bridge (initial milestone)

This helper starts the official scrcpy mirror for an Android phone connected to
the Windows PC. It supports selecting a USB ADB device or an already-authorized
ADB-over-Wi-Fi device.

## Requirements on the Windows PC

1. Install Android SDK Platform Tools so `adb.exe` is available in PATH.
2. Install scrcpy and make `scrcpy.exe` available in PATH.
3. Enable Developer options and USB debugging on Android, then authorize this PC.

## USB

Connect the phone with a data-capable USB cable, accept the debugging prompt,
then run from the repository root:

```powershell
py pcvr/windows/scrcpy_bridge.py --transport usb
```

If more than one USB Android device is connected, select one with
`--serial DEVICE_SERIAL`.

## Wi-Fi

Connect the phone and PC to the same trusted local network. On Android, open
Developer options > Wireless debugging and use its **IP address & port**.
Then run:

```powershell
py pcvr/windows/scrcpy_bridge.py --transport wifi --wifi 192.168.1.25:PORT
```

Replace the example address and PORT with the endpoint shown by Android.
For a device already connected with ADB over Wi-Fi, omit `--wifi`.

## Quality options

```powershell
py pcvr/windows/scrcpy_bridge.py --transport usb --max-size 1920 --bit-rate 12M
```

Use `--serial` to select a specific device. The script launches scrcpy with
audio disabled, keeps the phone awake, and opens a normal desktop mirror window.

## Current scope and limitations

**Implemented:** ADB device discovery, explicit USB/Wi-Fi selection, optional
ADB wireless connect, and scrcpy launch with configurable resolution/bitrate.

**Not implemented yet:** capturing scrcpy's decoded frames inside HyperXR,
stereo SBS composition, lens correction, low-latency frame transport into the
VR renderer, head-pose transport, and SteamVR/OpenXR runtime integration.
The scrcpy window is currently a source/mirroring bootstrap, not a finished VR
display. This does not replace the experimental camera-based motion estimate
in the Android app.
