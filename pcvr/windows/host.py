"""HyperXR PCVR host transport prototype (no video streaming yet)."""
from __future__ import annotations

import json
import socket
import subprocess
import threading
import time
from pathlib import Path

TCP_PORT = 9966
UDP_PORT = 9965
VERSION = 1
ROOT = Path(__file__).resolve().parents[1]


def ffmpeg_capabilities() -> dict:
    """Report FFmpeg/NVENC availability without assuming a GPU model."""
    try:
        result = subprocess.run(
            ["ffmpeg", "-hide_banner", "-encoders"],
            capture_output=True, text=True, timeout=8, check=False,
        )
        output = result.stdout + result.stderr
        return {
            "ffmpeg": True,
            "h264_nvenc": "h264_nvenc" in output,
            "hevc_nvenc": "hevc_nvenc" in output,
        }
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return {"ffmpeg": False, "h264_nvenc": False, "hevc_nvenc": False}


def local_ipv4_addresses() -> list[str]:
    addresses = set()
    try:
        for item in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            address = item[4][0]
            if not address.startswith("127."):
                addresses.add(address)
    except OSError:
        pass
    return sorted(addresses)


def discovery_loop(stop: threading.Event) -> None:
    beacon = {
        "service": "hyperxr-pcvr",
        "version": VERSION,
        "tcp_port": TCP_PORT,
        "name": socket.gethostname(),
    }
    payload = json.dumps(beacon).encode("utf-8")
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    sock.settimeout(1.0)
    try:
        while not stop.is_set():
            try:
                sock.sendto(payload, ("255.255.255.255", UDP_PORT))
            except OSError as exc:
                print(f"[discovery] {exc}")
            stop.wait(2.0)
    finally:
        sock.close()


def handle_client(conn: socket.socket, address: tuple) -> None:
    with conn:
        conn.settimeout(5.0)
        try:
            raw = conn.recv(4096)
            request = json.loads(raw.decode("utf-8"))
            if request.get("service") != "hyperxr-pcvr" or request.get("version") != VERSION:
                response = {"ok": False, "error": "incompatible_protocol"}
            else:
                response = {
                    "ok": True,
                    "service": "hyperxr-pcvr",
                    "version": VERSION,
                    "state": "transport_ready_video_not_implemented",
                    "host": socket.gethostname(),
                    "video": {"available": False, "reason": "streaming_pipeline_pending"},
                    "encoding": ffmpeg_capabilities(),
                    "usb_supported_via_tethering": True,
                }
            conn.sendall(json.dumps(response).encode("utf-8") + b"\n")
            print(f"[client] {address[0]} -> {response.get('state', response.get('error'))}")
        except (OSError, ValueError, json.JSONDecodeError) as exc:
            print(f"[client] {address}: {exc}")


def main() -> None:
    print("HyperXR PCVR host — transport prototype")
    print(f"IPv4: {', '.join(local_ipv4_addresses()) or 'not detected'}")
    print(f"TCP control: {TCP_PORT} | UDP discovery: {UDP_PORT}")
    print(f"Encoding: {json.dumps(ffmpeg_capabilities())}")
    print("Video streaming and SteamVR integration are not implemented in this milestone.")

    stop = threading.Event()
    discovery = threading.Thread(target=discovery_loop, args=(stop,), daemon=True)
    discovery.start()
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        server.bind(("0.0.0.0", TCP_PORT))
        server.listen(8)
        server.settimeout(1.0)
        while True:
            try:
                conn, address = server.accept()
            except socket.timeout:
                continue
            threading.Thread(target=handle_client, args=(conn, address), daemon=True).start()
    except KeyboardInterrupt:
        print("\nStopping host...")
    finally:
        stop.set()
        server.close()
        discovery.join(timeout=2.0)


if __name__ == "__main__":
    main()
