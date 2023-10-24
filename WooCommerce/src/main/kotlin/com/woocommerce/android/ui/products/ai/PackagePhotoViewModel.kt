package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Failure
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Initial
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.NoKeywordsFound
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Scanning
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Success
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import javax.inject.Inject

@HiltViewModel
class PackagePhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AIRepository,
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
        _viewState.update { _viewState.value.copy(state = Scanning) }
        launch {
            textRecognitionEngine.processImage(_viewState.value.imageUrl)
                .onSuccess { keywords ->
                    if (keywords.isNotEmpty()) {
                        _viewState.update {
                            _viewState.value.copy(
                                keywords = keywords.map { Keyword(it, true) }
                            )
                        }
                        generateNameAndDescription()
                    } else {
                        _viewState.update {
                            _viewState.value.copy(
                                state = NoKeywordsFound
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _viewState.update {
                        _viewState.value.copy(state = Failure(error.message ?: ""))
                    }
                }
        }
    }

    fun onKeywordChanged(index: Int, keyword: Keyword) {
        _viewState.update {
            _viewState.value.copy(
                keywords = _viewState.value.keywords.mapIndexed { i, old ->
                    if (i == index) {
                        old.copy(title = keyword.title, isChecked = keyword.isChecked)
                    } else {
                        old
                    }
                },
            )
        }
        val moreThanOneKeyword = _viewState.value.keywords
            .filter { it.isChecked }
            .joinToString(separator = "") { it.title }
            .isNotEmpty()
        _viewState.update {
            _viewState.value.copy(
                isRegenerateButtonEnabled = moreThanOneKeyword
            )
        }
    }

    private suspend fun generateNameAndDescription() {
        _viewState.update {
            _viewState.value.copy(state = Generating)
        }

        val keywords = _viewState.value.keywords.filter { it.isChecked }.joinToString { it.title }
        aiRepository.generateProductName(keywords)
            .onSuccess { name ->
                aiRepository.generateProductDescription(name, keywords)
                    .onSuccess { description ->
                        _viewState.update {
                            _viewState.value.copy(state = Success, title = name, description = description)
                        }
                    }
                    .onFailure { error ->
                        _viewState.update {
                            _viewState.value.copy(state = Failure(error.message ?: ""))
                        }
                    }
            }
            .onFailure { error ->
                _viewState.update {
                    _viewState.value.copy(state = Failure(error.message ?: ""))
                }
            }
    }

    fun onEditPhotoTapped() {
        triggerEvent(ShowMediaLibraryDialog)
    }

    fun onContinueTapped() {
        /* TODO */
    }

    fun onRegenerateTapped() = launch {
        generateNameAndDescription()
    }

    fun onMediaLibraryDialogRequested() {
        setMediaPickerDialogVisibility(true)
    }

    fun onMediaPickerDialogDismissed() {
        setMediaPickerDialogVisibility(false)
    }

    fun onMediaLibraryRequested(source: DataSource) {
        triggerEvent(ShowMediaLibrary(source))
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
        val isRegenerateButtonEnabled: Boolean = true,
        val state: GenerationState = Initial
    ) {
        data class Keyword(val title: String, val isChecked: Boolean)

        sealed class GenerationState {
            object Initial : GenerationState()
            object Scanning : GenerationState()
            object Generating : GenerationState()
            object Success : GenerationState()
            object NoKeywordsFound : GenerationState()
            data class Failure(val message: String) : GenerationState()
        }
    }
}
