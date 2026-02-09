# TensorFlow Lite Model Requirements

## Overview

The card-scanner module requires a TensorFlow Lite model for score classification. A placeholder file has been created, but you need to replace it with a trained model.

## Model Location

Place your trained model at:
```
card-scanner/src/main/assets/score_classifier_model.tflite
```

## Model Requirements

### Input Specification
- **Format**: Single grayscale image (1 channel)
- **Dimensions**: 224×64 pixels (width × height)
- **Data type**: Float32
- **Normalization**: [0.0, 1.0] (pixel values divided by 255)
- **Tensor shape**: `[1, 64, 224, 1]` (NHWC format: Batch, Height, Width, Channels)

### Output Specification
- **Format**: Softmax probabilities for 5 classes
- **Classes**: [0, 1, 2, 3, 5] (score values in trials scoring)
- **Data type**: Float32
- **Tensor shape**: `[1, 5]`

### Class Mapping
| Class Index | Score Value | Meaning |
|-------------|-------------|---------|
| 0           | 0           | Clean (no faults) |
| 1           | 1           | One fault |
| 2           | 2           | Two faults |
| 3           | 3           | Three faults |
| 4           | 5           | Failure (5 points) |

## Training Recommendations

### Dataset
- Collect images of score card rows under various conditions:
  - Different lighting (indoor/outdoor)
  - Different card conditions (clean/dirty/worn)
  - Different handwriting styles
  - Various angles (ideally normalized during preprocessing)

### Preprocessing
The model receives images preprocessed by OpenCV:
1. Resized to 224×64 pixels
2. Converted to grayscale
3. Normalized to [0.0, 1.0]

In Phase 4, additional preprocessing will be added:
- Contrast enhancement (CLAHE)
- Noise reduction (Gaussian blur)
- Rotation correction (perspective transform)

### Architecture Suggestions
- **MobileNetV2** (fast, mobile-optimized)
- **EfficientNet-Lite** (good accuracy/size tradeoff)
- Custom CNN (if you have sufficient training data)

### Export to TFLite
After training, convert to TensorFlow Lite:

```python
import tensorflow as tf

# Load trained Keras model
model = tf.keras.models.load_model('trained_model.h5')

# Convert to TFLite with optimizations
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS
]

tflite_model = converter.convert()

# Save model
with open('score_classifier_model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Testing Your Model

### 1. Unit Test (Android Instrumented Test)

Run the instrumented tests to verify model loading:

```bash
./gradlew :card-scanner:connectedAndroidTest
```

This will:
- Load the model from assets
- Run inference on a test image
- Verify output format is correct

### 2. Integration Test (In App)

Run the full app on a device/emulator:

```bash
./gradlew :app:installDebug
```

1. Navigate to leaderboard
2. Tap a rider's name
3. Tap camera icon in score entry screen
4. Capture an image of a score card
5. Verify scores appear in the form

Expected behavior:
- Processing completes in <1000ms
- 15 scores returned (one per section)
- All scores are valid (0, 1, 2, 3, or 5)

### 3. Check Logs

Monitor logcat for errors:

```bash
adb logcat | grep -E "CardScanner|TFLite"
```

## Performance Optimization (Phase 4+)

Once your model works, consider these optimizations:

### 1. XNNPACK Delegate (2-3x speedup)
Uncomment in `OpenCVCardScannerService.kt`:
```kotlin
val options = Interpreter.Options().apply {
    setNumThreads(4)
    addDelegate(XNNPackDelegate())  // <-- Uncomment this
}
```

### 2. GPU Delegate (10x speedup on supported devices)
Add to dependencies and initialize:
```kotlin
import org.tensorflow.lite.gpu.GpuDelegate

val gpuDelegate = GpuDelegate()
val options = Interpreter.Options().apply {
    addDelegate(gpuDelegate)
}
```

**Note**: GPU delegate requires model compatibility (no unsupported ops).

### 3. Model Quantization
Convert to INT8 quantized model for:
- 4x smaller file size
- 2-4x faster inference
- Minimal accuracy loss

```python
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_data_gen  # Required for INT8
converter.target_spec.supported_types = [tf.lite.constants.INT8]
```

## Current Limitations (Phase 3)

As of Phase 3 implementation:
1. **Grid detection is stubbed**: Entire image is passed as each of 15 rows
2. **Minimal preprocessing**: No contrast/brightness enhancement
3. **No optimization delegates**: Using CPU with 4 threads

These will be addressed in Phase 4 (real grid detection) and beyond.

## Troubleshooting

### Model fails to load
**Error**: `Failed to load TFLite model from assets/score_classifier_model.tflite`

**Solutions**:
- Verify file exists at correct location
- Check file is valid .tflite format (not corrupted)
- Ensure file is not empty (placeholder is 0 bytes)

### Inference returns wrong scores
**Causes**:
- Input normalization mismatch (model trained with different range)
- Wrong input dimensions
- Class mapping mismatch

**Debug**:
1. Log input tensor min/max values
2. Verify model input shape matches expected
3. Check training normalization matches implementation

### Out of memory errors
**Solutions**:
- Reduce model size (use quantization)
- Lower input resolution (e.g., 112×32 instead of 224×64)
- Call `mat.release()` to free OpenCV memory immediately after use

### Slow performance (>2 seconds)
**Solutions**:
- Add XNNPACK delegate
- Use quantized model
- Reduce input resolution
- Profile with Android Profiler to identify bottleneck

## Model File Checklist

Before deploying:
- [ ] Model file placed at `card-scanner/src/main/assets/score_classifier_model.tflite`
- [ ] Input shape is `[1, 64, 224, 1]`
- [ ] Output shape is `[1, 5]`
- [ ] Model expects Float32 inputs normalized to [0.0, 1.0]
- [ ] Tested with instrumented tests (pass)
- [ ] Tested in app with real score cards (reasonable accuracy)
- [ ] Inference completes in <1000ms on target device
