# Local POS PIN foundation

`WooPosLocalPINStore` stores an Admin PIN and an optional Cashier PIN per database local site ID.
Use the local ID for both WordPress.com and application-password stores. After successful sign-out,
logout erases local PINs before removing sites. Successful erasure prevents a reused local ID from
inheriting a previous account's PINs.

- PINs contain exactly four ASCII digits. Both roles must use different PINs.
- Admin creation enables protection. Removing Admin also removes Cashier. Removing Cashier leaves Admin.
- `WooPosPINCredentialSource` supplies hashes and operator permissions. `WooPosPINAuthenticator` checks
  these without depending on local storage or hard-coded role presets. M2 can replace the source.
- An empty credential list means protection is off. A failed read is a separate result: callers must
  keep POS locked and offer recovery. Never convert failure to an empty list.
- Hashing uses PBKDF2-HMAC-SHA256, 100,000 iterations, a fresh 16-byte random salt, and a 32-byte hash.
  Version 1 accepts only those parameters; malformed or unsupported records fail before derivation.
  Four-digit PINs have only 10,000 values. Hashing slows guessing but cannot protect copied hashes.
- Records are encrypted with AES-256-GCM using a non-exportable Android Keystore key. The local site
  ID is authenticated as additional data, so copying ciphertext to another store fails authentication.
  Atomic files under `Context.noBackupFilesDir` exclude records from Android backups and transfers.
  No PIN, salt, hash, or ciphertext is written to logs, preferences, Room, or network requests.
- Store operations run on the IO dispatcher and share a singleton mutex. Successful logout/closure
  awaits erasure before site removal. Failed sign-out preserves PIN protection. A cleanup failure is
  logged without blocking sign-out or misreporting a completed closure. Logout without a session also
  attempts erasure. Key deletion precedes file cleanup; either crypto erasure or record deletion removes
  usable credentials. If both fail, cleanup reports failure and logs it without blocking sign-out;
  intact old credentials may remain. This is a best-effort erasure limit, not a successful PIN reset.
  Unreadable remnants are still reported as storage failures, never as protection-off.
- Missing keys, modified ciphertext, unsupported schemas, duplicate roles, orphan Cashier records,
  invalid work factors and truncated records return storage failure. Reads never generate a key or
  silently reset credentials. Explicit Admin removal or logout can clear unreadable records.

This increment adds no setup UI, session role, lock enforcement, attempt counter, approval, trigger,
recovery, containment or analytics. The internal manual-lock shell still uses its disabled remote flag
and accepts arbitrary four-digit input until WOOMOB-4605 connects authentication. Callers must apply
that task's shared attempt limiter to unlock and approval; the authenticator itself does not limit attempts.

## Verification

Unit tests cover the role permissions, source replacement, PIN validation, PBKDF2 known vector,
fresh salts, store separation, record validation, removal and logout ordering/failure.
`WooPosEncryptedPINStorageTest` uses a separate test key and directory to check real Android Keystore
operations, encryption, store binding, tampering, missing keys and erasure. Its configured PINs are
synthetic fixtures; no production setup path is added for this test.
