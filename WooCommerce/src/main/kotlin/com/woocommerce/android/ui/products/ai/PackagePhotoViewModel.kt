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
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import javax.inject.Inject

@HiltViewModel
class PackagePhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: PackagePhotoBottomSheetFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(
        ViewState(navArgs.imageUrl)
    )

    val viewState = _viewState.asLiveData()

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
        // TBD
    }

    fun onMediaLibraryDialogRequested() {
        setMediaPickerDialogVisibility(true)
    }

    fun onMediaPickerDialogDismissed() {
        setMediaPickerDialogVisibility(false)
    }

    fun onMediaPickerLibraryRequested(source: DataSource) {
        triggerEvent(ShowMediaLibrary(source))
        setMediaPickerDialogVisibility(false)
    }

    fun onImageChanged(url: String) {
        _viewState.update {
            _viewState.value.copy(imageUrl = url)
        }
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
        val title: String = "Title",
        val description: String = "Description",
        val keywords: List<Keyword> = listOf(Keyword("Keyword 1", true), Keyword("Keyword 2", false)),
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
