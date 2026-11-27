# WoouSU

[![简体中文](https://img.shields.io/badge/%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-README__CN-blue?style=flat-square)](README_CN.md)
[![Telegram](https://img.shields.io/badge/Telegram-%40WoouSU-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/WoouSU)

![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)
![Android](https://img.shields.io/badge/Android-12%2B-green.svg)
![Kernel](https://img.shields.io/badge/Kernel-5.10%2B-orange.svg)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-lightgrey.svg)

基于 [KernelSU](https://github.com/tiann/KernelSU) 的内核级 Android Root 方案。在保留内核级 `su`、白名单与模块体系的同时，WoouSU 带来 **SUSFS 隐藏、KPM 内核模块、动态管理器签名** 与 **高度可定制的界面**，让 Root 既能藏得干净，也能用得顺手。

> 官方 Telegram 频道：[@WoouSU](https://t.me/WoouSU) · 获取更新、反馈问题、交流玩法。

---

## 为什么选择 WoouSU

### 高度个性化
- **Miuix / Material 双 UI**：一套内核，两种设计语言，随时切换。
- **背景图 + 区块不透明度**：为主界面设置专属背景，并独立调节内容区块的透明度。
- **模糊与悬浮底栏**：毛玻璃质感、悬浮导航，层级更清晰。
- **Monet 取色 / key color / 调色板**：跟随系统取色或手动指定主色，整套界面同步换色。
- **主题预览**：应用前先预览，所见即所得。

### SUSFS 隐藏
- 在内核层隐藏 root 痕迹，减少被应用检测的机会。
- 与挂载控制、App Profile 等隐藏机制协同工作。
- 需要 **编入 SUSFS 补丁的内核**，并配合提供 `ksu_susfs` 与开机规则的用户态模块。
- LKM 修补原厂内核时不含 SUSFS 代码，因此该模式不提供 SUSFS。

### KPM 内核模块
- 修补内核时勾选即可启用 KPM 支持。
- 支持 **跟随本次修补的内核** 集成，也可 **另行选择 boot 文件** 单独打补丁。
- 内置 `kptools` / `kpimg`，全程在管理器内完成，无需外部工具链。
- 生成与写入前后进行 **完整性校验**，并自动 **备份 / 还原原始 boot**。
- 完整的 `ksud kpm` 命令：`load`、`unload`、`list`、`info`、`control`、`version`。
- 放入 `/data/adb/kpm` 的模块会在开机时自动加载；加载失败会显示真实原因并输出内核日志。

### 动态管理器签名
- 让其它 KernelSU 系管理器也能配合 WoouSU 内核工作。
- 扫描已安装的管理器，一键写入签名；签名在内核中 **持久保存**，重启不丢失。
- 随时清除签名，恢复 WoouSU 管理器的默认信任状态，无需重新编译内核。

## 特性

- **内核级 `su`**：基于白名单的授权，未授权应用感知不到 `su`。
- **模块系统**：基于 overlayfs，支持元模块与模块 WebUI。
- **App Profile / 内核卸载**：按应用控制隐藏与挂载。
- **两种部署方式**
  - *LKM 修补*：在手机上直接修补原厂 `boot` / `init_boot`，不需要自定义内核。
  - *内建内核*：使用 `CONFIG_KSU=y` 编入内核，不加载 `kernelsu.ko`，可同时启用 SUSFS。
- **独立管理器**（`com.woousu.wo`）：使用自有证书签名，与上游 KernelSU 管理器互不兼容。
- **持续跟进上游**：为 8 个 KMI 提供按当前版本编译的 LKM 预编译产物。

## 兼容性

| 项目 | 要求 |
|---|---|
| Android | 12 及以上（管理器 `minSdk 31`） |
| 内核 | GKI 2.0 / 5.10+；4.14+ 需自行移植集成 |
| ABI | `arm64-v8a` |
| Boot 镜像 | `boot` 或 `init_boot`（取决于 ramdisk 所在分区） |
| SUSFS | 仅在编入 SUSFS 补丁的内核上可用 |
| 管理器 | `com.woousu.wo`，与上游及其它分支管理器互斥，只能安装一个 |

## 安装

安装管理器：从 Releases 下载 APK，或自行构建（参见[构建](#构建)）。

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

> 这种方式保留原厂内核，因此**没有 SUSFS**，也不具备原厂内核之外的内核特性。

### 方式二：刷入内建 WoouSU（可选 SUSFS）的内核

使用 `CONFIG_KSU=y` 构建内核（见[构建](#构建)），或从 Releases 下载预编译内核。

```bash
fastboot flash boot WoouSU-<设备>-<版本>.img
fastboot reboot
```

SUSFS 还需要配套的用户态模块（提供 `ksu_susfs` 二进制与开机规则）。只刷内核时 SUSFS 能力存在，但没有任何隐藏规则生效。

## KPM 使用

在修补内核时启用 KPM 后，可通过管理器或命令行管理模块：

```sh
ksud kpm list                 # 查看已加载模块
ksud kpm load <module.kpm>    # 加载模块
ksud kpm unload <name>        # 卸载模块
ksud kpm info <name>          # 查看模块信息
ksud kpm control <name>       # 运行时控制
ksud kpm version              # 查看版本
```

将 `.kpm` 文件放入 `/data/adb/kpm`，开机时会自动加载。

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

如果只是想让其它 KernelSU 系管理器配合 WoouSU 内核，可直接在管理器中使用「动态管理器签名」，无需重新编译内核。

## 常见问题

- **Play Integrity / 硬件认证**：解锁 Bootloader 与自定义内核会破坏硬件级认证。SUSFS 只隐藏痕迹，无法伪造 TEE。本项目不提供、也不支持任何泄露的 keybox。
- **刷完无限重启**：在 fastboot 中刷回原厂 `boot` / `init_boot`。请务必提前备份。
- **管理器显示「未安装」**：当前内核未启用 KSU，或管理器证书与内核预期证书不一致。可使用动态管理器签名切换信任的管理器证书。
- **SUSFS 命令无效**：内核未编入 SUSFS，或未安装规则/用户态模块。
- **KPM 加载失败**：查看管理器显示的失败原因与内核日志，确认模块与当前内核版本匹配。
- **能否使用自己的内核？** 可以。任何集成本仓库 `kernel/` 源码并开启 `CONFIG_KSU` 的内核都能用，SUSFS 是可选项。

## 社区与反馈

- Telegram 频道：[@WoouSU](https://t.me/WoouSU)
- 问题反馈：[GitHub Issues](https://github.com/WOOUMJ/WoouSU/issues)
- 版本发布：[GitHub Releases](https://github.com/WOOUMJ/WoouSU/releases)

## 致谢

- [KernelSU](https://github.com/tiann/KernelSU)：本项目的上游基础。
- [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu)：SUSFS 内核补丁与用户态工具，作者 simonpunk。
- KernelPatch：KPM 能力的来源。
- 所有为 KernelSU 贡献代码、翻译与测试的开发者。

## 许可

内核模块使用 **GPL-2.0**，仓库其余部分使用 **GPL-3.0**，详见 [LICENSE](LICENSE)。再分发时请保留原始版权声明与许可证文本，并注明与上游 KernelSU 的关系。

## 免责声明

刷机有风险：可能导致数据丢失、无法开机、保修失效。请自行备份并在理解风险的前提下操作，作者不对任何直接或间接损失负责。请遵守当地法律法规与设备厂商的服务条款。
