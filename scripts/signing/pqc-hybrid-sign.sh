#!/usr/bin/env bash
#
# pqc-hybrid-sign.sh  (Issue #901)
#
# Re-signs already-built release APKs with the rotated classical key + lineage,
# then verifies. Designed to run as a CI step AFTER `./gradlew assemble...Release`
# (see PQC_HYBRID_SIGNING_PLAN.md) or locally.
#
# Supports classical key rotation + lineage and, when supplied, the Android 17+
# V3.2 hybrid ML-DSA signer.
#
# Required env:
#   APKSIGNER                path to Android 17 build-tools apksigner
#   NEW_KEYSTORE             rotated classical keystore (e.g. new_release.jks)
#   NEW_ALIAS                alias in NEW_KEYSTORE
#   NEW_STORE_PASSWORD       keystore password
#   NEW_KEY_PASSWORD         key password
#   LINEAGE                  rotation lineage file (theoria.lineage)
# Optional hybrid signer:
#   PQC_KEY                  PKCS#8 ML-DSA private key
#   PQC_CERT                 X.509 certificate matching PQC_KEY
#   PQC_KEYSTORE             keystore containing the ML-DSA key (alternative)
#   PQC_KEY_ALIAS            alias in PQC_KEYSTORE
#   PQC_STORE_PASSWORD       keystore password
#   PQC_KEY_PASSWORD         key password
# Optional:
#   APK_DIR                  dir to scan for *.apk (default: app/build/outputs/apk-renamed)
#
set -euo pipefail

APKSIGNER="${APKSIGNER:?set APKSIGNER to the Android 17 build-tools apksigner}"
NEW_KEYSTORE="${NEW_KEYSTORE:?set NEW_KEYSTORE}"
NEW_ALIAS="${NEW_ALIAS:?set NEW_ALIAS}"
NEW_STORE_PASSWORD="${NEW_STORE_PASSWORD:?set NEW_STORE_PASSWORD}"
NEW_KEY_PASSWORD="${NEW_KEY_PASSWORD:?set NEW_KEY_PASSWORD}"
LINEAGE="${LINEAGE:?set LINEAGE to the rotation lineage file}"
APK_DIR="${APK_DIR:-app/build/outputs/apk-renamed}"

if [[ -n "${PQC_KEY:-}" || -n "${PQC_CERT:-}" ]]; then
  [[ -n "${PQC_KEY:-}" && -n "${PQC_CERT:-}" ]] || {
    echo "PQC_KEY and PQC_CERT must be supplied together" >&2
    exit 1
  }
  PQC_SIGNER_ARGS=(
    --next-signer
    --key "$PQC_KEY"
    --cert "$PQC_CERT"
    --hybrid-signer-role pqc
  )
elif [[ -n "${PQC_KEYSTORE:-}" ]]; then
  PQC_KEY_ALIAS="${PQC_KEY_ALIAS:?set PQC_KEY_ALIAS with PQC_KEYSTORE}"
  PQC_STORE_PASSWORD="${PQC_STORE_PASSWORD:?set PQC_STORE_PASSWORD with PQC_KEYSTORE}"
  PQC_KEY_PASSWORD="${PQC_KEY_PASSWORD:?set PQC_KEY_PASSWORD with PQC_KEYSTORE}"
  PQC_SIGNER_ARGS=(
    --next-signer
    --ks "$PQC_KEYSTORE"
    --ks-key-alias "$PQC_KEY_ALIAS"
    --ks-pass env:PQC_STORE_PASSWORD
    --key-pass env:PQC_KEY_PASSWORD
    --hybrid-signer-role pqc
  )
else
  PQC_SIGNER_ARGS=()
fi

mapfile -t APKS < <(find "$APK_DIR" -name "*.apk" -type f | sort)
if [[ ${#APKS[@]} -eq 0 ]]; then
  echo "No APKs found under $APK_DIR" >&2
  exit 1
fi

for apk in "${APKS[@]}"; do
  echo "==> Signing $apk"
  # Use apksigner's env: form so passwords are NOT placed on the process
  # argv (where they'd be visible to `ps`); they're read from the environment.
  "$APKSIGNER" sign \
    --ks "$NEW_KEYSTORE" \
    --ks-key-alias "$NEW_ALIAS" \
    --ks-pass "env:NEW_STORE_PASSWORD" \
    --key-pass "env:NEW_KEY_PASSWORD" \
    --lineage "$LINEAGE" \
    ${PQC_SIGNER_ARGS:+--hybrid-signer-role classical} \
    "${PQC_SIGNER_ARGS[@]}" \
    "$apk"

  echo "==> Verifying $apk"
  "$APKSIGNER" verify --verbose --print-certs "$apk"
done

echo "All APKs signed and verified."
if [[ ${#PQC_SIGNER_ARGS[@]} -eq 0 ]]; then
  echo "NOTE: classical+lineage only; supply PQC_KEY/PQC_CERT or PQC_KEYSTORE for V3.2 hybrid signing."
else
  echo "Android V3.2 classical + ML-DSA hybrid signer attached."
fi
