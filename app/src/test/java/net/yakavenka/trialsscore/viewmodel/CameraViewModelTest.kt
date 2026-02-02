package net.yakavenka.trialsscore.viewmodel

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.yakavenka.trialsscore.camera.ImageStorageRepository
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CameraViewModel
    private lateinit var mockImageStorage: ImageStorageRepository
    private val testRiderId = 42
    private val testLoopNumber = 2

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockImageStorage = mock(ImageStorageRepository::class.java)

        val savedStateHandle = SavedStateHandle().apply {
            set("riderId", testRiderId)
            set("loop", testLoopNumber)
        }

        viewModel = CameraViewModel(
            imageStorage = mockImageStorage,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun captureImage_setsSuccessState_whenCaptureSucceeds() = runTest {
        // Setup: Mock imageStorage to return a URI
        val testUri = Uri.parse("file://test_${testRiderId}_${testLoopNumber}.jpg")
        val mockImageCapture = mock(ImageCapture::class.java)
        viewModel.imageCapture = mockImageCapture

        `when`(mockImageStorage.captureImage(mockImageCapture, testRiderId, testLoopNumber))
            .thenReturn(testUri)

        // Action: Call viewModel.captureImage()
        viewModel.captureImage()
        advanceUntilIdle()

        // Assert: uiState is CameraUiState.Success with correct imageUri
        val state = viewModel.uiState.value
        assertThat(state, instanceOf(CameraUiState.Success::class.java))

        val successState = state as CameraUiState.Success
        assertThat(successState.imageUri, `is`(testUri))
    }

    @Test
    fun captureImage_setsErrorState_whenCameraNotInitialized() = runTest {
        // Setup: Create CameraViewModel, do NOT set imageCapture (it's null)
        // (imageCapture is not initialized in the viewModel by default)

        // Action: Call viewModel.captureImage()
        viewModel.captureImage()

        // Assert: uiState is CameraUiState.Error with message "Camera not initialized"
        val state = viewModel.uiState.value
        assertThat(state, instanceOf(CameraUiState.Error::class.java))

        val errorState = state as CameraUiState.Error
        assertThat(errorState.message, `is`("Camera not initialized"))
    }

    @Test
    fun captureImage_setsErrorState_whenCaptureFails() = runTest {
        // Setup: Mock imageStorage to throw an exception
        val mockImageCapture = mock(ImageCapture::class.java)
        viewModel.imageCapture = mockImageCapture

        `when`(mockImageStorage.captureImage(mockImageCapture, testRiderId, testLoopNumber))
            .thenAnswer { throw Exception("Simulated capture failure") }

        // Action: Call viewModel.captureImage()
        viewModel.captureImage()
        advanceUntilIdle()

        // Assert: uiState is CameraUiState.Error with exception message
        val state = viewModel.uiState.value
        assertThat(state, instanceOf(CameraUiState.Error::class.java))

        val errorState = state as CameraUiState.Error
        assertThat(
            errorState.message,
            `is`("Simulated capture failure")
        )
    }

    @Test
    fun resetState_returnsToReady() = runTest {
        // Setup: Create CameraViewModel, set uiState to Error
        val mockImageCapture = mock(ImageCapture::class.java)
        viewModel.imageCapture = mockImageCapture

        `when`(mockImageStorage.captureImage(mockImageCapture, testRiderId, testLoopNumber))
            .thenAnswer { throw Exception("Test error") }

        viewModel.captureImage()
        advanceUntilIdle()

        // Verify state is Error
        assertThat(viewModel.uiState.value, instanceOf(CameraUiState.Error::class.java))

        // Action: Call viewModel.resetState()
        viewModel.resetState()

        // Assert: uiState is CameraUiState.Ready
        assertThat(viewModel.uiState.value, `is`(CameraUiState.Ready as CameraUiState))
    }
}
