"""Remove JPEG capture metadata without decoding or recompressing image data."""

import os
from pathlib import Path
import struct
import tempfile
import zlib

MAX_IMAGE_BYTES = 35 * 1024 * 1024


def segments(data):
    """Yield (marker, original bytes); marker None means unchanged scan data."""
    if not data.startswith(b"\xff\xd8"):
        raise ValueError("Not a JPEG: missing SOI marker")
    yield 0xD8, data[:2]
    position = 2
    count = 0
    while position < len(data):
        count += 1
        if count > 4096:
            raise ValueError("Excessive JPEG segment count")
        start = position
        if data[position] != 0xFF:
            raise ValueError("Invalid JPEG marker boundary")
        while position < len(data) and data[position] == 0xFF:
            position += 1
        if position >= len(data):
            raise ValueError("Truncated JPEG marker")
        marker = data[position]
        position += 1
        if marker == 0xD9:
            yield marker, data[start:position]
            # Discard trailing data: it is not part of the JPEG image.
            return
        if marker in (0x00, 0xD8) or 0xD0 <= marker <= 0xD7:
            raise ValueError("Unexpected JPEG marker outside compressed scan")
        if marker == 0x01:
            yield marker, data[start:position]
            continue
        if position + 2 > len(data):
            raise ValueError("Truncated JPEG segment length")
        length = struct.unpack_from(">H", data, position)[0]
        if length < 2 or position + length > len(data):
            raise ValueError("Invalid JPEG segment length")
        position += length
        yield marker, data[start:position]
        if marker == 0xDA:
            scan_start = position
            cursor = position
            while True:
                candidate = data.find(b"\xff", cursor)
                if candidate < 0:
                    raise ValueError("Truncated JPEG compressed scan")
                cursor = candidate + 1
                while cursor < len(data) and data[cursor] == 0xFF:
                    cursor += 1
                if cursor >= len(data):
                    raise ValueError("Truncated JPEG compressed scan marker")
                if data[cursor] == 0 or 0xD0 <= data[cursor] <= 0xD7:
                    cursor += 1
                    continue
                yield None, data[scan_start:candidate]
                position = candidate
                break
    raise ValueError("Not a complete JPEG: missing EOI marker")


def segment_payload(segment):
    # Skip marker fill bytes and its two-byte length.
    marker_end = 0
    while segment[marker_end] == 0xFF:
        marker_end += 1
    return segment[marker_end + 3:]


def exif_orientation(segment):
    payload = segment_payload(segment)
    if not payload.startswith(b"Exif\x00\x00"):
        return None
    return tiff_orientation(payload[6:])


def tiff_orientation(tiff):
    if len(tiff) < 8 or tiff[:2] not in (b"II", b"MM"):
        raise ValueError("Malformed EXIF; orientation cannot be preserved safely")
    endian = "<" if tiff[:2] == b"II" else ">"
    if struct.unpack_from(endian + "H", tiff, 2)[0] != 42:
        raise ValueError("Malformed EXIF TIFF header")
    offset = struct.unpack_from(endian + "I", tiff, 4)[0]
    if offset + 2 > len(tiff):
        raise ValueError("Malformed EXIF directory")
    count = struct.unpack_from(endian + "H", tiff, offset)[0]
    if offset + 2 + count * 12 > len(tiff):
        raise ValueError("Truncated EXIF directory")
    for index in range(count):
        entry = offset + 2 + index * 12
        tag, field_type, field_count = struct.unpack_from(endian + "HHI", tiff, entry)
        if tag == 0x0112:
            if field_type != 3 or field_count != 1:
                raise ValueError("Unsupported EXIF orientation encoding")
            orientation = struct.unpack_from(endian + "H", tiff, entry + 8)[0]
            if orientation not in range(1, 9):
                raise ValueError("Invalid EXIF orientation")
            return orientation
    return None


def orientation_tiff(orientation):
    tiff = b"II" + struct.pack("<HIH", 42, 8, 1)
    return tiff + struct.pack("<HHIHHI", 0x0112, 3, 1, orientation, 0, 0)


def sanitize_jpeg(data):
    """Strip EXIF/XMP/IPTC/comments; retain ICC, Adobe color data and orientation."""
    if len(data) > MAX_IMAGE_BYTES:
        raise ValueError("JPEG exceeds the 35 MiB limit")
    parts = list(segments(data))
    orientations = {value for marker, part in parts if marker == 0xE1
                    for value in [exif_orientation(part)] if value is not None}
    if len(orientations) > 1:
        raise ValueError("Conflicting EXIF orientations")
    orientation = next(iter(orientations), 1)
    clean = bytearray(b"\xff\xd8")
    if orientation != 1:
        # A single TIFF tag: no GPS, thumbnail, camera serial, date, author or location.
        payload = b"Exif\x00\x00" + orientation_tiff(orientation)
        clean += b"\xff\xe1" + struct.pack(">H", len(payload) + 2) + payload
    for marker, part in parts[1:]:
        if marker == 0xE2 and segment_payload(part).startswith(b"MPF\x00"):
            # The output is one photo; a multi-picture index would contain stale offsets.
            continue
        if marker not in (0xE1, 0xED, 0xFE):
            clean += part
    return bytes(clean)


def png_chunks(data):
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("Not a PNG")
    position = 8
    first = True
    has_pixels = False
    count = 0
    while position + 12 <= len(data):
        count += 1
        if count > 4096:
            raise ValueError("Excessive PNG chunk count")
        length = struct.unpack_from(">I", data, position)[0]
        end = position + 12 + length
        if end > len(data):
            raise ValueError("Truncated PNG chunk")
        kind = data[position + 4:position + 8]
        payload = data[position + 8:end - 4]
        checksum = struct.unpack_from(">I", data, end - 4)[0]
        if zlib.crc32(kind + payload) & 0xFFFFFFFF != checksum:
            raise ValueError("Invalid PNG checksum")
        if first and (kind != b"IHDR" or length != 13):
            raise ValueError("Invalid PNG header")
        first = False
        has_pixels |= kind == b"IDAT"
        yield kind, payload, data[position:end]
        position = end
        if kind == b"IEND":
            if length != 0 or not has_pixels:
                raise ValueError("Invalid PNG ending")
            return
    raise ValueError("Incomplete PNG")


def sanitize_png(data):
    if len(data) > MAX_IMAGE_BYTES:
        raise ValueError("PNG exceeds the 35 MiB limit")
    output = bytearray(data[:8])
    for kind, payload, original in png_chunks(data):
        if kind == b"eXIf":
            orientation = tiff_orientation(payload)
            if orientation not in (None, 1):
                minimal = orientation_tiff(orientation)
                output += struct.pack(">I", len(minimal)) + kind + minimal
                output += struct.pack(">I", zlib.crc32(kind + minimal) & 0xFFFFFFFF)
        elif kind not in (b"tEXt", b"iTXt", b"zTXt", b"tIME"):
            output += original
    return bytes(output)


def sanitize_photo(data):
    """Catalog filenames can end in .jpg even when their actual bytes are PNG."""
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return sanitize_png(data)
    return sanitize_jpeg(data)


def replace_atomically(target, data):
    target = Path(target)
    if target.is_symlink():
        raise ValueError("Refusing to replace a symlink")
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(prefix=".photo-", suffix=".tmp",
                                         dir=target.parent, delete=False) as output:
            temporary = Path(output.name)
            output.write(data)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, target)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
