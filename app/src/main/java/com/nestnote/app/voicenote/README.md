# Voice-note backend

This package is the backend boundary for voice-note operations. UI code should depend on
`VoiceNoteRepository` rather than coordinating encrypted payload storage and metadata directly.

## Current storage components

- `VoiceAttachmentStore` (currently in the parent `com.nestnote.app` package): encrypted audio
  payloads, AES-GCM authentication, note/attachment identity binding, size limits, atomic file
  commit, and attachment copy/delete operations.
- `VoiceAttachmentManifest` (currently in the parent package): versioned metadata index, atomic
  writes, and conservative reconciliation.
- `VoiceNoteRepository` (this package): coordinates save/read/delete/list/reconciliation and
  exposes a single backend API for new callers.

Audio payload encryption uses its own Android Keystore alias and is separate from both ordinary
note encryption and Private Vault encryption. A note's encrypted text payload does not implicitly
encrypt or include its audio attachments.

## Voice-player presentation

The inline voice player supports four saved presentation styles: Compact, Full, Waveform, and
Minimal. Settings also provides a custom player-size control from 75% to 150% in 5% increments.
These preferences affect presentation only; they do not resample or rewrite the encrypted audio.
Existing installs retain their previous Compact/Full choice when no newer style preference exists.

## Follow-up plan

1. Migrate editor and note lifecycle call sites to `VoiceNoteRepository` in a dedicated change;
   preserve draft commit/discard behavior and ensure note deletion is durably committed before
   attachment cleanup.
2. Move `VoiceAttachmentStore` and `VoiceAttachmentManifest` into this package only after all
   imports, tests, and any test-only constructors have been migrated together. Keep the storage
   format and Keystore alias stable; a source-package refactor must not trigger data migration.
3. Add repository-level tests for metadata commit failure, deletion failure, corrupted ciphertext,
   missing Keystore key, process termination during save, duplicate IDs, and reconciliation that
   never deletes unindexed ciphertext.
4. Review ordinary-note encryption and backup/restore UX so voice attachments are clearly
   treated as separate encrypted files and never silently represented as included in portable note
   backups.
5. Run Android CI and device tests before treating the refactor as release-ready.

## Safety invariants

- Never write plaintext audio to disk.
- Never return plaintext until AES-GCM authentication succeeds.
- Do not delete unindexed ciphertext during recovery; metadata loss must not destroy recordings.
- Do not recreate a missing decryption key for existing ciphertext.
- Do not share encryption keys between voice notes, ordinary notes, and Private Vault.
