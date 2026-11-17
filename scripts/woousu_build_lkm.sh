#!/usr/bin/env bash
# Build a WoouSU LKM (wlan_gen4.ko) against any kernel tree, then de-brand it.
#
# usage: woousu_build_lkm.sh <kernel_tree> [defconfig]
#
# The kernel tree must be configured with:
#   CONFIG_KSU=m  CONFIG_KSU_STEALTH=y  CONFIG_MODULES=y
# The resulting module only loads on a kernel whose vermagic and symbol CRCs
# match this exact tree + config, so build it from the device's own kernel
# source (or an official GKI tree for the device's KMI).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TREE=${1:?kernel tree}
DEFCONFIG=${2:-}
SU=${SU_SRC_DIR:-$ROOT}
OUT=${OUT:-$PWD/out}
JOBS=$(nproc)

cd "$TREE"
if [ -n "$DEFCONFIG" ]; then
  make ARCH=arm64 LLVM=1 LLVM_IAS=1 CROSS_COMPILE=aarch64-linux-gnu- "$DEFCONFIG"
fi
scripts/config --module KSU --enable KSU_STEALTH --disable KSU_DEBUG
make ARCH=arm64 LLVM=1 LLVM_IAS=1 CROSS_COMPILE=aarch64-linux-gnu- olddefconfig >/dev/null

if [ ! -d drivers/kernelsu ]; then
  cp -a "$SU/kernel" drivers/kernelsu
  cp -a "$SU/uapi" drivers/uapi
  printf '\nsource "drivers/kernelsu/Kconfig"\n' >> drivers/Kconfig
  printf 'obj-$(CONFIG_KSU)\t\t+= kernelsu/\n' >> drivers/Makefile
fi

make ARCH=arm64 LLVM=1 LLVM_IAS=1 CROSS_COMPILE=aarch64-linux-gnu- \
     KCFLAGS=-Wno-error -j"$JOBS" prepare modules_prepare
make ARCH=arm64 LLVM=1 LLVM_IAS=1 CROSS_COMPILE=aarch64-linux-gnu- \
     KCFLAGS=-Wno-error -j"$JOBS" M=drivers/kernelsu modules

mkdir -p "$OUT"
cp drivers/kernelsu/kernelsu.ko "$OUT/"
python3 "$SU/scripts/woousu_harden_ko.py" "$OUT/kernelsu.ko" -o "$OUT/wlan_gen4.ko"
echo "built: $OUT/wlan_gen4.ko"
