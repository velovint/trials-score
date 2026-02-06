# Training Data Preparation Tool - Implementation Summary

**Date**: 2026-02-05
**Status**: Complete ✓

## Overview

Implemented a Gradle-based training data preparation tool for the trials score card ML model. The tool downloads, unpacks, and processes score card images to extract individual row images organized by score value.

## Architecture Decision: Copy vs. Share

**Initial Plan**: Share `CardImagePreprocessor` source between `card-scanner` and `training-tool` modules
**Final Implementation**: Copy `CardImagePreprocessor` to `training-tool`

### Rationale
- Simpler build configuration
- Avoided complex Gradle source set filtering
- Both modules can evolve independently if needed
- Source file is small (~70 lines) and stable
- No significant maintenance burden from duplication

## Implementation Details

### 1. CardImagePreprocessor Updates (card-scanner)

**File**: `card-scanner/src/main/java/net/yakavenka/cardscanner/CardImagePreprocessor.kt`

Changes:
- Removed `import android.util.Log`
- Removed `TAG` constant
- Replaced `Log.d()` with `println()`
- Added `import org.opencv.core.Rect`
- Implemented real row extraction (equal-height division):
  ```kotlin
  fun extractRowImages(image: Mat): List<Mat> {
      val imageHeight = image.height()
      val imageWidth = image.width()
      val rowHeight = imageHeight / NUM_SECTIONS

      return (0 until NUM_SECTIONS).map { rowIndex ->
          val y = rowIndex * rowHeight
          val height = if (rowIndex == NUM_SECTIONS - 1) {
              imageHeight - y  // Last row: take remaining pixels
          } else {
              rowHeight
          }
          val roi = Rect(0, y, imageWidth, height)
          Mat(image, roi).clone()  // Clone to create independent Mat
      }
  }
  ```

**Verification**: All card-scanner tests pass including `CardImagePreprocessorTest`

### 2. Gradle Configuration

**Files Modified**:
- `settings.gradle`: Added `include ':training-tool'`
- `gradle/libs.versions.toml`:
  - Added `kotlinx-cli = "0.3.6"`
  - Added `opencv-jvm = "4.9.0-0"`
  - Added `kotlin-jvm` plugin reference
- `build.gradle` (root):
  - Added `kotlin-jvm` plugin
  - Added training data tasks (see section 7)

### 3. training-tool Module

**New Module**: `training-tool/`

**build.gradle**:
```groovy
plugins {
    alias libs.plugins.kotlin.jvm
    id 'application'
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set('net.yakavenka.trialsscore.training.MainKt')
}

dependencies {
    implementation platform("org.jetbrains.kotlin:kotlin-bom:2.3.0")
    implementation "org.jetbrains.kotlin:kotlin-stdlib"
    implementation libs.opencv.jvm
    implementation libs.kotlinx.cli
    testImplementation libs.junit
    testImplementation libs.hamcrest.library
}
```

**Source Structure**:
```
training-tool/src/main/java/
├── net/yakavenka/cardscanner/
│   └── CardImagePreprocessor.kt       (copied from card-scanner)
└── net/yakavenka/trialsscore/training/
    ├── FilenameParser.kt              (new)
    ├── TrainingDataGenerator.kt       (new)
    └── Main.kt                        (new)
```

### 4. FilenameParser

**File**: `training-tool/src/main/java/net/yakavenka/trialsscore/training/FilenameParser.kt`

Features:
- Extracts 15-digit score sequences from filenames using regex `(\d{15})`
- Validates scores are in valid set: {0, 1, 2, 3, 5}
- Throws `IllegalArgumentException` for invalid filenames/scores

**Tests**: 8 test cases covering:
- Valid filenames (all scores, all zeros, all fives)
- Invalid scores (4, 6)
- Missing/too few digits
- Empty filenames

### 5. TrainingDataGenerator

**File**: `training-tool/src/main/java/net/yakavenka/trialsscore/training/TrainingDataGenerator.kt`

Features:
- Processes all JPG/JPEG/PNG images in input directory
- For each image:
  1. Parse scores from filename
  2. Load as grayscale
  3. Preprocess (resize to 640px)
  4. Extract 15 rows
  5. Save each row to `{score}/image_{idx}_row_{row}.png`
- Error handling: skip invalid files, continue processing
- Progress reporting: every 10 images
- Summary statistics: counts per score folder

**Mat Memory Management**:
- Properly releases all OpenCV Mats (image, preprocessed, rows)
- Uses try-finally blocks to ensure cleanup

### 6. Main Entry Point

**File**: `training-tool/src/main/java/net/yakavenka/trialsscore/training/Main.kt`

Features:
- CLI argument parsing with `kotlinx-cli`
- Required arguments: `--input-dir`, `--output-dir`
- OpenCV initialization: `OpenCV.loadLocally()`
- Input validation
- Exit codes: 0 (success), 1 (error)

**Usage**:
```bash
./gradlew :training-tool:run --args="--input-dir /path/to/images --output-dir /path/to/output"
```

### 7. Gradle Tasks (root build.gradle)

**Configuration**:
```groovy
ext {
    trainingDataUrl = project.findProperty('trainingDataUrl') ?:
                      System.getenv('TRAINING_DATA_URL') ?:
                      'https://example.com/training-data.tgz'
    trainingDataSourceDir = "$buildDir/training-data-source"
    trainingDataOutputDir = "$buildDir/training-data"
}
```

**Tasks**:

1. **downloadTrainingData**:
   - Downloads `.tgz` from configured URL
   - Skips if already downloaded
   - Supports custom URL via `-PtrainingDataUrl=...`

2. **unpackTrainingData**:
   - Depends on: `downloadTrainingData`
   - Unpacks archive to `build/training-data-source/`
   - Skips if already unpacked

3. **prepareTrainingData**:
   - Depends on: `unpackTrainingData`, `:training-tool:build`
   - Runs training data generator
   - Output to: `build/training-data/`

**Usage**:
```bash
# Use default URL
./gradlew prepareTrainingData

# Use custom URL
./gradlew prepareTrainingData -PtrainingDataUrl=https://example.com/data.tgz

# Via environment variable
export TRAINING_DATA_URL=https://example.com/data.tgz
./gradlew prepareTrainingData
```

### 8. Documentation

**Files Created**:
- `training-tool/README.md`: Complete usage guide with examples, troubleshooting
- Updated `CLAUDE.md`: Added training tool reference

## Output Structure

```
build/
├── training-data.tgz                    # Downloaded archive
├── training-data-source/                # Unpacked images
│   ├── IMG_001_012305555000005.jpg
│   └── ...
└── training-data/                       # Processed output
    ├── 0/                               # Clean sections
    │   ├── image_0_row_0.png
    │   └── ...
    ├── 1/                               # Score 1
    ├── 2/                               # Score 2
    ├── 3/                               # Score 3
    └── 5/                               # Score 5 (failure)
```

## Verification Results

All verification steps from the plan passed:

✓ 1. card-scanner builds successfully
✓ 2. card-scanner tests pass (including CardImagePreprocessorTest)
✓ 3. training-tool builds successfully
✓ 4. training-tool tests pass (FilenameParserTest: 8/8)
✓ 5. Gradle tasks defined (downloadTrainingData, unpackTrainingData, prepareTrainingData)
✓ 6. Dry-run shows correct task execution order
✓ 7. CLI --help works
✓ 8. Error handling works (non-existent directory)
✓ 9. Documentation complete

## Testing

### Unit Tests
- **FilenameParserTest**: 8 test cases, all passing
- Coverage: valid filenames, invalid scores, edge cases

### Integration Testing
- Build and compilation verified
- CLI argument parsing tested
- Error handling verified with invalid inputs

### Manual Testing (TODO)
- Create test dataset with sample images
- Run full pipeline with real data
- Verify output image quality and organization

## Dependencies Added

| Dependency | Version | Purpose |
|------------|---------|---------|
| kotlinx-cli | 0.3.6 | Command-line argument parsing |
| opencv-jvm | 4.9.0-0 | JVM OpenCV (image processing) |
| kotlin-jvm | 2.3.0 | Kotlin JVM plugin |

## Key Design Decisions

1. **JVM vs Android**: Pure JVM application for easier execution without emulator
2. **Copy vs Share**: Copied CardImagePreprocessor for simpler build configuration
3. **Equal-height division**: Sufficient for training data; real grid detection in Phase 4+
4. **Error handling**: Skip-on-error to process maximum data
5. **Mat cleanup**: Proper resource management with try-finally blocks
6. **Progress reporting**: Every 10 images to show activity without spam
7. **Gradle tasks**: Separate tasks for download/unpack/process for flexibility

## Future Enhancements

1. **Grid Detection**: Replace equal-height division with sophisticated grid detection
2. **Parallel Processing**: Process multiple images concurrently
3. **Data Augmentation**: Add rotation, scaling, brightness variations
4. **Validation**: Generate train/val/test splits
5. **Statistics**: Add more detailed statistics (file sizes, dimensions, etc.)
6. **Resume Support**: Skip already-processed images on re-run
7. **Configuration File**: Support YAML/JSON config for advanced options

## Notes

- CardImagePreprocessor now works in both Android (card-scanner) and JVM (training-tool) environments
- No Android dependencies remain in CardImagePreprocessor
- Training tool is standalone and not a runtime dependency of the main app
- Successfully tested on Fedora 43 with Java 21

## Related Files

- Implementation Plan: `/home/vitali/.claude/projects/-home-vitali-AndroidStudioProjects-TrialsScore/d6a5ada3-2488-4e85-aeb2-6f595b986c34.jsonl`
- User Documentation: `training-tool/README.md`
- Code Documentation: See inline comments in source files
