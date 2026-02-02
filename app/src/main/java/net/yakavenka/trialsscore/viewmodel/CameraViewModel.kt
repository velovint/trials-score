package net.yakavenka.trialsscore.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
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
import net.yakavenka.trialsscore.camera.ImageStorageRepository
import javax.inject.Inject

private const val TAG = "CameraViewModel"

sealed class CameraUiState {
    object Ready : CameraUiState()
    object Capturing : CameraUiState()
    data class Success(val imageUri: Uri) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageStorage: ImageStorageRepository,
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

    /**
     * Bind camera use cases to lifecycle and publish SurfaceRequest
     */
    fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
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
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    /**
     * Capture image and save to storage
     */
    fun captureImage() {
        val capture = imageCapture ?: run {
            _uiState.value = CameraUiState.Error("Camera not initialized")
            return
        }

        _uiState.value = CameraUiState.Capturing

        viewModelScope.launch {
            try {
                val uri = imageStorage.captureImage(capture, riderId, loopNumber)
                Log.d(TAG, "Image captured: $uri")
                _uiState.value = CameraUiState.Success(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Image capture failed", e)
                _uiState.value = CameraUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Reset UI state back to ready
     */
    fun resetState() {
        _uiState.value = CameraUiState.Ready
    }
}
