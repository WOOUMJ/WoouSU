plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = getVersionCode()
extra["managerVersionName"] = getVersionName()

// Single source of truth for the WoouSU version: <repo root>/VERSION.
// The kernel build reads the same file, so the manager and the patched image
// can never drift apart.
fun readWoouSuVersion(): Int {
    val versionFile = rootProject.file("../VERSION")
    check(versionFile.isFile) { "WoouSU VERSION file not found: ${versionFile.absolutePath}" }
    return versionFile.readText().trim().toIntOrNull()
        ?: error("Invalid WoouSU version in ${versionFile.absolutePath}")
}

fun getVersionCode(): Int {
    return readWoouSuVersion()
}

fun getVersionName(): String {
    return readWoouSuVersion().toString()
}
