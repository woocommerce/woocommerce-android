package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TapToPaySummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    fun onTryPaymentClicked() {
        // TODO
    }

    fun onBackClicked() {
        triggerEvent(Event.Exit)
    }
}
