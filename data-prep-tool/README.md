# Training Data Preparation Tool

## Usage

### Basic Usage

```bash
./gradlew prepareTrainingData
```

### Custom Training Data URL

Specify a custom URL via command-line property:

```bash
./gradlew prepareTrainingData -PtrainingDataUrl=https://example.com/data.tgz
```

Or environment variable:

```bash
export TRAINING_DATA_URL=https://example.com/data.tgz
./gradlew prepareTrainingData
```

Or in `gradle.properties`:

```properties
trainingDataUrl=https://example.com/data.tgz
```

## Output

Processed training data is organized by score in `build/training-data/`:

```
build/training-data/
├── 0/   # Clean sections (score 0)
├── 1/   # Score 1
├── 2/   # Score 2
├── 3/   # Score 3
└── 5/   # Score 5 (failure)
```

Each row image is grayscale, 640px wide, and saved as PNG.
