# NESTNOTE Threat Model

Last updated: 2026-10-23

## Scope

This threat model covers local note confidentiality, the Private Vault UI gate, private-note filtering, screenshot protection, and user-exported backups. It describes the current implementation boundary, not a claim that every threat has been eliminated.

## Assets

- Note titles, bodies, and metadata, including notes marked private.
- Local encrypted note payloads and the Android Keystore-managed storage key.
- Vault password-verification data and persisted failed-attempt/backoff state.
- Plaintext JSON backups exported by the user.
- Private-note content rendered in the editor, lists, search results, and Recents/screenshot surfaces.

## Security controls currently intended

- Local note payloads use AES-GCM with an Android Keystore-managed key.
- The vault password is stored as a salted hash; supported devices may offer Android biometric authentication.
- Failed password attempts trigger persisted cooldown/backoff.
- Private-note flags are preserved through backup/restore and ordinary views filter private notes.
- `FLAG_SECURE` is requested while the Private Vault or a private-note editor is visible.
- Lifecycle handling is intended to relock the vault and hide private editor UI when the activity stops.

These controls have different purposes and must not be presented as interchangeable.

## Critical distinction: UI lock vs cryptographic isolation

**Private Vault is currently a UI access gate, not a separately encrypted vault.**

The vault password does not derive, wrap, or unlock an independent key for private notes. Private notes use the same general Android Keystore-backed local note encryption as other notes. The password hash authenticates a user through the app's intended UI flow; it is not the key that encrypts the note payload.

Implications:

1. A successful UI lock does not mean private-note bytes have an additional encryption layer.
2. If an attacker bypasses the app UI or gains access to the running process or decrypted content, the vault password alone does not provide cryptographic isolation.
3. Exported JSON backups may contain readable private-note content. The private flag is a filtering label, not a cryptographic control.
4. `FLAG_SECURE` and view filtering reduce exposure through specific UI paths but do not encrypt content or defend against a compromised OS/runtime.

## Threats and mitigations

| Threat | Current mitigation | Residual risk / validation |
| --- | --- | --- |
| Casual access by navigating the app | Password/biometric UI gate and relocking | Does not provide separate content encryption; test all navigation and editor entry paths |
| Private editor remains visible after app backgrounds | Lifecycle handler hides editor and relocks | Verify process/activity recreation and saved editor state on device/emulator |
| Screenshot or Recents snapshot captures content | `FLAG_SECURE` while protected UI is visible | Validate on supported Android/OEM versions; no claim against external cameras or compromised OS |
| Private notes appear in normal lists/search | Private-note filtering and persisted flag | Test Home, categories, pinned, archived, search, insights, counts, links, and restored data |
| Exported backup discloses private content | User-facing disclosure and handling guidance | JSON is plaintext; anyone with file access can read it |
| Repeated password guessing | Persisted cooldown/backoff | Test restart, cooldown boundaries, max delay, clock changes, and recovery |
| Biometric prompt cancelled or fails | Authentication result must not grant access except on success | Exercise success, cancellation, failure, hardware unavailable, enrollment changes, and fallback |
| Local storage key lost/invalidated | Recovery path preserves original bytes | Data may be unrecoverable without a valid key; test migration and recovery on a test device |

## Trust boundaries

- **App UI boundary:** vault authentication, navigation state, editor visibility, filtering. This boundary can be bypassed by code/runtime compromise and is not a cryptographic boundary.
- **Local storage boundary:** encrypted payloads protected by Android Keystore while the key remains available and protected.
- **Export boundary:** user-selected JSON backup leaves the encrypted-storage boundary as plaintext. The recipient, destination provider, and subsequent copies are outside the app's control.
- **Operating-system boundary:** Android lifecycle, biometric APIs, screenshot restrictions, and Keystore are trusted platform services. A compromised OS is outside the guarantees of these app-level controls.

## Out of scope / not guaranteed

The current design does not claim protection against a rooted or otherwise compromised device, process instrumentation, a maliciously modified app, external-camera capture, plaintext backups, content deliberately shared/copied by the user, or recovery after irreversible loss of the Keystore key. Local encryption should not be described as protection for data after decryption in memory.

## Future cryptographic-isolation work

If separate cryptographic isolation is required, design it as a separate security project. At minimum, specify and review:

- independent vault key generation and protected key storage/wrapping;
- how the password and biometric credentials authorize key use without storing the raw password;
- authenticated encryption and integrity/error behavior;
- key rotation, password change, biometric enrollment changes, and recovery semantics;
- migration of existing private notes without losing or exposing data;
- explicit encrypted/plaintext backup and restore policy;
- failure-safe rollback, compatibility, tests, and a documented threat-model review.

Until this work is implemented and verified, product copy must continue to describe Private Vault as a UI gate over notes that use the app's general local-storage encryption.
