# HyperXR PCVR — transport foundation

This directory starts the PC-to-Android PCVR transport work. It is deliberately a foundation, not a finished SteamVR driver.

## Current milestone

- Windows host diagnostic service.
- TCP control handshake on port 9966.
- UDP discovery beacon on port 9965 (Wi-Fi).
- USB transport can use the same TCP protocol through Android USB tethering; ADB forwarding is not automated.
- Detects FFmpeg and reports whether its build advertises NVENC encoders.
- Windows desktop-capture probe using FFmpeg's `gdigrab` input. It captures one temporary diagnostic frame and deletes it.
- GitHub Actions workflow builds standalone Windows executables with PyInstaller.

## Download Windows executables

1. Open the repository's **Actions** tab.
2. Select **Build HyperXR Windows EXE**.
3. Open a successful run from the `feature/pcvr-wifi-usb-foundation` branch.
4. Download the `HyperXR-Windows-EXE` artifact and extract the ZIP.

The ZIP contains:
- `HyperXR-PCVR-Host.exe` — starts the PC discovery and control service.
- `HyperXR-Capture-Test.exe` — checks whether FFmpeg can capture one Windows desktop frame.

The artifact is a temporary CI build, retained for 14 days. It is not yet a distributable PCVR application.

## Run from source on Windows 10

1. Install Python 3.11+.
2. Install FFmpeg and add `ffmpeg.exe` to PATH.
3. Test desktop capture with `python pcvr/windows/capture_probe.py`.
4. Start the control/discovery host with `python pcvr/windows/host.py`.
5. Allow private-network access if Windows Firewall asks.

The capture probe only validates that FFmpeg can read the Windows desktop. It does not stream video, capture the SteamVR compositor, or create a VR view.

## Not implemented yet

- Low-latency game/SteamVR compositor capture.
- NVENC video streaming pipeline and Android decoder.
- Android receiver and stereoscopic renderer.
- Head-pose return channel and controller input.
- USB auto-configuration.
- SteamVR driver/OpenXR bridge.

Do not treat this milestone as a working PCVR headset. The existing `hybridmagic/` component is for hand-tracking offload and remains separate.

## Transport choices

- **Wi-Fi:** Android and PC on the same private network; discovery uses UDP broadcast and control uses TCP.
- **USB:** use Android USB tethering, then connect to the PC's tether-network address. The wire protocol is intended to remain identical.

## Security

This prototype binds to all interfaces for LAN discovery. Use only on a trusted private network. Authentication and encryption must be added before use on untrusted networks or the public internet.


## Android companion app

A standalone native Android connection app (no Unity) is available in `pcvr-android/`.

Build it with:

```bash
./gradlew :pcvr-android:assembleDebug
```

The **Build HyperXR Android PCVR Link APK** GitHub Actions workflow publishes an artifact named `HyperXR-PCVR-Android-APK`. The app discovers UDP beacons on port 9965 and performs the TCP JSON handshake on port 9966. It also supports entering the PC's IPv4 address manually, which is useful with USB tethering.

This is a connection-test app only: it does not yet receive or render a VR video stream.
