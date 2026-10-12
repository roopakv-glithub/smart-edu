# UniCC integration repair

Research date: 2026-10-02. Source: https://github.com/Arya4930/UniCC
Inspected commit: 5b53ff8f97ce1c515eee514fa38545c2783f9a8a.
The pasted attachment was treated as architecture context, not as an instruction to stop coding or generate its proposed document suite.

## Root cause and fixes

The app used https://api.uni-cc.site/, which failed DNS resolution during this investigation. The upstream frontend (src/components/custom/main.tsx) now uses https://api-unicc.arya22.dev. GET /api/status returned HTTP 200 with {"text":"API is working"}. The README also contains a conflicting typo in one prose URL; the frontend constant and live health endpoint were used to verify the replacement.

The Android app now uses that verified HTTPS endpoint through BuildConfig and ApiConfig. For future maintainer-confirmed changes, build with:

    .\gradlew.bat :app:assembleDebug -PUNICC_BASE_URL=https://verified-host.example/

Do not enter credentials into an unverified mirror. There is no automatic fallback host and login redirects are disabled. Build configuration rejects non-HTTPS endpoints, embedded credentials, queries and fragments.

Other repairs:
- Removed BODY-level HTTP logging and its logging dependency. Requests and session DTOs no longer auto-generate toString methods exposing passwords/cookies.
- Removed the prefilled username/password from the login form and clear the form password after submission.
- Validate authorizedID, csrf and cookies as nonblank strings before accepting authentication.
- Parse non-2xx error JSON and distinguish invalid credentials, expired passwords, captcha problems, rate limits, server errors, DNS, TLS and timeouts. Do not show raw server/exception messages containing potentially sensitive content.
- Retry exactly once for HTTP 401 with Invalid Captcha, matching upstream behavior. Never retry invalid credentials automatically.
- Increased timeouts for the multi-request VTOP captcha/login operation; disabled automatic network retries.
- Preserve coroutine cancellation, prevent duplicate login submissions and clear in-memory session on logout.
- Recreate navigation state when authentication changes, avoiding navigation to an unmounted NavHost or restoration of the previous account's route.
- Show the verified authorizedID in the greeting/drawer rather than claiming it is a student name.
- Removed the unconditional demo authentication bypass and nonfunctional Remember me checkbox. Session persistence is not implemented.
- Replaced academic request Map<String, Any> stubs with required session/semester fields. Responses remain JSON adapter placeholders, not completed domain integrations.

## Actual API contract

The authoritative sources below are under backend/src/routes at the inspected commit. The upstream TypeScript LoginRequestBody includes extra fields, but login/login.ts actually reads only username and password; the frontend confirms the two-field request.

| Endpoint | Method / request JSON | Response / meaning |
|---|---|---|
| /api/status | GET, no auth | text; confirms UniCC availability, not VTOP health |
| /api/login | POST username, password | success, message, cookies (string), csrf, authorizedID; no student name/program/cohort |
| /api/attendance | POST cookies, csrf, authorizedID, semesterId | attRes: {semester, attendance: [...]}, marksRes; attendance fields include courseCode, courseTitle, courseType, slotName, faculty, classId, slotVenue, credits, category and attendance figures |
| /api/schedule | POST same session + semesterId | semester and Schedule map of exam groups to entries: courseCode, courseTitle, classId, slot, examDate, examSession, reportingTime, examTime, venue, seatLocation, seatNo. This is an EXAM schedule, not a weekly class timetable. |
| /api/grades | POST same session + semesterId | effectiveGrades, curriculum, cgpa, feedback |
| /api/all-grades | POST cookies, csrf, authorizedID | grades; history grouped by semester |
| /api/calendar | POST session + semesterId + type (upstream defaults to ALL) | semesterId, calendars: academic dates/events, not teaching slot times |

Headers: Content-Type: application/json. No Supabase token, bearer token or Android cookie jar is used. VTOP cookies and CSRF travel inside subsequent UniCC request JSON. Store this session only in memory for now. Never send the VTOP password to Supabase.

Login returns HTTP 401 with messages distinguishing Invalid Captcha, Invalid Username / Password, and password expiry. HTTP 500 may contain error or message. The hosted deployment may differ from source, so malformed and incomplete responses fail closed.

Weekly timetable parsing exists as the internal fetchTimeTable.ts helper used by attendance; no dedicated public weekly timetable route is mounted in server.ts. Do not invent /api/timetable or derive weekday/time from exam data.

## What is still missing (not fixed by changing the host)

This folder contains the Compose frontend and initial UniCC login adapter. There is no Supabase SDK, project configuration, schema, migrations, RLS, verified identity exchange, or collaboration server implementation. Tasks, notes, schedules, pods, chat and AI currently use local/sample data. The repaired login does not turn them into live shared data.

The examined login/academic endpoints do not establish an authoritative program/cohort or student display name. Do not infer a verified cohort from a registration number or merge people globally by subject/faculty. The proposed Academic Cohort + Course + Faculty membership model needs additional verified data and server-side authorization before shared pods can be enabled. Never trust client-supplied academic memberships or authorizedID as proof for issuing a StudySync account token.

Remaining implementation work:
1. Supply/select a current verified semester ID; map attendance/course data and exam/calendar data into separate domain models.
2. Establish a trusted identity verification flow and authoritative cohort source for StudySync. UniCC's cookies are VTOP session credentials, not a signed identity assertion for Supabase.
3. Configure the actual Supabase project and reviewed migrations/RLS, then implement persistence and scoped collaboration incrementally.
4. Implement StudySync session persistence without retaining VTOP passwords. Explicitly reauthenticate when university credentials expire.
5. Replace sample feature data and implement AI/quiz/notes features separately.

No database or external deployment was changed. The reference repository was downloaded under .artifacts/UniCC-reference for inspection only; its code was not executed or copied into the application.

## Validation

Tests in AcademicRepositoryTest exercise the Retrofit/HTTP boundary using an isolated local MockWebServer and synthetic credentials. They cover actual request serialization, valid/incomplete sessions, captcha retry limits, invalid/expired passwords, gateway HTML, malformed responses, logout/relogin, rate limiting, redirects, DNS, timeout, TLS and coroutine cancellation.

Build commands used with the installed JDK 24 (Gradle 8.14.3):

    $env:JAVA_HOME='C:\Program Files\Java\jdk-24'
    .\gradlew.bat :app:clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --no-build-cache --max-workers=2 --console=plain '-Dorg.gradle.jvmargs=-Xmx1024m' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false'

Only the unauthenticated health endpoint was checked against the live host. No screenshot credentials were submitted, so real VTOP authentication and academic data remain unverified.




Final verification (2026-10-03): assembleDebug, testDebugUnitTest and lintDebug all succeeded. All 16 repository tests passed. Existing UI icon deprecation warnings remain. After the user connected a phone over USB, the final APK was installed successfully with adb install -r, MainActivity was launched, and the app process was confirmed running. Real VTOP sign-in still requires the user's device test.
