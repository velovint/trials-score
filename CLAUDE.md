# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TrialsScore is a motorcycle trials scoring application for Android.
Riders compete in multiple sections across multiple loops,
accumulating points from 0 (clean) to 5 (failure) per section.
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
- Key classes: `EventScoreViewModel`, `ScoreCardViewModel`, `EditRiderViewModel`, `EventSettingsViewModel`
- `RiderStandingTransformation` handles leaderboard sorting logic

**UI Layer** (`/components/`):
- Pure Jetpack Compose with Material 3
- Navigation defined in `TrialsScoreApplicationComponent.kt` (see UI Entry Points below)

### UI Entry Points

Map of key use cases to UI components (all in `/app/src/main/java/net/yakavenka/trialsscore/components/`):

| Use Case                              | Entry Point                    | Navigation Route                | Notes                                                       |
|---------------------------------------|--------------------------------|---------------------------------|-------------------------------------------------------------|
| View leaderboard                      | `LeaderboardScreen.kt`         | `leaderboard`                   | Start destination; displays all riders grouped by class     |
| Configure event (sections/loops/classes) | `EventSettingsScreen.kt`    | `settings`                      | Accessed via menu from `LeaderboardScreen`                  |
| Import riders from CSV                | `LeaderboardScreen.kt`         | `leaderboard`                   | Import action in menu, handled by `EventScoreViewModel`     |
| Add new rider                         | `EditRiderScreen.kt`           | `add_rider`                     | Accessed via FAB on `LeaderboardScreen`                     |
| Edit rider info                       | `EditRiderScreen.kt`           | `edit_rider/{riderId}`          | Accessed via pencil icon from `LoopScoreEntryScreen`        |
| Enter/view rider scores               | `LoopScoreEntryScreen.kt`      | `points_entry/{riderId}/{loop}` | Tap rider name from `LeaderboardScreen`                     |
| Share scores (screenshot)             | `ScreenshotLeaderboardScreen.kt` | `screenshot_view`             | Compact view for Android screenshots                        |
| Export results to CSV                 | `LeaderboardScreen.kt`         | `leaderboard`                   | Export action in menu, handled by `EventScoreViewModel`     |

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

**Core Tables**:
- `rider_score`: id (PK), name, riderClass
- `section_score`: Composite PK [riderId, loopNumber, sectionNumber], points (-1 to 5)

**Key DTOs**: `RiderScoreAggregate`, `RiderScoreSummary`, `SectionScore.Set`

Database pre-populated from `assets/score_database.db` with destructive migration fallback.

### Leaderboard Sorting Logic

`RiderStandingTransformation` sorts by: class → finished/unfinished → points (asc) → cleans (desc) → name (alpha). Standings calculated per class. Special symbols (⠂, ⠅, ⠇, ⠭) indicate incomplete loops.

### Data Flow

`Room DB → Repository (Flow) → ViewModel (LiveData) → UI (Compose State)`

UI observes LiveData with `observeAsState()`. Updates propagate automatically via Flow streams.

## Testing

**Naming conventions**:
- Test methods: `<methodBeingTested>_<expectation>_<optionalConditions>`
- Test fakes: `Fake<InterfaceName>` in same package as interface under `/app/src/test/java/`

**Unit tests** (`/test/`): JUnit with Hamcrest assertions, JavaFaker for data generation
**Instrumented tests** (`/androidTest/`): Compose testing with `ANDROIDX_TEST_ORCHESTRATOR` and `clearPackageData: true`

## Key Implementation Details

- **Score Entry**: Scores are lazily initialized (created in-memory, persisted only when scored). Loop navigation via tabs.
- **CSV Import/Export**: Uses OpenCSV. Export includes full results with per-section scores. Import adds riders (name, class only). See `docs/index.md` for format details.
- **Preferences**: DataStore with SharedPreferences migration on first launch
- **Room Schema**: Stored in `app/schemas/` (KSP arg `room.schemaLocation`)

## Technology Stack

Kotlin (JVM 17) • Jetpack Compose + Material 3 • Room + KSP • Dagger Hilt • DataStore • Coroutines/Flow • Gradle version catalog • SDK: Min 26, Target/Compile 35

**Release builds**: R8 with `minifyEnabled` and `shrinkResources`, custom rules in `proguard-rules.pro`
