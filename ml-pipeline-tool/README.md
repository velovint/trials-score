# ML Pipeline Tool

Android library module that prepares training data and orchestrates the Kaggle ML pipeline.

## Responsibilities

1. **Training data preparation** — instrumented tests use `:shared-cv` to preprocess raw score card images into labeled row images for model training.
2. **Kaggle pipeline tasks** — Gradle tasks manage the full model update lifecycle.
3. **Model distribution** — exposes the downloaded model to `:ml-inference` via a consumable Gradle configuration (`modelFiles`), so asset merging automatically depends on the download.

## Kaggle Pipeline Tasks

```bash
./gradlew :ml-pipeline-tool:downloadRawImages      # Download raw card images from Kaggle
./gradlew :ml-pipeline-tool:connectedAndroidTest   # Preprocess images into training data (requires device)
./gradlew :ml-pipeline-tool:uploadTrainingData     # Upload processed data to Kaggle dataset
./gradlew :ml-pipeline-tool:triggerKaggleTrain     # Push notebook and wait for training
./gradlew :ml-pipeline-tool:downloadModel          # Download trained .tflite model
./gradlew :ml-pipeline-tool:buildProductionModel   # Full pipeline in one command
```

Or via the root convenience wrapper:
```bash
./gradlew buildProductionModel   # delegates to :ml-pipeline-tool + runs verifyPipeline
```

## Version-Based Caching

Downloads are cached by Kaggle dataset/kernel version:
- `checkRawImagesVersion` → feeds `downloadRawImages` inputs (skips download if dataset unchanged)
- `checkModelVersion` → feeds `downloadModel` inputs (skips download if kernel output unchanged)

## Output Layout

```
build/
├── raw-pictures/raw/          # Downloaded raw card images (androidTest assets)
├── ml-models/                 # Downloaded .tflite model (published via modelFiles configuration)
└── training-data-upload/      # Staged training data for Kaggle upload
```

## Kaggle Files

```
kaggle/
├── notebooks/                 # Training notebook pushed to Kaggle Kernels
└── training-data/
    └── dataset-metadata.json  # Kaggle dataset metadata for uploads
```

## Prerequisites

- `~/.kaggle/kaggle.json` with valid API credentials
- Android device or emulator for `connectedAndroidTest`

## Related Documentation

- `tech-docs/training-pipeline-with-kaggle.md` — full pipeline architecture
- `ml-inference/README.md` — how the model is consumed
- `shared-cv/README.md` — CV preprocessing used during data prep
