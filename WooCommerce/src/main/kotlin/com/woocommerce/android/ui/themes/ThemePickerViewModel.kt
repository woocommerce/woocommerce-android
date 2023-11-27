package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.CarouselItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedStateHandle.getStateFlow(viewModelScope, ViewState())
    val viewState = _viewState.asLiveData()

    init {
        _viewState.update {
            _viewState.value.copy(
                carouselItems = listOf(
                    CarouselItem.Theme(screenshotUrl = getThemeUrl(themeName = "tsubaki")),
                    CarouselItem.Theme(screenshotUrl = getThemeUrl(themeName = "tazza")),
                    CarouselItem.Theme(screenshotUrl = getThemeUrl(themeName = "amulet")),
                    CarouselItem.Theme(screenshotUrl = getThemeUrl(themeName = "zaino")),
                    CarouselItem.Theme(screenshotUrl = getThemeUrl(themeName = "thriving-artist")),
                    CarouselItem.Theme(screenshotUrl = getThemeUrl(themeName = "attar")),
                    CarouselItem.Message(
                        title = resourceProvider.getString(
                            R.string.store_creation_theme_picker_carousel_info_item_title
                        ),
                        description = resourceProvider.getString(
                            R.string.store_creation_theme_picker_carousel_info_item_description
                        ),
                    ),
                )
            )
        }
    }

    private fun getThemeUrl(themeName: String) =
        "https://s0.wp.com/mshots/v1/https://${themeName}demo.wpcomstaging.com/" +
            "?demo=true/?w=1200&h=2400&vpw=400&vph=800"

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onSkipPressed() {
        triggerEvent(MoveToNextStep)
    }

    @Parcelize
    data class ViewState(
        val carouselItems: List<CarouselItem> = emptyList(),
        val isLoading: Boolean = false
    ) : Parcelable {
        sealed class CarouselItem : Parcelable {
            @Parcelize
            data class Theme(val screenshotUrl: String) : CarouselItem()

            @Parcelize
            data class Message(val title: String, val description: String) : CarouselItem()
        }
    }

    object MoveToNextStep : Event()
}
