# Changelog

All notable changes to NESTNOTE are documented here.

## [1.3.11] - Unreleased

This release candidate collects the note-editor and Markdown fixes merged while addressing [issue #61](https://github.com/Ravi-katta-dev/NESTNOTE/issues/61). This section is intentionally marked **Unreleased** until the release candidate passes the required CI and release checks.

### Fixed
- Corrected editor text contrast selection for dark surfaces by using surface luminance and theme-provided foreground colors.
- Improved Markdown blockquote and horizontal-rule preview rendering while keeping raw HTML-like content inert.
- Preserved meaningful whitespace, indentation, trailing-newline semantics, and LF/CRLF/lone-CR line endings in text conversion and list operations.
- Corrected selection remapping after line edits to better preserve cursor and reversed-selection positions.
- Hardened inline-format toggles and checklist/bullet transitions, including indentation, Enter behavior, and mixed line endings.
- Restricted actionable Markdown preview links to `http`, `https`, and `mailto` schemes; unsupported schemes are removed from actionable preview content.
- Improved accessibility touch-target sizing and added standalone Markdown heading semantics for preview segments.

### Tests and engineering
- Added regression coverage for Markdown rendering, formatting toggles, list/checklist transitions, link escaping and cursor placement, line-ending preservation, and safe-link handling.
- Added draft persistence/isolation and undo/redo regression coverage.
- Added Compose editor integration coverage for save/recovery, discard/reopen, preview/edit switching, formatting actions, link insertion, note switching, and Activity recreation.
- Added CI workflow coverage to execute Android emulator UI tests on pull requests.

### Known validation gaps
- Do not consider every item in issue #61 complete solely from this changelog. Check the issue and linked pull requests for exact scope and evidence.
- Confirm CI passes on the final release commit, including unit tests, lint, release compilation, and the configured emulator/UI-test jobs.
- Verify production signing and the exact release APK certificate; do not distribute unsigned or inspection-only artifacts.
- Record any remaining physical-device checks, including TalkBack semantics, large fonts, keyboard/insets, theme contrast, and process-death recovery, before declaring release readiness.

[Unreleased]: https://github.com/Ravi-katta-dev/NESTNOTE/compare/v1.3.10...HEAD
