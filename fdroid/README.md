# NESTNOTE F-Droid repository

This directory contains the F-Droid server configuration and metadata. The repository signing key must be separate from the production APK signing key.

## One-time keystore generation

**Important:** This is a `workflow_dispatch` workflow. GitHub only makes it manually runnable from the Actions UI after `.github/workflows/fdroid-keystore-init.yml` exists on the repository's default branch. Merge the PR containing this file first; while the PR is unmerged, the workflow will appear on the feature branch but not in the default-branch Actions workflow list.

1. Open the repository's **Actions** tab after the change is merged.
2. Select **Generate F-Droid Repo Keystore (Run Once)** and click **Run workflow**.
3. Wait for the workflow to finish. Download both short-lived artifacts: `fdroid-keystore` and `fdroid-keystore-base64`. The artifacts expire after one day.
4. Store the keystore securely as a backup. Never commit it to the repository or share it publicly.
5. Add the contents of the Base64 artifact as the repository Actions secret `FDROID_KEYSTORE_BASE64`.
6. Add the matching keystore password as `FDROID_KEYSTORE_PASS`, the repository key alias as `FDROID_KEY_ALIAS`, and the key password as `FDROID_KEY_PASS`. Use the values from the generated configuration/keystore setup. These must be repository secrets, not values committed to Git.
7. Record the SHA-256 fingerprint printed by the workflow. Keep this repository signing key and fingerprint stable; do not rerun key generation for subsequent releases.

**Security note:** The generation workflow prints the Base64 keystore to the Actions log as requested. Treat that output as secret material. Restrict access to the repository and workflow logs, and delete or rotate the key if it is exposed. Prefer downloading the short-lived artifact rather than copying the Base64 value from logs.

## Required GitHub Actions secrets

- `FDROID_KEYSTORE_BASE64`: base64-encoded dedicated F-Droid repository keystore
- `FDROID_KEYSTORE_PASS`: repository keystore password
- `FDROID_KEY_ALIAS`: repository key alias (for example `nestnote-repokey`)
- `FDROID_KEY_PASS`: repository key password

The publishing workflow decodes `FDROID_KEYSTORE_BASE64` into `fdroid/keystore.p12` and substitutes the other three secrets into `fdroid/config.yml` at runtime. The keystore is not committed.

## APK signing fingerprint

The production APK signing certificate fingerprint is different from the F-Droid repository signing fingerprint. Verify the production APK certificate and add its SHA-256 fingerprint to `AllowedAPKSigningKeys` in `metadata/com.nestnote.app.yml` before relying on repository publishing.
