package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Initial
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import javax.inject.Inject

@HiltViewModel
class PackagePhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val textRecognitionEngine: TextRecognitionEngine
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: PackagePhotoBottomSheetFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(
        ViewState(navArgs.imageUrl)
    )

    val viewState = _viewState.asLiveData()

    init {
        analyzePackagePhoto()
    }

    private fun analyzePackagePhoto() {
        launch {
            textRecognitionEngine.processImage(_viewState.value.imageUrl)
                .onSuccess { keywords ->
                    _viewState.update {
                        _viewState.value.copy(keywords = keywords.map { Keyword(it, true) })
                    }
                }
                .onFailure { error ->
                    _viewState.update {
                        _viewState.value.copy(description = error.toString())
                    }
                }
        }
    }

    fun onKeywordChanged(index: Int, keyword: Keyword) {
        _viewState.value = _viewState.value.copy(
            keywords = _viewState.value.keywords.mapIndexed { i, old ->
                if (i == index) {
                    old.copy(title = keyword.title, isChecked = keyword.isChecked)
                } else {
                    old
                }
            }
        )
    }

    fun onEditPhotoTapped() {
        triggerEvent(ShowMediaLibraryDialog)
    }

    fun onRegenerateTapped() {
        /* TODO */
    }

    fun onMediaLibraryDialogRequested() {
        setMediaPickerDialogVisibility(true)
    }

    fun onMediaLibraryDialogDismissed() {
        setMediaPickerDialogVisibility(false)
    }

    fun onDevicePickerRequested() {
        triggerEvent(ShowMediaLibrary(DEVICE))
        setMediaPickerDialogVisibility(false)
    }

    fun onCameraRequested() {
        triggerEvent(ShowMediaLibrary(CAMERA))
        setMediaPickerDialogVisibility(false)
    }

    fun onWpMediaLibraryRequested() {
        triggerEvent(ShowMediaLibrary(WP_MEDIA_LIBRARY))
        setMediaPickerDialogVisibility(false)
    }

    fun onImageChanged(url: String) {
        _viewState.update {
            _viewState.value.copy(imageUrl = url)
        }
        analyzePackagePhoto()
    }

    private fun setMediaPickerDialogVisibility(isVisible: Boolean) {
        _viewState.update {
            _viewState.value.copy(isMediaPickerDialogVisible = isVisible)
        }
    }

    object ShowMediaLibraryDialog : Event()

    data class ShowMediaLibrary(val source: DataSource) : Event()

    data class ViewState(
        val imageUrl: String,
        val isMediaPickerDialogVisible: Boolean = false,
        val title: String = "",
        val description: String = "",
        val keywords: List<Keyword> = emptyList(),
        val generationState: GenerationState = Initial
    ) {
        data class Keyword(val title: String, val isChecked: Boolean)

        sealed class GenerationState {
            object Initial : GenerationState()
            object Scanning : GenerationState()
            object Generating : GenerationState()
            object Success : GenerationState()
            data class Failure(val error: String) : GenerationState()
        }
    }
}
