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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@HiltViewModel
class OrderCreateEditGiftCardViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: OrderCreateEditGiftCardFragmentArgs by savedState.navArgs()

    private val codeFormatRegex by lazy {
        "^[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}$".toRegex()
    }

    private val _giftCard = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.giftCard.orEmpty()
    )
    val viewState = _giftCard
        .map { ViewState(giftCard = it, isValidCode = it.matches(codeFormatRegex)) }
        .asLiveData()

    fun onGiftCardChanged(giftCard: String) {
        _giftCard.value = giftCard
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(GiftCardResult(_giftCard.value)))
    }

    data class ViewState(
        val giftCard: String,
        val isValidCode: Boolean
    )

    @Parcelize
    data class GiftCardResult(val selectedGiftCard: String) : Parcelable
}
