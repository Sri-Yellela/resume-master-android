# Corruption and defect-pattern sweep — Android

Run 2026-09-29 against `draft-android` (`main`, starting at `a6ea57a`), per
`../draft/docs/CORRUPTION_SWEEP.md` (WORKLOG agent-queue item 6). Scope: `app/src` (main, test,
androidTest), `README.md`, `SYNC.md`, `ANDROID.md`, `PHASE_2A.md`, and the vendored `contract/`.

**Status key:** *fixed in `<hash>`* · *open* (safe to fix, not done here — usually because the fix is
UI-only and cannot be proven on the JVM) · *blocked-on-owner* (needs an owner decision, the server,
release signing, or a device).

## Baseline and result

| | Command | Result |
|---|---|---|
| Baseline, at `a6ea57a` | `./gradlew assembleDebug testDebugUnitTest` | **BUILD SUCCESSFUL** · 40 tests, 0 failures, 0 errors, 0 skipped (ExampleUnitTest 1, ContractDriftTest 7, ContractJobDecodeTest 24, ResumeMappingTest 8) |
| Final, at `ac19496` | same | **BUILD SUCCESSFUL** · 49 tests, 0 failures, 0 errors, 0 skipped (+9: JobsViewModelTest 2, TemplatesViewModelTest 2, SwipeResolveTest 4, JobCardTextTest 1). Introduced failures: 0 |

Counts are read from `app/build/test-results/testDebugUnitTest/*.xml` after deleting the directory,
so a stale result cannot be counted. The 13 instrumented tests (`androidTest`) were **not run** —
no device was attached; the AVD `rm_api36` exists but was not booted.

⚠ The first baseline attempt **failed**: `ERROR: JAVA_HOME is not set and no 'java' command could be
found in your PATH.` That is the expected consequence of removing `org.gradle.java.home` from
`gradle.properties` (Part 1): the command line now needs `JAVA_HOME`. The build was re-run with
`JAVA_HOME` = Studio's bundled `jbr`; the README now says so.

---

## Part 1 — literal escape / encoding corruption: CLEAN

Re-run 2026-09-29 (before this sweep; cited, not redone):

| Check | Doc's premise | Measured |
|---|---|---|
| Literal PowerShell `` `r`n `` escapes (outside docs that quote them) | 3 sites (`build.gradle.kts`, 2 in `libs.versions.toml`) | **0** |
| UTF-8 BOM | 55 of 57 text files | **0** files |
| Mixed CRLF/LF within one file | known history | **0** files |
| Mojibake (`â€”`, `Ã`, `ðŸ` …) | `SYNC.md` status glyphs | **0** — glyphs restored in `f337767` |
| `org.gradle.java.home` in `gradle.properties` | hardcoded Studio path | **absent** |
| Implausibly long lines | — | only `MockData.kt` string literals and `AndroidManifest.xml` line 5 (a one-line `<application>` tag) — cosmetic |

Every file edited in this sweep was re-checked for mixed line endings after the edit (all clean);
edits were made with a byte-preserving editor, never `sed -i`.

The doc's other Part 1 "known findings" are superseded: `JobRepository` no longer carries an origin
(it is `ApiClient.DEFAULT_BASE_URL` = `https://jobsviadraft.com`, resolved in `AppGraph`), and
`LinkedInAuthManager` no longer names `YOUR_DOMAIN.com` (`c40b343`). **The manifest deep-link host is
still `YOUR_DOMAIN.com` by design** — see Shape 1 #5.

Prior sweep commits in this repo: `f337767` (SYNC.md glyphs), `c40b343` (Shape 1, base URL),
`44a7e17` (Shape 6, AtsBands named a guard that did not exist).

---

## Part 2 — the six shapes

### Shape 1 — two sides of a contract that don't meet

| # | Location | Evidence | Severity | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | `JobsViewModel.loadMore` ↔ `JobRepository.uiJobs` | Swipe removed a card from the VM's `_jobs`; the next page replaced `_jobs` with the repository's accumulated list. Every card swiped since launch returned. Measured through the real repository + a local HTTP server: `expected:<[a3, a4, a5, b1, b2]> but was:<[a1, a2, a3, a4, a5, b1, b2]>` | **High** — fires on every board at the 17th swipe of a 20-row page | Derive the stack: repository list minus a `decided` id set | **fixed in `cd814f1`** (`JobsViewModelTest`) |
| 2 | `NavGraph.showTabs` ↔ `BottomTabBar.bottomTabs` | Tab routes spelled twice — the "three hardcoded tab lists" precedent | Low | Read `bottomTabs` | **fixed in `cd814f1`** |
| 3 | `CardStackLayout.resolve` ↔ `SwipeOverlay` | The release action and the drag label each decided the action, each with a bare `-60f` diagonal | Low | One `SwipeThresholds.resolve()`, `DIAGONAL_OFFSET` named | **fixed in `cd814f1`** (`SwipeResolveTest`, seen to fail on an injected `<=`) |
| 4 | `MockData.defaultResume` · `TemplatesViewModel` · `ResumePreviewScreen` | Default template `"modern"` spelled three times; the preview's `templates.first { … }` throws `NoSuchElementException` on an unknown id | Medium | `MockData.DEFAULT_TEMPLATE_ID` + `templateFor()` with fallback | **fixed in `dbb1970`** (`TemplatesViewModelTest`) |
| 5 | `LinkedInAuthManager.handleAuthCallback` ↔ `../draft/server.js` `/auth/:provider/callback` | App parses `linkedin_import=success&data=<base64 JSON>`. Server's callback redirects to `returnTo` with `authContext`, `oauthProvider`, `oauthStatus`, never emits either parameter, and ignores `source=android`. It is a LOGIN flow, not an import. Independently, the deep-link host is `YOUR_DOMAIN.com`, so no callback reaches the app | **High** (feature dead; `SYNC.md` said ✅) | Server: a mobile import endpoint; then `assetlinks.json`; then the manifest host | **blocked-on-owner** (server) — false comments corrected in `e8d4929` |
| 6 | `AuthRepository.signIn` ↔ contract `PublicUser` | Reads `user.email` and `user.name`; `PublicUser` has `id, username, isAdmin, planTier, applyMode, allowedModes, capabilities, domainProfileComplete` — neither field. `AuthedUser.email/name` are always `""`/`null` (unused today) | Low | Drop the fields or read what the contract defines | open |
| 7 | `ProfileScreen` ↔ `PublicUser.isAdmin` / `planTier` | Hardcoded "Sri Yellela" / "Admin • Pro" for every user; the contract carries the real values | Medium | Part of 2b1 | **blocked-on-owner** (2b1) |
| 8 | `AdminDashboardScreen` ↔ `AdminJobsScreen` / `MockData` | Subtitle says "5 sources"; the Jobs screen lists 3. "4 users, 1 suspended" and "8 launch flags" are literals beside the data they describe | Low | Derive, or remove with 2b1 | **blocked-on-owner** (2b1) |
| 9 | `ActionBadge.meta` ↔ `SwipeOverlay` ↔ README | Left swipe shows "SKIP" while dragging and "Snoozed" after; `Dislike` (unreachable) says "Skipped" | Low | One vocabulary | open |
| 10 | `LinkedInResumeFields` | `photoUrl` camelCase beside `given_name`/`family_name` snake_case in one type | Low | With #5 | blocked-on-owner |

**Checked and consistent:** `USER_AGENT = "resume-master-mobile"` matches the server's mint and revoke
(`server.js` 6677, 6685). `tiers_include=direct,guest` is split by `jobQuery.js`'s `toArray`. The
vendored contract is byte-identical to `../draft/contract/mobile-api.v1.json` (sha256 `0642924…`)
and its LF-normalised hash equals `CHECKSUMS.json` (`4283b9ef…`). `ContractJob` has exactly the
contract's 37 `Job` fields (`ContractDriftTest`).

### Shape 2 — a handler wired to nothing, or to the wrong thing

Searched by **identifier reference**: every callback-typed parameter and every declared member in
`app/src/main` was traced to its readers, not just `{}` call sites.

| # | Location | Evidence | Severity | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | `TemplatesViewModel.select` | Wrote `_selectedTemplateId`, read only by the picker's highlight. Preview and PDF read `resume.templateID`. `expected:<[classic]> but was:<[modern]>` | Medium | Write the resume's `templateID` | **fixed in `dbb1970`** |
| 2 | `ActionBadge(onDismiss)` + `JobsViewModel.lastAction` | `onDismiss` passed by no caller; `lastAction` a bare `SwipeAction`, so a StateFlow dropped the second identical swipe and it got no badge. Injected: `Actual: SwipeEvent(action=Queue, seq=0)` | Medium | `SwipeEvent(action, seq)`; badge keys on the event | **fixed in `dbb1970`** |
| 3 | `SwipeThresholds.MIN_VELOCITY` | 0 references | Low | Delete | **fixed in `dbb1970`** |
| 4 | `JobsScreen` Filter `IconButton(onClick={})`; `JobsViewModel.setFilters` | Dead control; `setFilters` has **0 callers** — the `UnifiedSearchBar` precedent exactly | Medium | A filter sheet calling `setFilters`, or remove the button | open |
| 5 | `AuthViewModel.signOut` / `AuthRepository.signOut` | **0 UI callers.** A user cannot withdraw this device's credential short of clearing app data | **High** | Sign-out row in Profile → `signOut()` → navigate to `login` | open (UI; needs a device to verify) |
| 6 | `SectionRow` swipe `EndToStart` | `if(section.isVisible) onHide() else onDelete()` — the ternary's fallback arm is an **unconfirmed, persisted delete** of a hidden section | Medium | Confirm or undo | open |
| 7 | `ResumeViewModel.reorderSections` | 0 callers, beside a `DragHandle` icon described "Reorder" | Low | Wire drag, or drop the handle | open |
| 8 | `SwipeAction.Dislike` | No gesture or button produces it; JobsViewModel's header calls it one of "the only actions that write" | Medium | Owner: should left be a dislike (server-persisted pass) or a local skip? | **blocked-on-owner** |
| 9 | Admin: FAB "Add listing", "Remove", "Impersonate read-only" (`onClick={}`); Suspend/Delete mutate local MockData | 2b1 decision: destructive controls stay dead until they reach a server | Medium | Build flavour (2b1) | **blocked-on-owner** (2b1) |
| 10 | `AssistChip(onClick={})` ×3 (flag platforms, user plan, queue status) | Display chips rendered as tappable | Low | Non-clickable label | open |
| 11 | Unreferenced declarations | `HapticUtil.softTap/mediumTap`, `BadgeState`, `SurfaceCard`, `Spacing`, `Radius`, `ApiException`, `ResumeViewModel.save()` and `.ready`, `LinkedInResumeFields.photoUrl/given_name/family_name`, `Job.postedDate`, `JobFeedPage.total/reason/droppedRows`, `Template.fontStyle` (and `layout`, in the PDF) | Low | Delete, or wire (`fontStyle`/`layout` are the template picker's unkept promise) | open |

### Shape 3 — silence reported as success

| # | Location | Evidence | Severity | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | `JobsViewModel.write` → `JobsScreen.BoardState` | A failed Star sets `_error`, but `BoardState` renders only when the stack is empty, and the next good page clears it. The header claims "A failure is surfaced, not swallowed" | Medium | Snackbar on `error` while cards remain | open (UI) |
| 2 | `SecureTokenStore.save` / `.clear` | `commit()` returns a Boolean that is discarded. Sign-in reports success when the token was not stored; sign-out reports signed out when it was not cleared | Medium | Check it; fail the sign-in / surface the clear failure | open (EncryptedSharedPreferences: androidTest only) |
| 3 | Feed `FeedError.UNAUTHORIZED` | Rendered as "Could not load your board"; `hasToken()` stays true, so every launch lands on a board that cannot load, with no way back to sign-in (see Shape 2 #5) | Medium | On 401: clear the token, route to `login` | open |
| 4 | `JobFeedPage.droppedRows` | Counted, read by nothing; `ContractJob.fromJson`'s doc says "the caller counts what it dropped" | Low | Log / surface a non-zero count | open |
| 5 | `ResumePreviewScreen` export | `exporting=true`, synchronous export on the main thread, `exporting=false` — "Exporting..." and the spinner can never render; file I/O on the main thread | Low | Run the export in a coroutine | open |
| 6 | `AdminFlagsScreen` snackbar | "changes apply on next app open across all platforms" after mutating local MockData | Medium | 2b1 | **blocked-on-owner** |
| 7 | `LinkedInAuthManager.handleAuthCallback`, `ResumeBuilderScreen` import notice | Decode failure → `null`, no message; "Name and email imported" shown even for a blank email | Low | With Shape 1 #5 | blocked-on-owner |

**Checked and accepted:** `ResumeViewModel.mutate` dropping edits before hydration is documented,
deliberate, and unreachable from the UI (nothing editable renders until `ready`). The `exhausted`
early-return in `JobRepository.feed` is unreachable (the VM checks `isExhausted` first).

### Shape 4 — NULL-hostile predicates over optional data

| # | Location | Evidence | Severity | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | `ResumeDao` (Room SQL) | Predicates only on non-null key columns (`resume_id`, `section_id`, `id`); the one nullable column, `template_id`, is never filtered | — | — | **clean** |
| 2 | `tiers_include` (client default ↔ `jobQuery.js`) | Server coalesces NULL to `'unknown'` in both `IN` and `NOT IN`; the client's `direct,guest` therefore excludes a NULL tier, matching `AutomationTier.from(null) = UNKNOWN` | — | — | **clean, consistent** |
| 3 | `SecureTokenStore.token()` | The absolute window is enforced only when `issuedAtMs > 0 && absoluteSeconds > 0`; a zero **fails open** — the token never expires locally. AuthRepository's comment claimed "instantly expired" | Low | Treat ≤ 0 as expired, or reject at `save` | comment corrected in `e8d4929`; code open |
| 4 | `ContractJob.toUiJob` | `postedDate = (discoveredAt ?: 0L) * 1000L` — null coerced to 1970-01-01, the "null is not zero" rule the ATS band follows | Low (no reader today) | Make `postedDate` nullable | open |
| 5 | `PagingMode.from` | null/unknown `paging` → `OFFSET`, which then reads `page`/`totalPages` as real | Low (no reader today) | Unknown → drop both | open |

### Shape 5 — a test that pins a defect or asserts the wrong thing

| # | Location | Evidence | Severity | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | `ContractJobDecodeTest` header | Called its org.json "REAL". It is Maven's; the phone runs AOSP's, where `optString(key, fallback)` on an explicit JSON null returns `"null"`, not `fallback` (from the AOSP source, not device-measured). The null-handling tests say nothing about bare `optString` | Medium | Header now says so, and why `optStringOrNull` must stay isNull-first | comment **fixed in `6225938`** |
| 1a | Bare `optString` on keys that could be null | `AuthRepository` (`authContext`, `token`, `user.username/email/name`), `LinkedInAuthManager`, `JobRepository.errorOf/codeOf/jobId` — on a device a JSON null there becomes the string `"null"` (e.g. a `"null"` token would be persisted) | Low (the server does not send null for these today) | isNull-guarded reads | open |
| 2 | `ContractDriftTest` checksum comment | "This repo is CRLF while the desktop repo runs autocrlf" — neither repo is CRLF in the object store (`git ls-files --eol`: `i/lf w/crlf` in both) | Low | Correct the reason | **fixed in `6225938`** |
| 3 | `ExampleUnitTest.addition_isCorrect` | `assertEquals(4, 2 + 2)` — asserts nothing about the app, and is 1 of the "40 JVM tests" every status doc quotes | Low | Delete (changes documented counts) | open |
| 4 | `ContractJobDecodeTest` "no band label renders the number" | Negative assertions over constant AtsBands copy for five scores. Meant to protect "the card never shows the score"; the render site, `JobCard.BandChip`, is untested, and the copy is constant, so it can fail only if someone edits AtsBands | Low | Pin at the render site (Compose test, device) | open |

**New tests in this sweep, each seen to FAIL before its fix:** `JobsViewModelTest` (2 — stack
resurrection, repeated confirmation), `TemplatesViewModelTest` (2 — selection reaches the resume;
unknown id falls back), `SwipeResolveTest` (4 — failed on an injected `<=` at the diagonal),
`JobCardTextTest` (1). `TemplatesViewModelTest`'s fallback case and `SwipeResolveTest` pin
refactors rather than behaviour changes; their failure was observed by injection.

**A harness race found and fixed (`ea6c1d0`):** the repeated-swipe case returned before its own page-2 prefetch landed, and one full run failed in tearDown with `Dispatchers.Main is used concurrently with setting it`. It now waits for the fetch; three consecutive `--rerun-tasks` runs were green.

**Checked and clean:** `ExampleInstrumentedTest` pins the applicationId deliberately.
`ResumeMappingTest` uses a fixture whose stored and natural orders disagree — it tests behaviour.

### Shape 6 — a claim with no code, or code with no claim

| # | Location | Evidence | Severity | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | `JobCard` expanded text | Appended a hardcoded "Requirements include strong Kotlin, product judgment, testing discipline…" to **every** job's description — invented requirements under a real employer's name on the live board | **High** | Show only the posting's text | **fixed in `e8d4929`** (`JobCardTextTest`) |
| 2 | `README.md` | "full admin panel" (MockData); "Velocity-sensitive card stack" (no velocity term; `MIN_VELOCITY` unreferenced — **both claims the doc listed were still present**); "Android Studio Ladybug (2024.2+)" (predates AGP 9.0.1); Play-store steps with no signing config; no mention of sign-in, the server, or `JAVA_HOME` | Medium | Rewrite to present truth, with a "Not working yet" list | **fixed in `ac19496`** |
| 3 | `SYNC.md` Android column | ✅ for LinkedIn import and manual application tracking (neither works / exists); ✅ for auth (sign-out unreachable); job feed ❌ / `/api/jobs` 🔲 while Android consumes it. **Doc premise partly stale:** auto-apply queue was already ❌ at HEAD | Medium | Rewrite the Android column; mark Web/iOS as not re-verified | **fixed in `ac19496`** |
| 4 | `ANDROID.md` | 2b2 "backup excludes … UNTOUCHED Studio templates" — false, the token is excluded from both channels; 2c "blocked on 2b" — the feed has largely landed; "Two base URLs, one fake" — half fixed in `c40b343` | Medium | Correct the status rows and findings | **fixed in `ac19496`** |
| 5 | `PHASE_2A.md` | Same false backup premise; same wrong CRLF reason as Shape 5 #2 | Low | Stale-premise note | **fixed in `ac19496`** |
| 6 | `AndroidManifest.xml` comment | "Nothing depends on this filter today … the import path does not need the deep link" — false: it is the only way the callback could reach the app | Medium | Correct it | **fixed in `e8d4929`** |
| 7 | `LinkedInAuthManager` header | "The server … redirects back with mapped name/email" — it does not (Shape 1 #5) | Medium | Correct it | **fixed in `e8d4929`** |
| 8 | `AuthRepository` comment | "Zero would make token() treat the credential as instantly expired" — it fails open (Shape 4 #3) | Low | Correct it | **fixed in `e8d4929`** |
| 9 | `ProfileScreen` | Hardcoded identity and plan for every user | Medium | 2b1 | **blocked-on-owner** |
| 10 | `JobCard` footer | "Nothing is sent to the employer until you approve it" — true that nothing is sent; there is no approve step (2d) | Low | Reword when 2d lands | open |
| 11 | Backup: `backup_rules.xml` keeps the résumé database in Google backup deliberately; ANDROID.md 2b2 step 2 says exclude it | Contradiction between a doc instruction and code | Medium | Owner decides; then verify with a real backup/restore on a device | **blocked-on-owner** |

**Checked and true:** `SecureTokenStore`'s claim that `rm_secure_prefs` is named in both XML files
(three places). `AtsBands` names only tests that exist (`44a7e17`).

---

## Blocked on the owner, the server, or a device

1. **App Links host** — `YOUR_DOMAIN.com` stays until `https://jobsviadraft.com/.well-known/assetlinks.json`
   serves this app's release fingerprint (server + release signing). By design; see the manifest.
2. **LinkedIn import** — needs a server-side mobile import endpoint (Shape 1 #5).
3. **Admin panel, 2b1** — build flavour; dead destructive controls; the Profile identity; the Flags
   snackbar; the dashboard's literal counts.
4. **Backup, 2b2** — whether the résumé is backed up; the backup/restore verification (device).
5. **Left swipe** — local skip or server-persisted dislike (`SwipeAction.Dislike` has no gesture).
6. **Store readiness** — no signing config, no Data Safety, `versionCode` 1, launcher icon,
   `com.draft.android` Play availability unverified.

## Commits

| Hash | Subject |
|---|---|
| `cd814f1` | fix: Shape 1 — every swiped card came back when the next page loaded |
| `dbb1970` | fix: Shape 2 — the template picker selected nothing, and a repeated swipe confirmed nothing |
| `ea6c1d0` | test: Shape 2 — JobsViewModelTest raced its own prefetch against resetMain |
| `6225938` | docs: Shape 5 — the decoder tests run a different org.json than the phone does |
| `e8d4929` | fix: Shape 6 — the expanded job card invented requirements for every employer |
| `ac19496` | docs: Shape 6 — README, SYNC.md, ANDROID.md and PHASE_2A.md described code that was not there |

No Shape 3 or Shape 4 code fixes: every candidate is either UI-only (unprovable on the JVM without a
device) or blocked; the one Shape 4 comment correction rides in `e8d4929`.

## Final

`./gradlew assembleDebug testDebugUnitTest` at `ac19496`: **BUILD SUCCESSFUL, 49 tests, 0 failures, 0 errors, 0 skipped.** Baseline was 40 / 0. Instrumented tests not run (no device).
