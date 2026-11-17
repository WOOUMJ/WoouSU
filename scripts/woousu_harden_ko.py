#!/usr/bin/env python3
"""Post-process a KernelSU/WoouSU LKM so it carries no obvious KSU identity.

What it does (all length-preserving, so ELF offsets stay valid):
  * rewrites the ASCII module name "kernelsu" -> "wlan_gen4" inside the .ko.
    That renames the loaded module (/proc/modules, /sys/module/...) and every
    symbol that contains "kernelsu" (e.g. kernelsu_init -> wlan_gen4_init),
    while relocation indices remain untouched.
  * strips debug/symbol-table extras.
  * reports any remaining tell-tale strings.

Usage:
    python3 scripts/woousu_harden_ko.py kernelsu.ko [-o wlan_gen4.ko]
    python3 scripts/woousu_harden_ko.py kernelsu.ko --name wlan_gen4

The replacement name must be exactly 9 bytes, the length of "kernelsu".
"""
from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
from pathlib import Path

OLD = b"kernelsu"
DEFAULT_NEW = b"wlan_gen4"


def walk_symbols(data: bytes) -> list[str]:
    names = re.findall(rb"[A-Za-z_][A-Za-z0-9_.$]{2,}", data)
    return sorted({n.decode("ascii", "ignore") for n in names
                   if b"kernelsu" in n or b"ksu" in n or b"KernelSU" in n})


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("ko", type=Path)
    ap.add_argument("-o", "--output", type=Path)
    ap.add_argument("--name", default=DEFAULT_NEW.decode(),
                    help="replacement module name (must be exactly 9 ASCII bytes)")
    ap.add_argument("--keep-symbols", action="store_true", help="do not strip the .ko")
    args = ap.parse_args()

    new = args.name.encode("ascii")
    if len(new) != len(OLD):
        raise SystemExit(f"replacement must be {len(OLD)} bytes, got {len(new)}")

    src: Path = args.ko
    dst: Path = args.output or src.with_name(src.name.replace("kernelsu", args.name))
    shutil.copy2(src, dst)
    data = bytearray(dst.read_bytes())

    hits = data.count(OLD)
    data = data.replace(OLD, new)
    dst.write_bytes(data)
    print(f"renamed {hits} occurrence(s) of 'kernelsu' -> '{args.name}' in {dst.name}")

    if not args.keep_symbols:
        strip = shutil.which("llvm-strip") or shutil.which("aarch64-linux-gnu-strip") \
            or shutil.which("strip")
        if strip:
            subprocess.run([strip, "--strip-debug", str(dst)], check=False)
            print(f"stripped debug info with {strip}")
        else:
            print("warning: no strip tool found; leaving symbols intact", file=sys.stderr)

    left = dst.read_bytes()
    if OLD in left:
        print("error: 'kernelsu' still present", file=sys.stderr)
        return 1
    telltale = walk_symbols(left)
    if telltale:
        print("note: residual identifier-like strings (not necessarily loaded):")
        for name in telltale[:20]:
            print(f"  {name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
