#!/usr/bin/env bash
#
# generate-pqc-keys.sh  (Issue #901)
#
# One-time, LOCAL key setup for hybrid (classical + ML-DSA) APK signing of
# GitHub releases. Run this on a secure machine; never commit the outputs.
#
# What it does today (runnable):
#   1. Generates a NEW classical RSA key (you cannot reuse the old one).
#   2. Builds a SigningCertificateLineage proving old-key -> new-key, so
#      existing installs accept updates signed with the new key.
#
#   3. Optionally generates an ML-DSA key in a JDK/provider that implements the
#      ML-DSA algorithm. The generated keystore can be passed to apksigner as
#      the PQC signer. Desktop JDKs without ML-DSA support fail explicitly.
#
# Usage:
#   APKSIGNER=$ANDROID_HOME/build-tools/37.0.0/apksigner \
#   OLD_KEYSTORE=app/release_key.jks OLD_ALIAS=<old> \
#     scripts/signing/generate-pqc-keys.sh
#
set -euo pipefail

APKSIGNER="${APKSIGNER:-apksigner}"
OLD_KEYSTORE="${OLD_KEYSTORE:?set OLD_KEYSTORE to the current release_key.jks}"
OLD_ALIAS="${OLD_ALIAS:?set OLD_ALIAS to the current key alias}"

NEW_KEYSTORE="${NEW_KEYSTORE:-new_release.jks}"
NEW_ALIAS="${NEW_ALIAS:-theoria-new}"
LINEAGE="${LINEAGE:-theoria.lineage}"
VALIDITY_DAYS="${VALIDITY_DAYS:-10000}"
PQC_KEYSTORE="${PQC_KEYSTORE:-pqc_release.jks}"
PQC_ALIAS="${PQC_ALIAS:-theoria-pqc}"
PQC_STORE_PASSWORD="${PQC_STORE_PASSWORD:-}"
PQC_KEY_PASSWORD="${PQC_KEY_PASSWORD:-$PQC_STORE_PASSWORD}"

echo "==> 1/3 Generating new classical RSA-4096 key ($NEW_KEYSTORE / $NEW_ALIAS)"
if [[ -f "$NEW_KEYSTORE" ]]; then
  echo "    $NEW_KEYSTORE already exists, skipping keytool generation."
else
  keytool -genkeypair -v \
    -keystore "$NEW_KEYSTORE" \
    -alias "$NEW_ALIAS" \
    -keyalg RSA -keysize 4096 \
    -validity "$VALIDITY_DAYS"
fi

echo "==> 2/3 Building rotation lineage ($LINEAGE): $OLD_ALIAS -> $NEW_ALIAS"
"$APKSIGNER" rotate \
  --out "$LINEAGE" \
  --old-signer --ks "$OLD_KEYSTORE" --ks-key-alias "$OLD_ALIAS" \
  --new-signer --ks "$NEW_KEYSTORE" --ks-key-alias "$NEW_ALIAS"

echo "==> 3/3 Generating ML-DSA key ($PQC_KEYSTORE / $PQC_ALIAS)"
if [[ -z "$PQC_STORE_PASSWORD" ]]; then
  echo "    Set PQC_STORE_PASSWORD to generate the ML-DSA keystore." >&2
  exit 1
fi
if [[ -f "$PQC_KEYSTORE" ]]; then
  echo "    $PQC_KEYSTORE already exists, skipping ML-DSA key generation."
else
  if ! keytool -genkeypair -v \
      -keystore "$PQC_KEYSTORE" \
      -storepass "$PQC_STORE_PASSWORD" \
      -keypass "$PQC_KEY_PASSWORD" \
      -alias "$PQC_ALIAS" \
      -keyalg ML-DSA \
      -validity "$VALIDITY_DAYS" \
      -dname "CN=Theoria PQC Signing Key"; then
    echo "ML-DSA generation requires a JDK/provider with ML-DSA KeyPairGenerator support." >&2
    exit 1
  fi
fi

echo
echo "Done. Base64-encode and store as GitHub secrets (DO NOT COMMIT):"
echo "    base64 -i $NEW_KEYSTORE  -> secret SIGNING_KEY_NEW"
echo "    base64 -i $LINEAGE       -> secret SIGNING_LINEAGE"
echo "    base64 -i $PQC_KEYSTORE  -> secret SIGNING_PQC_KEYSTORE"
echo "Also record fingerprints for release notes:"
echo "    keytool -list -v -keystore $NEW_KEYSTORE -alias $NEW_ALIAS"
