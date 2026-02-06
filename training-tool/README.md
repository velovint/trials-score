# Training Data Preparation Tool

Prepares training data for the trials score card ML model by processing images with 15-digit score sequences.

## Overview

This tool:
1. Downloads a `.tgz` archive of score card images
2. Unpacks the images to a build directory
3. Processes each image to extract individual row images
4. Organizes extracted rows by score value into folders: `0/`, `1/`, `2/`, `3/`, `5/`

## Usage

### Basic Usage

```bash
./gradlew prepareTrainingData
```

This will:
- Download training data from the default URL (or configured URL)
- Unpack to `build/training-data-source/`
- Process images and output to `build/training-data/`

### Custom Training Data URL

You can specify a custom URL in three ways:

#### 1. Command-line property:
```bash
./gradlew prepareTrainingData -PtrainingDataUrl=https://example.com/data.tgz
```

#### 2. Environment variable:
```bash
export TRAINING_DATA_URL=https://example.com/data.tgz
./gradlew prepareTrainingData
```

#### 3. gradle.properties file:
Add to `gradle.properties`:
```properties
trainingDataUrl=https://example.com/data.tgz
```

### Individual Tasks

Run tasks separately if needed:

```bash
# Download only
./gradlew downloadTrainingData -PtrainingDataUrl=https://example.com/data.tgz

# Unpack only (requires download first)
./gradlew unpackTrainingData

# Process only (requires unpack first)
./gradlew :training-tool:run --args="--input-dir build/training-data-source --output-dir build/training-data"
```

## Input Format

Input images must have 15-digit score sequences in their filenames. Examples:
- `IMG_001_012305555000005.jpg` - scores: 0,1,2,3,0,5,5,5,5,0,0,0,0,0,5
- `555555555555555_test.png` - scores: 5,5,5,5,5,5,5,5,5,5,5,5,5,5,5
- `000000000000000_clean.jpg` - scores: 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0

Valid scores are: `0`, `1`, `2`, `3`, `5`

Images can be in JPG, JPEG, or PNG format.

## Output Structure

```
build/training-data/
├── 0/              # Clean sections (score 0)
│   ├── image_0_row_0.png
│   ├── image_0_row_4.png
│   └── ...
├── 1/              # Score 1
│   ├── image_1_row_2.png
│   └── ...
├── 2/              # Score 2
│   └── ...
├── 3/              # Score 3
│   └── ...
└── 5/              # Score 5 (failure)
    └── ...
```

Each row image is:
- Grayscale (single channel)
- 640px wide (height varies based on original aspect ratio)
- Saved as PNG

## Processing Details

For each input image:
1. Parse 15-digit score sequence from filename
2. Load image as grayscale
3. Resize to 640px width (maintaining aspect ratio)
4. Extract 15 individual row images (equal-height division)
5. Save each row to appropriate score folder

File naming: `image_{image_index}_row_{row_index}.png`
- `image_index`: Sequential index of input image (0, 1, 2, ...)
- `row_index`: Row number within image (0-14, top to bottom)

## Troubleshooting

### "Input directory does not exist"
- Ensure `downloadTrainingData` and `unpackTrainingData` tasks completed successfully
- Check that `build/training-data-source/` contains image files

### "Filename does not contain 15-digit score sequence"
- Verify filenames match the pattern (must contain exactly 15 consecutive digits)
- Example: `IMG_012305555000005.jpg` ✓ | `IMG_01230.jpg` ✗

### "Invalid score values"
- Only scores 0, 1, 2, 3, 5 are allowed
- Score 4 is invalid in trials scoring (use 3 or 5 instead)

### "Failed to load image"
- Check image file is not corrupted
- Ensure file extension matches actual format (JPG/PNG)

### OpenCV errors
- OpenCV is loaded automatically via `nu.pattern.OpenCV.loadLocally()`
- If errors occur, ensure JVM can access native libraries

## Configuration

### Default URL
The default training data URL is configured in root `build.gradle`:
```groovy
ext {
    trainingDataUrl = project.findProperty('trainingDataUrl') ?:
                      System.getenv('TRAINING_DATA_URL') ?:
                      'https://example.com/training-data.tgz'  // Change default here
}
```

### Output Directories
Configured in root `build.gradle`:
```groovy
ext {
    trainingDataSourceDir = "$buildDir/training-data-source"  // Unpacked images
    trainingDataOutputDir = "$buildDir/training-data"         // Processed output
}
```

## Dependencies

- **Kotlin 2.3.0** (JVM target 21)
- **OpenCV 4.9.0** (JVM, via org.openpnp:opencv)
- **kotlinx-cli 0.3.6** (Command-line parsing)

## Architecture Notes

This tool:
- Is a pure JVM application (not Android)
- Reuses `CardImagePreprocessor` from the `card-scanner` module
- Uses JVM OpenCV instead of Android OpenCV
- Can run on any JVM without Android SDK/emulator

The `CardImagePreprocessor` currently uses simple equal-height row division. Phase 4+ will implement sophisticated grid detection for production use.
