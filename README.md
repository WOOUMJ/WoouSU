# WoouSU

[![简体中文](https://img.shields.io/badge/%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-README__CN-blue?style=flat-square)](README_CN.md)

![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)
![Android](https://img.shields.io/badge/Android-12%2B-green.svg)
![Kernel](https://img.shields.io/badge/Kernel-5.10%2B-orange.svg)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-lightgrey.svg)

A **KernelSU-based, kernel-level root solution for Android**, with optional SUSFS support and a rebranded manager.

---

## Features

- **Kernel-level `su`** with allowlist-based access control (only granted apps can see `su`).
- **Module system** (overlayfs-based), including metamodules and module WebUI.
- **App Profile** and **kernel umount** for per-app hiding and mount control.
- **Two deployment modes**
  - *LKM patching*: patch the stock `boot` / `init_boot` image on-device; no custom kernel required.
  - *Built-in kernel*: compile with `CONFIG_KSU=y`; nothing is loaded as `kernelsu.ko`.
- **Rebranded manager** (`com.woousu.wo`) signed with its own certificate, independent of the upstream KernelSU manager.

## Compatibility

| Item | Requirement |
|---|---|
| Android | 12 or newer (manager `minSdk 31`) |
| Kernel | GKI 2.0 / 5.10+; 4.14+ is possible with manual integration |
| ABI | `arm64-v8a` |
| Boot image | `boot` or `init_boot` (wherever the ramdisk lives) |
| SUSFS | Only on a kernel compiled with the SUSFS patches |

## Installation

### Requirements

- Unlocked bootloader.
- A stock `boot` / `init_boot` image matching your **current** firmware build (never mix versions).
- A backup of the stock image so you can roll back.

### Install the manager

Download the APK from Releases, or build it yourself (see [Building](#building)). The manager package name is `com.woousu.wo`. It is **not** compatible with the upstream KernelSU manager or managers from other forks — install only one.

### Option A — Patch the stock boot image (works on any KernelSU-supported device)

1. Open the manager, choose **Install**.
2. Select your stock `boot.img` or `init_boot.img`.
3. Flash the patched image:

   ```bash
   fastboot flash boot   patched_boot.img     # or:
   fastboot flash init_boot patched_init_boot.img
   fastboot reboot
   ```

4. Verify:

   ```sh
   su -c 'ksud --version'
   ```

> This mode keeps your stock kernel, so **SUSFS is not available** — there is no SUSFS code in the stock kernel.

### Option B — Flash a kernel with built-in WoouSU (and optionally SUSFS)

Build a kernel with `CONFIG_KSU=y` (see [Building](#building)) or download a prebuilt kernel from Releases.

```bash
fastboot flash boot WoouSU-<device>-<version>.img
fastboot reboot
```

SUSFS also needs a userspace module that provides the `ksu_susfs` binary and boot-time rules. With only the kernel flashed, SUSFS exists but no hiding rules are active.

## Building

### ksud and manager

```bash
cd userspace/ksud
cargo ndk -t arm64-v8a build --release

mkdir -p ../../manager/app/src/main/jniLibs/arm64-v8a
cp target/aarch64-linux-android/release/ksud \
   ../../manager/app/src/main/jniLibs/arm64-v8a/libksud.so

cd ../../manager
./gradlew assembleRelease
```

The manager build requires `libksud.so` to be present before Gradle runs.

### Kernel

- **LKM helper**: `scripts/woousu_build_lkm.sh <kernel-tree> [defconfig]` integrates `kernel/` into a kernel tree and builds the module.
- **Manual integration**: place this repository (or a symlink) as `drivers/kernelsu`, add it to `drivers/Makefile` / `drivers/Kconfig`, then enable `CONFIG_KSU=y` (built-in) or `CONFIG_KSU=m` (LKM).
- **SUSFS**: apply the public patches to the kernel tree before building:

  ```bash
  ./scripts/woousu_apply_susfs.sh <kernel-source-root> gki-android15-6.6
  ```

  Patch refs map to the kernel line (e.g. `gki-android13-5.15`, `gki-android14-6.1`, `gki-android15-6.6`). Hunks may need a manual rebase — do not force-apply blindly.

### Signing

Set the signing values through environment variables or `manager/sign.example.properties`:

```
KEYSTORE_FILE=path/to/release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

The kernel trusts exactly one manager certificate (`KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH` in `kernel/Kbuild`). If you replace the keystore, run `scripts/woousu_signature.sh` and rebuild the kernel, otherwise the manager will not be recognized.

## FAQ

- **Play Integrity / hardware attestation** — unlocking the bootloader and running a custom kernel breaks hardware-backed verification. SUSFS hides traces; it cannot fake TEE. This project does not ship or support leaked keyboxes.
- **Bootloop after flashing** — flash your stock `boot` / `init_boot` back in fastboot. Always keep a backup.
- **Manager says "not installed"** — the running kernel is not KSU-enabled, or the manager certificate does not match the kernel's expected certificate.
- **SUSFS commands do nothing** — the kernel was not built with SUSFS, or no rules/userspace module are installed.
- **Can I use my own kernel?** — Yes. Any kernel that integrates this repository's `kernel/` sources and enables `CONFIG_KSU` works; SUSFS is optional.

## Credits

- [KernelSU](https://github.com/tiann/KernelSU) — the upstream project this work is based on.
- [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu) — SUSFS kernel patches and userspace tooling, by simonpunk.
- All KernelSU contributors, translators and testers.

## License

The kernel module is licensed under **GPL-2.0**; the rest of this repository is licensed under **GPL-3.0**. See [LICENSE](LICENSE). Redistribution must keep the original copyright notices and license texts, and must state its relationship to upstream KernelSU.

## Disclaimer

Flashing custom images can void your warranty, wipe your data, or brick your device. Unlock and flash at your own risk. The authors are not responsible for any damage, data loss, or legal consequences. Follow your local laws and your device vendor's terms of service.
