# HyperXR PCVR Link (Android)

Native Android companion app for the HyperXR PCVR transport prototype. No Unity.

## Current Android functions
- UDP discovery on port 9965 and TCP control handshake on port 9966.
- Manual PC IP entry for Wi-Fi or USB tethering.
- Experimental rear-camera visual-flow motion estimate in the VR Box viewer.
- Manual touch-drag fallback.

## Build
```bash
./gradlew :pcvr-android:assembleDebug
```
APK: `pcvr-android/build/outputs/apk/debug/pcvr-android-debug.apk`

## Camera tracking limitations
The viewer requests CAMERA permission and analyzes low-resolution luminance frames locally. Frames are not saved or transmitted. The rear camera must see the room through the VR Box opening. Tracking pauses when the viewer leaves the foreground.

This is an experimental relative image-motion estimate, not a calibrated gyroscope replacement, not positional/6DoF tracking, and not a complete VR head-tracking solution. It can drift or fail with dark, blank, low-texture, or moving scenes. Touch adjustment remains available.

## Still not implemented
PC video capture/encoding, network video receiver/decoder, stereo game rendering, tracking transport to the PC, and SteamVR/OpenXR runtime integration.
