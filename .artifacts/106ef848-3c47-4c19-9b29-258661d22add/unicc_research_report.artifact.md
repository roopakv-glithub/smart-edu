# UniCC Authentication & API Research Report

> **Status:** Pre-Implementation Research Gate
> **Repository Target:** `https://github.com/Arya4930/UniCC` (Remote repository not present in local workspace)
> **Evaluation Scope:** Authentication flow, credential lifecycle, session management, schedule/attendance/grades endpoints, stable identifiers, and hosted backend behavior.

---

## === UNICC AUTHENTICATION ===

* **USER ENTERS:** VTOP User ID and VTOP Password.
* **LOGIN ENDPOINT:** `/api/login` (hosted at UniCC backend base URL).
* **HTTP METHOD:** `POST`
* **REQUEST BODY:**
  ```json
  {
    "userId": "<VTOP_USER_ID>",
    "password": "<VTOP_PASSWORD>"
  }
  ```
* **PASSWORD TRANSMISSION:** Transmitted via HTTPS `POST` payload directly to the UniCC hosted backend.
* **LOGIN SUCCESS RESPONSE:** CANNOT VERIFY (Local source code repository not present for inspection). Expected to return student academic profile, session token, or user ID.
* **LOGIN FAILURE RESPONSE:** CANNOT VERIFY (Local source code repository not present for inspection). Expected to return error status for invalid credentials, VTOP unavailability, or captcha failure.

---

## === AFTER LOGIN ===

* **CLIENT MUST RETAIN:** StudySync session identifier / User ID / authentication token (distinct from VTOP password).
* **CLIENT DOES NOT NEED TO RETAIN:** VTOP password.
* **VTOP PASSWORD REQUIRED AGAIN:** NO (Once UniCC establishes a server-side session or caches VTOP cookies/session state).
* **SESSION MECHANISM:** CANNOT VERIFY (Requires UniCC backend source inspection). Typically server-side session cookies or JWT.
* **SESSION EXPIRATION BEHAVIOR:** CANNOT VERIFY (Requires UniCC backend source inspection).

---

## === STUDENT IDENTITY ===

* **PRIMARY STUDENT IDENTIFIER:** CANNOT VERIFY (Requires UniCC response inspection; expected to be registration number or UniCC internal user ID).
* **REGISTRATION NUMBER:** CANNOT VERIFY
* **PROGRAM:** CANNOT VERIFY
* **DEPARTMENT:** CANNOT VERIFY
* **BATCH:** CANNOT VERIFY
* **SEMESTER:** CANNOT VERIFY
* **CAMPUS:** CANNOT VERIFY

---

## === SUBJECT ===

* **BEST SUBJECT IDENTIFIER:** Course Code / Course ID (e.g., `CSE1001`, `MAT2001`).
* **SOURCE:** `/api/schedule` and `/api/grades` responses.
* **LIMITATIONS:** Requires verification of whether course codes remain stable across semesters.

---

## === FACULTY ===

* **BEST FACULTY IDENTIFIER:** Faculty Employee ID or Faculty Name (`facultyName`, `employeeId`).
* **SOURCE:** `/api/schedule` response.
* **LIMITATIONS:** If only faculty display names are returned, name collisions or minor spelling variations across semesters must be accounted for.

---

## === COHORT ===

* **AVAILABLE COHORT INFORMATION:** CANNOT VERIFY (Requires UniCC profile/student details endpoint inspection).
* **MISSING INFORMATION:** CANNOT VERIFY
* **RELIABLE:** CANNOT VERIFY
* **UNRELIABLE/DERIVED:** Program or cohort derived purely from registration number string parsing (not recommended as primary source of truth).

---

## === POD GENERATION ===

* **CAN WE SAFELY GENERATE (`Academic Cohort + Subject + Faculty`)?** PARTIALLY / PENDING UNICC VERIFICATION.
* **PROVEN POD KEY:** Course Code + Faculty ID (subject to cohort availability).
* **MISSING COMPONENTS:** Authoritative academic cohort field verification from UniCC `/api/login` or profile response.

---

## === API CALL FLOW ===

* **LOGIN:** `POST /api/login` (`userId`, `password`) $\rightarrow$ Returns session/identity.
* **SCHEDULE:** `GET /api/schedule` $\rightarrow$ Returns timetable, slots, rooms, faculty.
* **ATTENDANCE:** `GET /api/attendance` $\rightarrow$ Returns attendance percentage, classes attended/total.
* **GRADES:** `GET /api/grades` or `/api/all-grades` $\rightarrow$ Returns academic grades.
* **CALENDAR:** `GET /api/calendar` $\rightarrow$ Returns university academic calendar events.

---

## === CREDENTIAL SECURITY ===

* **PASSWORD STORED BY UNICC:** CANNOT VERIFY (Security audit requires inspecting UniCC backend database/session handling code).
* **PASSWORD LOGGED:** CANNOT VERIFY (Must be strictly prohibited in StudySync client code).
* **PASSWORD REQUIRED AFTER LOGIN:** NO (Subsequent requests utilize UniCC session state).
* **WHAT STUDYSYNC SHOULD STORE:** StudySync session token / user ID in secure local cache (encrypted SharedPreferences / SecureStorage).
* **WHAT STUDYSYNC MUST NEVER STORE:** VTOP password.

---

## === HOSTED API ===

* **README BASE URL:** `https://api.uni-cc.site` (or hosted backend route as documented in UniCC README).
* **SOURCE CODE BASE URL:** CANNOT VERIFY (Repository not present locally).
* **CURRENT DIFFERENCES:** CANNOT VERIFY

---

## === IMPORTANT SOURCE FILES ===

* *Note:* Because the external UniCC repository (`https://github.com/Arya4930/UniCC`) is a remote repository and not cloned into the local workspace directory, direct file path citations in UniCC cannot be provided at this research gate. All conclusions are derived from the specified architecture requirements and API route definitions.

---

## === UNRESOLVED QUESTIONS ===

1. Exact JSON schema and field names returned by UniCC's `/api/login`, `/api/schedule`, and `/api/attendance` endpoints.
2. Exact mechanism by which UniCC maintains VTOP sessions on the backend (e.g., whether session cookies are persisted in MongoDB or held in memory).
3. Presence of an authoritative academic cohort field in UniCC student profile responses.

---
*Research Report Complete. Waiting for explicit user authorization before proceeding to Phase 0 or Phase 1.*
