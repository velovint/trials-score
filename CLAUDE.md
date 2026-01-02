# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TrialsScore is a motorcycle trials scoring application for Android.
Riders compete in multiple sections across multiple loops,
accumulating points from 0 (clean) to 5 per section.
Lower total score wins.

## User Documentation

For complete usage instructions, see `docs/index.md`. This includes:
- Application setup and configuration
- Event workflow (configure event → import riders → enter scores → share/export results)
- CSV import/export format details
- Screenshot sharing instructions
- Device recommendations

Reference this file when planning features related to user workflows, data import/export formats, or understanding the end-user experience.

## Build Commands

### Build the app
```bash
./gradlew build
```

### Run unit tests
```bash
./gradlew test
```

### Run a specific test class
```bash
./gradlew testDebugUnitTest --tests "net.yakavenka.trialsscore.viewmodel.RiderStandingTransformationTest"
```

### Run instrumented tests (requires emulator/device)
```bash
./gradlew connectedAndroidTest
```

### Commit messages
Use concise commit messages and include only a summary for simple changes.

## Architecture

### Layer Structure

The application code is located in `app` module and follows MVVM architecture with three main layers:

**Data Layer** (`/data/`):
- Room database with `ScoreDatabase` as single source of truth
- `RiderScoreDao` provides reactive Flow-based queries
- Repositories abstract data operations:
  - `SectionScoreRepository` - CRUD for riders and section scores. Most operations return partial/as-entered data that needs to be normalized to fill gaps for not entered sections. 
  - `ScoreSummaryRepository` - Aggregated leaderboard data
  - `UserPreferencesRepository` - Event settings via DataStore
  - `CsvExchangeRepository` - CSV Import/export functionality

**ViewModel Layer** (`/viewmodel/`):
- All ViewModels are `@HiltViewModel` with constructor-injected dependencies
- `EventScoreViewModel` - Leaderboard, import/export, data management
- `ScoreCardViewModel` - Individual rider score entry
- `EditRiderViewModel` - Rider creation/editing
- `EventSettingsViewModel` - Event configuration
- `RiderStandingTransformation` - Complex sorting logic for leaderboard rankings

**UI Layer** (`/components/`):
- Pure Jetpack Compose with Material 3
- Navigation routes defined in `TrialsScoreApplicationComponent.kt`:
  - `leaderboard` - Main screen (start destination)
  - `points_entry/{riderId}/{loop}` - Score entry
  - `add_rider` / `edit_rider/{riderId}` - Rider management
  - `settings` - Event configuration
  - `screenshot_view` - Full leaderboard view

### UI Entry Points

Map of key use cases to UI components (all in `/app/src/main/java/net/yakavenka/trialsscore/components/`):

| Use Case | Entry Point | Navigation Route | Notes |
|----------|-------------|------------------|-------|
| View leaderboard | `LeaderboardScreen.kt` | `leaderboard` | Start destination; displays all riders grouped by class |
| Configure event (sections/loops/classes) | `EventSettingsScreen.kt` | `settings` | Accessed via menu from `LeaderboardScreen` |
| Import riders from CSV | `LeaderboardScreen.kt` | `leaderboard` | Import action in menu, handled by `EventScoreViewModel` |
| Add new rider | `EditRiderScreen.kt` | `add_rider` | Accessed via FAB on `LeaderboardScreen` |
| Edit rider info | `EditRiderScreen.kt` | `edit_rider/{riderId}` | Accessed via pencil icon from `LoopScoreEntryScreen` |
| Enter/view rider scores | `LoopScoreEntryScreen.kt` | `points_entry/{riderId}/{loop}` | Tap rider name from `LeaderboardScreen` |
| Share scores (screenshot) | `ScreenshotLeaderboardScreen.kt` | `screenshot_view` | Compact view for Android screenshots |
| Export results to CSV | `LeaderboardScreen.kt` | `leaderboard` | Export action in menu, handled by `EventScoreViewModel` |

When implementing features, consult `docs/index.md` for user workflow context and expected behavior.

### Dependency Injection

Hilt provides all dependency injection:
- `TrialsScoreApplication` is `@HiltAndroidApp`
- `DatabaseModule` (`/data/DatabaseModule.kt`) provides:
  - `ScoreDatabase` (Singleton)
  - `RiderScoreDao`
  - `DataStore<Preferences>` with SharedPreferences migration
- All repositories are `@Singleton` with `@Inject` constructors
- ViewModels use `@HiltViewModel` with injected repositories

### Database Schema

**RiderScore** (table: `rider_score`):
- `id` (PK, auto-generated), `name`, `riderClass`

**SectionScore** (table: `section_score`):
- Composite PK: `[riderId, loopNumber, sectionNumber]`
- `points` - Score value (-1 = not attempted, 0-5 = actual score)

**Important DTOs**:
- `RiderScoreAggregate` - Embeds RiderScore with list of SectionScores (1-to-many)
- `RiderScoreSummary` - Query result with calculated totals and standings
- `SectionScore.Set` - Helper for managing collections with business logic

Database is pre-populated from `assets/score_database.db` and uses destructive migration fallback.

### Leaderboard Sorting Logic

The `RiderStandingTransformation` class implements complex sorting:
1. Group by rider class
2. Within each class, finished riders rank before unfinished
3. Finished riders sort by: points (ascending), then cleans (descending)
4. Unfinished riders sort alphabetically by name
5. Standings calculated separately per class

Special symbols (⠂, ⠅, ⠇, ⠭) indicate incomplete loops in the UI.

### Data Flow

Reactive data flow using Kotlin coroutines:
```
Room DB → Repository (Flow) → ViewModel (LiveData) → UI (Compose State)
```

UI observes LiveData with `observeAsState()`. Write operations launch in ViewModel coroutine scope. Updates propagate automatically via Flow streams.

## Testing

### Test Naming Conventions

**Test methods**: `<methodBeingTested>_<expectation>_<optionalConditions>`
- Example: `fetchOrInitRiderScore_returnsOnlyRequestedLoop_whenScoresExist`

**Test fakes/doubles**: `Fake<InterfaceName>`
- Example: `FakeRiderScoreDao`, `FakeFileStorage`, `FakeDataStore`
- Pattern: Always prefix with "Fake" followed by the interface name
- Location: Test source set (`/app/src/test/java/`) under the same package as the interface
  - Example: `RiderScoreDao` interface in `net.yakavenka.trialsscore.data` → `FakeRiderScoreDao` in `/app/src/test/java/net/yakavenka/trialsscore/data/`

**Unit Tests** (`/test/`):
- Pure JUnit tests with Hamcrest assertions
- Uses JavaFaker for test data generation

**Instrumented Tests** (`/androidTest/`):
- UI tests using Compose testing framework
- Runs with `ANDROIDX_TEST_ORCHESTRATOR` for test isolation
- Tests run with `clearPackageData: true` for clean state

## Key Implementation Details

### Score Entry
- Score radio buttons display score value as background overlay
- Points entry screen uses tabs for loop navigation
- Scores are lazily initialized: blank sections are created in-memory when viewing a loop, and persisted to the database only when scored
- Real-time calculation of totals displayed in UI

### CSV Import/Export
Uses OpenCSV library. Export format includes full results with per-section scores. 
Import adds riders (name, class only).

### Preferences Migration
`UserPreferencesRepository` includes migration from SharedPreferences to DataStore on first launch.

### Room Schema Location
Room schema files stored in `app/schemas/` directory (configured via KSP arg `room.schemaLocation`).

## Technology Stack

- **Language**: Kotlin (JVM target 17)
- **UI**: Jetpack Compose, Material 3
- **Database**: Room with KSP annotation processing
- **DI**: Dagger Hilt
- **Storage**: DataStore for preferences
- **Navigation**: Jetpack Compose Navigation
- **Async**: Kotlin Coroutines and Flow
- **Build**: Gradle with version catalog (`libs.versions.toml`)
- **SDK**: Min 26, Target/Compile 35

## ProGuard/R8

Release builds use:
- `minifyEnabled = true`
- `shrinkResources = true`
- Custom rules in `proguard-rules.pro`
