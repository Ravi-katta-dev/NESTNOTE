# Large-file refactoring audit and execution plan

## Goal

Reduce high-risk source-file concentration in NESTNOTE through small, behavior-preserving extractions. File size is a signal, not a goal by itself. Do not mix this work with feature changes, storage migrations, or encryption redesign.

## Audit ranking

The audit identified these primary candidates on `main`:

| Priority | File | Approx. lines | First responsibility boundaries |
|---|---|---:|---|
| P0 | `NestNoteApp.kt` | 2,150 | App state, note actions, persistence coordination, ordinary-note encryption actions, Markdown import orchestration |
| P0 | `SettingsScreen.kt` | 2,652 | Settings composition, appearance, voice-player preferences, privacy settings, backup/restore, shared controls |
| P1 | `VoiceNoteAudio.kt` | 1,097 | Recording lifecycle, audio focus, WAV/PCM decoder, importer UI, recording controls |
| P1 | `NoteEditor.kt` | 1,034 | Editor state/undo, actions, draft recovery, save coordination |
| P2 | `NoteEditorSections.kt` | 924 | Editor top bar, bottom bar, body and independent presentation sections |
| P2 | `InlineVoicePlayer.kt` | 726 | Playback lifecycle, notification/audio focus, layout-specific UI |
| P3 | `NoteText.kt` | 602 | Text utilities grouped by domain |
| P3 | `PrivateVault.kt` | 469 | Vault security policy vs dialogs/biometric UI |
| P3 | `NoteStore.kt` | 450 | Backend selection, serialization and validation/merge helpers |

Approximate sizes are from the source audit and should be remeasured when each extraction begins.

## P0 execution order

1. **SettingsScreen first:** move settings-only constants and presentation sections to focused files without changing preference keys, defaults, ordering, or callback semantics.
2. **SettingsScreen sections:** extract appearance, voice-player settings, privacy, and backup/restore one at a time. Keep `SettingsScreen` as the composition/navigation owner.
3. **NestNoteApp:** after a test baseline, extract note actions and coordinators behind existing repository APIs. Keep the app root as the composition root.
4. Verify each extraction with Android CI and targeted regression tests before continuing.

## Safety constraints

- Preserve stable LazyColumn keys, settings preference keys/defaults and sort ordering.
- Preserve save ordering, draft recovery, cancellation and error handling.
- Keep encryption and Keystore operations behind existing storage/security APIs.
- Do not duplicate sources of truth for UI or playback state.
- Do not introduce plaintext staging files or parallel persistence paths.
- Do not claim performance improvements without profiling.

## Acceptance checklist

- [ ] Build and existing tests pass at the exact PR head.
- [ ] Settings sections preserve existing preference keys/defaults and ordering.
- [ ] Note save, encryption, draft recovery and import flows remain behavior-compatible.
- [ ] UI smoke tests cover settings navigation and editor interaction.
- [ ] Each extraction remains a small, reviewable change.
