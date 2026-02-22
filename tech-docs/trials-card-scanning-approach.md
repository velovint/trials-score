# Trials Card Scanning Approach (v3.0)

## 1. Card Structure & Scoring

### Physical Layout

- **Card Header:** Top ~25–30% of card height. Contains rider name, event metadata, and loop labels. Not part of the data grid.
- **Data Grid:** Occupies the remaining ~70–75% of card height. Contains:
  - **Grid Header Row:** The topmost row of the grid. Contains column labels (0, 1, 2, 3, 5) and possibly a section/notes column label. **Not a scored row.**
  - **Scoring Rows:** 15 rows, each representing one section. Exactly one mark per row.
  - **Non-Scoring Rows (optional):** Section-number rows or note-column header rows may appear interspersed — for example, a divider row or section-group label row between groups of sections.
- **Grid Columns:** At minimum 5 score columns (0, 1, 2, 3, 5). Cards may include additional columns: a narrow section-number column and/or a wide notes column.

### Scoring Logic

- **Output:** An array of 15 integers `[score_1, ..., score_15]`.
- Exactly one mark per scored row; the grid header row and any non-scoring rows are ignored during classification.

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

## 7. Detailed Preprocessing Algorithm

The preprocessing pipeline uses structural grid properties (vertical/horizontal lines, aspect ratio) to reliably extract 15 row images from score cards regardless of orientation or background.

### Component Overview

The pipeline is composed of four components wired by `CardScanningPipeline`:

```
CardScanningPipeline
├── OpenCVCardIsolator        (boundary detection + sideways rotation)       → Mat
├── MorphologicalRowSegmenter (cell detection + orientation fix + cluster)   → List<RowRegion>
├── OpenCVRowNormalizer       (crop + resize to 640×66)                      → List<RowImage>
└── TFLiteRowClassifier       (LiteRT or pixel density)                      → Int
```

---

### 7.1 Camera Capture
**Entry:** User taps camera icon in `LoopScoreEntryScreen.kt`
- CameraX captures JPEG image via `CameraViewModel.captureImage()`
- `ImageProxy` converted to OpenCV grayscale Mat
- Handles both YUV and JPEG formats with bitmap fallback

### 7.2 Phase 1: Card Boundary Detection & Isolation
**Component:** `OpenCVCardIsolator.isolate()`

**Steps:**
1. Convert to grayscale (if needed)
2. Canny edge detection to find card edges
3. Find contours, select largest quadrilateral
4. Perspective transform to flatten card (removes tilt/skew)
5. Resize to standard width (640px), maintain aspect ratio

**Output:** Isolated card image (640 × H pixels)

**Validation:**
```
aspect_ratio = card_height / card_width
if aspect_ratio < 0.7 or aspect_ratio > 2.0:
    ERROR: "Invalid card boundary - expected portrait format"
```

### 7.3 Phase 2a: Sideways Orientation Detection
**Strategy:** Check aspect ratio immediately after boundary detection (fast, simple).

**Steps:**
1. Calculate aspect ratio from card dimensions
2. If `aspect_ratio < 1.0` (width > height):
   - Card is **SIDEWAYS** (landscape)
   - Rotate 90° to portrait orientation
   - Re-validate aspect ratio > 1.0

**Output:** Portrait-oriented card (height > width)

**Performance:** ~0.1ms (just comparing dimensions)

**Note:** Orientation is checked after Phase 1 card boundary detection rather than on the raw image. This is intentional — the photograph itself may be portrait or landscape regardless of how the card is held, so the raw image aspect ratio carries no information about card orientation. Only the isolated card crop has a meaningful aspect ratio.

### 7.4 Phase 2b: Grid Region Detection (Cell Detection)
**Component:** `MorphologicalRowSegmenter` (internal step)

**Strategy:** Use morphological operations to enhance grid line structure, detect enclosed rectangular cells, and compute grid bounding box from their positions. This approach is column-agnostic — cards with extra columns (section numbers, notes) are handled naturally since row detection depends only on Y positions.

**Updated context:** The card header (~25–30% of card height) and grid header row are present above the 15 scoring rows. The filtering logic naturally handles the full card layout:

**Steps:**
1. **Apply adaptive threshold** to handle variable lighting:
   ```
   adaptiveThreshold(ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, blockSize=11, C=2)
   ```

2. **Enhance grid lines** via morphological OPEN:
   - Vertical kernel (`1 × 30px`): preserves vertical column separators
   - Horizontal kernel (`30px × 1`): preserves horizontal row separators
   - Combine: `bitwise_OR(vertical_result, horizontal_result)`
   - `MORPH_CLOSE` with small kernel (3×3) to bridge broken line segments

3. **Detect cell candidates** via contour detection:
   - Invert the combined image (cells become dark blobs on light background)
   - `findContours(RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)`

4. **Filter contours** for scoring cells:
   - Compute median contour area → `median_area`
   - Keep contours where area ∈ `[median_area × 0.4, median_area × 2.5]`
   - Aspect ratio (width/height) ∈ `[0.6, 1.8]` (roughly square scoring cells)
   - **Grid header cells** (top row of grid) have the same size as scoring cells and will be detected — they are removed in Phase 3 (row extraction) by cluster analysis.
   - **Section-number column cells** are narrow (aspect ratio < 0.6) — excluded by aspect ratio filter.
   - **Notes column cells** are wide (aspect ratio > 1.8) — excluded by aspect ratio filter.
   - **Non-scoring divider rows** (if any) have similar cell size to scoring cells; excluded in Phase 3 by cluster cell count.

5. **Validate and compute grid bounding box**:
   ```
   if valid_cells.count < 45:
       ERROR: "Insufficient grid cells detected (need at least 45, found N)"
   grid_bounds = bounding rect of all valid cells
   ```

**Output:** List of valid cell bounding boxes + grid bounding box

**Debug:** Save image with valid cell contours drawn

### 7.5 Phase 2c: Upside-Down Detection
**Component:** `MorphologicalRowSegmenter` (internal step)

**Strategy:** Check if grid is in top or bottom of card (grid should be in bottom 50-70%). If upside-down, correct cell coordinates in-place rather than re-running Phase 2b.

**Steps:**
1. Calculate grid center position from Phase 2b output:
   ```
   grid_center_y = grid_bounds.y + (grid_bounds.height / 2)
   relative_position = grid_center_y / card_height
   ```

2. If `relative_position < 0.45`:
   - Card is **UPSIDE DOWN** (grid in top half)
   - Invert all cell Y-coordinates:
     ```
     for each cell:
         cell.y = card_height - (cell.y + cell.height)
     ```
   - Recompute `grid_bounds` from the corrected cell positions

**Output:** Cell list and grid bounds with corrected Y-coordinates, portrait-oriented card with grid in bottom 50-70%

**Validation:** Grid should occupy 60-70% of card height

**Note:** With the card header occupying ~25–30% of card height and the grid header row adding a small additional offset, the scoring grid center will sit at roughly 55–85% of card height in a correctly-oriented portrait card. The threshold `relative_position < 0.45` reliably distinguishes upside-down cards (grid in the upper half) from normal ones.

### 7.6 Phase 3: Row Extraction from Cell Positions
**Component:** `MorphologicalRowSegmenter` (produces `List<RowRegion>`)

**Strategy:** Sort detected cells by Y coordinate, cluster into rows, classify clusters as scoring vs. non-scoring, and compute row boundaries directly from cell positions — no line detection required.

**Steps:**
1. **Sort cells** by Y coordinate (top → bottom)

2. **Estimate cell height** from median cell bounding box height

3. **Cluster into rows**:
   - Group consecutive cells where Y gap < `0.5 × estimated_cell_height`
   - Each cluster represents one candidate row

4. **Classify clusters:**
   - Clusters with ≥ 3 cells and area ratio consistent with scoring cells → **candidate scoring rows**
   - The topmost cluster that aligns with the grid header position (top of grid bounding box) → **grid header row**, excluded
   - Clusters with < 3 cells, or cell aspect ratios inconsistent with scoring cells → **non-scoring rows**, excluded

5. **Validate scoring cluster count**:
   ```
   if scoring_clusters.count < 12 or any scoring_cluster has < 3 cells:
       ERROR: "Cannot extract 15 rows from detected cells"
   ```

6. **Compute row bounds** for each of the 15 scoring clusters:
   ```
   row_top    = min(cell.y for cell in cluster) - padding
   row_bottom = max(cell.y + cell.height for cell in cluster) + padding
   ```

**Output:** Exactly 15 `RowRegion`s (top/bottom Y coordinates) corresponding to scoring rows only.

**Note:** The cluster count before filtering may be 16+ (15 scoring + grid header + any dividers). The algorithm tolerates this naturally via the classification step. Column count does not affect row detection — cards with 5–8 columns all produce the same 15-row result from the same Y-clustering logic.

### 7.7 Phase 4: Row Image Extraction
**Component:** `OpenCVRowNormalizer.normalize(card, regions)`

**Steps:**
1. **Extract row regions** using top/bottom bounds from Phase 3 (full card width, 15 rows)
2. **Resize for ML inference**: 640×66 pixels per row
3. **Validate dimensions**: Row height should be 20-100px before resize

Input `RowRegion` list already contains only the 15 scored rows — no further filtering needed here.

**Output:** 15 row images (640×66 pixels each) as `List<RowImage>` (direct `ByteBuffer`, float32, normalized [0,1])

### 7.8 ML Classification
**Component:** `TFLiteRowClassifier.classify(row: RowImage)`

- Loads TFLite model (`score_classifier_model.tflite`)
- For each row:
  1. Pass `row.buffer` directly to `Interpreter.run()` (ByteBuffer is pre-normalized by `OpenCVRowNormalizer`)
  2. Run TFLite inference → 5-class probabilities
  3. Select class with highest probability
  4. Map to score value: `[0→0, 1→1, 2→2, 3→3, 4→5]`

**Output:** `Map<Int, Int>` (section number → score)

**Alternative (no ML):** Since Phase 2b already provides individual cell bounding boxes with known column positions, classification could instead be done by finding which scoring cell in each row contains the most dark pixels — the mark is a large filled circle that strongly dominates any cell it occupies. This would eliminate the model entirely. Not pursued for now as ML handles ambiguous or non-standard marks more robustly.

### 7.9 Persistence & Display
**File:** `CameraViewModel.applyScanResult()`
- Creates `SectionScore` entities for each detected score
- Saves to Room database (UPSERT operation)
- Navigates back to score entry screen
- Room Flow emits updates → LiveData → Compose UI
- Scores appear as selected radio buttons

### 7.10 Key Components and Files

| Component | Class | Module |
|---|---|---|
| Orchestrator | `CardScanningPipeline` | `:ml-inference` |
| Boundary detection + rotation | `OpenCVCardIsolator` | `:shared-cv` |
| Grid cell detection + clustering | `MorphologicalRowSegmenter` | `:shared-cv` |
| Row normalization | `OpenCVRowNormalizer` | `:shared-cv` |
| Score classification | `TFLiteRowClassifier` | `:ml-inference` |
| App-facing API | `CardScannerService` (interface) | `:ml-inference` |
| Service implementation | `OpenCVCardScannerService` | `:ml-inference` |
| Data types | `CardScannerTypes.kt` | `:shared-cv` |

**Supporting files:**
- **Camera:** `CameraScreen.kt`, `CameraViewModel.kt`
- **Data:** `SectionScoreRepository.kt`, `RiderScoreDao.kt`, `SectionScore.kt`
- **Display:** `ScoreCardViewModel.kt`, `LoopScoreEntryScreen.kt`

---

## 8. Error Handling & Robustness

### Critical Errors (Stop Processing)
- ❌ Card boundary detection fails (no quadrilateral found)
- ❌ Aspect ratio invalid (< 0.7 or > 2.0) - indicates bad crop
- ❌ Insufficient grid cells detected (< 45 cells) - grid not visible or too damaged
- ❌ Fewer than 12 scoring row clusters with 3+ cells - cannot extract 15 rows

### Debug Mode
Enable via `FileScanDebugObserver` (injected into pipeline components) to save intermediate images:
- `01_card_boundary.png` - Detected card contour
- `02_enhanced_lines.png` - Combined vertical+horizontal morphological result
- `03_detected_cells.png` - Valid cell contours with aspect ratio/area filter applied
- `04_row_bounds.png` - Row cluster boundaries overlaid on grid
- `05_row_00.png` ... `05_row_14.png` - Extracted rows

---

## 9. Alternative Approaches Considered

### For Sideways Detection (Phase 2a)
**Chosen: Aspect Ratio Check**
- Pro: Extremely fast (~0.1ms), simple, uses physical card properties
- Con: Requires accurate boundary detection

**Alternative: Vertical Line Count**
- Idea: Count vertical lines; sideways card has ~15 lines (row separators) instead of ~5 (column separators)
- Pro: Uses structural grid properties, works even with poor boundary detection
- Con: Slower (~50-100ms for Hough Transform), more complex
- **Use case:** Fallback if aspect ratio is ambiguous (0.8-1.2, nearly square)

### For Grid Region Detection & Row Extraction (Phases 2b + 3)
**Chosen: Morphological Cell Detection**
- Pro: Column-agnostic (handles any number of extra columns), self-validating via cell count, robust to variable lighting via adaptive threshold, eliminates need for line-spacing analysis
- Con: Requires cells to be reasonably intact; large marks or card damage can break cell borders (mitigated by MORPH_CLOSE)

**Alternative: Hough Transform (vertical + horizontal lines)**
- Idea: Detect vertical column separators to find grid bounding box (Phase 2b), then horizontal separators to find row boundaries (Phase 3)
- Pro: Precise line positions, tolerates cell border damage better
- Con: Two separate Hough passes, sensitive to parameter tuning, requires window-search to filter header noise, assumes a fixed column count
- **Rejected because:** More complex, column-count-dependent, brittle parameter tuning

---

## 10. Algorithm Properties

```
✅ Fast-fail validation        - Invalid aspect ratio stops processing immediately
✅ Structural detection        - Morphological cell detection; column count not assumed
✅ Multi-stage orientation     - Sideways (Phase 2a) → Upside-down (Phase 2c)
✅ Grid-header aware           - Topmost cluster excluded from scoring row output
✅ Extra column tolerant       - Section-number and notes columns filtered by aspect ratio
✅ Non-scoring row tolerant    - Interleaved divider rows excluded by cluster classification
✅ Clear error conditions      - No silent failures, explicit validation
✅ Debug support               - Intermediate images saved via FileScanDebugObserver
```

**Handles all test cases:**
- Normal cards ✓
- Cards with background ✓
- Sideways cards (90° rotation) ✓
- Upside-down cards (180° rotation) ✓
- Cards with card header (~30% height) ✓
- Cards with grid header row ✓
- Cards with section-number column ✓
- Cards with notes column ✓
- Cards with interleaved non-scoring rows ✓
