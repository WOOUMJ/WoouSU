#!/usr/bin/env python3
"""Sync the KSU version baked into the prebuilt LKM blobs with VERSION.

The kernel module stores KERNEL_SU_VERSION either as an immediate
(`mov wX, #<version>`) or as a 16-byte rodata template. The committed
*.ko files are prebuilt, so bumping VERSION alone leaves the driver
reporting an old version (e.g. 30002) after flashing a patched image.

Run this before rebuilding ksud (the blobs are embedded into libksud.so):

    python3 scripts/patch_lkm_version.py
"""
from __future__ import annotations

import glob
import pathlib
import struct
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT / "VERSION"
LKM_VERSION_FILE = ROOT / "LKM_VERSION"
LKM_GLOB = "userspace/ksud/bin/*/*_kernelsu.ko"

MOVZ_W = 0x52800000
MOVZ_MASK = 0xFFE00000


def read_sections(data: bytes):
    shoff = struct.unpack_from("<Q", data, 0x28)[0]
    shentsize = struct.unpack_from("<H", data, 0x3A)[0]
    shnum = struct.unpack_from("<H", data, 0x3C)[0]
    shstrndx = struct.unpack_from("<H", data, 0x3E)[0]

    def header(index: int):
        off = shoff + index * shentsize
        name, _, _, _, offset, size = struct.unpack_from("<IIQQQQ", data, off)
        return name, offset, size

    _, str_off, str_size = header(shstrndx)
    strtab = data[str_off:str_off + str_size]

    sections = []
    for i in range(shnum):
        name_off, offset, size = header(i)
        end = strtab.find(b"\0", name_off)
        name = strtab[name_off:end].decode()
        sections.append((name, offset, size))
    return sections


def patch_movz(data: bytearray, offset: int, size: int, old: int, new: int) -> int:
    count = 0
    for i in range(offset, offset + size - 4 + 1, 4):
        ins = struct.unpack_from("<I", data, i)[0]
        if (ins & MOVZ_MASK) == MOVZ_W and ((ins >> 5) & 0xFFFF) == (old & 0xFFFF):
            struct.pack_into("<I", data, i, (ins & ~(0xFFFF << 5)) | ((new & 0xFFFF) << 5))
            count += 1
    return count


def patch_rodata(data: bytearray, offset: int, size: int, old: int, new: int) -> int:
    count = 0
    end = offset + size - 16
    for i in range(offset, end):
        if struct.unpack_from("<I", data, i)[0] != old:
            continue
        if data[i + 4:i + 16] == b"\0" * 12:
            struct.pack_into("<I", data, i, new)
            count += 1
    return count


def main() -> int:
    new = int(VERSION_FILE.read_text().strip())
    old = int(LKM_VERSION_FILE.read_text().strip()) if LKM_VERSION_FILE.is_file() else 30002
    if old == new:
        print(f"LKM already at {new}, nothing to do")
        return 0
    if not (0 < new < 0x10000):
        print(f"VERSION {new} does not fit in a 32-bit immediate", file=sys.stderr)
        return 1

    files = sorted(glob.glob(str(ROOT / LKM_GLOB)))
    if not files:
        print("No LKM blobs found", file=sys.stderr)
        return 1

    for path in files:
        data = bytearray(pathlib.Path(path).read_bytes())
        sections = read_sections(data)
        patched = 0
        for name, offset, size in sections:
            if name.startswith(".text"):
                patched += patch_movz(data, offset, size, old, new)
            elif name == ".rodata":
                patched += patch_rodata(data, offset, size, old, new)
        if patched == 0:
            print(f"ERROR: no version site found in {path} (expected {old})", file=sys.stderr)
            return 1
        pathlib.Path(path).write_bytes(data)
        print(f"{pathlib.Path(path).name}: patched {patched} site(s) {old} -> {new}")

    LKM_VERSION_FILE.write_text(f"{new}\n")
    print(f"LKM_VERSION -> {new}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
