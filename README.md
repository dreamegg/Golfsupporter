# ⛳ Golf Score Tracker

An offline-first Android app for tracking golf scores, penalties, and results for
2–8 players over an 18-hole round. Built from the PRD (v1.3) — this repository
implements the **MVP (v1.0)** scope.

## Features (MVP)

- **Game setup wizard (3 steps):** players (2–8, with default names A/B/C… and
  quick-pick chips that remember previously-used names), an easy-to-read per-hole
  par grid (front/back groups, tap to cycle 3→4→5), and game options.
- **Game history:** completed rounds are kept and browsable from the home screen
  ("지난 게임 보기"); tap any past game to view (and still edit) its result.
- **Round types:** `FULL_18`, `FRONT_9`, `BACK_9`, and `SPLIT` (front/back with a
  pause in between).
- **Two score-input modes:**
  - **Button mode** — `[−] value [+]` tap controls.
  - **Scroll mode** — vertical swipe with a drum-roll display and haptic ticks
    (20 dp per step; swipe up = under par, down = over par).
- **Automatic score naming** (Eagle / Birdie / Par / Bogey …) with colour coding.
- **Resume / continue:** every hole, penalty change, and `onStop` is auto-saved.
  The home screen shows a "continue" banner for any in-progress round.
- **Front-nine interstitial:** after hole 9 (FULL_18 / SPLIT) a summary screen
  offers "play back nine now" or "play back nine later".
- **Penalty system:** toggleable, with 8 built-in penalties + custom items, entered
  via a bottom sheet. Penalties are kept fully independent from score (per PRD).
- **Result screen:** ranking, horizontally-scrolling scorecard with front/back/total
  subtotals, and a penalty summary tab.
- **Edit mode:** tap a cell to edit a score or penalty, with real-time recalculation,
  edited-cell highlighting, edit-history (`ScoreEdit`) persistence, and cancel/save.
- **Share:** result summary via the Android share sheet.

## Architecture

MVVM + a thin Clean-Architecture split, all offline via Room.

```
ui/            Jetpack Compose screens + ViewModels (Hilt)
  home/        Continue banner + new game entry
  setup/       3-step setup wizard
  round/       Round play (button/scroll cards, penalty sheet, interstitial)
  result/      Result + edit mode
  navigation/  Navigation Compose graph
  theme/       Material 3 theme
data/
  model/       Domain models (PRD Section 5)
  local/       Room entities, DAOs, converters, DB, default penalties
  repository/  GameRepository (single source of truth) + mappers
di/            Hilt module
util/          ScoreLabel, RoundRules (pure logic, unit-tested)
```

### Tech stack
- Kotlin, Jetpack Compose, Material 3
- MVVM + StateFlow, Navigation Compose
- Room (SQLite) for offline persistence
- Hilt for DI
- KSP for annotation processing

## Persistence model

Room tables mirror the PRD data model: `game_sessions` (with flattened
`GameState`), `game_settings`, `players`, `hole_configs`, `hole_scores`,
`penalty_records`, `penalty_types` (global catalogue), and `score_edits`.

Auto-save triggers (PRD Section 3.3):
1. "Next hole" → confirmed hole score + penalties.
2. Penalty change → immediate save.
3. `onStop` → current (unconfirmed) hole snapshot + live state.

## Location features (v2.0 — phase 1)

The location-based features from PRD Section 10.1 are implemented:

- **GPS course detection** (`CourseRepository.nearby`) — finds courses within 5 km
  of the current location, sorted by distance (Haversine).
- **Course search** by name (offline against the local cache).
- **Par auto-load** — selecting a course fills the per-hole pars (G-004).
- **Weather + wind** in the round header — temperature, condition emoji, and
  wind direction/speed, auto-refreshed every 30 minutes (G-006/G-007/G-008).
- **Offline-first** (G-009/G-010): courses are cached in Room; a bundled sample
  catalog (`SampleCourses`) is seeded on first launch so detection works without
  a network. Weather and the remote course API degrade silently to "off" when
  unavailable.

Architecture additions:
```
data/location/   LatLng (+ Haversine) and FusedLocationProvider
data/weather/    Weather model, Retrofit WeatherApi (OpenWeatherMap), repository
data/course/     GolfCourse model, Room cache (CachedCourseEntity/CourseDao),
                 GolfCourseApi (stub by default), CourseRepository, SampleCourses
di/NetworkModule Retrofit/OkHttp + location/course bindings
```

The remote golf-course source is bound to a no-network stub
(`StubGolfCourseApi`); swap the binding in `NetworkModule` for a Retrofit-backed
implementation (GolfAPI.io / TheGolfAPI) when a provider/key is available.

### Configuration

Weather requires an OpenWeatherMap API key. Add it to `local.properties`:

```properties
OPENWEATHER_API_KEY=your_key_here
```

(or set the `OPENWEATHER_API_KEY` environment variable). Without a key, weather
is simply not shown — everything else still works offline.

## Building

Requires Android Studio (Koala+) with Android SDK and network access to Google's
Maven repository.

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew test               # run JVM unit tests (ScoreLabel, RoundRules)
```

- `minSdk` 26 (Android 8.0), `targetSdk`/`compileSdk` 34.
- No runtime permissions required — all data is stored in the local Room DB.

## Roadmap (not yet implemented)

- v1.1: result sharing extras, game history browser, scroll-mode sensitivity,
  penalty reordering, original-value tooltips.
- v2.0 (phase 2/3): team/betting games (skins, nassau, stableford, …),
  random team draw, and money settlement. See PRD Section 10.2.
