package com.woocommerce.android.ui.orders.creation.giftcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditGiftCardViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _giftCard = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ""
    )
    val giftCard = _giftCard.asLiveData()

    fun onGiftCardChanged(giftCard: String) {
        _giftCard.value = giftCard
    }
}
