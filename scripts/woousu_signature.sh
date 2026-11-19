#!/usr/bin/env bash
# Bind the WoouSU manager signing certificate to the kernel module.
#
# The kernel only trusts a manager whose APK signing certificate matches
# KSU_EXPECTED_SIZE / KSU_EXPECTED_HASH in kernel/Kbuild. Run this once after
# creating (or replacing) your release keystore, then rebuild the kernel.
#
#   ./scripts/woousu_signature.sh                 # create keystore + patch Kbuild
#   ./scripts/woousu_signature.sh --verify app.apk
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE="${KEYSTORE_FILE:-$ROOT/manager/woousu-release.jks}"
ALIAS="${KEY_ALIAS:-woousu}"
PASS="${KEYSTORE_PASSWORD:-}"
KBUILD="$ROOT/kernel/Kbuild"

if [[ "${1:-}" == "--verify" ]]; then
    apk="${2:?usage: $0 --verify app.apk}"
    SDK="${ANDROID_HOME:-/opt/android-sdk}"
    APKSIGNER="$(find "$SDK/build-tools" -name apksigner 2>/dev/null | sort -V | tail -1)"
    "$APKSIGNER" verify --print-certs "$apk"
    exit 0
fi

: "${PASS:?set KEYSTORE_PASSWORD before running this script}"

if [[ ! -f "$KEYSTORE" ]]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" -alias "$ALIAS" \
        -keyalg RSA -keysize 2048 -validity 36500 -storetype PKCS12 \
        -storepass "$PASS" -keypass "$PASS" \
        -dname "CN=WoouSU,O=WoouSU,C=CN"
fi

cert="$(mktemp)"; trap 'rm -f "$cert"' EXIT
keytool -exportcert -keystore "$KEYSTORE" -alias "$ALIAS" -storepass "$PASS" -file "$cert"

size="$(stat -c '%s' "$cert")"
hash="$(sha256sum "$cert" | cut -d' ' -f1)"

python3 - "$KBUILD" "$size" "$hash" <<'PY'
import re, sys
path, size, digest = sys.argv[1:4]
text = open(path, encoding="utf-8").read()
text = re.sub(r"KSU_EXPECTED_SIZE \?= 0x[0-9a-fA-F]+",
              f"KSU_EXPECTED_SIZE ?= 0x{int(size):04x}", text)
text = re.sub(r"KSU_EXPECTED_HASH \?= [0-9a-fA-F]+",
              f"KSU_EXPECTED_HASH ?= {digest}", text)
open(path, "w", encoding="utf-8").write(text)
PY

echo "certificate size : $size bytes (0x$(printf '%04x' "$size"))"
echo "certificate hash : $hash"
echo "patched          : $KBUILD"
