package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.model.UiString

sealed class SelectPaymentMethodViewState {
    object Loading : SelectPaymentMethodViewState()
    data class Success(
        val paymentUrl: String,
        val orderTotal: String,
        val isPaymentCollectableWithExternalCardReader: Boolean,
        val isPaymentCollectableWithTapToPay: Boolean,
        val isScanToPayAvailable: Boolean,
        val learnMoreIpp: LearnMoreIpp,
    ) : SelectPaymentMethodViewState()

    data class LearnMoreIpp(
        val label: UiString,
        val onClick: () -> Unit,
    )
}
