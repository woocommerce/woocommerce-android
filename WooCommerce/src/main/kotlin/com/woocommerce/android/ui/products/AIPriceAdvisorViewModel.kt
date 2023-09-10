package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AIPriceAdvisorViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    enum class AdviceType {
        REGULAR_PRICE,
        SALE_PRICE
    }
}
