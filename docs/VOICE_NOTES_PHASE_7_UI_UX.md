# Voice Notes Phase 7 — UI/UX Quality

Tracking issue: #28  
Parent implementation: #35  
Scope: user-facing voice-note interactions, accessibility, and regression handling.

## 7.1 Interaction states and recording flow
- [x] Audit editor styling against existing NESTNOTE typography, spacing, colors, and components. (Code review: voice UI uses the existing Material 3 Card/Button/OutlinedButton/Text components, theme typography and colorScheme, consistent 14 dp card padding/10 dp vertical spacing, and 8 dp control spacing; status/helper text uses onSurfaceVariant.)
- [ ] Make Record, Pause, Resume, Stop, Play, and playback Stop controls clearly labeled, easy to tap, and visually distinct by state.
- [ ] Show recording/playback state at a glance; prevent invalid simultaneous actions.
- [ ] Show elapsed recording time and playback progress/duration where available; handle unknown duration gracefully.
- [ ] Distinguish a new unsaved recording from recordings already saved to the note.
- [ ] Prevent rapid repeated taps from creating duplicate recordings, saves, or playback sessions.
- [x] Verify empty, one-recording, multiple-recording, long-title, error, and restored-note states. (Code-path review: empty state has “No recordings yet”; attachments render from the store in an indexed list; row titles are in a weighted Column with wrapping Text; status surfaces permission/start/save/playback errors; noteId effect reloads persisted attachments and refreshes the count. Static/code review only; no device verification claimed.)

## 7.2 Feedback, errors, and destructive actions
- [ ] Show concise permission-denied guidance and a clear retry/settings path without repeatedly prompting.
- [ ] Provide feedback for recording started, paused, resumed, saved, stopped, discarded, playback started/stopped, and failures.
- [ ] Confirm destructive discard/delete actions where needed; explain what is removed and preserve previously saved attachments when discarding an editor draft.
- [ ] Ensure unsaved-change warnings accurately reflect adding, removing, saving, or discarding a recording.
- [ ] Display the 25 MiB limit and explain what happens when the limit or a recording error is reached.
- [ ] Keep loading/disabled states understandable and leave controls recoverable after failures.

## 7.3 Backup disclosure and accessibility
- [ ] Clearly disclose at the point of use and in backup/settings UI that v1 backups are notes-only and exclude voice audio.
- [ ] Provide meaningful screen-reader labels and state descriptions with logical focus order; never rely on color or icons alone.
- [ ] Check touch targets, text scaling, light/dark themes, contrast, and narrow-screen layouts using static/code-level checks where feasible.
- [ ] Keep controls usable with keyboard/navigation accessibility where supported; avoid losing focus after state changes.

## 7.4 Lifecycle and regression checks
- [ ] Review backgrounding, audio-focus interruptions/incoming calls, rotation/configuration changes, and process recreation; add automated or code-level checks where feasible.
- [ ] Review recovery paths for permission denial, capture/playback failures, discard, and preservation of saved attachments.
- [ ] Capture and resolve UI regressions found during implementation.
- [ ] ~~Manual emulator and physical-device usability pass~~ Archived by project decision; not a release gate.

## Implementation progress (validation pending)
- Record startup now has a single-action gate covering active recording, startup, permission requests, and playback; a JVM regression test covers each blocked state.
- Recording UI now shows an elapsed `mm:ss` indicator and distinguishes paused from active capture.
- Permission denial now provides concise guidance and an explicit app-settings action; permission-policy tests already cover denial, retry, and stale callbacks.
- These changes are not marked complete until the Android CI checks pass on the exact PR head. No emulator/physical-device verification is claimed.

## Follow-up implementation notes (CI intentionally deferred until 7.4 work is complete)
- Added a shared Record action gate covering active recording, startup, pending microphone permission, and active playback, with JVM regression coverage for each blocked state.
- Added an elapsed recording timer and visible active/paused/starting feedback.
- Added permission-denial guidance with an explicit app-settings action and invalidated pending permission/startup UI state when the app leaves the foreground.
- Marked newly staged attachments as not yet saved to the note and added descriptive content descriptions to recording/playback controls.
- Playback start/stop status feedback is visible; saved rows display duration derived from the documented encrypted envelope and PCM16 mono 16 kHz format, with an explicit unknown-duration fallback and JVM tests.
- Added point-of-use disclosure for the 25 MiB recording limit and that a limit/microphone failure discards partial audio. The Backup & privacy settings already explain that v1 backups contain notes/settings only and excluded voice audio will not return on restore.
- Lifecycle review: backgrounding stops capture/playback and abandons audio focus; disposal removes the observer and stops active sessions. Configuration changes therefore stop active capture rather than silently continuing; process recreation relies on the durable encrypted store/manifest and must not be represented as device-tested.
- Static UI review: recording controls use the app Material theme and text labels in addition to icons; saved attachment labels wrap in a column to avoid crowding narrow layouts. Emulator/physical-device testing remains archived.
- Automated CI has intentionally not been run yet, per the work sequence: finish review and implementation through 7.4 first, then run CI on the exact PR head. Keep checklist boxes open until the code and test evidence are reviewed; no device verification is claimed.

## Completion evidence
Mark an item complete only with code review, automated test, or recorded evidence. Do not claim device verification without evidence. Emulator and physical-device execution remains archived and is not required for this milestone.


### 7.1 completion evidence (PR #37 code review)
- Reviewed `VoiceNoteAudio.kt` against the existing Material 3 design system: themed Card and buttons, theme typography/color roles, consistent spacing, and text labels alongside icons. No bespoke colors or typography were introduced.
- Walked the empty, single/multiple attachment, long row-title, permission/startup/playback error, and restored-note code paths. The empty-state text is explicit; attachments are rendered from the durable store; each row uses a weighted text column so titles can wrap; status feedback covers asynchronous failures; and changing `noteId` reloads the stored attachment list and count.
- This is a static/code-level review, not a visual/device test. Per project decision emulator and physical-device execution remain archived. Exact-head CI remains deferred until all Phase 7 tasks through §7.4 are complete.
