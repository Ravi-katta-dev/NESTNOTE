# NESTNOTE self-hosted F-Droid repository

This workflow publishes a signed repository index to the `gh-pages` branch. It is a **self-hosted repository**, not an official F-Droid listing.

## One-time repository signing key setup

The repository signing key is separate from the Android APK signing key. Generate it on a trusted local machine and store it securely; do not commit it or upload it as a GitHub Actions artifact. Artifacts in a public repository may be downloadable by other users.

Requirements: Java 17 or newer with `keytool`, and a shell with `base64`.

1. Generate a PKCS12 key. Use a strong password and save it in your password manager:

   ```sh
   umask 077
   mkdir -p "$HOME/nestnote-fdroid-key"
   cd "$HOME/nestnote-fdroid-key"
   read -rsp 'New F-Droid repository key password: ' FDROID_KEYSTORE_PASS
   printf '\\n'
   keytool -genkeypair -noprompt \\
     -keystore keystore.p12 -storetype PKCS12 \\
     -storepass "$FDROID_KEYSTORE_PASS" -keypass "$FDROID_KEYSTORE_PASS" \\
     -alias nestnote-repokey -keyalg RSA -keysize 4096 -validity 10000 \\
     -dname "CN=NESTNOTE Repo, OU=Development, O=Ravi-katta-dev, C=IN"
   ```

2. Add these GitHub Actions repository secrets under **Settings → Secrets and variables → Actions**:

   - `FDROID_KEYSTORE_BASE64`: the one-line output of `base64 < keystore.p12 | tr -d '\\n'`
   - `FDROID_KEYSTORE_PASS`: the password used above
   - `FDROID_KEY_ALIAS`: `nestnote-repokey`
   - `FDROID_KEY_PASS`: the same password used above

   Do not paste passwords into workflow files, issues, pull requests, or logs. Clear shell variables and remove temporary key files only after you have safely backed up the keystore.

3. Confirm the public certificate fingerprint and keep it for repository-client setup:

   ```sh
   keytool -exportcert -keystore keystore.p12 -storetype PKCS12 \\
     -storepass "$FDROID_KEYSTORE_PASS" -alias nestnote-repokey \\
     -file repo-cert.der
   keytool -printcert -file repo-cert.der
   ```

4. The workflow also requires the existing APK-signing secrets `KEYSTORE_BASE64`, `KEY_ALIAS`, and `KEY_STORE_PASSWORD`. It verifies that the downloaded release APK is signed by that certificate before indexing it.

## Publishing behavior

- The workflow downloads APK assets matching `NESTNOTE-*.apk` from the exact release that triggered the run.
- It generates the F-Droid index and publishes the `fdroid/repo` directory to `gh-pages`.
- The repository URL configured in `config.yml` is only live after a successful publication. Verify the index and certificate fingerprint before sharing the URL.
- If repository signing secrets are absent, the workflow stops without generating or exposing private signing material.
