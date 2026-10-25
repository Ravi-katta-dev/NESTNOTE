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

Baseline sizes are approximate counts from the initial source audit. Current counts below were measured from the PR branch source at this checkpoint.

## P0 execution order

1. **SettingsScreen first:** move settings-only constants and presentation sections to focused files without changing preference keys, defaults, ordering, or callback semantics.
2. **SettingsScreen sections:** extract appearance, layout, voice-player settings, privacy, and backup/restore one at a time. Keep `SettingsScreen` as the composition/navigation owner.

## PR #99 progress

- [x] Extract stable item keys, sort options, and pure settings-search matching into `SettingsScreenModel.kt`.
- [x] Extract shared settings presentation components into `SettingsComponents.kt`.
- [x] Extract the voice-player settings dialog into `VoicePlayerSettingsDialog.kt`.
- [x] Extract encrypted backup/restore UI into `BackupRestoreScreen.kt`.
- [x] Extract the appearance section into `SettingsAppearanceSection.kt`.
- [x] Extract the dashboard layout/density section into `SettingsLayoutSection.kt`.
- [x] Extract notes/search behavior settings into `SettingsBehaviorSection.kt` with category draft changes passed through callbacks.
- [x] Extract editor settings into `SettingsEditorSection.kt`; preference writes remain owned by the parent screen.
- [x] Extract voice-player preferences/preview into `SettingsVoicePlayerSection.kt`.
- [x] Extract privacy settings into `SettingsPrivacySection.kt`.
- [x] Extract data/archive entry and storage-backend selection into `SettingsDataSection.kt`; backend change requests still flow through the parent's confirmation state.
- [x] Review reset and dialog coordination: the parent `SettingsScreen` retains reset-dialog ownership because confirmation coordinates the external reset callback, local editor defaults, custom-font file cleanup, and preference removal. Storage-backend confirmation remains separately owned by the parent and dismisses before scheduling the callback.
- [x] Remove unused imports from extracted settings screens and shared components; keep each composable's callback parameters explicit so the parent remains the state owner.
- [x] Extract the independent boot landing screen from `NestNoteApp.kt` into `BootLandingScreen.kt`; keep the existing boot-sound preference key and user toggle behavior.
- [x] Add targeted regression coverage for unique LazyColumn item keys and stable sort choices alongside the existing settings-search tests.
- [x] Re-check obvious stale imports in the extracted files and add pure JVM regression coverage for stable settings keys/sort choices. Full build/test verification remains pending.

## Measured progress on PR #99 branch

Counts are current source line counts, not estimates; line counts may vary slightly with trailing newlines.

| File | Initial audit baseline | Current branch | Change | Status |
|---|---:|---:|---:|---|
| `SettingsScreen.kt` | 2,652 | 551 | -2,101 lines (-79.2%) | Section extraction pass complete; parent remains composition/state owner |
| `NestNoteApp.kt` | ~2,150 | 2,086 | -64 lines (~3.0%) | Boot landing UI extracted; next P0 target |

The PR compare currently reports `SettingsScreen.kt` at 2,193 deletions and 92 additions, which is a net reduction of 2,101 lines. Extracted settings sections/dialogs/components are now in focused files; no claim is made that build or tests pass until they are run.

### Next-file boundary audit: `NestNoteApp.kt`

- Current size: 2,086 lines on this branch; the initial audit estimated ~2,150 lines.
- Existing boot landing UI has already moved to `BootLandingScreen.kt` (the app-root diff removes 65 lines and adds one replacement line).
- Identified stateful action clusters inside `NestNoteApp`: persistence (`persist` / `requestPersist`, around lines 372–464), ordinary-note encryption (`encryptOrdinaryNote`, `decryptOrdinaryNote`, and selection actions, around lines 465–539), deletion/sort/reset preference actions (around lines 540–590), and Markdown import orchestration (`importMarkdownUris`, around line 591).
- These clusters share composition-owned state and callbacks. Extracting them wholesale before a baseline would risk changing save ordering, error handling, draft recovery, encryption verification, or UI state. The next implementation step is therefore to establish the existing build/test baseline, then extract one narrow, testable helper at a time while leaving orchestration in `NestNoteApp`.

3. **NestNoteApp (next file):** prioritize a read-only boundary audit first. Do not move persistence, encryption, draft-recovery, or linked-note navigation logic until a test baseline is available. Keep `NestNoteApp` as the composition root.
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

### Markdown import extraction progress

- [x] Extracted the first-heading/title/body transformation from `NestNoteApp.importMarkdownUris()` into the pure `MarkdownNoteParser.kt` helper. URI access, display-name lookup, note-ID allocation, category assignment, persistence and user feedback remain in the app root.
- [x] Added JVM regression cases for first-heading extraction, subsequent headings, fallback file names, blank fallback titles, whitespace trimming and empty documents in `MarkdownNoteParserTest.kt`.
- [ ] Run `MarkdownNoteParserTest` and the Android app compile/test tasks against the latest PR head; GitHub source edits alone do not establish passing verification.
- Next: check the exact diff and CI results, then choose the next small extraction based on tests rather than moving persistence or encryption wholesale.


### Ordinary-note selection extraction progress

- [x] Extracted selection eligibility and ordered replacement application into the pure `OrdinaryNoteSelection.kt` helper.
- [x] Kept cryptographic encrypt/decrypt calls, authenticated round-trip verification, failure handling, persistence, and success feedback in `NestNoteApp`.
- [x] Added JVM regression coverage for private-note exclusion, already-encrypted exclusion, selected-note ordering, replacement ordering, and empty replacements.
- [ ] Confirm the latest Android CI run includes this exact commit and passes; the previously reported successful run predates this extraction unless its SHA matches the new head.
- Next: wait for/inspect CI on the updated head before selecting another extraction.
