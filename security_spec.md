# SomaTime Firestore Security Specification

## 1. Data Invariants
1. **Default Deny**: No Firestore path is readable or writable unless matched explicitly.
2. **PII Isolation**: Parent emails and phone numbers exist ONLY under `/users/{userId}/private/{docId}`, readable and writable strictly when `request.auth.uid == userId && request.auth.token.email_verified == true`.
3. **No Code Enumeration**: `/pairings`, `/teachers`, and `/teacher_assignments` strictly set `allow list: if false;` so no client can scrape or enumerate pairing or teacher codes.
4. **Strict Pairing Code Key**: `/pairings/{pairingCode}` requires `pairingCode` to match `^SOMA-[A-Z0-9]{4,12}$` and enforces immutable `pairingCode`, `childId`, and `ownerUid` after creation.
5. **Parent-Controlled Class Links**: `linkedTeacherCodes` on `/pairings/{pairingCode}` is capped at `<= 10` entries and is writable ONLY through `isParentSettingsUpdate()` (explicitly excluded from `isLearnerSyncUpdate()`), preventing children from adding unapproved teachers.
6. **Bounded Payload & Type Safety**: All strings have explicit `.size()` bounds, arrays are capped (`subjects <= 8`, `linkedTeacherCodes <= 10`, `recentAssignments <= 12`, `completedLessonIds <= 50`), and numeric limits (`grade` 1..12, `dailyCapMins` 15..300, `exchangeRateMins` 5..60, `availableScreenSeconds` 0..28800) prevent poisoned payloads.
7. **Teacher-Scoped Assignment Delivery**: Public school-wide assignment buckets (`/school_assignments/{schoolGradeKey}`) are locked down (`allow read, write: if false;`). Teachers publish only to `/teacher_assignments/{teacherCode}`, which requires the parent `/teachers/{teacherCode}` document to exist with a matching `school` (and matching `ownerUid` when authenticated). Learners only receive assignments from teacher codes explicitly linked by their parent.

## 2. Adversarial Payloads Checked
- Shadow field injection (`isAdmin: true`) on `/pairings/{pairingCode}`, `/teachers/{teacherCode}`, or `/teacher_assignments/{teacherCode}` -> rejected by `keys().hasOnly(...)` and `affectedKeys().hasOnly(...)`.
- Rogue teacher pushing assignments directly to a school name via `/school_assignments/{schoolGradeKey}` -> rejected (`if false`).
- Writing `/teacher_assignments/{teacherCode}` without an existing `/teachers/{teacherCode}` doc or with a mismatched `school` -> rejected by `exists()` + `get()` Master Gate.
- Learner device mutating `linkedTeacherCodes` or `licenseActive` on `/pairings/{pairingCode}` -> rejected because `isLearnerSyncUpdate()` excludes those keys.
- Listing `/pairings`, `/teachers`, or `/teacher_assignments` collections -> rejected by `allow list: if false`.
- Cross-user PII read on `/users/{otherUid}/private/profile` -> rejected by `request.auth.uid == userId`.
