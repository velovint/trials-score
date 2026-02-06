package net.yakavenka.trialsscore.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.lifecycle.ProcessCameraProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.yakavenka.cardscanner.CardScannerService
import net.yakavenka.cardscanner.ScanResult
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import javax.inject.Inject
import androidx.core.content.ContextCompat
import androidx.camera.lifecycle.awaitInstance
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.nio.ByteBuffer
import java.util.concurrent.Executor

private const val TAG = "CameraViewModel"

sealed class CameraUiState {
    object Ready : CameraUiState()
    object Capturing : CameraUiState()
    object Processing : CameraUiState()
    data class Success(val scanResult: ScanResult) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cardScanner: CardScannerService,
    private val sectionScoreRepository: SectionScoreRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val riderId: Int = checkNotNull(savedStateHandle["riderId"]) as Int
    private val loopNumber: Int = checkNotNull(savedStateHandle["loop"]) as Int

    private val _uiState = MutableLiveData<CameraUiState>(CameraUiState.Ready)
    val uiState: LiveData<CameraUiState> = _uiState

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    // Camera components - initialized in bindCamera
    internal var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    internal var executor: Executor? = null  // Executor for takePicture callbacks (injectable for testing)

    /**
     * Bind camera use cases to lifecycle and publish SurfaceRequest.
     *
     * Uses modern CameraX 1.4+ awaitInstance() API for suspending coroutine access
     * to ProcessCameraProvider, replacing legacy ListenableFuture + addListener pattern.
     *
     * Stores executor (not context) to avoid context leaks. Executor is safe to hold
     * in ViewModel and can be injected for testing.
     */
    fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        // Store executor instead of context to avoid memory leaks
        this.executor = ContextCompat.getMainExecutor(context)
        viewModelScope.launch {
            try {
                val cameraProvider = ProcessCameraProvider.awaitInstance(context)
                cameraProvider.unbindAll()

                // Create Preview use case
                preview = Preview.Builder().build()

                // Create ImageCapture use case
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Set surface provider to capture SurfaceRequest
                preview?.setSurfaceProvider { surfaceRequest ->
                    _surfaceRequest.value = surfaceRequest
                }

                // Bind use cases to lifecycle
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error binding camera", e)
                _uiState.value = CameraUiState.Error("Camera binding failed: ${e.message}")
            }
        }
    }

    /**
     * Capture image from camera, process with scanner, and apply scores to database.
     *
     * Flow:
     * 1. Capture image to memory via callback (no disk storage)
     * 2. Extract grayscale Y plane from ImageProxy (zero-copy)
     * 3. Pass ByteBuffer directly to CardScannerService
     * 4. Apply scores directly to database via applyScanResult()
     * 5. Navigate back (score entry screen will show scores from database)
     *
     * Optimized to minimize memory copies:
     * - Extracts Y plane (grayscale) directly from YUV ImageProxy
     * - Passes ByteBuffer to scanner (no Bitmap intermediate)
     * - Scanner wraps ByteBuffer into Mat (zero-copy operation)
     *
     * Executor is set during bindCamera() and stored to avoid context leaks.
     */
    fun captureImage() {
        val capture = imageCapture ?: run {
            _uiState.value = CameraUiState.Error("Camera not initialized")
            return
        }

        val exec = executor ?: run {
            _uiState.value = CameraUiState.Error("Executor not initialized")
            return
        }

        _uiState.value = CameraUiState.Capturing

        // Direct callback approach - executor already stored from bindCamera()
        capture.takePicture(
            exec,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Launch coroutine only for processing (where suspend work is needed)
                    viewModelScope.launch {
                        try {
                            // Convert ImageProxy to grayscale Mat
                            // Keep ImageProxy open until conversion completes
                            val grayscaleMat = imageProxyToGrayscaleMat(image)

                            // Transition to processing state
                            _uiState.value = CameraUiState.Processing

                            // Extract scores from Mat
                            val scanResult = cardScanner.extractScores(grayscaleMat)

                            // Release Mat after processing
                            grayscaleMat.release()

                            // Apply scores directly to database
                            applyScanResult(scanResult)

                            Log.d(TAG, "Image scanned successfully: $scanResult")
                            _uiState.value = CameraUiState.Success(scanResult)
                        } catch (e: Exception) {
                            Log.e(TAG, "Image processing/scan failed", e)
                            _uiState.value = CameraUiState.Error(e.message ?: "Unknown error")
                        } finally {
                            // Always close ImageProxy to release camera buffer
                            image.close()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    _uiState.value = CameraUiState.Error(exception.message ?: "Capture failed")
                }
            }
        )
    }

    /**
     * Apply scan result scores directly to database.
     *
     * Filters out invalid section numbers and applies each score.
     * Uses the same logic as ScoreCardViewModel.applyScanResult().
     */
    private suspend fun applyScanResult(scanResult: ScanResult) {
        when (scanResult) {
            is ScanResult.Success -> {
                scanResult.scores.forEach { (sectionNumber, points) ->
                    if (sectionNumber > 0) {
                        val sectionScore = SectionScore(riderId, loopNumber, sectionNumber, points)
                        sectionScoreRepository.updateSectionScore(sectionScore)
                    }
                }
                Log.d(TAG, "Applied ${scanResult.scores.size} scanned scores to database")
            }
            is ScanResult.Failure -> {
                Log.e(TAG, "Scan failed: ${scanResult.error}")
                _uiState.value = CameraUiState.Error(scanResult.error)
            }
        }
    }

    /**
     * Convert ImageProxy to grayscale OpenCV Mat.
     *
     * Extracts Y plane (luminance) from YUV_420_888 format and creates CV_8UC1 Mat.
     * Handles row padding/strides efficiently.
     *
     * @param image ImageProxy from camera (must remain open during this call)
     * @return Grayscale Mat (CV_8UC1) - caller must release when done
     */
    private fun imageProxyToGrayscaleMat(image: ImageProxy): Mat {
        val width = image.width
        val height = image.height

        // 1. Get the Y-plane (the first plane in YUV_420_888)
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // 2. Create the destination Mat
        val grayscaleMat = Mat(height, width, CvType.CV_8UC1)

        // 3. Optimized Copy: Handle padding/strides
        // If pixelStride is 1 and no row padding, we can do a bulk put
        if (pixelStride == 1 && rowStride == width) {
            val data = ByteArray(width * height)
            buffer.get(data)
            grayscaleMat.put(0, 0, data)
        } else {
            // Handle row-by-row copy to strip out padding (strides)
            val rowData = ByteArray(width)
            for (row in 0 until height) {
                buffer.position(row * rowStride)
                buffer.get(rowData, 0, width)
                grayscaleMat.put(row, 0, rowData)
            }
        }

        return grayscaleMat
    }

    /**
     * Reset UI state back to ready
     */
    fun resetState() {
        _uiState.value = CameraUiState.Ready
    }
}
