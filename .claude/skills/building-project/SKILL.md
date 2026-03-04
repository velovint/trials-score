---
name: building-project
description: Build, test, and validate the TrialsScore Android project. Use this skill whenever the user asks to build the app, run tests, debug, validate the ML pipeline, prepare training data, or retrieve test logs. Covers unit tests, instrumented tests, ML pipeline validation, and logcat retrieval.
---

# Building TrialsScore

This skill provides all build, test, and validation commands for the TrialsScore Android project.

## Verifying the project builds

When asked to "verify that the project builds" or "verify the build",
this means compile the application using the existing production model and confirm all tests pass.

```bash
./gradlew check connectedCheck
```

**Retraining the model** (`./gradlew buildProductionModel`) is a separate workflow and is NOT part of verifying the build.

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

### Run a specific instrumented test class
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="net.yakavenka.cardscanner.CardScanningPipelineTest"
```

### Run a specific instrumented test method
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="net.yakavenka.cardscanner.CardScanningPipelineTest#scan_returnsSuccess_whenAllStepsSucceed"
```

## Retrieving Test Logs

### Locate instrumented test logcat files
Each instrumented test gets its own logcat file at:
```
<module>/build/outputs/androidTest-results/connected/debug/<device>/logcat-<ClassName>-<methodName>.txt
```

### Filter logcat output
Example — retrieve logs filtered for a specific tag:
```bash
grep "MorphologicalRowSegmenter" "shared-cv/build/outputs/androidTest-results/connected/debug/Pixel_5_API_33(AVD) - 13/logcat-net.yakavenka.cardscanner.MorphologicalRowSegmenterTest-segment_producesCorrectNumberOfRows.txt"
```

## ML Pipeline (Proof of Concept)

The ML pipeline for CV-based card scanning uses instrumented tests for data preparation and validation. Key modules:

- **:shared-cv** - `CardImagePreprocessor` for card detection, grid line extraction, row segmentation
- **:data-prep-tool** - Training data preparation via instrumented tests
- **:ml-inference** - TensorFlow Lite model inference and validation

### Prepare training data
Run instrumented tests to prepare training data:
```bash
./gradlew :data-prep-tool:connectedAndroidTest
```

**Process:**
1. Load score card images from test assets
2. Use `CardImagePreprocessor` to detect card, extract grid lines, segment into 15 rows
3. Resize rows to 640×66 pixels
4. Export to device storage organized by label (0/, 1/, 2/, 3/, 5/)

**Retrieve prepared data from device:**
```bash
adb pull /storage/emulated/0/Android/data/net.yakavenka.dataprep/files/training-data/
```

**Label format:** Row labels are provided manually (CSV or inspection). Label 9 indicates "skip this row" (corrupted/unclear data).

### Validate ML pipeline
Run all ML pipeline tests:
```bash
./gradlew :shared-cv:connectedAndroidTest :ml-inference:connectedAndroidTest
```

## Project Structure

**Module Dependencies:**
```
:app → :ml-inference → :shared-cv
:app → :shared-cv
:data-prep-tool → :shared-cv
```

For detailed implementation notes, see `tech-docs/IMPLEMENTATION_PLAN.md`.

## Notes

- Legacy command-line tasks (`prepareTrainingData`, `packageTrainingData`) are temporarily disabled during POC development as the `:training-tool` module was converted to an Android library (`:data-prep-tool`).
- Instrumented tests require an Android emulator or connected device.
- Use specific test class/method commands to debug individual tests rather than running the full suite.
