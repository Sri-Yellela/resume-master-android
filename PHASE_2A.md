# Android Phase 2a — toolchain, auth, API layer, persistence

✅ **Phase 2a itself is COMPLETE** — all four steps landed and verified on a real emulator,
40 JVM + 13 instrumented tests. See `../resume-master/docs/aj2-android-phase2a.md`.
The prompt below is reference only. **Two items remain, both new:**

---

## ✅ OWNER DECISION — admin panel: BUILD FLAVOUR (decided 2026-09-05)

The six admin screens are **not deleted**. They move behind a build flavour so the UI survives for
internal builds and can never reach a Play Store release.

```
1. Put ui/admin/ (Dashboard, Users, Jobs, Queue, Flags, Analytics) behind a build flavour —
   `internal` includes them, `release`/`play` excludes them. SOURCE-SET separation, not a runtime
   flag: a runtime flag still ships the code and the strings, and a flag can be flipped.
2. Remove the admin entry point from ProfileScreen in the release flavour. It is currently a
   tappable row beside a hardcoded "Admin • Pro" label, reachable by every user, with no auth to
   gate against.
3. NavGraph admin routes must not exist in the release flavour. Absent beats 404, and 404 beats
   rendering fabricated data.
4. ⛔ The destructive controls stay DEAD in BOTH flavours until they reach a real server:
   "Delete", "Suspend", "Impersonate read-only". They currently mutate a local MutableStateFlow and
   look like they work. Do NOT wire them here — an internal build that appears to delete a user and
   does not is worse than one with the buttons removed.
5. VERIFY BY BUILDING THE RELEASE FLAVOUR and grepping the bundle: no admin class, no admin string
   resource, no admin route. Then build `internal` and confirm the screens still render. A flavour
   that excludes the source but leaves strings in the release bundle is a half-fix a reviewer finds.
```

---

## ⛔ BACKUP EXCLUDES — user data is on device NOW

```
android:allowBackup="true" with backup_rules.xml and data_extraction_rules.xml both still the
UNTOUCHED Studio templates (comments only). This was flagged as "write the excludes BEFORE the first
token exists". The token now exists — and so does a persisted résumé, since Room landed.

Both are currently swept into Google cloud backup by default.

1. Write real excludes in BOTH files — they cover different mechanisms (Auto Backup vs Android 12+
   device-to-device transfer) and filling only one leaves the other open.
2. Exclude the auth token store (EncryptedSharedPreferences / DataStore) and the Room database
   holding the résumé. A résumé carries a home address, phone and employment history.
3. Consider android:allowBackup="false" outright. Decide deliberately: backup is a real convenience
   for a résumé builder, so excludes are probably better than a blanket off — but state which you
   chose and why.
4. This feeds the Play Data Safety declaration, which must not contradict the live policy at
   https://jobsviadraft.com/privacy. If the token is backed up, that is a disclosable data flow.

VERIFY: trigger a backup and restore on an emulator and confirm the token and résumé do NOT come
back. Inspecting the XML is not sufficient — these rules fail quietly when the path is wrong.
```

---

## Phase 2a — original prompt (COMPLETE, reference only)

Phase 1 (audit) is complete and accepted. This is Phase 2a only.

**Backend repo:** `../resume-master` (sibling directory). Contract at
`../resume-master/contract/mobile-api.v1.json`, currently **v1.1.0**.

---

## Standing conventions

```
Session-aware: read files in scope by SYMBOL, not line number. Reconstruct state from the repo, not
from status docs — both READMEs here describe unshipped behaviour. Regression-proof: for every
modified module review its dependents and children and fix them in the SAME pass, reporting each
with a verdict. Close with a REPORT + REAL-run verification, then commit & push as ONE focused
commit per sub-phase.

EVIDENCE RULE: a passing build is not evidence of behaviour. Confirm UI by screenshot and data flow
by real request against the running backend.
```

## ⛔ The four constraints — carry into every decision

**1. A SWIPE QUEUES, IT NEVER SUBMITS.** Applications reach real employers under the user's real
name and cannot be recalled. Phase 1 found this violated in five places — README, `SwipeAction.kt`,
`CardStackLayout.kt`, `SwipeOverlay.kt`, and `ActionBadge.kt`, which displayed **"Application sent"
with a green check for 2.5 seconds having sent nothing.** All fixed; do not reintroduce. Velocity
may distinguish a stronger signal, but the stronger signal must also only queue.

**2. GENERATION IS DEFERRED TO APPROVAL, never queue time.** ~$0.04 per generation; swiping is ~1s
per job. Right-swipe must cost nothing.

**3. GATED JOBS CANNOT BE COMPLETED ON A PHONE.** No extension exists on Android, so the desktop
handoff (which borrows the user's authenticated portal session under a per-gesture `activeTab`
grant) has no analogue. Never queue them into a state the phone cannot resolve.

**4. ELIGIBILITY ANSWERS ARE NEVER GESTURED.** Work authorisation, sponsorship, years of experience
and custom answers are attestations to an employer. Stored profile only.

---

## Order within this task — not negotiable

```
toolchain → auth → contract-typed API layer (incl. automationTier) → resume persistence
```

**NO FEED IN THIS TASK.**

---

## 1 · Toolchain

The build has **never been verified** — the audit machine had no JDK, no Android SDK, no Studio.
"It assembles" is currently unknown, not true.

- `gradle.properties` hardcodes `org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr`,
  a path that will not exist for anyone whose Studio is elsewhere. Remove it; resolve JDK 17 from
  the toolchain.
- `compileSdk = 35` with `agp = "9.0.1"`. **AGP 9 requires compileSdk 36+.**
  `android.suppressUnsupportedCompileSdk=35` suppresses the warning, not the minimum. This is the
  prime suspect for the next failure after the syntax fix.
- Re-verify the three literal `` `r`n `` repairs (`app/build.gradle.kts`, and two in
  `gradle/libs.versions.toml` which broke the version catalog).
- UTF-8 BOM on 55 of 57 text files. Kotlin tolerates it; **Gradle's TOML parser is the open
  question** — `tomllib` rejected `libs.versions.toml` outright.
- `packaging { excludes += "META-INF/native-image/**" }` may be insufficient for iText 9
  duplicate-resource conflicts.

**Report a verbatim assemble result before proceeding.**

## 2 · Auth — ⛔ use the right credential

```
POST /api/auth/login         -> authContext   SESSION-BOUND. DO NOT PERSIST.
GET  /api/auth/mobile-token  -> token         sessionLess, durable. THIS is the credential.
```

A login-issued token stores `session_sid = req.sessionID` and is swept by
`revokeBrowserAuthContexts`. The mobile mint stores NULL and that sweep deliberately never touches
it. **Persisting the login token produces intermittent, untraceable sign-outs.**

Flow: login → `GET /api/auth/mobile-token` → persist in EncryptedSharedPreferences (Keystore-backed)
→ `Authorization: Bearer` thereafter. Idle 7d sliding, absolute 90d.
`POST /api/auth/revoke-mobile-token` for sign-out.

**⛔ Before the first token exists:** `android:allowBackup="true"` with empty `backup_rules.xml` and
`data_extraction_rules.xml` (both untouched Studio templates) means the token is swept into Google
cloud backup by default. **Write the excludes FIRST.**

There is currently no login screen, no credential entry, no token storage of any kind.
`LinkedInAuthManager` is profile-import only and is explicitly not a session.

## 3 · API layer — generated from the contract, not hand-written

Replace the hand-rolled `JobRepository` parser. Do not patch it.

- **snake_case vs camelCase.** The parser reads `salary_min`, `salary_max`, `salary_currency`,
  `posted_at`, `contract_type`; the server emits camelCase. All five fail **silently to null** via
  `optString().ifEmpty{null}` — salary never renders, tags always empty, remote never labelled.
  Confirmed against the generated schema.
- `matchScore = 0` and `logoColor = "#888888"` are hardcoded in `toUiJob()` despite the server
  providing real values.
- `json.getJSONArray("attribution")` **throws** on the cache-empty path (that branch omits the key),
  so a successful 200 empty board reports as a network failure. Adzuna's ToS also requires
  attribution be displayed — swallowing it is a compliance problem. Use `optJSONArray`.
- The `Job` model carries **10 fields against the server's 37**.
- **Eight contract fields have no null coalescing server-side**, so `JSON.stringify` deletes them —
  they arrive **absent, not null**. A Kotlin decoder throws on a missing key it would accept as
  null. The contract types them optional; honour that.
- `JobRepository.search()` currently has **zero callers** and sends no `Authorization` header to a
  `requireAuth` endpoint. `GET /api/jobs` requires auth; `GET /api/jobs/generic` is the only public
  feed.
- **`automationTier` must land in the `Job` model in THIS change**, not after. `gated` / `account` /
  `unknown` are `completableOnMobile: false` — note **unknown is not completable**, which is broader
  than "gated". Filter **server-side** via `tiers_include` / `tiers_exclude`, never client-side: the
  server pages before the client filters, so hiding rows after the fact yields short pages and a
  count that disagrees with the list.
- Use **`/api/jobs/interact`**, not `PATCH /api/jobs/{id}/starred` — the latter **toggles**, so a
  retried swipe on a flaky phone network undoes itself and returns 200. The contract excludes the
  toggle routes.
- **Cursor paging** (v1.1.0). Build against cursors, not offset — the offset path silently skips
  rows the user has swiped away (measured: 6 of 25 with 3 swipes per page). Cursors are valid only
  for the filter set that produced them; restart paging on any filter change and handle the
  filter-mismatch rejection explicitly.
- `APPLY_DAILY_QUEUE_CAP` is a typed response field (`DailyCap` / `QueueCap` schemas) carrying
  `limit` and `remaining`. **Render `remaining`; never swallow it.**
- `sourcePlatform` is a trap — it looks like the ATS name and is not. Use `automationTier` for
  anything user-facing.
- **`CHECKSUMS.json` hashing is LF-normalised on purpose** — the desktop repo runs
  `core.autocrlf=true` while this repo is CRLF. A raw-byte verify here fails spuriously.

## 4 · Resume persistence

The builder is **in-memory only**; process death loses every edit. Invisible while data is mock,
real data loss the moment it is not. Room is declared (`runtime`, `ktx`, `compiler`, KSP) and
**entirely unused** — zero `@Entity` anywhere in the source tree.

If a Room schema models jobs or applications, compare it field by field against the contract. A
local schema disagreeing with the server is Shape 1 and will fail silently.

## 5 · Also in this pass

`ui/jobs/JobCard.kt:20` has `if(open) Button(onClick={}){Text("Apply")}` — a tap, not a gesture, and
a literal no-op, so it cannot submit. The copy is misleading. Resolve it.

---

## Deferred — not this task

- **The feed.** Phase 2b.
- **The review queue.** Endpoints already exist and are well specified: `GET /api/apply/pending`
  ("the review inbox"), `POST /api/apply/approve` ("THE moment of submission"),
  `POST /api/apply/reject`, `GET /api/apply/questions`, `POST /api/apply/answers`,
  `GET /api/apply/readiness`.
- **The admin panel** — 6 screens of fabricated data, ungated because there is no auth to gate
  against, with Delete / Suspend / Impersonate controls that look functional and hit nothing.
  Owner's decision pending; recommendation is a build flavour, not deletion. **Do not ship it in a
  Play build.**

## Store readiness gaps (record, do not fix here)

No keystore, no `signingConfigs`, `isMinifyEnabled = false` on release, no Play Console evidence, no
Data Safety declaration, no privacy-policy link, `versionCode` never incremented, default Android
Studio launcher icon, and the `YOUR_DOMAIN.com` deep-link host with `autoVerify="true"`.

Data Safety must be authored against the live policy at `https://jobsviadraft.com/privacy` and must
not contradict it.

---

## Verify

Assemble succeeds (verbatim result). Login → mobile-token → an authenticated `GET /api/jobs`
returning real rows with salary, tags, remote label and `automationTier` populated. A gated job is
distinguishable. Backup excludes present before any token is written. Resume edits survive process
death. Screenshot each.
