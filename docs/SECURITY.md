# Security model and Private Vault boundaries

Last updated: 2026-10-23

## Summary

NESTNOTE uses two different controls that must not be confused:

- **Local-storage encryption** protects the app's stored note payloads at rest using AES-GCM and an Android Keystore-managed key.
- **Private Vault locking** is an application-level UI access gate. Its password is verified using a salted password hash, and supported devices may offer biometric authentication. It controls access through the app's intended UI flow; it does **not** derive or unlock a separate encryption key for private notes.

Consequently, the current Private Vault is a privacy and navigation boundary, **not a cryptographically isolated vault**. A private note is stored under the same general local note-storage encryption system as other notes. The vault password alone does not provide independent encryption of private-note content.

## What each control does and does not guarantee

| Control | Intended protection | Not a guarantee of |
| --- | --- | --- |
| AES-GCM + Android Keystore for local note payloads | Confidentiality and integrity of the app's encrypted local payload while the key remains protected | Protection after content is decrypted for display, from a compromised/unlocked runtime, or from plaintext exports |
| Vault password / biometric gate | Restricts access through the intended Private Vault UI flow | A second encryption layer, a separate cryptographic key, or protection from all UI/control-flow bypasses |
| `FLAG_SECURE` | Requests that Android block screenshots and non-secure display/Recents capture for the protected window | Universal protection against external cameras, a compromised OS, every OEM-specific capture path, or content already copied/exported |
| Private-note filtering | Keeps flagged notes out of supported ordinary app views and searches | Cryptographic access control over raw storage, backups, logs, or unreviewed note-derived surfaces |
| Password-attempt backoff | Slows repeated attempts through the app's vault password flow | Device-wide rate limiting or resistance to a compromised process/device |

## Backups and data movement

User-exported JSON backups are **plaintext** and may include private-note titles and bodies. The private-note flag is preserved through backup/restore, but that flag is metadata, not encryption. Treat an exported backup as sensitive data and store/share it accordingly. Anyone with access to the exported file can read its contents without unlocking the in-app Private Vault.

Local storage encryption and vault UI locking do not change the plaintext nature of exported JSON. Do not describe backups as encrypted unless a separate, verified encrypted-export format is implemented.

## Threat-model assumptions

The current design assumes Android and the app process are not actively compromised, the Android Keystore key remains available and protected, and users understand that export/share actions can move note content outside the app's storage boundary. The UI gate is intended to protect against casual access through normal app navigation; it is not a defense against an attacker who can modify the app, instrument its process, exploit the OS, or directly obtain decrypted/exported content.

Potentially exposed assets include private-note titles and bodies, metadata, and plaintext exported backups. Relevant threats include shoulder-surfing, returning to a still-visible editor after backgrounding, screenshots/Recents previews, private-note leakage into ordinary lists/search/counts, and backup disclosure. Lifecycle handling, `FLAG_SECURE`, and filtering reduce specific risks but require regression and device validation.

## Required user-facing wording

Prefer:

> Private Vault adds a password/biometric gate to viewing private notes inside NESTNOTE. It does not use a separate encryption key for those notes. Local note data is encrypted at rest by NESTNOTE's Android Keystore-backed storage, but exported JSON backups are plaintext and may contain private notes.

Avoid claims such as “separately encrypted vault,” “vault-password-encrypted notes,” or “private notes remain encrypted in exported backups.”

## Release requirements

- Keep the Private Vault release checklist in `docs/RELEASE_CHECKLIST.md` as a release gate.
- Test backgrounding, activity/process recreation, biometric cancellation/failure/unavailability, screenshot/Recents behavior, backup round trips, and all ordinary note-derived views on supported Android versions.
- Do not infer device behavior from source inspection or a successful build alone.
- Any future cryptographic-isolation design must separately specify key derivation/provisioning, authenticated encryption, key lifecycle and recovery, password changes, migration of existing private notes, backup/export semantics, and failure-safe rollback. Do not imply that this design exists until implemented and independently tested.
