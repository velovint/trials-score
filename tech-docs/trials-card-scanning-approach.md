# Trials Card Scanning Approach (v2.0)

## 1. Card Structure & Scoring
- **Data Grid:** 15 rows (sections) × 5 columns (penalty scores: 0, 1, 2, 3, 5).
- **Scoring Logic:** Exactly one mark per row.
- **Output:** An array of 15 integers `[score_1, ..., score_15]`.

---

## 2. Overall Pipeline Strategy: The "Data Flywheel"
The approach is a two-phase process that balances predictable math with flexible pattern recognition.

### Phase 1: Grid Detection (Computer Vision)
- **Tool:** OpenCV for Android.
- **Goal:** Isolate the data grid from headers and extract 15 individual row images.
- **Strategy:** Instead of training a model, use a **predictable algorithm** (Morphological operations + line spacing analysis).
- **UX Integration:** Implement a **Viewfinder UI** to guide the user's hand, ensuring the card is aligned before processing begins.

### Phase 2: Row Classification (Machine Learning)
- **Tool:** LiteRT (formerly TensorFlow Lite).
- **Goal:** Classify each row image into one of 5 classes [0, 1, 2, 3, 5].
- **Strategy:** **Row-level classification** is more robust than cell-level as the model sees the relative position of the mark within the full row context.

---

## 3. Training & Data Management

### Realistic Training Data
- **Generalization:** Do not use perfect digital scans. Collect ~50 real photos of physical cards in varying light (shadows, sunlight) and backgrounds.
- **Auto-Labeling:** Use the Phase 1 OpenCV logic to programmatically crop rows from these photos, then manually verify labels to build a dataset of 1,500–2,500 rows.
- **Augmentation:** Use Python libraries (`Albumentations`) during training to artificially add rotation, blur, and contrast shifts to mimic shaky hands and poor camera sensors.

### MLOps & Storage
- **Source:** Use **Google Cloud Storage (GCS)** or Google Drive for image datasets to avoid bloating the Git repository.
- **Training Environment:** Use **Google Colab** with **TFLite Model Maker** (Python) for free GPU access and simplified transfer learning.
- **Format:** Package training data as **TFRecords** for faster loading during training.

---

## 4. Android Implementation Stack

### Framework: LiteRT via Play Services
- **APK Size:** Using the **Play Services Runtime** reduces app size by ~15MB as the engine is shared across the device.
- **Performance:** Enable **GPU or XNNPACK delegates** via the `CompiledModel` API to achieve sub-30ms inference per row.

### Memory & Threading
- **Native Memory:** Manually call `Mat.release()` on all OpenCV objects to prevent "Out of Memory" crashes in the JNI layer.
- **Background Processing:** Run all CV and ML logic on a background worker or coroutine to keep the CameraX preview smooth.

### Gradle Workflow Automation
- **Unified Project:** Keep Python training scripts in an `/ml` subfolder of the Android project.
- **Auto-Sync:** Use a Gradle `Exec` task to "pull" the latest `model.tflite` from the training source into the Android `assets/` folder during the `preBuild` phase.

---

## 5. Verification & Testing
- **Golden Set:** Maintain a folder of 50 "difficult" card images.
- **Automated Tests:** Every build should run these images through the pipeline.
    - **Pass criteria:** 100% grid detection (15 rows found) and >98% classification accuracy.

---

## 6. Summary of Key Decisions
- **Predictable Logic for Grids:** No ML for geometry; use OpenCV math for speed and reliability.
- **Python for Training:** Utilize Python's superior ML ecosystem (Keras/TensorFlow) for the "model creation" phase.
- **LiteRT for Inference:** High performance and low overhead on mobile.
- **Viewfinder UX:** Collaborate with the user to get a clean scan rather than trying to fix highly distorted images in code.

---

## 7. Implementation Flow Overview

### 7.1 Camera Capture
**Entry:** User taps camera icon in `LoopScoreEntryScreen.kt`
- CameraX captures JPEG image via `CameraViewModel.captureImage()`
- `ImageProxy` converted to OpenCV grayscale Mat
- Handles both YUV and JPEG formats with bitmap fallback

### 7.2 Image Preprocessing
**File:** `CardImagePreprocessor.preprocessImage()`
1. **Grayscale conversion** - Ensure single-channel image
2. **Auto-rotation** - Rotate landscape images to portrait (score cards are vertical)
3. **Card detection** - Use Canny edges + contours to isolate card from background
4. **Resize** - Scale to 640px width, preserve aspect ratio

**Output:** Normalized grayscale image (640×N pixels)

### 7.3 Row Extraction
**File:** `CardImagePreprocessor.extractRowImages()`
- **Hough Transform** detects horizontal grid lines separating rows
- Clusters and filters lines to find 15 evenly-spaced sections
- Extracts individual row images between grid lines
- **Fallback:** Equal-height division if grid detection fails

**Output:** 15 row images (typically 640×66 pixels each)

### 7.4 ML Classification
**File:** `OpenCVCardScannerService.kt`
- Loads TFLite model (`score_classifier_model.tflite`)
- For each row:
  1. Resize to model input size (640×66)
  2. Convert Mat → Float32 ByteBuffer (normalized [0-1])
  3. Run TFLite inference → 5-class probabilities
  4. Select class with highest probability
  5. Map to score value: [0, 1, 2, 3, 5]

**Output:** `Map<Int, Int>` (section number → score)

### 7.5 Persistence & Display
**File:** `CameraViewModel.applyScanResult()`
- Creates `SectionScore` entities for each detected score
- Saves to Room database (UPSERT operation)
- Navigates back to score entry screen
- Room Flow emits updates → LiveData → Compose UI
- Scores appear as selected radio buttons

### 7.6 Key Files
- **Camera:** `CameraScreen.kt`, `CameraViewModel.kt`
- **Processing:** `CardImagePreprocessor.kt`, `OpenCVCardScannerService.kt`
- **Data:** `SectionScoreRepository.kt`, `RiderScoreDao.kt`, `SectionScore.kt`
- **Display:** `ScoreCardViewModel.kt`, `LoopScoreEntryScreen.kt`