# NESTNOTE Release & Validation Checklist

Use this checklist for each candidate release. A successful build is necessary, but it is not by itself evidence that the app is ready to ship. Record the commit, Android version/device, test result, and any accepted risks in [WORKLOG.md](WORKLOG.md).

## 1. Automated checks

- [ ] Android CI passes on the exact candidate commit.
- [ ] Unit tests pass.
- [ ] Debug APK builds successfully.
- [ ] Release APK builds with the configured R8/resource shrinking settings.
- [ ] Instrumentation tests compile; if run on an emulator/device, record the actual result separately.
- [ ] No new unexplained warnings, dependency vulnerabilities, or manifest permissions are introduced.

## 2. Core note-taking regression

On a clean install and an upgrade install where practical:

- [ ] Create, edit, save, cancel, and delete a note.
- [ ] Confirm unsaved edits are protected through Back/cancel and activity recreation/rotation.
- [ ] Verify pin, archive, restore, sort, search, and category filtering.
- [ ] Verify note colors and editor backgrounds in light and dark themes.
- [ ] Confirm preferences persist after app restart.
- [ ] Test keyboard visibility, scrolling, system bars/edge-to-edge, narrow screens, and larger font/display scaling.
- [ ] Check icon labels, touch targets, contrast, and TalkBack usability.

## 3. Backup and recovery

- [ ] Export notes to a user-selected JSON file and confirm expected fields survive round-trip.
- [ ] Restore a valid backup and confirm new IDs are added, newer copies win, and existing notes are not deleted.
- [ ] Reject malformed JSON, missing required fields, duplicate IDs, invalid values, and oversized inputs without changing existing notes.
- [ ] Confirm restore validates the full import before applying any changes.
- [ ] Verify legacy-record compatibility, including records without a creation timestamp.
- [ ] Test storage/read/write failure paths and confirm saved data is not silently replaced by starter notes.
- [ ] Confirm privacy copy accurately explains whether local storage, automatic backup, and exported files are encrypted or included in backup.

## 4. Android storage and privacy

- [ ] Verify the shipped manifest and applicable backup/data-extraction rules on supported Android versions.
- [ ] Confirm the chosen automatic-backup policy matches actual platform behavior; do not infer all transfer behavior from `allowBackup` alone.
- [ ] If encrypted storage is shipped, test migration from existing plaintext records, repeat launches, corrupt records, missing keys, and recovery guidance.
- [ ] Confirm exported JSON is handled as plaintext unless export encryption is explicitly implemented.
- [ ] Review permissions, exported components, and newly added dependencies.

## 5. Production signing and artifact integrity

- [ ] Confirm the production signing key is available only through the approved secret/managed signing path.
- [ ] Confirm tagged production releases fail if permanent signing configuration is absent or invalid.
- [ ] Verify the generated APK signature and compare its certificate fingerprint with the independently recorded expected production certificate.
- [ ] Ensure a temporary CI key is never used for a public production artifact.
- [ ] Ensure signing secrets, keystore bytes, and private key material are not logged or committed.
- [ ] Confirm the signing-key backup and recovery procedure is documented and access is restricted.
- [ ] Install the signed artifact and test an upgrade over the previous production build where available.
- [ ] Review release notes, version/tag, privacy disclosures, and artifact contents before publishing.

## 6. Release decision

- [ ] All required checks above have an explicit result; unrun manual checks are marked **Not run**, not passed.
- [ ] Remaining risks have a written explanation and explicit owner approval.
- [ ] The release commit and CI run are recorded in WORKLOG.md.
- [ ] Repository owner approves publication. Do not merge or publish automatically as a consequence of checklist completion.

## Result record

Copy this section into WORKLOG.md for each candidate:

- Candidate commit:
- CI run:
- Device/emulator and Android version:
- Manual checks completed:
- Checks not run:
- Signing certificate fingerprint verified:
- Known issues / accepted risks:
- Release decision and owner approval:
