package com.woocommerce.android.ui.pospromo

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PosPromoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTracker: PosPromoAnalyticsTracker,
    private val utmProvider: PosPromoUtmProvider,
) : ScopedViewModel(savedStateHandle) {

    private val _state = MutableStateFlow(PosPromoState())
    val state: StateFlow<PosPromoState> = _state.asStateFlow()

    init {
        analyticsTracker.trackModalViewed()
        analyticsTracker.trackSlideViewed(slideIndex = 0)
    }

    fun onNextClick() {
        val currentPage = _state.value.currentPage
        val maxPage = _state.value.pages.size - 1
        if (currentPage < maxPage) {
            val newPage = currentPage + 1
            _state.value = _state.value.copy(currentPage = newPage)
            analyticsTracker.trackSlideViewed(slideIndex = newPage)
        }
    }

    fun onDismiss() {
        analyticsTracker.trackModalDismissed()
    }

    fun onExploreClick() {
        analyticsTracker.trackExploreClicked()
        val url = utmProvider.getUrlWithUtmParams(WOO_POS_DOCS_URL)
        triggerEvent(NavigateToExplore(url))
    }

    data class NavigateToExplore(val url: String) : MultiLiveEvent.Event()

    companion object {
        private const val WOO_POS_DOCS_URL = "https://woocommerce.com/mobile/pos/learn-more"
    }
}
