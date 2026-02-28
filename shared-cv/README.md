# Shared Computer Vision Library

Shared OpenCV-based computer vision components for motorcycle trials score card image processing.

## Overview

This library provides core image processing capabilities for:
- **Card Isolation**: Detecting and extracting score cards from camera images
- **Row Segmentation**: Dividing isolated cards into individual score rows
- **Row Normalization**: Preparing row images for ML inference
- **Error Handling**: Structured error reporting for scan failures

## Core Components

### CardIsolator
Detects and isolates score cards from input images.
- Input: Full camera image
- Output: Isolated card region (640×1753 pixels)
- Method: Edge detection and contour analysis

### RowSegmenter / MorphologicalRowSegmenter
Segments isolated card into individual score rows.
- Input: Isolated card image
- Output: Row regions (Y-coordinate pairs)
- Method: Morphological operations to identify row clusters

### RowNormalizer / OpenCVRowNormalizer
Normalizes row images for ML inference.
- Input: Row region and card image
- Output: `RowImage` (66×640 normalized float buffer, native byte order)
- Format: [0, 1] float values, suitable for TensorFlow Lite
- Buffer type: `ByteBuffer` (direct, native byte order)

### RowClassifier
Interface for score classification (implemented by `ml-inference` module).

## Architecture

```
Camera Image
    ↓
CardIsolator (detect & extract)
    ↓
Isolated Card (640×1753)
    ↓
RowSegmenter (identify row locations)
    ↓
Row Regions (Y-coordinates)
    ↓
RowNormalizer (extract & normalize)
    ↓
RowImage (ByteBuffer, 66×640, float32, [0,1])
    ↓
RowClassifier (ML inference)
```

## Error Handling

`ScanError` provides structured error types:
- `CardNotFound` - Card detection failed
- `InvalidCard` - Card detected but processing failed
- `RowSegmentationFailed` - Row extraction failed
- `ImageLoadFailed` - Input image corruption/invalid format

See `ScanDebugObserver` for debug observation of scan pipeline.

## Technology

- **OpenCV**: 4.10.0 (Android SDK)
- **Kotlin**: 2.3.0
- **SDK**: Min 26, Target/Compile 35

## Testing

Unit and instrumented tests use:
- JUnit with Hamcrest assertions
- `ANDROIDX_TEST_ORCHESTRATOR` with `clearPackageData: true`
- Test naming: `<methodBeingTested>_<expectation>_<optionalConditions>`

## Integration

The `ml-inference` module depends on this library for score classification. See `ml-inference/README.md` for the complete pipeline.
