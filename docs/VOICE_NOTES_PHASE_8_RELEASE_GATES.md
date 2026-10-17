# Voice Notes Phase 8 — Automated Validation and Release Gates

Tracking issue: #28  
Parent implementation: #35  
Scope: automated CI evidence, security/regression review, merge gates, and artifact verification.

## 8.1 Exact-head CI
- [x] Unit tests, API 36 debug build, and release compilation passed on previously tested commit `618aa0896bcbc35b6ef912fdd9e72a8a38c87ef2` in [run 39048049532](https://github.com/Ravi-katta-dev/NESTNOTE/actions/runs/39048049532).
- [x] Add emulator-independent tests for manifest validation, encrypted-store behavior, permission policy, playback PCM format, recording lifecycle transitions, fake PCM capture failures/size limit, and lifecycle races.
- [ ] Confirm all required CI checks pass on the latest reviewed PR head, including documentation/checklist updates; fix and rerun any failure.
- [ ] Confirm test results belong to the exact commit under review, not an earlier head.

## 8.2 Security and regression review
- [x] Document the v1 notes-only backup policy; voice audio is excluded.
- [ ] Review the final diff for plaintext-audio leakage, attachment cleanup/deletion/Undo regressions, permission regressions, manifest compatibility, and partial-clip commit risks.
- [ ] Investigate and resolve unexplained build/test warnings or newly introduced dependencies/permissions.

## 8.3 Merge and artifact verification
- [ ] Merge only after required exact-head CI checks pass and review is complete.
- [ ] Record the merge commit SHA and verify required CI against merged code.
- [ ] Verify the expected APK artifact is produced and linked to the correct workflow run and commit.
- [ ] Before a production release, verify signing, certificate fingerprint, published artifact integrity, and release notes.

## 8.4 Evidence and closeout
- [ ] Record exact tested/merge commit SHA, CI run URL and result, and APK/artifact or release URL.
- [ ] Record checks not run and known limitations; never imply a release is published unless the artifact is available.
- [x] Emulator and physical-device execution is archived by project decision; it is not a release gate.

## Completion rule
Automated CI and emulator-independent tests are the validation gate. Do not claim device-only behavior has been verified without evidence. Do not merge or mark release-ready until applicable checks and evidence are complete.
