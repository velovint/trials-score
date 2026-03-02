# ML Inference Module

TensorFlow Lite model inference for score classification in motorcycle trials score cards.

## Overview

This module provides TensorFlow Lite (LiteRT) inference for classifying individual score rows from score cards as one of 5 possible scores (0, 1, 2, 3, 5).

## Architecture

### Pipeline

```
RowImage (66×640 float buffer)
    ↓
TFLite Interpreter.run()
    ↓
Score Classification (0, 1, 2, 3, 5)
```

### Model Details

- **Format**: TensorFlow Lite (.tflite)
- **Input**: 66×640×1 float32 tensor, normalized [0, 1]
- **Output**: Classification scores for 5 classes
- **Inference**: CPU (4 threads), optional GPU acceleration
- **Location**: `assets/score_classifier_model.tflite` (auto-downloaded at build time)

### Model Download

The model is downloaded automatically during the build process via Gradle configuration:
1. `:ml-pipeline-tool` defines the `downloadModel` task and publishes `build/ml-models/` via the `modelFiles` consumable configuration
2. `:ml-inference` resolves the `mlModelArtifact` configuration and adds `ml-models/` as an asset source directory
3. Asset merging automatically depends on `downloadModel` — no manual wiring needed

To manually download the model:
```bash
./gradlew :ml-pipeline-tool:downloadModel
```

## Dependencies

- **shared-cv**: For card scanning and row normalization
- **TensorFlow Lite**: 2.17.0
- **TensorFlow Lite GPU**: Optional GPU support
- **TensorFlow Lite Support**: 0.4.4 (with TFLite API excluded to avoid conflicts)
- **OpenCV**: 4.10.0
- **Kotlin**: 2.3.0

## Key Classes

Check source code in `src/main/java/net/yakavenka/mlinference/` for:
- `TFLiteInterpreter` - Wrapper for TensorFlow Lite inference
- Score classification logic
- Input/output buffer management

## Testing

- Unit tests: JUnit with Hamcrest
- Instrumented tests: `ANDROIDX_TEST_ORCHESTRATOR` with `clearPackageData: true`
- Test data: Real score card images with known scores

## SDK & Build

- Min SDK: 26
- Target/Compile SDK: 35
- Kotlin JVM target: 17

## Related Documentation

- `shared-cv/README.md` - Card scanning and preprocessing pipeline
- `ml-pipeline-tool/README.md` - Training data preparation and Kaggle pipeline tasks
- `CLAUDE.md` - Project overview and guidelines
