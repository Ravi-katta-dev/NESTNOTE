# NestNote Redesign Prototype Parity Audit

Date: 2026-10-19
Reference: `NestNote redesign prototype.html`
Scope: Compare the supplied HTML prototype with the current Jetpack Compose implementation. This is a source audit; no Android device or emulator was connected, so visual, keyboard, accessibility, and motion findings remain unverified until manual validation.

## Executive Summary
The main redesign architecture is present in Android: the floating navigation bar, More hub, editor toolbar and menu, right-side tool rail, appearance previews, draft Apply/Cancel behavior, markdown preview, voice-note entry point, themes, note cards, search, filtering, and persistence flows all have implementations.

The remaining work is primarily:
1. Confirm behavior and visual parity on a real phone at narrow, large-font, dark-theme, and keyboard-visible sizes.
2. Close small prototype parity gaps and validate empty/loading/error states.
3. Improve editor and dashboard performance based on profiling.
4. Add end-to-end coverage for core interactions.

## Status Legend
- **Implemented**: Android owner exists and behavior is represented in source/tests.
- **Partial**: Behavior exists, but differs from the prototype or lacks required validation.
- **Pending**: No reliable implementation or verification is present.

## Performance follow-up

### Completed source-level improvement
- [x] Cache the configured Markwon renderer in `MarkdownPreview` so plugin setup is not repeated on each preview render. Renderer uses the application context to avoid retaining an Activity.
  - File: `app/src/main/java/com/nestnote/app/MarkdownPreview.kt`
  - Commit: `2c1c9ef4f2559810022d8f50bf72abdec5e4ca09`
  - This reduces repeated renderer construction, but does **not** establish that Markdown parsing or UI jank is fixed. CI and device profiling are still required.

### Remaining performance work
- [ ] Capture a cold-start and note-editing system trace on a physical device; identify the actual main-thread stalls before claiming a performance fix.
- [ ] Verify startup storage loading stays off the main thread. Current `NestNoteApp` loads with `withContext(Dispatchers.IO)`; confirm by trace that no other startup work blocks the first frames.
- [ ] Audit note persistence: `persist()` calls private and general store saves synchronously. Refactor carefully so encryption/storage I/O cannot block the main thread while preserving save ordering, failure reporting, recovery behavior, and no-plaintext-fallback guarantees.
- [ ] Derive filtered/sorted notes only when their inputs change and avoid recalculating counts unrelated to the current destination. Filtering already runs on `Dispatchers.Default`; verify recomposition behavior before changing it.
- [ ] Avoid reparsing Markdown when the content has not changed, especially in `AndroidView` update callbacks; retain safe link-scheme filtering and styling.
- [ ] Measure checklist parsing and word-count work during continuous typing before optimizing. If traces show cost, narrow recomposition scope or cache derived results.
- [ ] Run unit tests, debug build, instrumentation compilation, and device smoke tests on the final commit; record exact commands and outcomes in the work log.

## Other pending validation
- [ ] Run the core editor journeys on a physical Android 13 device: create/edit, keyboard, Markdown preview, open/close appearance rail, cancel/apply, save/reopen, and discard.
- [ ] Validate screenshot protection and Recents behavior on the target ROM.
- [ ] Check accessibility, large fonts, keyboard/insets, reduced motion, and empty/loading/error states.
- [ ] Reconcile this audit with `ROADMAP.md` and other project documentation after verified changes.

No performance claim should be marked complete without CI results and a device trace.
