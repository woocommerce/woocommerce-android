package com.woocommerce.android.ui.woopospromo

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosPromoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private val _state = MutableStateFlow(WooPosPromoState())
    val state: StateFlow<WooPosPromoState> = _state.asStateFlow()

    fun onNextClick() {
        val currentPage = _state.value.currentPage
        val maxPage = _state.value.pages.size - 1
        if (currentPage < maxPage) {
            _state.value = _state.value.copy(currentPage = currentPage + 1)
        }
    }

    companion object {
        const val WOO_POS_DOCS_URL = "https://woocommerce.com/document/woo-mobile-app-point-of-sale-mode/"
    }
}
