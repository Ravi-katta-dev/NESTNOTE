# NESTNOTE P1 quality and regression audit

Base: `main` @ f5d0502. PR #10 (`feature/phase-7-dashboard-ux`) compared by diff only; its head was not built.
Evidence levels: **Confirmed** = reproduced by running code. **Source-confirmed** = read in source, not executed. **Unverified** = risk only.

## Fixed in this branch (Confirmed, reproduced, regression-tested)

| # | Journey | Severity | Defect | Fix |
|---|---|---|---|---|
| F1 | Editing | Medium | Indented list lines (`  ☐ x`) count as checklist items in the view and progress, but the toolbar turns them into `☐   ☐ x`; bullet toggle stacks the same way; Enter does not continue the list. | `NoteText` prefix handling is indentation-aware; Enter keeps indentation. |
| F2 | Reminders | Low | "Later today" yields tomorrow's date from 21:00 onward. | Added `NoteText.laterTodayOrNull`. **UI not wired** (needs `MainActivity.kt`, see S1). |

## Open, needs `MainActivity.kt` (overlaps PR #10, not changed here; re-verify against PR #10 head first)

| # | Journey | Severity | Evidence | Proposed fix / test |
|---|---|---|---|---|
| S1 | Reminders | Low | `MainActivity.kt` ~L1104 always shows "Later today". | Hide the item when `laterTodayOrNull(now) == null`; UI test at a fixed clock needs an injectable clock. |
| S2 | Delete / Undo | Medium | `deleteNote` ignores the `persist()` result (~L193): a failed save or recovery-lock still shows "Note deleted" + Undo and closes the editor. | Only show snackbar/close on `persist(...) == true`. Instrumented test with a failing store. |
| S3 | Search | Low | Search runs on `title + " " + body + " " + category` (~L270): a query with a space can match across fields ("plan 1." matches title "Project Plan" + body "1. Finalize…"). Query is not trimmed. | Match each field separately on a trimmed query; extract a pure `matchesQuery` and unit-test it. |
| S4 | Empty states | Low | Single "A fresh page awaits / Create a note" for no-results, empty Archive and empty Reminders (~L463). PR #10 has a search-specific commit (7fcb415): verify. | Per-view copy; clear-search action. |
| S5 | Archive | Low | Archive menu item uses the Delete icon, Restore uses Home (~L911). | Archive/Unarchive icons. |
| S6 | Archive | Low | Editor shows "Archive note" for already archived notes and offers no Restore; card menu allows Pin on archived notes. | Show Restore in editor for archived notes; hide Pin when archived. |
| S7 | Settings | Medium | `SwitchRow` row and Switch both toggle (~L792). **Already fixed in PR #10 (240adb7).** | None here. |

## Unverified risks

- `NoteStore.merge` is O(existing x incoming) and runs inside composition while the restore dialog is open; a 10,000-note backup could stall the UI. A merged result above `MAX_NOTES` is only reported as a generic "Save failed". Not run (needs Android classes).
- Android `org.json` `optString` on a JSON `null` may import the text "null" (backup import). Needs a Robolectric test.
- `lineStart/lineEnd` handle `\n` only; a lone `\r` could misalign checklist line indexes.
- Reduced motion, keyboard/insets, large fonts, contrast and TalkBack: not assessable without a device; record as manual checks.

## Validation performed

GitHub Actions CI passed on exact branch head `f13bf6768f2320cc12f3cf0fb4f4bddb4dac0090`: [run #38687195037](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/38687195037).

- Unit tests passed.
- Debug APK build passed.
- Minified release APK build passed.
- Android instrumentation tests compiled successfully, but were **not executed on a device/emulator**.
- CI uploaded its configured build artifact. This is not a production release/signing validation.
- The six added tests cover indentation-aware checklist/bullet operations, list continuation/termination, checklist progress consistency, and late-day reminder boundaries.
- NOT run: device/emulator interaction, visual review, TalkBack, keyboard/insets, large font, reduced motion, physical-device smoke test, or production APK certificate verification.
- Required before merge: review the PR diff and retain the device-only checks above as explicitly outstanding.

## Files / overlap / merge order

Changed: `NoteText.kt`, `NoteTextTest.kt`, this file. None are touched by PR #10, so no overlap expected. Independent of PR #10; merge in either order. S1-S6 should follow PR #10.


## Codebase audit refresh — 2026-10-15

**Scope:** read-only source review of `main` at `935fd1f7a6892d6d87668b73930ec540335e9aaf`, including `MainActivity.kt`, `NoteStore.kt`, `NoteText.kt`, manifest/build configuration, unit and Compose instrumentation tests, CI workflows, README, roadmap, and UI/UX contract. Findings below are source-confirmed unless explicitly marked as a risk; no emulator or physical-device execution was performed for this audit.

### Findings ranked by impact

| Priority | Finding | Evidence / impact | Recommended fix and acceptance check |
|---|---|---|---|
| P0 | Compose UI and application state are concentrated in one very large `MainActivity.kt` (~100 KB). | Source-confirmed. UI, preference state, navigation, editor, settings, persistence orchestration, and dialogs are tightly coupled; fixes are harder to isolate and review. | Incrementally extract only existing logical sections into reusable composables/state holders when touching them; avoid a wholesale rewrite or a file explosion. Require unchanged user flows and exact-head CI for each extraction. |
| P0 | Critical UI instrumentation tests are compiled by required CI but not executed. | Confirmed by `.github/workflows/android-ci.yml`; emulator workflow is manual-only. Build success cannot prove create/edit/discard/restore/settings journeys work. | Run `connectedDebugAndroidTest` on API 36 in a dedicated workflow or owner-run emulator before release; publish test reports and make failures visible. Cost-conscious alternative: keep it manually triggered but require a recorded passing run for release candidates. |
| P0 | Restore merge is potentially quadratic for large imports. | Source-confirmed: `NoteStore.merge` checks existing IDs repeatedly when constructing order (`existing.none { ... }`) and restore processing is initiated from the UI flow. With the 10,000-note limit this can create avoidable work/jank. | Build an ID set once and use it for membership; validate merged result size before persistence and report a specific restore-limit error. Add large synthetic merge tests and assert ordering/counts. |
| P1 | Delete/Undo must be gated on successful persistence. | Previously recorded source finding in this audit: verify the current `deleteNote` call path against the branch before treating this as fixed. A failed write or recovery lock must not show a false “deleted” success or close the editor. | Keep the delete snackbar, Undo action, and editor close conditional on a successful save. Add a failing-store/recovery-lock regression test. |
| P1 | Search matching and empty-state behavior need a field-by-field regression pass. | Existing audit identified cross-field concatenation matches and untrimmed queries; search-specific empty-state work has existed on other branches, so re-check current main before coding. | Match normalized query independently against title/body/category; test whitespace, case, category-only hits, and false cross-field matches. Distinguish empty Home, no search results, empty Archive, and empty Reminders. |
| P1 | Archive actions can be semantically inconsistent. | Existing audit identified a Delete icon for Archive, Home for Restore, archived notes exposing inappropriate actions, and no editor Restore action; verify each against current main before implementation. | Reuse Material icons and a shared action policy; archived notes should expose Restore, not Pin/Archive. Add UI tests for both grid/list and editor actions. |
| P1 | UI reliability/accessibility gates remain incomplete. | CI compiles instrumentation but doesn't run it; no current evidence in this audit for TalkBack, large font, narrow landscape, keyboard/IME insets, reduced motion, or contrast on all note wallpaper combinations. | Execute the existing manual UI workflow and a documented device matrix. Add automated semantics/interaction tests where practical; do not infer accessibility from compilation. |
| P1 | Reminder is currently metadata, not a delivered reminder. | README explicitly says date/time is stored only; no scheduling/notification permission or receiver/service is declared in the manifest. | Product decision: either keep it clearly labelled as a reminder *time* with no alert, or implement scheduled local notifications using WorkManager/AlarmManager as requirements demand. If implemented, cover reboot/reschedule, permission denial, time-zone changes, cancellation/edit, archived/deleted notes, and Android 13+ notification permission. |
| P1 | Backup is plaintext JSON by design and can contain private note content. | README discloses plaintext export; local payload is encrypted with Android Keystore. | Keep an explicit export warning, consider optional password-protected export only if users request it, and test that logs/errors never include note contents. Validate keystore-loss/recovery and legacy plaintext migration on a device before release. |
| P2 | No user-visible autosave/draft recovery or undo/redo history comparable to mature editors. | NESTNOTE uses explicit Save/Cancel and has unsaved-change protection; Markor advertises autosave with undo/redo. These are product gaps, not necessarily defects for the current interaction contract. | First add process-death/rotation draft recovery or reliable autosave only after deciding semantics with explicit Save/Cancel. If adding undo/redo, reuse platform text editing/Compose behavior before implementing a custom history stack. |
| P2 | No quick-capture home-screen widget or filesystem/interoperable note format. | Fossify Notes advertises a customizable widget; Markor emphasizes plaintext/Markdown and files usable by other tools. NESTNOTE currently uses app-private encrypted storage and JSON backup. | Consider a widget for quick capture as a differentiated feature. Consider Markdown/text export as an additional export format without replacing encrypted local storage or existing JSON backup. |
| P2 | No multi-device sync or attachments/rich text. | README lists no account/cloud sync; current editor is plain text with checklist/bullet prefixes. These are scope choices rather than bugs. | Decide product direction before implementation. If sync is chosen, define conflict resolution/encryption/privacy first; if rich text is chosen, evaluate a mature compatible editor/library rather than creating a custom format without migration/export plans. |

### Open-source comparison and reuse decision

- **[Android Material 3 for Compose](https://developer.android.com/develop/ui/compose/designsystems/material3):** best baseline for theming, semantic components, color roles, and accessibility-aware controls. Prefer existing Material 3 APIs already present in the dependency graph; avoid hand-built equivalents where a system component fits.
- **[Markor](https://github.com/gsantner/markor) — Apache 2.0 app code:** useful product/engineering reference for offline-first text editing, interoperable plain-text formats, undo/redo, autosave, and format-related editor actions. Reuse specific code only after checking the exact file's license/header and dependencies; architectural ideas can be adopted without copying.
- **[Fossify Notes](https://github.com/FossifyOrg/Notes) — GPL-3.0:** useful comparison for auto-save, quick capture, widgets, and minimal offline notes. Treat implementation source as reference-only unless NESTNOTE's own license and distribution compatibility are confirmed.
- **[Tubular](https://github.com/polymorphicshade/Tubular) and [Metiq](https://github.com/metiq-xyz/android-app):** these were the user's examples, but they are not direct note-editor equivalents and were identified as GPL-licensed. Use them to study interaction ideas only until license compatibility is established.

**Reuse policy:** existing NESTNOTE implementation → official AndroidX/Material 3 component → permissively licensed, dependency-compatible source with attribution/license review. Do not copy GPL code into the app on assumption of compatibility. Do not add libraries merely to replace a small existing component.

### Recommended execution order

1. **P0 reliability:** run the UI instrumentation workflow; fix any reproducible data-loss/false-success paths; optimize restore merge and test size limits.
2. **P1 correctness:** verify/fix delete/Undo, search matching/empty states, and archive action semantics against current `main`.
3. **P1 validation:** run narrow-screen, landscape, keyboard/insets, large-font, dark-theme, and TalkBack checks and record evidence.
4. **P2 product choices:** decide actual reminder delivery, widget/quick capture, Markdown export, and whether autosave is compatible with explicit Save/Cancel.
5. **Architecture:** refactor incrementally as affected screens are improved, retaining the current local-first encrypted store and JSON backup compatibility.

**Audit boundary:** this is a source-based audit and comparison, not a claim that each listed risk has been reproduced. Existing items already fixed on another branch must be rechecked against `main` before new code is written. No third-party source code was copied as part of this audit.
