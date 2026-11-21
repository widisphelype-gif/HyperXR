# PCVR transport protocol v1

This is a small JSON control-plane handshake. Video and pose packets are intentionally not specified until codec, timing, and Android decoder constraints are validated.

## Discovery (Wi-Fi)

The PC sends a UDP broadcast every two seconds to port 9965:

```json
{"service":"hyperxr-pcvr","version":1,"tcp_port":9966,"name":"PC-NAME"}
```

## Connect (Wi-Fi or USB)

The Android client opens TCP port 9966 and sends one UTF-8 JSON object:

```json
{"service":"hyperxr-pcvr","version":1,"client":"HyperXR-Android"}
```

The host replies with a JSON object terminated by newline. A successful reply currently reports `state=transport_ready_video_not_implemented`. This is a deliberate explicit status, not a claim that streaming works.

## USB

USB tethering provides an IP route and uses the same TCP handshake. ADB forwarding may be added later, but is not currently automated.

## Planned next protocol revision

- Capability negotiation (H.264/HEVC, resolution, refresh rate).
- Timestamped encoded video frames.
- Timestamped head pose and controller events.
- Session pairing and authenticated encryption.
