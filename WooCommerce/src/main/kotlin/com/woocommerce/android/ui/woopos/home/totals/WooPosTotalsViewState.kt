package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosTotalsViewState : Parcelable {
    data object Loading : WooPosTotalsViewState()

    data class Checkout(
        val totals: Totals,
        val readerStatus: ReaderStatus,
    ) : WooPosTotalsViewState()

    sealed class Totals : Parcelable {
        @Parcelize
        data object Hidden : Totals()

        @Parcelize
        data class Visible(
            val orderSubtotalText: String,
            val orderTaxText: String,
            val orderTotalText: String,
        ) : Totals()
    }

    data class PaymentSuccess(val orderTotalText: String) : WooPosTotalsViewState()

    sealed class ReaderStatus : Parcelable {
        @Parcelize
        data class Preparing(
            val title: String,
            val subtitle: String,
        ) : ReaderStatus()

        @Parcelize
        data class CheckingOrder(
            val title: String,
            val subtitle: String,
        ) : ReaderStatus()

        @Parcelize
        data class ReadyForPayment(
            val title: String,
            val subtitle: String,
        ) : ReaderStatus()

        @Parcelize
        data class Disconnected(
            val title: String,
            val subtitle: String,
            val actionButtonLabel: String,
        ) : ReaderStatus()

        @Parcelize
        object Unavailable : ReaderStatus()
    }

    data class PaymentInProgress(
        val title: String,
        val subtitle: String,
    ) : WooPosTotalsViewState()

    data class PaymentFailed(
        val title: String,
        val subtitle: String,
        val retryPaymentButtonLabel: String,
        val isReturnToCheckoutButtonVisible: Boolean = false,
    ) : WooPosTotalsViewState()

    data class Error(val message: String) : WooPosTotalsViewState()
}
