package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CarouselState.Success.CarouselItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val themeRepository: ThemeRepository,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ThemePickerFragmentArgs by savedStateHandle.navArgs()

    val viewState = combine(
        flowOf(navArgs.isFromStoreCreation),
        loadThemes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = CarouselState.Loading
        ),
        loadCurrentTheme().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = CurrentThemeState.Hidden
        )
    ) { isFromStoreCreation, carouselState, currentThemeState ->
        ViewState(
            isFromStoreCreation = isFromStoreCreation,
            carouselState = carouselState,
            currentThemeState = currentThemeState
        )
    }.asLiveData()

    private fun loadThemes(): Flow<CarouselState> = flow {
        emit(CarouselState.Loading)
        val result = themeRepository.fetchThemes().fold(
            onSuccess = { result ->
                CarouselState.Success(
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
                                    R.string.theme_picker_carousel_info_item_title
                                ),
                                description = resourceProvider.getString(
                                    R.string.theme_picker_carousel_info_item_description
                                )
                            )
                        )
                )
            },
            onFailure = { CarouselState.Error }
        )
        emit(result)
    }

    private fun loadCurrentTheme(): Flow<CurrentThemeState> = flow {
        if (navArgs.isFromStoreCreation) {
            emit(CurrentThemeState.Hidden)
            return@flow
        }
        emit(CurrentThemeState.Loading)
        val result = themeRepository.fetchCurrentTheme().fold(
            onSuccess = { theme ->
                CurrentThemeState.Success(theme.name)
            },
            onFailure = {
                triggerEvent(Event.ShowSnackbar(R.string.theme_picker_loading_current_theme_failed))
                CurrentThemeState.Hidden
            }
        )
        emit(result)
    }

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onSkipPressed() {
        triggerEvent(NavigateToNextStep)
    }

    fun onThemeTapped(themeUri: String) {
        triggerEvent(NavigateToThemePreview(themeUri, navArgs.isFromStoreCreation))
    }

    data class ViewState(
        val isFromStoreCreation: Boolean,
        val carouselState: CarouselState,
        val currentThemeState: CurrentThemeState
    )

    sealed interface CarouselState {
        object Loading : CarouselState

        object Error : CarouselState

        data class Success(
            val carouselItems: List<CarouselItem> = emptyList()
        ) : CarouselState {
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

    sealed interface CurrentThemeState : Parcelable {
        @Parcelize
        object Hidden : CurrentThemeState

        @Parcelize
        object Loading : CurrentThemeState

        @Parcelize
        data class Success(val themeName: String) : CurrentThemeState
    }

    object NavigateToNextStep : Event()
    data class NavigateToThemePreview(val themeId: String, val isFromStoreCreation: Boolean) : Event()
}
