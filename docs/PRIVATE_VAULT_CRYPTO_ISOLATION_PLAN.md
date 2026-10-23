# Private Vault: Independent Cryptographic Isolation Plan

Status: design and implementation requirements only. This document does not claim that independent vault encryption has been implemented.

Related work:
- Private Vault UI/lifecycle and privacy-boundary hardening: [PR #90](https://github.com/Ravi-katta-dev/NESTNOTE/pull/90)
- Existing UI-vs-encryption security documentation: [Security Model](SECURITY.md) and [Threat Model](THREAT_MODEL.md)

## Goal

Ensure private-note payloads are cryptographically isolated from ordinary notes, so that access to the ordinary-note storage key or a bypass of the intended Private Vault UI does not by itself decrypt private-note content. Preserve the existing UI gate as a separate defense-in-depth control.

## Current state and non-goals

Today, the vault password is verified as an authentication secret and does not derive, wrap, or unlock an independent vault encryption key. Private notes use the app's general local-storage encryption. Filtering, lifecycle relocking, and `FLAG_SECURE` are useful controls but are not cryptographic isolation.

This task must not be marked complete based only on a UI change, a private-note flag, or a successful build. It is also not a promise to protect against a compromised OS, a maliciously modified app, or content already exported in plaintext.

## Required design decisions

Before implementation, document and review:

1. **Threat model and invariants**
   - Define the attacker capabilities and the confidentiality guarantees expected from a vault-specific key.
   - State what happens when the app process is compromised, the device is unlocked, or the Android Keystore key is invalidated.
   - Ensure ordinary-note operations cannot accidentally request vault decryption.

2. **Independent key hierarchy**
   - Use a randomly generated vault data-encryption key (DEK), separate from the ordinary-note storage key.
   - Protect the DEK with a reviewed Android Keystore-backed wrapping/unlocking design.
   - Decide how password authentication and biometric authentication authorize key use. Do not store the raw password or treat a password-verification hash as an encryption key.
   - If password-derived key material is proposed, specify a suitable password KDF, parameters, salt, versioning, and upgrade strategy. Review offline-guessing and device-performance risks.
   - Use authenticated encryption with unique nonces and explicit format/version metadata; bind record identity and purpose as authenticated data where appropriate.

3. **Password and biometric lifecycle**
   - Password changes must re-wrap or rotate key material without exposing plaintext notes or leaving mixed key states.
   - Biometric enrollment changes, cancellation, lockout, unavailable hardware, and fallback must fail closed and follow a documented policy.
   - Define whether biometrics authorize a Keystore operation, unlock a wrapped DEK, or merely pass the UI gate; these are not interchangeable guarantees.

4. **Migration and rollback safety**
   - Migrate existing private notes from the current storage format without changing ordinary notes.
   - Make migration restartable and atomic per record or transaction: write and verify new ciphertext before removing old ciphertext.
   - On any failure, preserve original bytes, report a recoverable error, and never silently replace notes with empty or starter data.
   - Handle partial migration, app upgrade/downgrade, process death, key invalidation, and interrupted password changes.
   - Do not silently downgrade a vault record to ordinary-note encryption or plaintext.

5. **Backups and data movement**
   - Decide whether private notes are excluded from ordinary backups (current user-facing plaintext exports should continue to exclude them) or supported through a separately designed encrypted export.
   - Never add private notes back to plaintext JSON exports without an explicit, clearly labelled user action and security review.
   - Define restore behavior when the vault key is unavailable; never imply that local Keystore keys are portable with an exported file.

6. **Data model and access boundaries**
   - Ensure private records are encrypted/decrypted only through a dedicated vault-aware repository/API.
   - Keep private-note metadata and identifiers from leaking through search, logs, analytics, insights, notifications, widgets, links, and ordinary backup paths.
   - Treat filtering as defense in depth, not as the confidentiality mechanism.

## Implementation and validation checklist

- [ ] Approve a written key hierarchy and cryptographic protocol before coding.
- [ ] Add a versioned private-record ciphertext format with authenticated encryption and vault-specific key separation.
- [ ] Implement a dedicated vault key lifecycle with explicit authorization and fail-closed behavior.
- [ ] Add safe migration from existing private records with interruption and rollback handling.
- [ ] Implement and document password change, biometric enrollment change, key rotation, and key-loss/recovery behavior.
- [ ] Ensure ordinary plaintext backup excludes private notes; design encrypted export separately if required.
- [ ] Add unit tests for tampering, wrong keys, wrong passwords, nonce/format errors, and cross-boundary key separation.
- [ ] Add migration tests for empty, mixed, large, malformed, partially migrated, and interrupted datasets.
- [ ] Add regression tests proving ordinary-note APIs cannot decrypt vault records and vault APIs cannot silently fall back to ordinary keys.
- [ ] Test lifecycle, biometric outcomes, process death, reboot, and Keystore invalidation on supported Android versions/devices.
- [ ] Update the security model, threat model, release checklist, backup documentation, and user-facing claims.
- [ ] Perform a security review of the final implementation and verify the release build.

## Acceptance criteria

This work is complete only when:
- Private-note ciphertext is encrypted under key material independent of the ordinary-note storage key.
- The vault password is not stored and is not confused with a password-verification hash or the DEK.
- No UI/filtering bypass can decrypt private content without the dedicated key authorization path.
- Migration and recovery preserve original data on failure.
- Ordinary plaintext backups do not contain private content.
- Automated tests and device validation demonstrate the stated guarantees, and documentation accurately describes residual risks.

## Relationship to PR #90

PR #90 addresses UI/lifecycle hardening and ordinary-surface privacy filtering. It explicitly does **not** implement independent cryptographic isolation. Keep the two scopes separate: merge and validate the lifecycle/privacy fixes independently, then implement this cryptographic project as a dedicated, reviewed change set. The implementation PR must reference this plan and PR #90, and must not claim release readiness until the acceptance criteria above are met.
