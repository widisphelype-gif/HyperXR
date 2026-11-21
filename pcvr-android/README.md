# HyperXR PCVR Link (Android)

Standalone native Android companion app for the HyperXR PCVR transport prototype. No Unity.

## What it does
- Listens for HyperXR UDP discovery beacons on port 9965.
- Lets the user enter a host IPv4 address manually (useful for USB tethering or networks that block broadcast).
- Connects to TCP port 9966 and sends the version 1 JSON handshake.
- Displays the host response and FFmpeg/NVIDIA NVENC capability flags.

## Build
From the repository root:

```bash
./gradlew :pcvr-android:assembleDebug
```

APK: `pcvr-android/build/outputs/apk/debug/pcvr-android-debug.apk`

## Connect
1. Build/run `pcvr/windows/host.py` on Windows, or use the Windows host executable.
2. Allow Python/HyperXR through Windows Firewall on a private network.
3. Put Android and PC on the same Wi-Fi, tap **Procurar PC na rede**, then **Conectar ao PC**.
4. For USB tethering, enable USB tethering on Android and enter the PC's tethering-side IPv4 address manually if broadcast discovery does not cross that interface.

This milestone confirms network discovery and control handshake only. Video streaming, headset rendering, tracking transport and SteamVR/OpenXR PC runtime integration are not implemented yet.


## VR Box viewer milestone

After a successful host handshake, the app enables **Abrir visualização VR Box**. This opens an immersive landscape viewer with two side-by-side eye panels (SBS layout) and touch-drag manual view adjustment. It is currently a visual shell that displays a waiting-for-video message; it does not yet decode a PC stream. Since some phones lack a gyroscope, this milestone uses touch input rather than head-orientation sensors.

For a usable PCVR experience, the next required milestone is implementing PC-side game capture/encoding and a low-latency stream receiver/decoder in this Android app, followed by stereo frame rendering and proper head tracking.
