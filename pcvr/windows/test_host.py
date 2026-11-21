import importlib.util
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("host.py")
SPEC = importlib.util.spec_from_file_location("hyperxr_pcvr_host", MODULE)
HOST = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(HOST)


class HostTests(unittest.TestCase):
    def test_capability_shape(self):
        result = HOST.ffmpeg_capabilities()
        self.assertEqual(set(result), {"ffmpeg", "h264_nvenc", "hevc_nvenc"})
        self.assertTrue(all(isinstance(value, bool) for value in result.values()))

    def test_addresses_are_ipv4_strings(self):
        for address in HOST.local_ipv4_addresses():
            self.assertIsInstance(address, str)
            self.assertEqual(len(address.split(".")), 4)

    def test_protocol_constants(self):
        self.assertEqual(HOST.TCP_PORT, 9966)
        self.assertEqual(HOST.UDP_PORT, 9965)
        self.assertEqual(HOST.VERSION, 1)


if __name__ == "__main__":
    unittest.main()
