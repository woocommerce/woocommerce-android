package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.Error
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.Loading
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.Success
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.Success.CarouselItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val themeRepository: ThemeRepository,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedStateHandle.getStateFlow<ViewState>(viewModelScope, Loading)
    val viewState = _viewState.asLiveData()

    private val navArgs: ThemePickerFragmentArgs by savedStateHandle.navArgs()

    init {
        viewModelScope.launch {
            loadThemes()
        }
    }

    private suspend fun loadThemes() {
        themeRepository.fetchThemes().fold(
            onSuccess = { result ->
                _viewState.update {
                    Success(
                        carouselItems = result
                            .filter { theme -> theme.demoUrl != null }
                            .map { theme ->
                                CarouselItem.Theme(
                                    themeId = theme.id,
                                    name = theme.name,
                                    screenshotUrl = AppUrls.getScreenshotUrl(theme.demoUrl!!),
                                    demoUri = theme.demoUrl
                                )
                            }
                            .plus(
                                CarouselItem.Message(
                                    title = resourceProvider.getString(
                                        string.theme_picker_carousel_info_item_title
                                    ),
                                    description = resourceProvider.getString(
                                        string.theme_picker_carousel_info_item_description
                                    )
                                )
                            )
                    )
                }
            },
            onFailure = {
                _viewState.update { Error }
            }
        )
    }

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onSkipPressed() {
        triggerEvent(MoveToNextStep)
    }

    fun onThemeTapped(themeUri: String) {
        triggerEvent(NavigateToThemePreview(themeUri, navArgs.isFromStoreCreation))
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object Loading : ViewState

        @Parcelize
        object Error : ViewState

        @Parcelize
        data class Success(
            val carouselItems: List<CarouselItem> = emptyList()
        ) : ViewState {
            sealed class CarouselItem : Parcelable {
                @Parcelize
                data class Theme(
                    val themeId: String,
                    val name: String,
                    val screenshotUrl: String,
                    val demoUri: String
                ) : CarouselItem()

                @Parcelize
                data class Message(val title: String, val description: String) : CarouselItem()
            }
        }
    }

    object MoveToNextStep : Event()
    data class NavigateToThemePreview(val themeId: String, val isFromStoreCreation: Boolean) : Event()
}
