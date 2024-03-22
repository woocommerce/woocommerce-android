package com.woocommerce.android.ui.themes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CarouselState.Success.CarouselItem.Message
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CarouselState.Success.CarouselItem.Theme
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val themeRepository: ThemeRepository,
    private val resourceProvider: ResourceProvider,
    private val crashLogger: CrashLogging,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val currentTheme = MutableStateFlow<CurrentThemeState>(CurrentThemeState.Hidden)
    private val carouselState = MutableStateFlow<CarouselState>(CarouselState.Loading)
    val viewState = combine(
        carouselState,
        currentTheme
    ) { carouselState, currentThemeState ->
        val updatedCarouseState = removeCurrentThemeFromCarouselItems(carouselState, currentThemeState)
        ViewState(
            carouselState = updatedCarouseState,
            currentThemeState = currentThemeState
        )
    }.asLiveData()

    init {
        loadData()
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.THEME_PICKER_SCREEN_DISPLAYED,
            properties = mapOf(
                AnalyticsTracker.KEY_THEME_PICKER_SOURCE to AnalyticsTracker.VALUE_THEME_PICKER_SOURCE_SETTINGS
            )
        )
    }

    private fun loadThemes() {
        viewModelScope.launch {
            carouselState.update { CarouselState.Loading }
            val result = themeRepository.fetchThemes().fold(
                onSuccess = { result ->
                    CarouselState.Success(
                        carouselItems = result
                            .filter { theme -> theme.demoUrl != null }
                            .map { theme ->
                                Theme(
                                    themeId = theme.id,
                                    name = theme.name,
                                    screenshotUrl = AppUrls.getScreenshotUrl(theme.demoUrl!!)
                                )
                            }
                            .plus(
                                Message(
                                    title = resourceProvider.getString(
                                        R.string.theme_picker_carousel_info_item_title
                                    ),
                                    description = resourceProvider.getString(
                                        resourceId = R.string.theme_picker_carousel_info_item_description_settings
                                    )
                                )
                            )
                    )
                },
                onFailure = { CarouselState.Error }
            )
            carouselState.update { result }
        }
    }

    private fun loadCurrentTheme() {
        viewModelScope.launch {
            currentTheme.value = CurrentThemeState.Loading
            themeRepository.fetchCurrentTheme().fold(
                onSuccess = { theme ->
                    currentTheme.value = CurrentThemeState.Success(theme.name, theme.id)
                },
                onFailure = {
                    // Wait for the carousel to load
                    carouselState.filter { it !is CarouselState.Loading }
                        .take(1)
                        .onEach {
                            if (it is CarouselState.Success) {
                                // If the carousel loaded successfully, show a snackbar
                                triggerEvent(ShowSnackbar(R.string.theme_picker_loading_current_theme_failed))
                            }
                            currentTheme.value = CurrentThemeState.Hidden
                        }
                        .launchIn(viewModelScope)
                }
            )
        }
    }

    private fun removeCurrentThemeFromCarouselItems(
        carouselState: CarouselState,
        currentThemeState: CurrentThemeState
    ) = if (carouselState is CarouselState.Success && currentThemeState is CurrentThemeState.Success) {
        carouselState.copy(
            carouselItems = carouselState.carouselItems
                .filter {
                    when (it) {
                        is Theme -> it.themeId != currentThemeState.themeId
                        else -> true
                    }
                }
        )
    } else {
        carouselState
    }

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onThemeTapped(theme: Theme) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.THEME_PICKER_THEME_SELECTED,
            properties = mapOf(AnalyticsTracker.KEY_THEME_PICKER_THEME to theme.themeId)
        )
        triggerEvent(NavigateToThemePreview(theme.themeId))
    }

    fun onCurrentThemeUpdated(themeId: String, themeName: String) {
        currentTheme.value = CurrentThemeState.Success(themeName = themeName, themeId = themeId)
    }

    fun onThemeScreenshotFailure(themeName: String, throwable: Throwable) {
        @Suppress("MagicNumber")
        fun Int.isRedirect() = this in 300..399

        if (throwable is HttpException && throwable.response.code.isRedirect()) {
            // A redirect means that the screenshot is not cached by MShot
            val message = "Screenshot for theme $themeName is unavailable"
            crashLogger.sendReport(Exception(message))
        }
    }

    fun onRetryTapped() {
        loadData()
    }

    private fun loadData() {
        loadCurrentTheme()
        loadThemes()
    }

    data class ViewState(
        val carouselState: CarouselState,
        val currentThemeState: CurrentThemeState
    )

    sealed interface CarouselState {
        object Loading : CarouselState

        object Error : CarouselState

        data class Success(
            val carouselItems: List<CarouselItem> = emptyList()
        ) : CarouselState {
            sealed class CarouselItem {
                data class Theme(
                    val themeId: String,
                    val name: String,
                    val screenshotUrl: String
                ) : CarouselItem()

                data class Message(val title: String, val description: String) : CarouselItem()
            }
        }
    }

    sealed interface CurrentThemeState {
        object Hidden : CurrentThemeState

        object Loading : CurrentThemeState

        data class Success(val themeName: String, val themeId: String) : CurrentThemeState
    }

    data class NavigateToThemePreview(val themeId: String) : Event()
}
