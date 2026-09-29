# Draft - Cross-Platform Sync

Last updated: 2026-05-08. **Android column re-verified against this repository's code on
2026-09-29** (`docs/CORRUPTION_SWEEP_ANDROID.md`). The Web and iOS columns were NOT re-checked from
here and are as stale as their date; do not read them as verified.

⚠ Until 2026-09-29 this table marked Android ✅ for LinkedIn import and application tracking,
neither of which has ever worked on Android, in the present tense.

## Feature Registry

| Feature | Web | iOS | Android | Notes |
|---------|-----|-----|---------|-------|
| Resume builder | ✅ | ✅ | ✅ | Android: on-device only (Room); not synced to the server |
| Template picker | ✅ | ✅ | ⚠ | Android: selection sets the resume's template, but the PDF applies only its accent colour |
| PDF export | ✅ | ✅ | ✅ | |
| User auth / profile | ✅ | ✅ | ⚠ | Android: sign-in + durable mobile token work; no screen reaches sign-out; Profile is hardcoded |
| LinkedIn OIDC Import | ✅ | ✅ | ❌ | Android: server never sends the `linkedin_import`/`data` the app parses; deep-link host is a placeholder |
| Extension (scrape-free) | ✅ | n/a | n/a | Chrome companion sends visible JD text to ATS tool |
| Manual application tracking | ✅ | ✅ | ❌ | Android: no code |
| Auto-apply queue | ❌ | ❌ | ❌ | Android: a right-swipe queues locally, in memory, and submits nothing |
| Job feed | ❌ | ❌ | ✅ | Android: consumes `GET /api/jobs` (cursor paging, `tiers_include`). Row was "Job feed (Adzuna)"; Web/iOS cells not re-checked |

## Pending Sync Items

### Adzuna + Indeed Job Feed
- Shipped on: not yet (stub in place)
- Missing on: Web, iOS, Android
- Priority: High
- Notes: Phase 1 of API migration puts stub at /api/jobs. Full implementation follows after Adzuna credentials are added to .env.

## Data Model Version

Current shared model version: 1.0.1

(Stale for Android: the model this repo decodes is generated from the vendored contract,
`contract/mobile-api.v1.json`, **v1.1.1**. The changelog below predates it.)

### Model changelog
- 1.0.1: LinkedIn import is OIDC name/email only; job feed is API-backed stub pending Adzuna/Indeed services.
- 1.0.0: Resume, Job, Template, SwipeAction defined.

## Phase: Scraping Removal + OIDC Migration

### Completed This Phase
| Feature | Web | iOS | Android | Extension |
|---|---|---|---|---|
| LinkedIn OIDC Import | ✅ | ✅ | ❌ (see registry) | ✅ (popup button) |
| Extension scrape-free rebuild | ✅ | n/a | n/a | ✅ |
| Apify / scraping code removed | ✅ | n/a | n/a | ✅ |
| /api/jobs stub (Adzuna-ready) | ✅ | 🔲 | ✅ (consumed) | n/a |
| Job auto-apply automation removed | ✅ | ✅ | ✅ | n/a |
| Privacy docs updated | ✅ | n/a | n/a | n/a |

### Deferred to Next Phase
| Item | Tracking |
|---|---|
| DB schema rename (scraped_jobs → jobs) | docs/migration/DEFERRED_SCRAPE_CLEANUP.md |
| Admin panel scrape monitor → sync monitor | docs/migration/DEFERRED_SCRAPE_CLEANUP.md |
| Test file updates | docs/migration/DEFERRED_SCRAPE_CLEANUP.md |
| Adzuna + Indeed aggregator implementation | Next Codex prompt |

### Credentials Needed Before Next Phase
ADZUNA_APP_ID and ADZUNA_APP_KEY in .env
(Get from developer.adzuna.com — instant approval)
