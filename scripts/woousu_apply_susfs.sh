#!/usr/bin/env bash
# Apply the public SUSFS kernel patch + KernelSU integration patch to a kernel
# tree that already contains this WoouSU/KernelSU source.
#
#   ./scripts/woousu_apply_susfs.sh <kernel-source-root> [susfs-ref]
#
# <kernel-source-root> must contain:
#   KernelSU/          (this repository, usually a symlink or copy)
#   fs/ include/ ...   (the actual kernel source)
#
# susfs-ref is a branch of https://gitlab.com/simonpunk/susfs4ksu matching the
# kernel, e.g. gki-android13-5.15, gki-android14-6.1, gki-android15-6.6.
#
# The SUSFS patches are written against specific kernel/KernelSU revisions. If
# a hunk fails, rebase it by hand; do not force-apply blindly.
set -euo pipefail

KROOT="${1:?usage: $0 <kernel-source-root> [susfs-ref]}"
REF="${2:-gki-android15-6.6}"
WORK="${SUSFS_WORKDIR:-/tmp/susfs4ksu}"

if [[ ! -d "$KROOT/KernelSU/kernel" ]]; then
    echo "error: $KROOT/KernelSU does not look like the WoouSU/KernelSU tree" >&2
    exit 1
fi

if [[ ! -d "$WORK/.git" ]]; then
    git clone --depth 1 --branch "$REF" https://gitlab.com/simonpunk/susfs4ksu.git "$WORK"
else
    git -C "$WORK" fetch --depth 1 origin "$REF"
    git -C "$WORK" checkout --detach FETCH_HEAD
fi

PATCHES="$WORK/kernel_patches"
[[ -f "$PATCHES/50_add_susfs_in_${REF}.patch" ]] || {
    echo "error: no patch for ref '$REF' (looked for 50_add_susfs_in_${REF}.patch)" >&2
    exit 1
}

apply() {
    local patch="$1" dir="$2"
    echo "==> $(basename "$patch") -> $dir"
    if ! patch -d "$dir" -p1 --forward --dry-run < "$patch"; then
        echo "   patch does not apply cleanly; resolving by hand is required" >&2
        return 1
    fi
    patch -d "$dir" -p1 --forward < "$patch"
}

apply "$PATCHES/50_add_susfs_in_${REF}.patch" "$KROOT"

# The KernelSU-side integration patch is relative to the KernelSU checkout.
if [[ -f "$PATCHES/KernelSU/10_enable_susfs_for_ksu.patch" ]]; then
    if ! patch -d "$KROOT/KernelSU" -p1 --forward --dry-run \
            < "$PATCHES/KernelSU/10_enable_susfs_for_ksu.patch"; then
        echo
        echo "The KernelSU integration patch did not apply to this revision."
        echo "Newer KernelSU changed the hook layout; port these pieces manually:"
        echo "  - kernel/Kbuild: CONFIG_KSU_SUSFS + susfs objects/include paths"
        echo "  - kernel/Kconfig: SUSFS feature switches"
        echo "  - kernel/core/init.c: susfs_init() call and ksu hooks"
        echo "  - fs/susfs.c, include/linux/susfs*.h from $PATCHES"
        echo "Then enable your features from userspace with the ksu_susfs tool."
        exit 2
    fi
    patch -d "$KROOT/KernelSU" -p1 --forward \
        < "$PATCHES/KernelSU/10_enable_susfs_for_ksu.patch"
fi

cat <<EOF

SUSFS sources applied. Next:
  1. wire SUSFS into the kernel build and enable the CONFIG_KSU_SUSFS_* options
  2. build + flash the kernel (or the LKM, renamed with scripts/woousu_harden_ko.py)
  3. install the ksu_susfs userspace module from $WORK/ksu_module_susfs
  4. enable runtime hiding (command names differ slightly per branch; check
     \`ksu_susfs --help\`), typically:
       ksu_susfs enable_susfs
       ksu_susfs hide_sus_mnts_for_all_procs
       ksu_susfs spoof_uname <kernel-release> <kernel-version>
       ksu_susfs add_sus_path /data/adb/ksu
       ksu_susfs add_sus_path /data/adb/modules
EOF
