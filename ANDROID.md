# Android — living work doc

**Repo:** `resume-master-android` · **Backend:** `../draft` ·
**Contract:** `../draft/contract/mobile-api.v1.json` (read the version from the file)

**Last reconciled:** 2026-09-07. Status rows 2b2 and 2c and "Findings to carry forward" corrected
2026-09-29 by the corruption sweep (`docs/CORRUPTION_SWEEP_ANDROID.md`) — they were claims the code
had already overtaken.

> ⚠ **Re-derive state from the repo before starting anything.** The desktop equivalent of this doc
> has been the stale thing three times — agents land work faster than a doc gets updated. Check
> `git log` and the test counts yourself; treat every status below as a claim to verify.

---

## Status

| Phase | State |
|---|---|
| **Phase 1** — audit | ✅ DONE. Findings folded into this doc |
| **Phase 2a** — toolchain · auth · API layer · persistence | ✅ DONE — 40 JVM + 13 instrumented tests, real emulator. `../draft/docs/aj2-android-phase2a.md` |
| **Phase 2b1** — admin build flavour | **OPEN** |
| **Phase 2b2** — backup excludes | **PARTLY DONE** — the token store (`rm_secure_prefs.xml`) is excluded from BOTH channels in `backup_rules.xml` and `data_extraction_rules.xml`. The résumé database is deliberately still backed up, which contradicts step 2 below: **owner decision**. Never verified by a real backup/restore |
| **Phase 2c** — swipe feed | **PARTLY LANDED**, not blocked: `GET /api/jobs` with cursor paging and restart-on-filter-change, `/api/jobs/interact`, the ATS band chip, server-side `tiers_include`. Missing: left is Skip (local-only, returns next launch), not a pass — `Dislike` has no gesture; tap expands, there is no detail view; no filter UI (`setFilters` has zero callers) |
| **Phase 2d** — review queue | OPEN, blocked on 2c |
| **Store readiness** | OPEN — nothing signed, no Data Safety declaration |

---

## ⛔ The four constraints — carry into every decision

**1 · A SWIPE QUEUES, IT NEVER SUBMITS.** Applications reach real employers under the user's real
name and cannot be recalled.

Phase 1 found this violated in **five** places, and it reached a confirmation that lied:
`README.md`, `models/SwipeAction.kt` (`data object Apply`), `ui/jobs/CardStackLayout.kt`,
`ui/jobs/SwipeOverlay.kt` (`"APPLY NOW"` in green with a check), and `ui/jobs/ActionBadge.kt` —
which displayed **"Application sent"** for 2.5 seconds having sent nothing, because the view model
already routed `Apply` into the same local queue list.

All five fixed. `QueuePriority` now does `queue.add(0, job)` — queue-and-prioritise. Velocity may
distinguish a stronger signal; **the stronger signal must also only queue.** Do not reintroduce it.

**2 · GENERATION IS DEFERRED TO APPROVAL.** Desktop task D landed this. A right-swipe must cost
nothing; the model call happens when the user approves. Caps: `APPLY_DAILY_APPROVAL_CAP` (30) now
bounds spend, and the old queue cap bounds a free action. Both are typed response fields carrying
`limit` and `remaining` — **render `remaining`; never swallow it.**

**3 · GATED JOBS CANNOT BE COMPLETED ON A PHONE.** `automationTier` `gated`/`account` (Workday,
Meta, Amazon) needs a desktop extension borrowing the user's authenticated portal session under a
per-gesture `activeTab` grant. No extension exists on Android. **`unknown` is also
`completableOnMobile: false`** — broader than "gated". Filter **server-side** via
`tiers_include`/`tiers_exclude`, never client-side: the server pages before the client filters, so
hiding rows afterwards yields short pages and a count that disagrees with the list.

**4 · ELIGIBILITY ANSWERS ARE NEVER GESTURED.** Work authorisation, sponsorship, years of
experience and custom answers are attestations. Stored profile only.

---

## OPEN 2b1 — Admin panel behind a build flavour

**Owner decision, 2026-09-05: build flavour, not deletion.** The six screens survive for internal
builds and can never reach a Play release.

```
1. Put ui/admin/ (Dashboard, Users, Jobs, Queue, Flags, Analytics) behind a build flavour —
   `internal` includes, `release`/`play` excludes. SOURCE-SET separation, not a runtime flag: a
   runtime flag still ships the code and the strings, and a flag can be flipped.
2. Remove the admin entry point from ProfileScreen in the release flavour. It is currently a
   tappable row beside a hardcoded "Admin • Pro" label, reachable by every user, with no auth to
   gate against.
3. NavGraph admin routes must not exist in the release flavour. Absent beats 404; 404 beats
   rendering fabricated data.
4. ⛔ The destructive controls stay DEAD in BOTH flavours until they reach a real server: "Delete",
   "Suspend", "Impersonate read-only". They mutate a local MutableStateFlow and look like they work.
   An internal build that appears to delete a user and does not is worse than one with the buttons
   removed.
5. VERIFY BY BUILDING THE RELEASE BUNDLE AND GREPPING IT: no admin class, no admin string resource,
   no admin route. Then build `internal` and confirm the screens still render. A flavour that
   excludes the source set but leaves strings in the release bundle is a half-fix a reviewer finds.
```

## OPEN 2b2 — Backup excludes

> ⚠ **The first paragraph below is stale** (checked 2026-09-29). Both XML files now exclude
> `rm_secure_prefs.xml`; the résumé database is intentionally NOT excluded (see the comment in
> `backup_rules.xml`), which is the open owner decision against step 2. VERIFY has not been done.

```
android:allowBackup="true" with backup_rules.xml AND data_extraction_rules.xml both still the
UNTOUCHED Studio templates (comments only). Flagged in Phase 1 as "write the excludes BEFORE the
first token exists". The token now exists — and so does a persisted résumé, since Room landed.
Both are currently swept into Google cloud backup by default.

1. Write real excludes in BOTH files. They cover different mechanisms — Auto Backup and Android 12+
   device-to-device transfer — and filling only one leaves the other open.
2. Exclude the auth token store (EncryptedSharedPreferences / DataStore) and the Room database
   holding the résumé. A résumé carries a home address, phone and employment history.
3. Consider android:allowBackup="false" outright. Decide deliberately and state which you chose:
   backup is a real convenience for a résumé builder, so targeted excludes are probably better than
   a blanket off.
4. This feeds the Play Data Safety declaration, which must not contradict the live policy at
   https://jobsviadraft.com/privacy. If the token is backed up, that is a disclosable data flow.

VERIFY: trigger a real backup and restore on an emulator; confirm the token and résumé do NOT come
back. Inspecting the XML is NOT sufficient — these rules fail quietly when a path is wrong.
```

---

## OPEN 2c — Swipe feed (after 2b)

✅ **BUILT 09-30 (`f7fdc2c`)** — DeckViewModel, Today's draft on the shared deal, right = server Draft, two gestures; 58/58 JVM, 15/15 instrumented, checked on the emulator.

⛔ **REVISED 09-30 — 2c is now the DECK**, per `../draft/docs/UI_REDESIGN.md` §3 and §5.2. The text
block below is kept for its still-true constraints (cursor paging for "All jobs", `interact` not the
toggles, the band not a number). What changes:

- The feed is `GET /api/draft/today` — the day's FROZEN deal, shared with the web — not the paged board.
- **Right = Draft on the server:** `POST /api/apply/runs`, `approvalMode` omitted, `surface:"deck"`.
  ⛔ Today's right swipe (`Queue` / `QueuePriority`) records LOCAL intent only — it reaches no server,
  is lost on restart, and its badge says "Queued for review". Until 2c lands the badge must say
  what is true.
- **Left = Pass** (saved; `8445e88`, unchanged). **Save = a button**, not the diagonal: the diagonal
  Star and the hard-right "Queue first" are removed (two accident-prone motions, two intents, and
  "Queue first" has no server meaning).
- The finished-deck state links to 2d.

```
Build against the contract, not against the old hand-rolled parser (replaced in 2a).

- Right = shortlist/queue (free). Left = pass. Tap = detail. See constraint 1: no gesture submits.
- CURSOR PAGING, not offset. Offset silently skips rows the user swiped away — measured server-side
  at 6 of 25 with 3 swipes per page, because every dislike sets disliked=1 and the default board
  excludes disliked rows. Cursors are valid only for the filter set that produced them: restart
  paging on any filter change and handle the filter-mismatch rejection explicitly.
- Use /api/jobs/interact, NOT PATCH /api/jobs/{id}/starred — the latter TOGGLES, so a retried swipe
  on a flaky phone network undoes itself and returns 200. The contract excludes the toggle routes.
- The card must carry what a ~2-second decision needs and no more. A thin card produces volume
  without fit, which makes the product useless while looking busy. Show the ATS BAND (Strong /
  Moderate / Weak / Not enough signal) — not a number. ρ = 0.746 supports ordering, not precision.
- Gate on automationTier per constraint 3.
```

## OPEN 2d — Review queue (after 2c)

```
THIS IS THE PRODUCT'S PROMISE, not a secondary screen. A user who swipes 40 and reviews 0 has spent
money and applied to nothing. It must be as easy to work through as swiping is.

Endpoints already exist and are well specified in the contract:
  GET  /api/apply/pending     "Applications previewed and awaiting the user's approval. The review inbox."
  POST /api/apply/approve     "Approve previewed applications and submit them. THE moment of submission."
  POST /api/apply/reject · GET /api/apply/questions · POST /api/apply/answers · GET /api/apply/readiness
Schemas: PendingItem · PendingResume · OpenQuestion · DailyCap · QueueCap · BlockingJob ·
UserCapabilities

Approving shows each resolved answer with its provenance and confidence, and is where generation
happens. Group by obstacle where one action unblocks many (portal batches) and by application where
many obstacles block one — the desktop panel learned this the hard way.
```

---

## Store readiness — all open

| Item | State |
|---|---|
| Keystore | ❌ none; `.gitignore` correctly excludes `*.jks`/`*.keystore` |
| `signingConfigs` | ❌ absent — release has no signing block |
| `isMinifyEnabled` | ⚠ false on release |
| Play Console | ❌ no evidence in-repo |
| Data Safety | ❌ none — depends on 2b2 |
| Privacy policy link | ❌ not in app or manifest |
| `versionCode` | 1, never incremented |
| Deep link host | ❌ `YOUR_DOMAIN.com` with `autoVerify="true"` — App Links verification will fail |
| App icon | ⚠ default Studio green robot |
| Permissions | ✅ INTERNET, VIBRATE — minimal and appropriate |

---

## Findings to carry forward

**Two base URLs, one fake — half fixed.** `LinkedInAuthManager` now reads `AppGraph.baseUrl`
(`c40b343`); every request resolves through `ApiClient.DEFAULT_BASE_URL` /
`EMULATOR_HOST_BASE_URL`. The `AndroidManifest` deep-link host is still `YOUR_DOMAIN.com`, by design
until `https://jobsviadraft.com/.well-known/assetlinks.json` is served (see the manifest comment).
The LinkedIn import behind it is broken server-side too — the callback never carries the parameters
the app parses.

**The shared-repository bug (fixed in 2a, worth remembering).** `ResumeViewModel` built its own
repository and `viewModel()` scopes to the `NavBackStackEntry`, so builder and preview each held a
separate mock-seeded instance. Every edit was invisible in the preview, and **"Export as PDF" wrote
the mock résumé to a file the user then shared.** Now shared via `AppGraph`.

**`fallbackToDestructiveMigration` is deliberately absent.** On this database it means "silently
delete the user's résumé on the first schema change." Omitting it throws loudly in development
instead. Do not add it.

**README claims Phase 1 disproved — corrected 2026-09-29.** "full admin panel" (6 MockData
screens), "Velocity-sensitive card stack" (false on Android — `resolve()` read displacement only and
`MIN_VELOCITY = 800f` was declared and never referenced; the constant is now deleted, `dbb1970`;
**it is genuinely true on iOS**, so do not carry the finding across). "Swipe-card job feed" is now
true — it is the live board. `SYNC.md`'s Android column was rewritten against the code the same day.
