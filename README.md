# Draft — Android

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android_9+-3DDC84?style=flat&logo=android&logoColor=white)

Native Android companion to Draft. Sign in with a Draft account; the swipe-card feed is the live
board from `GET /api/jobs`, cursor-paged and filtered server-side to jobs a phone can complete.
Resume builder with section-level swipe actions, stored on the device. Template picker and PDF
export.

There is also an admin panel — six screens that render **MockData only** (fabricated users, flags,
queue and analytics), reachable by every user from Profile. It is due to move behind an internal
build flavour (`ANDROID.md`, 2b1) and must not ship in a Play build.

*Every statement below was checked against the code on 2026-09-29; see
`docs/CORRUPTION_SWEEP_ANDROID.md`.*

## Interactions
- Card stack with spring snap-back. **Displacement-based, not velocity-sensitive**: a release is
  resolved from how far the card travelled (`SwipeThresholds.resolve`). There is no velocity term.
- Swipe right: queue | Firm swipe right: queue first | Up-and-right: star | Left: skip
  - **Nothing is ever sent to an employer from a swipe.** Queueing is local and in memory only —
    the review queue (`ANDROID.md`, 2d) does not exist yet, so a queued job goes nowhere and is
    forgotten when the app closes.
  - Star is saved to your account (`/api/jobs/interact`). Skip saves nothing; a skipped job comes
    back on the next launch.
- Material swipe actions on resume sections: edit, hide, delete
- A badge confirming each swipe

## Not working yet
- **LinkedIn import** — broken on both sides: the server never sends what the app parses, and the
  deep-link host is still a placeholder. Needs a server change first.
- **Sign out** — the code exists; no screen reaches it.
- **The Jobs filter button** does nothing.
- **Section reordering** — the drag handle is decorative.
- **Profile** shows a hardcoded name and "Admin • Pro" for every user.
- Templates change only the PDF's accent colour; their font style and layout are not applied.

## Getting Started

### Requirements
- An Android Studio release that supports **AGP 9.0.1** and `compileSdk = 37`. (This README used
  to say Ladybug 2024.2, which predates AGP 9 and cannot open the project.)
- Android 9+ device or emulator (API 28+)
- A Draft server. Debug builds talk to `http://10.0.2.2:3001` — the host machine as seen from the
  emulator; release builds to `https://jobsviadraft.com`.

### Run
1. Clone the repo
2. Open in Android Studio
3. Run on emulator or device (Shift+F10, or Ctrl+R on macOS)

From the command line, `./gradlew` needs `JAVA_HOME` pointing at a JDK 17+ — for example Studio's
bundled `jbr`. `gradle.properties` deliberately no longer pins `org.gradle.java.home`.

```
./gradlew assembleDebug testDebugUnitTest
```

### Play Store
**Not ready.** There is no signing config, no Data Safety declaration, and the App Links host is a
placeholder. See the store-readiness table in `ANDROID.md`.

## Related Repos
- [Draft Web](https://github.com/Sri-Yellela/draft)
- [Draft iOS](https://github.com/Sri-Yellela/resume-master-ios)
