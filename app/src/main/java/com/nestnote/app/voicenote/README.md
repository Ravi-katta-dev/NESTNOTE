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

The inline voice player supports Compact, Full, Waveform, Minimal, and Transport layouts. Transport adds explicit 10-second rewind/forward controls alongside seeking. Settings provides a custom player-size control from 75% to 150% in 5% increments.
An Android media-style notification appears during active inline playback with the recording label, elapsed/total time, pause/resume, and stop controls. Lock-screen public content hides the recording label. On Android 13+, NESTNOTE requests notification permission contextually on first playback and remembers that it asked; if denied, playback still works and the user can enable notifications in system settings. Current playback remains foreground-only: the existing lifecycle safety rule stops audio when NESTNOTE leaves the foreground. These preferences affect presentation only; they do not resample or rewrite the encrypted audio.
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


## Follow-up decision: centralize policy before moving storage primitives

The repository now has regression coverage for:
- successful encrypted save/list/read;
- metadata-commit failure, verifying the newly written payload is removed;
- per-note bulk cleanup without deleting another note's recording; and
- recovery discovery of ciphertext whose metadata row is missing.

The recording pipeline intentionally continues to use `VoiceAttachmentStore` as its streaming encryption sink. The player also currently consumes the store abstraction for authenticated reads. Moving those classes by package rename before introducing and testing equivalent streaming interfaces would create a broad mechanical change with little safety benefit.

This sequencing follows the same established storage invariants documented by Android's `AtomicFile`: finish writing and sync before publishing a replacement, and provide explicit mutual exclusion because atomic replacement alone is not a lock. Reference: https://developer.android.com/reference/androidx/core/util/AtomicFile

Next step after CI: introduce a narrow streaming/payload interface only if it reduces direct coupling without buffering audio into memory or staging plaintext on disk. Keep current storage path, manifest schema, AES-GCM envelope, and Keystore alias stable. Do not merge until the exact-head CI result is green and device playback/recording checks pass.
