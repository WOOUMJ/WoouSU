# WoouSU

![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)
![Android](https://img.shields.io/badge/Android-12%2B-green.svg)
![Kernel](https://img.shields.io/badge/Kernel-5.10%2B-orange.svg)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-lightgrey.svg)

基于 [KernelSU](https://github.com/tiann/KernelSU) 的内核级 Android Root 方案，支持可选的 SUSFS，并提供独立改名的管理器。

---

## 特性

- **内核级 `su`**：基于白名单的授权，未授权应用感知不到 `su`。
- **模块系统**：基于 overlayfs，支持元模块与模块 WebUI。
- **App Profile / 内核卸载**：按应用控制隐藏与挂载。
- **两种部署方式**
  - *LKM 修补*：在手机上直接修补原厂 `boot` / `init_boot`，不需要自定义内核。
  - *内建内核*：使用 `CONFIG_KSU=y` 编入内核，不加载 `kernelsu.ko`。
- **独立管理器**（`com.woousu.wo`）：使用自有证书签名，与上游 KernelSU 管理器互不兼容。

## 兼容性

| 项目 | 要求 |
|---|---|
| Android | 12 及以上（管理器 `minSdk 31`） |
| 内核 | GKI 2.0 / 5.10+；4.14+ 需自行移植集成 |
| ABI | `arm64-v8a` |
| Boot 镜像 | `boot` 或 `init_boot`（取决于 ramdisk 所在分区） |
| SUSFS | 仅在编入 SUSFS 补丁的内核上可用 |

## 安装

### 前置条件

- Bootloader 已解锁。
- 与原机**当前固件版本完全一致**的原厂 `boot` / `init_boot` 镜像（不要混用版本）。
- 已备份原厂镜像，便于回滚。

### 方式一：修补原厂 boot 镜像（适用于任何 KernelSU 支持的设备）

1. 打开管理器，进入「安装」。
2. 选择原厂 `boot.img` 或 `init_boot.img`。
3. 刷入修补后的镜像：

   ```bash
   fastboot flash boot   patched_boot.img     # 或：
   fastboot flash init_boot patched_init_boot.img
   fastboot reboot
   ```

4. 验证：

   ```sh
   su -c 'ksud --version'
   ```

> 这种方式保留原厂内核，因此**没有 SUSFS**——原厂内核里没有 SUSFS 代码。

### 方式二：刷入内建 WoouSU（可选 SUSFS）的内核

使用 `CONFIG_KSU=y` 构建内核（见[构建](#构建)），或从 Releases 下载预编译内核。

```bash
fastboot flash boot WoouSU-<设备>-<版本>.img
fastboot reboot
```

SUSFS 还需要配套的用户态模块（提供 `ksu_susfs` 二进制与开机规则）。只刷内核时 SUSFS 能力存在，但没有任何隐藏规则生效。

## 构建

### ksud 与管理器

```bash
cd userspace/ksud
cargo ndk -t arm64-v8a build --release

mkdir -p ../../manager/app/src/main/jniLibs/arm64-v8a
cp target/aarch64-linux-android/release/ksud \
   ../../manager/app/src/main/jniLibs/arm64-v8a/libksud.so

cd ../../manager
./gradlew assembleRelease
```

管理器构建前必须先放入 `libksud.so`。

### 内核

- **LKM 辅助脚本**：`scripts/woousu_build_lkm.sh <内核树> [defconfig]` 会把 `kernel/` 集成进内核树并编译模块。
- **手动集成**：把本仓库（或软链接）放到 `drivers/kernelsu`，在 `drivers/Makefile` / `drivers/Kconfig` 中注册，然后按需开启 `CONFIG_KSU=y`（内建）或 `CONFIG_KSU=m`（LKM）。
- **SUSFS**：编译前先给内核树打公开补丁：

  ```bash
  ./scripts/woousu_apply_susfs.sh <内核源码根目录> gki-android15-6.6
  ```

  补丁 ref 与内核版本对应（如 `gki-android13-5.15`、`gki-android14-6.1`、`gki-android15-6.6`）。补丁不一定能干净应用，冲突时需要手工 rebase，不要盲目强制应用。

### 签名

通过环境变量或 `manager/sign.example.properties` 配置签名：

```
KEYSTORE_FILE=path/to/release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

内核只信任一张管理器证书（`kernel/Kbuild` 中的 `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH`）。更换密钥后必须运行 `scripts/woousu_signature.sh` 并重新编译内核，否则管理器不会被识别。

## 常见问题

- **Play Integrity / 硬件认证**：解锁 Bootloader 与自定义内核会破坏硬件级认证。SUSFS 只隐藏痕迹，无法伪造 TEE。本项目不提供、也不支持任何泄露的 keybox。
- **刷完无限重启**：在 fastboot 中刷回原厂 `boot` / `init_boot`。请务必提前备份。
- **管理器显示「未安装」**：当前内核未启用 KSU，或管理器证书与内核预期证书不一致。
- **SUSFS 命令无效**：内核未编入 SUSFS，或未安装规则/用户态模块。
- **能否使用自己的内核？** 可以。任何集成本仓库 `kernel/` 源码并开启 `CONFIG_KSU` 的内核都能用，SUSFS 是可选项。

## 致谢

- [KernelSU](https://github.com/tiann/KernelSU)：本项目的上游基础。
- [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu)：SUSFS 内核补丁与用户态工具，作者 simonpunk。
- 所有为 KernelSU 贡献代码、翻译与测试的开发者。

## 许可

内核模块使用 **GPL-2.0**，仓库其余部分使用 **GPL-3.0**，详见 [LICENSE](LICENSE)。再分发时请保留原始版权声明与许可证文本，并注明与上游 KernelSU 的关系。

## 免责声明

刷机有风险：可能导致数据丢失、无法开机、保修失效。请自行备份并在理解风险的前提下操作，作者不对任何直接或间接损失负责。请遵守当地法律法规与设备厂商的服务条款。
