package com.woocommerce.android.ui.products.ai

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.Companion.PRODUCT_DETAILS_FROM_SCANNED_TEXT_FEATURE
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Failure
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Initial
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.NoKeywordsFound
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Scanning
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Success
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import javax.inject.Inject

@HiltViewModel
class PackagePhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AIRepository,
    private val textRecognitionEngine: TextRecognitionEngine,
    private val tracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val DEFAULT_LANGUAGE_ISO = "en"
    }
    private val navArgs: PackagePhotoBottomSheetFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(
        ViewState(navArgs.imageUrl)
    )

    val viewState = _viewState.asLiveData()

    init {
        tracker.track(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DISPLAYED)
        analyzePackagePhoto()
    }

    private fun analyzePackagePhoto() {
        _viewState.update { _viewState.value.copy(state = Scanning) }
        launch {
            textRecognitionEngine.processImage(_viewState.value.imageUrl)
                .onSuccess { keywords ->
                    tracker.track(
                        AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_SCAN_COMPLETED,
                        mapOf(
                            AnalyticsTracker.KEY_SCANNED_TEXT_COUNT to keywords.size
                        )
                    )

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
                    tracker.track(
                        AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_SCAN_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_DESC to error.message,
                        )
                    )

                    _viewState.update {
                        _viewState.value.copy(state = NoKeywordsFound)
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

    private suspend fun identifyLanguage(keywords: String): String {
        return aiRepository.identifyISOLanguageCode(
            text = keywords,
            feature = PRODUCT_DETAILS_FROM_SCANNED_TEXT_FEATURE
        ).fold(
            onSuccess = { language ->
                tracker.track(
                    AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS,
                    mapOf(
                        AnalyticsTracker.KEY_DETECTED_LANGUAGE to language,
                        AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION_FROM_PACKAGE_PHOTO
                    )
                )
                language
            },
            onFailure = { error ->
                trackAICompletionError(error, AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED)
                DEFAULT_LANGUAGE_ISO
            }
        )
    }

    private fun trackAICompletionError(error: Throwable, event: AnalyticsEvent) {
        tracker.track(
            event,
            mapOf(
                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                AnalyticsTracker.KEY_ERROR_TYPE to (error as? JetpackAICompletionsException)?.errorType,
                AnalyticsTracker.KEY_ERROR_DESC to (error as? JetpackAICompletionsException)?.errorMessage,
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION_FROM_PACKAGE_PHOTO
            )
        )
    }

    private suspend fun generateNameAndDescription() {
        _viewState.update {
            _viewState.value.copy(state = Generating)
        }

        val checkedKeywords = _viewState.value.keywords.filter { it.isChecked }
        val keywords = checkedKeywords.joinToString { it.title }
        val language = identifyLanguage(keywords)
        aiRepository.generateProductNameAndDescription(keywords, language)
            .onSuccess { result ->
                tracker.track(
                    AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DETAILS_GENERATED,
                    mapOf(
                        AnalyticsTracker.KEY_DETECTED_LANGUAGE to language,
                        AnalyticsTracker.KEY_SELECTED_TEXT_COUNT to checkedKeywords.size
                    )
                )
                _viewState.update {
                    _viewState.value.copy(state = Success, title = result.name, description = result.description)
                }
            }
            .onFailure { error ->
                trackAICompletionError(error, AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DETAIL_GENERATION_FAILED)
                _viewState.update {
                    _viewState.value.copy(state = Failure(error.message ?: ""))
                }
            }
    }

    fun onEditPhotoTapped() {
        tracker.track(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_CHANGE_PHOTO_BUTTON_TAPPED)
        triggerEvent(ShowMediaLibraryDialog)
    }

    fun onContinueTapped() {
        tracker.track(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_CONTINUE_BUTTON_TAPPED)
        triggerEvent(
            ExitWithResult(
                PackagePhotoData(
                    title = _viewState.value.title,
                    description = _viewState.value.description,
                    keywords = _viewState.value.keywords.filter { it.isChecked }.map { it.title }
                )
            )
        )
    }

    fun onRegenerateTapped() = launch {
        tracker.track(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_REGENERATE_BUTTON_TAPPED)
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

    @Parcelize
    data class PackagePhotoData(
        val title: String,
        val description: String,
        val keywords: List<String>
    ) : Parcelable
}
