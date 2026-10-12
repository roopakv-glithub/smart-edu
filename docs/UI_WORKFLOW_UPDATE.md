# UI and study-material workflow update

Implemented and installed on the connected I2407 on 2026-10-03.

- Profile uses the available display name, with "Name was not fetched" when no actual name is available. Removed registration-number substitution, sample email, degree and term details.
- Home navigation clears the previous destination instead of restoring a subpage.
- Removed location pins from upcoming events and calendar cards; calendar heading is "Events".
- Theory/lab entries with the same course and faculty combine into one compact pod card. Lab-only uses </>; embedded theory/lab uses { }. Original membership permissions remain enforced, and the combined view reads both channels. Different faculty remain separate.
- Tasks accept at most 25 words, with a database constraint and form counter. The dialog scrolls, subject choice remains visible for personal tasks, and Others creates a personal task. The time input uses 38% of the row and due-date display is limited to 45% of the card header.
- Saved task deadlines create calendar events using the phone's local date/time, including dates that cross midnight from UTC.
- Notes follow subjects -> faculty/common materials -> files. Subject lists appear even when no files have been uploaded. Common uploads carry explicit common-material metadata; faculty uploads retain the verified class faculty. Materials can be read across faculty for the same base subject and semester. Students cannot label uploads as another faculty's materials.
- Saved app sessions and duplicate-safe import behavior remain intact.

Validation: Android debug build, lint and all 27 unit tests passed. Database rollback tests passed for private/shared task access, personal completion, no duplicate imports, common-material access, forged-faculty rejection and 26-word title rejection. Security advisor reported only the project-wide existing leaked-password-protection setting; the app uses verified university session exchange rather than user-created Supabase passwords.

The installed app process had no crash entries. Final visual verification was initially blocked by the phone lock screen; no lock was bypassed.
