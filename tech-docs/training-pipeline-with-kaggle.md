# Automated ML Pipeline: Android-Native OpenCV & LiteRT

This document defines the architecture and testing gates for the card scanning pipeline, ensuring **consistency** between training data preparation and on-device inference.

> **Architecture Note:** This pipeline uses Android-only modules (not KMP) because LiteRT and OpenCV expose fundamentally different APIs between Android and JVM platforms. Attempting to share code would require extensive abstraction layers that add complexity without meaningful benefit.

## 🏗️ Project Modules

* **`:shared-cv`**: Android library module. Contains OpenCV grid extraction logic.
* **`:ml-inference`**: Android library module. Contains the **LiteRT (TFLite) Classifier**.
* **`:data-prep-tool`**: Android "Worker" module. Uses `:shared-cv` within **instrumented tests** to create training datasets from raw images.
* **`:app`**: Android Application. Coordinates CameraX, `:shared-cv`, and `:ml-inference`.

---

## 📋 The Workflow Loop

1.  **Local Prep:** `:data-prep-tool` orchestrates data preparation via instrumented tests:
    *   Gradle downloads raw score card images from Kaggle dataset and pushes to device storage
    *   Instrumented test loads images, converts to `Mat`, and passes to `:shared-cv` pre-processor
    *   `:shared-cv` performs OpenCV operations (Hough Transform, grid extraction, row cropping)
    *   Test writes processed images to **AndroidX Test TestStorage** output directory, organized by score (0/, 1/, 2/, 3/, 5/)
    *   Gradle automatically pulls TestStorage outputs from device to `build/outputs/managed_device_android_test_additional_output/` after test completion
    *   Uploads training dataset from `build/` ➔ **Kaggle Dataset**
2.  **Remote Train:** Kaggle trains the model ➔ Gradle downloads the `.tflite` file.
3.  **Local Validate:** `:ml-inference` instrumented tests run the new model against a "Golden Set" to verify accuracy.
4.  **Deploy:** If accuracy passes, the model is bundled into the `:app`.

---

## 🧪 Testing Strategy

All validation runs as Android instrumented tests on emulator/device. This eliminates cross-platform inconsistencies at the cost of slower feedback loops.

### Tier 1: CV Logic Validation
* **Module:** `:shared-cv`
* **Action:** Instrumented tests with sample score card images validate Hough Transform, grid extraction, and preprocessing accuracy.
* **Why:** Catch bugs in CV algorithms before they affect downstream modules. No mocking - native OpenCV only.

### Tier 2: Model Validation Gate
* **Module:** `:ml-inference`
* **Action:** Instrumented tests load the `.tflite` model and run it against 100 "Golden" images.
* **Failure:** If accuracy < 95%, the Gradle build **fails** before reaching the app.

### Tier 3: End-to-End Integration
* **Module:** `:app`
* **Action:** Uses mock camera images to test the full UI flow on an emulator ("Camera ➔ CV ➔ ML" wiring).

---

## ⚠️ Critical Gotchas

1.  **OpenCV Initialization:**
    * All modules use `OpenCVLoader.initDebug()` before any OpenCV operations.
    * Instrumented tests must initialize OpenCV in `@Before` setup methods.

2.  **The "Mat" Contract:**
    * Always pass `org.opencv.core.Mat` between modules. Convert `Bitmap` only at the entry/exit points to keep the core logic pure.

3.  **Hardware Acceleration:**
    * Both training data preparation and inference run on Android devices/emulators.
    * LiteRT may use **GPU/NNAPI delegates** depending on device capabilities.
    * *Solution:* Use consistent delegate configuration across data prep and inference to avoid floating-point drift.

---

## 🐘 Automation Hooks (build.gradle.kts)

```kotlin
// Validation only - verify model quality (requires emulator/device)
task("verifyPipeline") {
    group = "verification"
    dependsOn(":shared-cv:connectedAndroidTest")       // CV logic validation
    dependsOn(":ml-inference:connectedAndroidTest")    // Model accuracy gate
    dependsOn(":app:connectedAndroidTest")             // Integration validation
}

// Full automation: Prep -> Train -> Validate -> Deploy (requires emulator/device)
task("buildProductionModel") {
    dependsOn(":data-prep-tool:connectedAndroidTest")  // Step 1: Prep & Upload
    dependsOn("triggerKaggleTrain")                   // Step 2: Remote Training
    dependsOn("downloadModel")                         // Step 3: Fetch result
    finalizedBy("verifyPipeline")                      // Step 4: Validation Gate
}