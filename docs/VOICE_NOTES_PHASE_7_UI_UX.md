# Voice Notes Phase 7 — UI/UX Quality

Tracking issue: #28  
Parent implementation: #35  
Scope: user-facing voice-note interactions, accessibility, and regression handling.

## 7.1 Interaction states and recording flow
- [ ] Audit editor styling against existing NESTNOTE typography, spacing, colors, and components.
- [ ] Make Record, Pause, Resume, Stop, Play, and playback Stop controls clearly labeled, easy to tap, and visually distinct by state.
- [ ] Show recording/playback state at a glance; prevent invalid simultaneous actions.
- [ ] Show elapsed recording time and playback progress/duration where available; handle unknown duration gracefully.
- [ ] Distinguish a new unsaved recording from recordings already saved to the note.
- [ ] Prevent rapid repeated taps from creating duplicate recordings, saves, or playback sessions.
- [ ] Verify empty, one-recording, multiple-recording, long-title, error, and restored-note states.

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

## Completion evidence
Mark an item complete only with code review, automated test, or recorded evidence. Do not claim device verification without evidence. Emulator and physical-device execution remains archived and is not required for this milestone.
