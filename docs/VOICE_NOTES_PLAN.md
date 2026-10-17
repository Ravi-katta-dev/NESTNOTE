# Voice Notes — Foundation to Production Plan

Status: **In progress — encrypted storage and delete/Undo safeguards merged; recorder, editor UX, backup policy, and device validation remain**  
Tracking issue: [#28](https://github.com/Ravi-katta-dev/NESTNOTE/issues/28)

## Progress snapshot (2026-10-17)

- [x] Encrypted app-private attachment storage merged in [PR #30](https://github.com/Ravi-katta-dev/NESTNOTE/pull/30) (merge commit `38a18f1e964df09446962cbc2a53e428a54749e7`).
- [x] Attachment deletion/orphan-cleanup helpers merged in [PR #31](https://github.com/Ravi-katta-dev/NESTNOTE/pull/31) (merge commit `8eeead7756916b6902d850ea918b26e956f4d555`). Orphan cleanup is a helper only; safe launch-time reconciliation is not wired in.
- [x] Existing note delete/Undo flow preserves attachments during Undo and removes them only after the note remains deleted in [PR #32](https://github.com/Ravi-katta-dev/NESTNOTE/pull/32) (merge commit `9350182c2bc442c1c82857909e5dfd73a72bad2b`).
- [x] Robolectric storage tests cover encryption round-trip, no plaintext stored, tamper/wrong-key rejection, invalid IDs, size bounds, write-failure cleanup, listing and deletion. PR #30 CI passed; PR #31 CI passed; PR #32 exact-head CI passed.
- [ ] Not yet implemented: recorder/player engine, microphone permission UX, editor controls, manifest/reconciliation integration, audio backup/export policy, accessibility/device matrix, release signing and production rollout.
- Device/emulator tests have **not** been run; CI/instrumentation compilation is not counted as device execution.

## Product contract for v1

Voice notes are user-initiated audio recordings attached to an existing NESTNOTE note. Users can record, pause/resume, stop and save, play back, rename, and delete recordings. Multiple recordings per note are supported. Recording is foreground-only; there is no account, network upload, background recording, transcription, or AI summary in v1.

## Phase 0 — Architecture and threat model

- Keep current note IDs, JSON shape, text-only notes, and legacy backup readers compatible.
- Use a stable attachment ID plus parent note ID. Keep audio bytes out of SharedPreferences and out of the note JSON.
- Define supported container/codec, maximum duration/file size, disk-space behavior, attachment count, and orphan cleanup policy before UI work.
- Use app-private files and a dedicated versioned AES-GCM envelope backed by Android Keystore. Each file gets a fresh random 96-bit IV, a 128-bit authentication tag, and AAD binding the app, purpose, format version, and attachment identity.
- Avoid plaintext staging where the platform permits. If the platform recorder requires a temporary plaintext file, keep it app-private, tightly scoped, remove it on every exit path, and document/test the exposure window before accepting the design.
- Define what happens when a key is missing/invalidated, ciphertext is corrupted, metadata is stale, or a save is interrupted. Fail closed; preserve recoverable bytes and never silently overwrite.
- Define backup/restore before coding. Do not silently include encrypted blobs in existing JSON backup or accidentally export plaintext. Any portable audio export must be explicit and user initiated.

## Phase 1 — Storage foundation

- [x] Implement an audio attachment repository independent of Compose (`VoiceAttachmentStore`).
- [x] Add atomic temp-to-final encrypted commits, identity validation, bounded reads/writes, and safe delete semantics.
- [ ] Store a durable attachment manifest separately from audio bytes. Note-delete cleanup after the Undo window is merged; manifest integration remains pending.
- [x] Add a tested orphan-directory cleanup helper. [ ] Wire safe launch-time reconciliation and orphaned-manifest handling.
- Inject key, filesystem and ID/time dependencies where practical to make failure tests deterministic.

**Gate:** repository tests prove encrypted-at-rest storage, round-trip, wrong-key/tamper rejection, interrupted write behavior, cleanup, and safe metadata validation.

## Phase 2 — Recording and playback engine

- Introduce recorder/player interfaces and a small explicit state machine: Idle, RequestingPermission, Recording, Paused, Finalizing, Playing, Error.
- Use Android platform APIs behind the interfaces; no network or cloud dependencies.
- Handle permission denial, unavailable/busy microphone, codec initialization errors, storage exhaustion, audio focus/interruption, and activity lifecycle events.
- Bound recording duration and file size. Cancel/discard and stop/save must be distinct operations.

**Gate:** deterministic state-transition tests plus Android instrumented tests for actual recording and playback on a real emulator/device.

## Phase 3 — Editor experience

- Add a contextual voice-note action to the existing editor rather than creating a parallel note editor.
- Show live elapsed time and clear record/pause/resume/stop/discard controls.
- Request RECORD_AUDIO only after the user explicitly starts recording.
- List attached recordings with duration, playback/progress, rename and delete confirmation.
- Provide accessible labels and announcements, large touch targets, light/dark theme support, large-font layouts, and clear recoverable error messages.
- Preserve current Save/Cancel and stale-draft protections; attachment operations must not unexpectedly save or discard unrelated text edits.

**Gate:** all controls functional; no hidden recording; no unsaved note data loss during interruption or navigation.

## Phase 4 — Data lifecycle and backup

- Verify multiple recordings per note, rename, note deletion, restore merge, duplicate metadata, and missing/orphaned files.
- Keep existing text-only notes and legacy JSON backup behavior unchanged.
- Decide and implement explicit audio backup/import behavior with size/schema limits and clear security warnings. Until this gate is complete, audio is excluded from the existing JSON backup format.
- Test upgrade from existing installs and repeated launches.

**Gate:** restore and migration tests pass without partial mutation, silent loss, or plaintext audio leakage.

## Phase 5 — Test and harden

### Automated
- Unit tests for IDs, metadata, limits, encryption envelope, atomic commits, cleanup, state machine, and failure mapping.
- Robolectric tests where platform behavior is faithfully represented.
- Instrumented tests for microphone permission, real recorder/player behavior, interruption, and lifecycle.
- Regression tests for note save/cancel, note deletion, legacy notes, backup/restore, and current encrypted text storage.
- Tamper tests for truncated/modified ciphertext and wrong/missing key; injected failures for disk full and I/O errors.

### Manual device matrix
- Permission allowed/denied/revoked and microphone occupied.
- Start/pause/resume/stop/discard/playback, app background/foreground, rotation, calls/audio-focus interruption, process kill, reboot and relaunch.
- Low storage, max duration, empty/short recording, corrupted data, key invalidation and recovery.
- TalkBack, large font/display scaling, light/dark mode, narrow portrait, landscape/tablet, and keyboard/insets.
- Upgrade an existing install with text notes and existing backups; install the exact minified release APK.

**Evidence rule:** CI compilation of instrumentation tests is not the same as executing them. Record device/emulator model, Android version, tested commit, outcome and known limitations in WORKLOG.md.

## Phase 6 — Release and production

- Update README/privacy/data handling docs, RELEASE_CHECKLIST.md, WORKLOG.md, and release notes.
- Require passing unit tests, debug build, minified release build, and instrumentation-test compilation on the exact PR head.
- Execute and record manual/device checks; do not mark unrun checks complete.
- Verify production signing certificate against the recorded fingerprint and install/test the exact candidate APK.
- Roll out in a tagged release with a concise privacy statement: recordings stay on-device, are encrypted at rest, and are not uploaded; describe key-loss and backup limitations.
- If a post-release data-loss/security issue appears, stop rollout and prepare a tested recovery fix.

## Release acceptance criteria

- Explicit user action and permission are required for every recording.
- Audio is encrypted at rest; integrity/key errors fail closed and preserve recoverable data.
- No cloud upload, background recording, or transcription in v1.
- Existing notes, note IDs, text encryption and legacy JSON backup compatibility remain intact.
- All new controls are accessible and functional; errors are understandable and recoverable.
- Exact-head CI and device validation evidence are recorded before production distribution.

## Out of scope for v1

Speech-to-text, AI summaries, cloud sync, automatic sharing, background recording, and cross-device synchronization. These require separate privacy, offline capability, consent, and product decisions.
