package net.yakavenka.trialsscore.viewmodel

import android.content.Context
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.yakavenka.cardscanner.CardScannerService
import net.yakavenka.cardscanner.ScanResult
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import javax.inject.Inject
import androidx.core.content.ContextCompat
import androidx.camera.lifecycle.awaitInstance
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
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val riderId: Int = checkNotNull(savedStateHandle["riderId"]) as Int
    private val loopNumber: Int = checkNotNull(savedStateHandle["loop"]) as Int

    private val _uiState = MutableLiveData<CameraUiState>(CameraUiState.Ready)
    val uiState: LiveData<CameraUiState> = _uiState

    private val _surfaceRequest = MutableLiveData<SurfaceRequest?>(null)
    val surfaceRequest: LiveData<SurfaceRequest?> = _surfaceRequest

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
                            // Transition to processing state
                            _uiState.value = CameraUiState.Processing

                            // Extract scores from Bitmap
                            val scanResult = cardScanner.extractScores(image.toBitmap())

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
                val numSections = userPreferencesRepository.userPreferencesFlow.first().numSections
                scanResult.scores.forEach { (sectionNumber, points) ->
                    if (sectionNumber in 1..numSections) {
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
     * Reset UI state back to ready
     */
    fun resetState() {
        _uiState.value = CameraUiState.Ready
    }

    /**
     * Cleanup resources when ViewModel is destroyed.
     * Closes TFLite interpreter and any native resources held by CardScannerService.
     */
    override fun onCleared() {
        super.onCleared()
        cardScanner.cleanup()
    }
}
