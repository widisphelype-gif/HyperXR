# HyperXR PCVR — transport foundation

This directory starts the PC-to-Android PCVR transport work. It is deliberately a foundation, not a finished SteamVR driver.

## Current milestone

- Windows host diagnostic service.
- TCP control handshake on port 9966.
- UDP discovery beacon on port 9965 (Wi-Fi).
- USB transport can use the same TCP protocol through Android USB tethering or an ADB forward; no ADB automation is included yet.
- Detects FFmpeg and reports whether its build advertises NVENC encoders.

## Not implemented yet

- Game capture / SteamVR compositor integration.
- NVENC video encoding pipeline.
- Android receiver and stereoscopic renderer.
- Head-pose return channel and controller input.
- USB auto-configuration.

Do not treat this milestone as a working PCVR headset. The existing `hybridmagic/` component is for hand-tracking offload and remains separate.

## Run on Windows 10

1. Install Python 3.11+.
2. Optional: install an FFmpeg build with NVIDIA NVENC support and add `ffmpeg.exe` to PATH.
3. Run `python pcvr/windows/host.py`.
4. Allow private-network access if Windows Firewall asks.

The host prints its local addresses and waits for a compatible Android client. The client is a future milestone.

## Transport choices

- **Wi-Fi:** Android and PC on the same private network; discovery uses UDP broadcast and control uses TCP.
- **USB:** use Android USB tethering, then connect to the PC's tether-network address; alternatively, a future setup helper can establish an ADB port forward. The wire protocol is identical.

## Security

This prototype binds to all interfaces for LAN discovery. Use only on a trusted private network. Authentication and encryption must be added before use on untrusted networks or the public internet.
