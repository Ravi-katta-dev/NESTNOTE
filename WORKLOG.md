# NESTNOTE Work Log

Last updated: 2026-10-11

> Maintenance rule: update this log in the same commit as every implementation commit, including the commit SHA, summary, and validation status. If a separate commit is unavoidable, update the log immediately afterward.  
Repository: [Ravi-katta-dev/NESTNOTE](https://github.com/Ravi-katta-dev/NESTNOTE)

This activity record is assembled from visible repository commits, pull requests, and GitHub Actions runs. Statuses reflect the latest checks observed when written; use the linked run for live status.

## Current snapshot
- Native Android app using Kotlin, Jetpack Compose, and Material 3.
- Target SDK Android 16 / API 36; minimum SDK 26.
- `main` includes merged PRs #1, #2, and #3.
- Phase 4 is in progress on `feature/phase-4-note-taking`, tracked by draft PR #4. **PR #4 is open and must not be merged without explicit approval.**
- Existing note persistence uses SharedPreferences / NoteStore; Phase 2 preserves its format.
- Emulator UI workflow is manual-only by project decision and is not a required CI gate.
- No physical-device testing is claimed.

## Pull requests
| PR | Title | Status | Scope |
|---|---|---|---|
| [#1](https://github.com/Ravi-katta-dev/NESTNOTE/pull/1) | feat: Material 3 personalization and flexible note creation | **Merged** 2026-10-11 | Phase 1 dashboard modernization and personalization. |
| [#2](https://github.com/Ravi-katta-dev/NESTNOTE/pull/2) | feat: resilient inline note editor | **Merged** 2026-10-11 | Unified create/edit experience and draft safety. |
| [#3](https://github.com/Ravi-katta-dev/NESTNOTE/pull/3) | Expand settings | **Merged** 2026-10-11 | Theme, colors, layout, sorting, and note display preferences. |
| [#4](https://github.com/Ravi-katta-dev/NESTNOTE/pull/4) | feat: modernize everyday note-taking experience | **Open draft** | Phase 4 editor refinement, sort options, and timestamp reliability. |

## Work completed
### Foundation and delivery automation
- Created the native Kotlin / Jetpack Compose Android app.
- Added Android CI/CD and unit/instrumentation-test scaffolding.
- Fixed SDK setup and Compose UI-test dependency issues in CI.
- Added automated APK prereleases from main and corrected the release artifact path.
- Kept required CI focused on API 36; emulator UI workflow was made manually triggered after emulator reliability issues.

### Phase 1 — home screen (merged)
- Modernized the dashboard toward a calm, compact note-taking layout.
- Added compact/comfortable density preference and adaptive overview.
- Added note sort options and soft pastel / wallpaper-adaptive note colors.
- Improved category-chip styling, note-card color rendering in list layout, and create-note action.
- Added tests for dashboard/search/sorting, categories/empty state, note-color choices, settings/layout preferences, and related interactions.
- PR: [#1](https://github.com/Ravi-katta-dev/NESTNOTE/pull/1).

### Phase 2 — editor resilience (in progress)
Branch: `feature/phase-2-note-editor`; PR: [#2](https://github.com/Ravi-katta-dev/NESTNOTE/pull/2).
- Changed title, body, category, color, and reminder fields to Compose saveable state keyed to note ID.
- Added discard confirmation for explicit editor close and Cancel when meaningful unsaved changes exist.
- Added an instrumentation test for discard confirmation.
- Preserved NoteStore schema and local persistence format.
- Remaining: restore editor visibility/selection after activity recreation; route system/back and sheet dismissal through the same unsaved-change guard; review create/edit consistency and finish CI validation.

## Validation and CI records
- [Android CI — Phase 1 merged head](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38129704278): success.
- [Android Release — Phase 1 merged head](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38129704259): success.
- [Android CI — earlier Phase 1 commit](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38129329854): success.
- [Android CI — earlier Phase 2 commit](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38129805061): cancelled after a newer commit was pushed.
- [Android CI — current Phase 2 head f27e190](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38129835034): **in progress** when written. Do not record it as passed until complete.
- [Android UI Tests (API 36)](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38129320213): separate/manual workflow; not a required CI gate under the current project decision.

## Commit history
Visible commit history, newest first (short SHA, UTC date, subject; merge commits included).
| Commit | Date (UTC) | Subject |
|---|---|---|
| `a7532b3` | 2026-10-11 | Merge PR #1 — Material 3 personalization and flexible note creation |
| `1d3c600` | 2026-10-11 | test: remove weak dashboard assertion |
| `1f2a5dd` | 2026-10-11 | ci: keep emulator UI tests manually triggered |
| `af7f825` | 2026-10-11 | test: cover dashboard categories and empty state |
| `d85a7b5` | 2026-10-11 | ci: run API 36 UI tests on pull requests |
| `994230e` | 2026-10-11 | fix: pass selected note color style in list layout |
| `8faefe7` | 2026-10-11 | test: cover selectable adaptive and pastel note colors |
| `54b4615` | 2026-10-11 | ui: use prominent round create-note action |
| `823b27d` | 2026-10-11 | feat: add selectable pastel and wallpaper-adaptive note colors |
| `8861a7e` | 2026-10-11 | test: cover reference home search and sorting |
| `763cd46` | 2026-10-11 | feat: add working note sort options and soft note colors |
| `1767d6d` | 2026-10-11 | ui: match calm compact NestNote home design |
| `bcc3a83` | 2026-10-11 | test: scroll through settings in UI regression coverage |
| `f22f464` | 2026-10-11 | test: expose settings scroll container for UI automation |
| `6fc92d0` | 2026-10-11 | ci: compile Android UI tests in pull request validation |
| `37a7ac1` | 2026-10-11 | test: cover modern home overview and saved layout preference |
| `fc29813` | 2026-10-11 | ui: modernize home with adaptive overview and note section |
| `2c02926` | 2026-10-11 | ui: align note category chips with Material You colors |
| `61b32e8` | 2026-10-11 | fix: remove duplicate dashboard density state |
| `a449cea` | 2026-10-11 | feat: add compact and comfortable dashboard density |
| `c1aca0c` | 2026-10-11 | feat: add compact and comfortable dashboard density |
| `b73b2ff` | 2026-10-11 | test: target the accessible search action |
| `bfcdf05` | 2026-10-11 | test: cover editor preferences and Material 3 note creation |
| `fa204d0` | 2026-10-11 | feat: add Material 3 personalization and note editor options |
| `91f712a` | 2026-10-11 | Add manually triggered API 36 UI test workflow |
| `c3d0104` | 2026-10-11 | Keep required CI API 36-only and independent of emulator boot |
| `5055efa` | 2026-10-11 | Stabilize Android UI test emulator boot |
| `8e90fe4` | 2026-10-11 | Fix Compose UI test assertion API |
| `e0a02dd` | 2026-10-11 | Fix automatic release APK artifact path |
| `afcc172` | 2026-10-11 | Enable automatic APK prereleases from main |
| `d55e566` | 2026-10-11 | Fix Compose UI test dependency version resolution |
| `f50c31f` | 2026-10-11 | Fix Android release SDK setup by dropping obsolete tools package |
| `ac4a8ea` | 2026-10-11 | Fix Android CI SDK setup by dropping obsolete tools package |
| `03fee55` | 2026-10-11 | Add temporary CI APK signing for validation |
| `34e4fa3` | 2026-10-11 | Add Android CI/CD and unit/E2E tests |
| `d4a5f26` | 2026-10-11 | Add native Android 16 app with Kotlin and Jetpack Compose |
| `129b668` | 2026-10-11 | Initial NESTNOTE application commit |

## Recent Phase 2 commits
| `be7da73` | Refined the launcher vector to include its own rounded-square deep-teal background, avoiding a full-bleed square when used as a legacy launcher drawable. The generated reference image was used as visual direction; vector remains a hand-built approximation, not a pixel-exact embedded PNG. | CI pending. |

| `9c1b6b2` | Added a custom NESTNOTE launcher vector drawable inspired by the generated icon concept (deep teal tile, layered pastel notes, folded golden corner, writing lines, N mark, pencil). | Pending CI. |
| `dada8ae` | Applied `@drawable/nestnote_launcher` as the Android application icon in the manifest. | Pending CI. |

| `ca8a723` | Fixed note-card foreground contrast: pastel cards now use stable dark primary text, secondary text, and icons rather than wallpaper-derived yellow/bright theme colors; adaptive cards still use Material 3 foreground colors. | CI pending; verify build before merge. |
| Commit | Change | Validation |
|---|---|---|
| `e6db24a` | Save editor visibility and selected note ID across activity recreation; updated release workflow to use a rolling `latest` prerelease titled `NESTNOTE Latest Build` in the same branch commit `c47242e`. | Android CI failed at Kotlin compile on `MainActivity.kt:338` due to one remaining assignment to the now-derived `editorNote`; corrected in follow-up commit (see next row). |
| `124873a` | Fixed remaining note-card open handler to update `editorNoteId`. | CI failed due to conflicting/mistyped derived editor-note declaration; fixed in the following commit. |
| `def342a` | Corrected derived selected-note lookup to `notes.firstOrNull { it.id == editorNoteId }`. | CI re-run pending; current head is not yet validated. |

| `86469e6` | Added a parent-level unsaved-draft guard for system/back and sheet/dialog dismissal requests, while keeping editor Save/confirmed Cancel paths able to close directly. | CI pending; behavior needs automated validation. |

## Maintenance
Update this file when a PR merges, an implementation stage completes, or a material CI decision/status changes. Record factual outcomes only; distinguish planned, in-progress, and verified work.


## Phase 3 — Settings expansion

| Commit | Work | Validation |
|---|---|---|
| `63fb556` | Added persistent preferences for default sort order, note preview visibility, and date/reminder visibility. | CI pending. |
| `360535f` | Added the Note behavior group to Settings with controls for sorting, previews, and timestamps/reminders. | CI pending. |
| `a2e9b0a` | Connected note-card rendering to the preview and timestamp preferences. | CI pending. |

## Validation policy

- Normal Android CI is required for each meaningful increment.
- Emulator UI workflow remains manual-only; visual verification must be recorded as manual when performed.
- Do not mark Phase 3 complete until preference persistence and existing note/editor flows are verified.
- Preserve the existing NoteStore format and user notes.

## Next steps

1. Run Android CI and fix any compile/test failures.
2. Add tests for preference persistence and preview/timestamp toggles.
3. Add default note category and starting category behavior.
4. Expand settings into Appearance, Layout, Note behavior, and Data & privacy groups without rewriting note storage.

| `dc9d246` | Fixed the list-layout NoteCard call so preview and timestamp preferences apply consistently in both grid and list modes. | CI pending; caught during code review before CI. |


## Phase 3 plan refinement — 2026-10-11

- Reviewed Android Developers Settings design guidance and Google Keep's note settings patterns before expanding the feature set.
- Plan: audit and stabilize existing controls; then Appearance, Layout, Note behavior, and persistence/testing in small validated increments.
- Keep settings grouped and scannable; every control must affect the UI and persist. Preserve NoteStore and only consider DataStore after auditing existing preference storage and migration safety.
- `dc9d246`: fixed preview/timestamp settings propagation in list layout. `b1dfcb6`: updated this worklog. CI run `38135347194` is in progress for the latest branch head at time of check.


### Follow-up implementation — 2026-10-11

| Commit | Work | Validation |
|---|---|---|
| `3db8c07` | Made the dashboard sort control label reflect the active sort mode (Recently edited vs Title A–Z), so the visible selection stays in sync with the persistent sort preference and menu selection. | Code change committed; CI intentionally not polled per user instruction. |

| `a0431be` | Added a persistent default note category setting and applied it to newly created notes in the primary editor; existing notes retain their saved category. | Code change committed; CI intentionally not polled per user instruction. |

| `838277e` | Fixed the Quick dialog editor to honor the saved default note category for new notes, matching the sheet and full-screen editor behavior; editing an existing note still preserves its category. | Code change committed; checks not polled per user instruction. |

| `39b87ca` | Fixed dashboard sort menu persistence: selecting Recently edited or Title A–Z now saves the preference immediately, matching Settings and surviving app recreation. | Source-level review; Android CI for this exact commit not yet observed. |

### Phase 3 final review — 2026-10-11


| `39b87ca` | Dashboard sort menu now writes its selection to preferences immediately. | Source reviewed; CI for this change pending. |
| `806ab2e` | Clarified Settings grouping with Layout, Layout density, Note editor, and Data & privacy; privacy copy explicitly states cloud sync and built-in export/import are not currently provided. | Source reviewed; CI for this change pending. |
| `8c3cba0` | Extended the dashboard sorting UI test to assert the selected sort mode is persisted. | Test added; UI workflow remains manual-only by project decision. |


## Phase 4 — Everyday note-taking (in progress)

- PR #3 (Settings expansion) was merged into `main`; Phase 4 work starts from the updated `main` on `feature/phase-4-note-taking`.
- `075db52`: first UI modernization increment for the note editor. Replaced the decorative text close mark with an accessible Material icon, updated title/body fields to use rounded Material 3 tonal surfaces and clearer writing placeholders, enlarged the body writing area, and made selected note colors more visible with an outlined selection state.
- Validation: Android CI run [#38136566682](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38136566682) passed for the PR head `71737e1`. Emulator UI workflow remains manual-only. No emulator or physical-device validation is claimed.
- Next increments: review remaining editor actions and reminder affordances; then tackle sorting/created timestamps, note colors/categories, archive/undo, and reminder reliability in separate tested steps.


### Phase 4 visual direction

The following reference images supplied for this project are inspiration only; they are not NESTNOTE screenshots or assets. Implementation should translate the visual direction into native Jetpack Compose / Material 3 patterns rather than copying another app.

1. [Android note-taking app inspiration — modern note cards and dashboard](https://images.openai.com/static-rsc-4/zPyVdHCYj04ct21srgrGTcWu_17kCukCcLmEEGjEC8BnOsuYqI_9G4maUXl6Ia60eRonulOwA0uukYCIjW1zGTLG-SG-LL7xMGMBZzBaxwWiVzG2JTS3EPwl2aifome1AsdR_eFxjErd0ipI77gsfueLC6m8TlUgjoydJK0WcZPTz6LeQw6Rv5c7cHCpnF9r?purpose=fullsize)
2. [Minimal note-taking UI inspiration — clean hierarchy and writing space](https://images.openai.com/static-rsc-4/toLmSKmVIwCJpy33vaFcGMEMLl5PfFobOvb7V5sE8rzQO68T8pqatmExHE64MiczCHrfO5OFwQ7pEqxsU5IdhSLC9jmOKX1wgu6X_D_UuH4iMTS_M_ZLw8qF45PI_Z9k-9ZePKbItX9huFMmRjuwyIRsNTTqIBxDCQj_71TxGObzBJf70d-UrjIMYjoYX3ug?purpose=fullsize)
3. [Google Keep formatting inspiration — readable typography and note controls](https://images.openai.com/static-rsc-4/Hfu_zLkOAJMCUEC24TajWwplqoK-sqrfB-Z2eP9_2CBy373ZUWP_9r-lxGYe8b-HcQiPBTZkGufQohXuOxRYIH5vbD9xbk03NXvZHZyKnYyW08wEA9OQw2m5WE5SwE3yv5erK7sETVFQH5Z4vmS3JhXlhCCWUUMTSGWgt5CWnpn-vgSLCgzINJUAIYxuuEx3?purpose=fullsize)

Design principles:
- Current Android / Material 3 feel, never legacy Android 5 styling.
- Calm compact dashboard, generous readable editor canvas, rounded tonal fields and restrained elevation.
- Clear title/body hierarchy, comfortable keyboard editing, accessible icon labels and touch targets.
- Light/dark theme parity, dynamic color support, readable pastel-card contrast, and responsive insets.
- Preserve existing note data and editor behavior while modernizing; visual polish must not regress save, cancel, or draft restoration.


### Phase 4 implementation increment — created-date sorting

- `7308052`: Added a backward-compatible `created` timestamp to the existing JSON note record. Older records without the field fall back to their saved ID; editing a note preserves its creation timestamp.
- `b77c543`: Added Recently created sorting while retaining pinned notes at the top and preserving Recently edited / Title A–Z. The chosen sort mode persists using the existing preferences key.
- `b677784`: Extended the sorting UI test to check that Recently created is available and persists as `CREATED`.
- Validation: Android CI passed for the earlier editor UI increment at run [#38136566682](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38136566682). The current changes have triggered a new CI run; do not mark them verified until that run finishes. Emulator UI workflow remains manual-only.


### Phase 4 implementation increment — creation timestamp regression coverage

- Added unit tests proving older saved JSON without a `created` field loads with a deterministic ID-based fallback, and that the original creation timestamp remains stable when an edited note is saved with a newer `updated` value.
- Validation: Android CI run [#38137019018](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38137019018) passed for the prior Phase 4 head. The new regression tests are committed here and require a fresh CI result. Emulator UI workflow remains manual-only; no device testing is claimed.


## Phase 4 current progress
Branch: `feature/phase-4-note-taking`; draft PR: [#4](https://github.com/Ravi-katta-dev/NESTNOTE/pull/4). Reference images are recorded in the PR description and this branch's earlier Phase 4 entries as inspiration, not app screenshots.

- Modernized the editor's Material 3 surfaces, title/body hierarchy, color swatches, and close affordance.
- Added persisted creation timestamps and Recently created sorting, preserving pinned-first ordering and falling back safely for older stored notes.
- Added unit regression tests for legacy JSON without `created` and for preserving creation time while edit time advances.
- Latest confirmed Android CI: [run #38137412765](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38137412765) — **success** on commit `90081fe`.
- Next: undo deletion, make reminder status explicit (without implying notifications are scheduled), then continue pin/archive behavior and test coverage. Emulator UI workflow remains manual-only; no physical-device testing is claimed.

### Commit `90081fe` — test: cover legacy and stable note creation timestamps
- Added two NoteStore regression tests and updated the roadmap/worklog status record in the follow-up documentation commit.
- Validation: [Android CI run #38137412765](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38137412765) succeeded for this code head.


### Phase 4 increment — undo deletion and reminder clarity

- Added a Material 3 snackbar with an Undo action to note deletion from dashboard cards and the editor. Undo restores the deleted note to the persisted collection and retains its original metadata.
- Added explicit reminder copy clarifying that the selected date/time is saved as note metadata and Android notifications are not enabled yet.
- Initial CI failure was a Kotlin compile error caused by removing the old `toastMessage` state while leaving its save-toast references. Reused the new shared snackbar host for save feedback and renamed the editor delete callback to avoid shadowing the shared delete helper.
- Validation: CI run [#38138296608](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38138296608) found a remaining function-reference mismatch in the full-screen editor (`onDelete = deleteNote`). Follow-up fix uses the same guarded `editorDeleteAction` callback as the sheet editor. Re-run CI before treating this increment as verified. Emulator UI workflow remains manual-only; no physical-device testing is claimed.


### Phase 4 increment — persistent archive state

- Added a backward-compatible `archived` field to the existing note JSON format; legacy records default to active.
- Added an Archived navigation destination and an Archive action on note cards. Archiving unpins the note and removes it from active home/category/reminder views without deleting its content.
- Fixed the list-layout delete action to use the shared Undo snackbar helper, matching grid and editor deletion.
- Added NoteStore tests for archived-state round-trip and legacy default behavior.
- Validation: prior editor callback fix passed Android CI run [#38138686605](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38138686605). This archive increment needs its own Android CI run; emulator UI tests remain manual-only.
