# WoouSU

[![简体中文](https://img.shields.io/badge/%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-README__CN-blue?style=flat-square)](README_CN.md)
[![Telegram](https://img.shields.io/badge/Telegram-%40WoouSU-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/WoouSU)

![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)
![Android](https://img.shields.io/badge/Android-12%2B-green.svg)
![Kernel](https://img.shields.io/badge/Kernel-5.10%2B-orange.svg)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-lightgrey.svg)

A **KernelSU-based, kernel-level root solution for Android**. WoouSU keeps KernelSU's kernel-level `su`, allowlist and module system, and adds **SUSFS hiding, KPM kernel modules, dynamic manager signatures** and a **highly customizable manager**.

> Official Telegram channel: [@WoouSU](https://t.me/WoouSU) — updates, support and discussion.

---

## Why WoouSU

### Personalization
- **Miuix / Material dual UI** — one kernel, two design languages, switch anytime.
- **Background image + per-section opacity** — set your own wallpaper and tune the transparency of content sections independently.
- **Blur and floating bottom bar** — glass-like layers and a floating navigation bar.
- **Monet color extraction / key color / palette** — follow the system palette or pick your own, applied across the whole UI.
- **Theme preview** — see the result before you apply it.

### SUSFS hiding
- Hides root traces at the kernel level to reduce detection by apps.
- Works together with mount control and App Profile.
- Requires a kernel built with the **SUSFS patches**, plus a userspace module that provides `ksu_susfs` and boot-time rules.
- Patching a stock kernel through LKM does **not** provide SUSFS, because the stock kernel contains no SUSFS code.

### KPM (KernelPatch Module)
- Enable KPM support with a checkbox while patching the kernel.
- Integrate it into the **kernel being patched**, or **choose another boot image** and patch it separately.
- Bundled `kptools` / `kpimg` — everything happens inside the manager, no external toolchain.
- **Integrity verification** before and after writing, plus automatic **backup / restore of the original boot**.
- Full `ksud kpm` command set: `load`, `unload`, `list`, `info`, `control`, `version`.
- Modules placed in `/data/adb/kpm` are loaded automatically at boot.

### Dynamic manager signature
- Lets other KernelSU-based managers work with a WoouSU kernel.
- Scans installed managers and writes the selected signature into the kernel, **persisted across reboots**.
- Clear it at any time to restore the default WoouSU trust state — no kernel rebuild required.

## Features

- **Kernel-level `su`** with allowlist-based access control (only granted apps can see `su`).
- **Module system** (overlayfs-based), including metamodules and module WebUI.
- **App Profile** and **kernel umount** for per-app hiding and mount control.
- **Two deployment modes**
  - *LKM patching*: patch the stock `boot` / `init_boot` image on-device; no custom kernel required.
  - *Built-in kernel*: compile with `CONFIG_KSU=y`; nothing is loaded as `kernelsu.ko`, and SUSFS can be enabled as well.
- **Rebranded manager** (`com.woousu.wo`) signed with its own certificate; the trusted certificate can be switched via dynamic manager signature.
- **Upstream tracking**: prebuilt LKM artifacts for 8 KMIs, compiled for the current version.

## Compatibility

| Item | Requirement |
|---|---|
| Android | 12 or newer (manager `minSdk 31`) |
| Kernel | GKI 2.0 / 5.10+; 4.14+ is possible with manual integration |
| ABI | `arm64-v8a` |
| Boot image | `boot` or `init_boot` (wherever the ramdisk lives) |
| SUSFS | Only on a kernel compiled with the SUSFS patches |
| Manager | `com.woousu.wo`; own certificate, with dynamic manager signature support |

## Installation

The manager can be downloaded as an APK from Releases, or built from source (see [Building](#building)).

### Requirements

- Unlocked bootloader.
- A stock `boot` / `init_boot` image matching your **current** firmware build (never mix versions).
- A backup of the stock image so you can roll back.

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

> This mode keeps your stock kernel, so **SUSFS is not available** — there is no SUSFS code in the stock kernel, nor any other kernel feature beyond what the stock kernel provides.

### Option B — Flash a kernel with built-in WoouSU (and optionally SUSFS)

Build a kernel with `CONFIG_KSU=y` (see [Building](#building)) or download a prebuilt kernel from Releases.

```bash
fastboot flash boot WoouSU-<device>-<version>.img
fastboot reboot
```

SUSFS also needs a userspace module that provides the `ksu_susfs` binary and boot-time rules. With only the kernel flashed, SUSFS exists but no hiding rules are active.

## KPM usage

After enabling KPM while patching the kernel, manage modules from the manager or the command line:

```sh
ksud kpm list                 # list loaded modules
ksud kpm load <module.kpm>    # load a module
ksud kpm unload <name>        # unload a module
ksud kpm info <name>          # show module info
ksud kpm control <name>       # runtime control
ksud kpm version              # print version
```

Drop `.kpm` files into `/data/adb/kpm` to load them automatically at boot.

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

To make another KernelSU-based manager work with a WoouSU kernel, use **Dynamic manager signature** in the manager instead — no kernel rebuild needed.

## FAQ

- **Play Integrity / hardware attestation** — unlocking the bootloader and running a custom kernel breaks hardware-backed verification. SUSFS hides traces; it cannot fake TEE. This project does not ship or support leaked keyboxes.
- **Bootloop after flashing** — flash your stock `boot` / `init_boot` back in fastboot. Always keep a backup.
- **Manager says "not installed"** — the running kernel is not KSU-enabled, or the manager certificate does not match the kernel's expected certificate. Dynamic manager signature can switch the trusted certificate.
- **SUSFS commands do nothing** — the kernel was not built with SUSFS, or no rules/userspace module are installed.
- **KPM fails to load** — check the reported reason and kernel logs, and make sure the module matches your kernel version.
- **Can I use my own kernel?** — Yes. Any kernel that integrates this repository's `kernel/` sources and enables `CONFIG_KSU` works; SUSFS is optional.

## Community

- Telegram channel: [@WoouSU](https://t.me/WoouSU)
- Issues: [GitHub Issues](https://github.com/WOOUMJ/WoouSU/issues)
- Releases: [GitHub Releases](https://github.com/WOOUMJ/WoouSU/releases)

## Credits

- [KernelSU](https://github.com/tiann/KernelSU) — the upstream project this work is based on.
- [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu) — SUSFS kernel patches and userspace tooling, by simonpunk.
- KernelPatch — the source of KPM support.
- All KernelSU contributors, translators and testers.

## License

The kernel module is licensed under **GPL-2.0**; the rest of this repository is licensed under **GPL-3.0**. See [LICENSE](LICENSE). Redistribution must keep the original copyright notices and license texts, and must state its relationship to upstream KernelSU.

## Disclaimer

Flashing custom images can void your warranty, wipe your data, or brick your device. Unlock and flash at your own risk. The authors are not responsible for any damage, data loss, or legal consequences. Follow your local laws and your device vendor's terms of service.
