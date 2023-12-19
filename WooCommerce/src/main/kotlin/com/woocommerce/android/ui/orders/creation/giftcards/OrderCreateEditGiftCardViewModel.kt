package com.woocommerce.android.ui.orders.creation.giftcards

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditGiftCardViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: OrderCreateEditGiftCardFragmentArgs by savedState.navArgs()

    private val _giftCard = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.giftCard.orEmpty()
    )
    val giftCard = _giftCard.asLiveData()

    fun onGiftCardChanged(giftCard: String) {
        _giftCard.value = giftCard
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(GiftCardResult(_giftCard.value)))
    }

    @Parcelize
    data class GiftCardResult(val selectedGiftCard: String) : Parcelable
}
