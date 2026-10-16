# NESTNOTE F-Droid repository

This directory contains the F-Droid server configuration and metadata. The repository signing key is separate from the production APK signing key.

## Required GitHub Actions secrets

- `FDROID_KEYSTORE_BASE64`: base64 of the dedicated F-Droid repo keystore (`keystore.p12`)
- `FDROID_KEYSTORE_PASS`: repo keystore password
- `FDROID_KEY_ALIAS`: repo key alias (for example `nestnote-repokey`)
- `FDROID_KEY_PASS`: repo key password

Never reuse the APK signing keystore or commit any keystore/password.

## One-time setup

Generate the repository key once on a trusted machine using `fdroid init` in this directory (or `keytool` with PKCS12), record the SHA-256 certificate fingerprint, back up the keystore securely, and add the four secrets above. Do not regenerate the key for subsequent releases.

The APK signing certificate fingerprint is separate. Add the verified production APK certificate fingerprint to `AllowedAPKSigningKeys` in `metadata/com.nestnote.app.yml` before enabling distribution.
