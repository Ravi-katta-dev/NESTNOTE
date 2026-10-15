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

No Android SDK/Gradle is available in this environment and no push credentials, so CI has not run for this branch.

- Kotlin 2.1.20 compiler + a minimal JUnit shim compiled `NoteText.kt` with `NoteTextTest.kt` and ran every `@Test`.
- Baseline on `main`: 15/15 pass. After the change: 21/21 pass. The 6 new tests all fail against `main`'s `NoteText.kt` (verified).
- NOT run: `NoteStoreTest`, `NoteBackupTest` (Robolectric), instrumentation tests, `assembleDebug/Release`, any device check.
- Required before merge: `gradle testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest` via the repo's Android CI.

## Files / overlap / merge order

Changed: `NoteText.kt`, `NoteTextTest.kt`, this file. None are touched by PR #10, so no overlap expected. Independent of PR #10; merge in either order. S1-S6 should follow PR #10.
