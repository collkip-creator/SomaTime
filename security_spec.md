# SomaTime Firestore Security Specification

## 1. Data Invariants
1. **Default Deny**: No Firestore path is readable or writable unless matched explicitly.
2. **PII Isolation**: Parent emails and phone numbers exist ONLY under `/users/{userId}/private/{docId}`, readable and writable strictly when `request.auth.uid == userId && request.auth.token.email_verified == true`.
3. **No Code Enumeration**: `/pairings` strictly sets `allow list: if false;` so no client can scrape or enumerate active pairing codes.
4. **Strict Pairing Code Key**: `/pairings/{pairingCode}` requires `pairingCode` to match `^SOMA-[A-Z0-9]{4,12}$` and enforces immutable `pairingCode`, `childId`, and `ownerUid` after creation.
5. **Bounded Payload & Type Safety**: All strings have explicit `.size()` bounds, arrays are capped at `<= 50` items, and numeric limits (`grade` 1..12, `dailyCapMins` 15..300, `exchangeRateMins` 5..60, `availableScreenSeconds` 0..28800) prevent poisoned payloads.
6. **Action-Based Updates**: Updates to `/pairings/{pairingCode}` are partitioned into `isLearnerPairOrSyncUpdate()` and `isParentSettingsUpdate()` using `affectedKeys().hasOnly(...)`.

## 2. Adversarial Payloads Checked
- Shadow field injection (`isAdmin: true`) on `/pairings/{pairingCode}` -> rejected by `keys().hasOnly(...)` and `affectedKeys().hasOnly(...)`.
- Listing `/pairings` collection -> rejected by `allow list: if false`.
- Oversized 1MB strings in `childName` or `school` -> rejected by `.size()` bounds.
- Mutating immutable `pairingCode`, `childId`, or `ownerUid` on update -> rejected by immutability checks.
- Cross-user PII read on `/users/{otherUid}/private/profile` -> rejected by `request.auth.uid == userId`.
- Unverified email claim on `/users/{uid}` -> rejected by `request.auth.token.email_verified == true`.
