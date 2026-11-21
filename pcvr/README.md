# HyperXR PCVR — transport foundation

This directory starts the PC-to-Android PCVR transport work. It is deliberately a foundation, not a finished SteamVR driver.

## Current milestone

- Windows host diagnostic service.
- TCP control handshake on port 9966.
- UDP discovery beacon on port 9965 (Wi-Fi).
- USB transport can use the same TCP protocol through Android USB tethering; ADB forwarding is not automated.
- Detects FFmpeg and reports whether its build advertises NVENC encoders.
- Windows desktop-capture probe using FFmpeg's `gdigrab` input. It captures one temporary diagnostic frame and deletes it.

## Run on Windows 10

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
