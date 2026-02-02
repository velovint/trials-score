# Trials Card Scanning Approach

## Card Structure

**Physical Layout:**
- **Header:** Card number, class, rider name, departure/arrival info
- **Grid Header:** Column labels for scores (0, 1, 2, 3, 5) and section numbers
- **Data Grid:** 15 rows (sections) × 5 columns (scores)
  - Each row = one section/obstacle
  - Each column = penalty score value
  - One mark per row at the intersection of section and score

**Scoring Logic:**
- Exactly ONE score per section
- Score recorded by marking/punching the cell at row-section × column-score intersection
- Example: 3 points for section 1 → mark at row 1, column "3"

**Output:**
Array of 15 integers: `[score_1, score_2, ..., score_15]`

---

## Overall Approach

```
Image Input → Grid Detection → Row Extraction → ML Classification → Score Array
              (OpenCV)          (OpenCV)         (TensorFlow Lite)
```

### Phase 1: Grid Detection (Computer Vision)

**Goal:** Isolate the data grid and extract 15 individual row images

**Method:**
1. Preprocess: grayscale, edge detection
2. Detect horizontal lines using morphological operations
3. Identify evenly-spaced grid lines (filter out headers by spacing pattern)
4. Extract 15 row images from grid

**Key Challenge:** Separate data grid from header sections

**Solution:** Grid rows have consistent spacing; headers don't. Use line spacing analysis to identify and skip header rows.

### Phase 2: Score Classification (Machine Learning)

**Goal:** Determine which score column (0, 1, 2, 3, or 5) is marked in each row

**Recommended Approach: Row-Level Classification**
- Input: Full row image (contains all 5 score columns)
- Output: Single classification from 5 classes [0, 1, 2, 3, 5]
- Process: 15 classifications (one per row)

**Why Row-Level vs Cell-Level:**
- Simpler: 15 classifications instead of 75 (15 rows × 5 columns)
- Matches structure: Each row has exactly one mark
- More robust: Model sees full context of mark position
- Less sensitive to grid alignment errors

**Alternative (Cell-Level):**
- Classify each of 75 cells as marked/unmarked
- Requires more precise grid detection
- More training data needed

---

## ML Framework Choice

### TensorFlow Lite (Recommended)

**Why:**
- Offline operation (trials often in remote areas)
- Better performance (20-30ms per row vs 30-50ms for ML Kit)
- Full control over optimization
- Smaller app size impact
- No dependency on Google Play Services

**Trade-offs:**
- More implementation code
- Steeper learning curve
- Manual model management

### ML Kit (Alternative)

**Why:**
- Faster development
- Higher-level APIs
- Firebase integration for model updates

**Trade-offs:**
- Requires internet for some features
- Larger app size
- Less control over inference

---

## Training Data

**Row-Level Model:**
- 300-500 row images per class (5 classes)
- Total: ~1,500-2,500 labeled row images
- Balance across all score values
- Include variety: different marks, lighting, angles

**Collection:**
- Scan 100-200 complete cards
- Extract rows programmatically
- Label which score is marked
- Augment: rotation, brightness, contrast

---

## Grid Detection Details

### Line Detection Method

**Morphological Operations (Recommended):**
```
1. Apply horizontal kernel → detect horizontal lines
2. Find y-coordinates of all lines
3. Calculate spacing between consecutive lines
4. Find median spacing (= grid row height)
5. Keep only lines with consistent spacing (the data grid)
6. Skip first line (grid header), extract next 15 rows
```

**Alternative: Hough Transform**
- Detect edges, apply probabilistic Hough
- More flexible for damaged cards
- More parameters to tune

### Header Separation

**Line Spacing Analysis:**
- Grid rows = evenly spaced
- Headers = irregular spacing
- Filter by spacing consistency

**Other Options:**
- Fixed ROI crop (if cards always aligned)
- Contour detection (find largest rectangle)
- Template matching for grid header

---

## Android Implementation Stack

**Libraries:**
- OpenCV for Android (grid detection)
- TensorFlow Lite (ML inference)
- CameraX (camera capture)

**Key Components:**
- `CardProcessor`: Grid detection and row extraction
- `RowClassifier`: TensorFlow Lite model wrapper
- `CardScoreExtractor`: Orchestrates pipeline

**Performance Targets:**
- Row inference: <30ms
- Full card: <500ms
- Offline capable
- App size: <5MB increase

---

## Error Handling

**Common Issues:**
- Card at extreme angle → real-time alignment feedback
- Poor lighting → image enhancement preprocessing
- Multiple/no marks in row → confidence thresholding, flag for review
- Damaged cards → fallback to manual entry

**Validation:**
- Each row must have exactly one score
- All 15 sections must have scores
- Scores must be in {0, 1, 2, 3, 5}

---

## High-Level Pipeline

```kotlin
class CardScoreExtractor(context: Context) {
    private val cardProcessor = CardProcessor()
    private val rowClassifier = TFLiteRowClassifier(context)
    
    fun extractScores(imagePath: String): List<Int> {
        // 1. Extract 15 row images from card
        val rows = cardProcessor.extractScoreRows(imagePath)
        
        // 2. Classify each row
        val scores = rows.map { rowMat ->
            val score = rowClassifier.classifyRow(rowMat)
            rowMat.release()
            score
        }
        
        return scores
    }
}
```

---

## Summary

**Two-Phase Approach:**
1. **Computer Vision (OpenCV):** Detect grid structure, extract rows
2. **Machine Learning (TFLite):** Classify each row to determine score

**Key Decisions:**
- Row-level classification (not cell-level)
- TensorFlow Lite (not ML Kit)
- Morphological operations for line detection
- Offline-first design

**Critical Success Factors:**
- Robust grid detection across real-world variations
- Well-trained model with diverse data
- Fast processing for good UX
- Graceful error handling with manual fallback
