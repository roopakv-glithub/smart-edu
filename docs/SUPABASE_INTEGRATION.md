# Supabase integration — 2026-10-03

Project: `pkccuhjqxgqpyrtiykse` (roopakv2007's Project).

## Delivered

- Deployed three SQL migrations and `studysync-session` Edge Function.
- Fixed the identity check to use the protected `/vtop/content` dashboard, normalize duplicate Set-Cookie headers, and follow only bounded same-origin redirects. The app reports connected only after cloud identity and academic storage succeed.
- Registration numbers are normalized and unique; profile lookup reuses an existing account, and concurrent creation handles conflicts by re-reading the winner. Snapshot and membership imports use database upserts. Repeat-import rollback tests confirm no duplicates.
- Android passwords go only to UniCC. The function receives the temporary VTOP cookie, verifies identity directly with VTOP over HTTPS, independently fetches academic data, and issues a separate Supabase session. Neither passwords nor VTOP cookies are persisted.
- Fixed VTOP's missing CA chain using public Sectigo intermediate/root certificates validated by the Windows trusted root store. Certificate and hostname verification remain enabled.
- Subjects and attendance are saved by user and semester. Exams are saved with full dates; failed exam refreshes preserve the prior saved exams. Android reads the persisted snapshot back before displaying it.
- Pods are derived on the server from verified semester, class instance, course and faculty. UniCC does not supply a reliable program/cohort field, so the app does not guess cohort from registration numbers; class instance provides conservative isolation.
- Tasks are saved to Supabase, with a personal option and separate per-user completion state. Shared tasks require an enrolled pod. Due dates are stored in UTC.
- Personal calendar events persist separately from read-only university exam entries.
- Notes upload to a private 20 MB bucket. Metadata includes subject and faculty; same-subject students in the semester can discover notes across faculty. Downloads use short-lived signed URLs.
- Pod chat loads/sends/deletes persisted messages and refreshes every 15 seconds while the conversation screen is open. This is polling, not a Realtime subscription.
- Settings persist; sample tasks, notes, pods, chats and leaderboard data were removed. AI is explicitly marked not connected.
- Mobile uses a publishable key plus the user's JWT. Privileged keys stay inside the server environment. The previously embedded legacy administrator key was removed, legacy API keys disabled, and the previous HS256 signing key revoked. Current ECC signing and modern secret/publishable keys remain active.

## Verification

- Android assembleDebug, unit tests and lintDebug passed; 23 unit tests cover academic mapping, login handling, persistent session restoration, automatic refresh/retry and sign-out cleanup.
- Transactional RLS tests passed: different-faculty task isolation, classmate sharing, personal task privacy, own snapshots, same-subject note discovery, per-user completion and denial of self-enrolment/academic forgery. Test data rolled back.
- Modern-key Auth admin creation -> magic-link token exchange -> authenticated RLS read passed. The temporary auth test account was removed.
- Deployed function rejects malformed input with 400 and invalid VTOP sessions with 401 after the CA fix.
- Supabase security advisor: no issues found.
- Installed debug APK on connected I2407 (`10BF5915RA001YL`), launched successfully, no crash entries for the current app process.

## User verification still needed

Connect your VTOP account once. Subsequent opens and Refresh read saved Supabase data. Use Import from VTOP only when you want a new university import. A real student's VTOP session has not been used in automated testing; personal subjects cannot be confirmed without that sign-in. Empty semester data is shown explicitly instead of sample subjects. App access/refresh credentials, selected semester and subject snapshot are persisted with Android Keystore AES-GCM in the no-backup directory. Reopening restores the app and reads Supabase directly; expired access tokens refresh automatically, with one retry on a rejected token. VTOP credentials are never persisted. VTOP reconnection is only for importing new university data, not viewing saved records.

Weekly lecture times are not supplied by UniCC's public schedule endpoint (that endpoint supplies exams). Subjects display their returned slot and venue. AI/quiz generation and notification delivery are not implemented by this integration.

## Maintenance

SQL files are under `supabase/migrations`; `supabase/tests/access_control.sql` contains rollback-only access checks. CLI 2.109.1 `db push` failed on its profile reader; the SQL was applied using `supabase db query --linked --file ...`, then recorded in the standard migration history. Edge deploy:

```
supabase functions deploy studysync-session --project-ref pkccuhjqxgqpyrtiykse --use-api --no-verify-jwt
```

The function deliberately performs VTOP session authentication itself before creating any identity or fetching private records. Do not replace that verification with a client-supplied registration number. Keep the public CA bundle updated when VTOP changes its certificate issuer.

References: [UniCC source](https://github.com/Arya4930/UniCC), [Supabase key migration](https://supabase.com/docs/guides/getting-started/migrating-to-new-api-keys), [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security).
