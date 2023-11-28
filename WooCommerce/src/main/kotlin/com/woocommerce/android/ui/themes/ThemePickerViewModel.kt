package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.CarouselItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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
                    CarouselItem.Theme(name = "tsubaki", screenshotUrl = getThemeUrl(themeUrl = "wordpress.org")),
                    CarouselItem.Theme(name = "tazza", screenshotUrl = getThemeUrl(themeUrl = "facebook.com")),
                    CarouselItem.Theme(name = "amulet", screenshotUrl = getThemeUrl(themeUrl = "twitter.com")),
                    CarouselItem.Theme(name = "zaino", screenshotUrl = getThemeUrl(themeUrl = "apple.com")),
                    CarouselItem.Theme(name = "thriving-artist", screenshotUrl = getThemeUrl(themeUrl = "dennikn.sk")),
                    CarouselItem.Theme(name = "attar", screenshotUrl = getThemeUrl(themeUrl = "android.com")),
                    CarouselItem.Message(
                        title = resourceProvider.getString(
                            R.string.theme_picker_carousel_info_item_title
                        ),
                        description = resourceProvider.getString(
                            R.string.theme_picker_carousel_info_item_description
                        ),
                    ),
                )
            )
        }
    }

    private fun getThemeUrl(themeUrl: String) =
        "https://s0.wp.com/mshots/v1/https://$themeUrl?demo=true/?w=1200&h=2400&vpw=400&vph=800"

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onSkipPressed() {
        triggerEvent(MoveToNextStep)
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
                data class Theme(val name: String, val screenshotUrl: String) : CarouselItem()

                @Parcelize
                data class Message(val title: String, val description: String) : CarouselItem()
            }
        }
    }

    object MoveToNextStep : Event()
}
