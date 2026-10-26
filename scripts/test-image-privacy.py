#!/usr/bin/env python3
"""Offline privacy regression tests; Python standard library only."""
import base64
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zlib
from jpeg_privacy import (MAX_IMAGE_BYTES, exif_orientation, orientation_tiff,
                          png_chunks, replace_atomically, sanitize_photo, segments)

BASELINE = base64.b64decode('/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAACAAMDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDyGiiivSOA/9k=')
PROGRESSIVE = base64.b64decode('/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wgARCAACAAMDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAb/xAAUAQEAAAAAAAAAAAAAAAAAAAAE/9oADAMBAAIQAxAAAAGQCT//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAn//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/AX//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/AX//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/An//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/IX//2gAMAwEAAgADAAAAEAf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/EH//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/EH//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/EH//2Q==')
PNG = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAMAAAACCAIAAAASFvFNAAAAFElEQVR4nGPk0nBjAAMmCMXAwAAACHAAfEwPWYwAAAAASUVORK5CYII=')


def jpeg_segment(marker, payload):
    return b"\xff" + bytes([marker]) + struct.pack(">H", len(payload) + 2) + payload


def png_chunk(kind, payload):
    return (struct.pack(">I", len(payload)) + kind + payload
            + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF))


class ImagePrivacyTests(unittest.TestCase):
    def test_jpeg_variants_preserve_compressed_pixels_and_orientation(self):
        for fixture in (BASELINE, PROGRESSIVE):
            for orientation in range(1, 9):
                with self.subTest(progressive=fixture is PROGRESSIVE, orientation=orientation):
                    metadata = jpeg_segment(0xE1, b"Exif\0\0" + orientation_tiff(orientation))
                    metadata += jpeg_segment(0xE1, b"http://ns.adobe.com/xap/1.0/\0private-location")
                    metadata += jpeg_segment(0xED, b"Photoshop 3.0\0private-location")
                    metadata += jpeg_segment(0xFE, b"private-location")
                    profile = jpeg_segment(0xE2, b"ICC_PROFILE\0synthetic-profile")
                    original = fixture[:2] + metadata + profile + fixture[2:]
                    clean = sanitize_photo(original + b"private-location")
                    self.assertNotIn(b"private-location", clean)
                    self.assertIn(profile, clean)
                    self.assertEqual(sanitize_photo(clean), clean)
                    self.assertEqual(
                        b"".join(v for k, v in segments(original) if k is None),
                        b"".join(v for k, v in segments(clean) if k is None))
                    found = [exif_orientation(v) for k, v in segments(clean) if k == 0xE1]
                    self.assertEqual(found, [] if orientation == 1 else [orientation])

    def test_multi_picture_index_is_removed(self):
        marker = jpeg_segment(0xE2, b"MPF\0stale-index")
        self.assertNotIn(marker, sanitize_photo(BASELINE[:2] + marker + BASELINE[2:]))

    def test_png_preserves_idat_profile_and_orientation(self):
        header_end = 8 + 25
        for orientation in range(1, 9):
            metadata = png_chunk(b"eXIf", orientation_tiff(orientation))
            for kind in (b"tEXt", b"iTXt", b"zTXt", b"tIME"):
                metadata += png_chunk(kind, b"private-location")
            profile = png_chunk(b"iCCP", b"synthetic-icc\0\0" + zlib.compress(b"profile"))
            original = PNG[:header_end] + metadata + profile + PNG[header_end:]
            clean = sanitize_photo(original + b"private-location")
            self.assertNotIn(b"private-location", clean)
            self.assertIn(profile, clean)
            self.assertEqual(sanitize_photo(clean), clean)
            self.assertEqual(
                b"".join(v for k, v, raw in png_chunks(original) if k == b"IDAT"),
                b"".join(v for k, v, raw in png_chunks(clean) if k == b"IDAT"))
            exif = [v for k, v, raw in png_chunks(clean) if k == b"eXIf"]
            self.assertEqual(exif, [] if orientation == 1 else [orientation_tiff(orientation)])

    def test_corrupt_or_truncated_images_fail(self):
        corrupt_png = PNG[:-8] + b"invalid!"
        for invalid in (b"", b"text", b"\xff\xd8", b"\xff\xd8\xff\xe1\0\1",
                        b"\xff\xd8\xff\xda\0\2\x11", PNG[:-12], corrupt_png):
            with self.subTest(length=len(invalid)), self.assertRaises(ValueError):
                sanitize_photo(invalid)

    def test_oversized_input_fails(self):
        with self.assertRaises(ValueError):
            sanitize_photo(b"\xff\xd8" + b"x" * MAX_IMAGE_BYTES)

    def test_excessive_small_segments_fail(self):
        with self.assertRaises(ValueError):
            sanitize_photo(b"\xff\xd8" + b"\xff\x01" * 4097 + b"\xff\xd9")
        with self.assertRaises(ValueError):
            sanitize_photo(PNG[:33] + png_chunk(b"tEXt", b"x") * 4097 + PNG[33:])

    def test_atomic_replacement_refuses_symlinks(self):
        with tempfile.TemporaryDirectory() as name:
            target = Path(name) / "photo.jpg"
            replace_atomically(target, BASELINE)
            self.assertEqual(target.read_bytes(), BASELINE)
            link = Path(name) / "link.jpg"
            link.symlink_to(target)
            with self.assertRaises(ValueError):
                replace_atomically(link, PROGRESSIVE)
            self.assertEqual(target.read_bytes(), BASELINE)

    def test_downloader_rejects_untrusted_urls(self):
        spec = importlib.util.spec_from_file_location("fetch_wallpapers", Path(__file__).with_name("fetch-wallpapers.py"))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        for url in ("http://upload.wikimedia.org/x.jpg", "https://untrusted.example/x.jpg",
                    "https://user:pass@upload.wikimedia.org/x.jpg",
                    "https://upload.wikimedia.org:444/x.jpg", "https://127.0.0.1/x.jpg"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                module.validate_url(url)
        self.assertEqual(module.validate_url("https://upload.wikimedia.org/x.jpg"),
                         "https://upload.wikimedia.org/x.jpg")


if __name__ == "__main__":
    unittest.main()
