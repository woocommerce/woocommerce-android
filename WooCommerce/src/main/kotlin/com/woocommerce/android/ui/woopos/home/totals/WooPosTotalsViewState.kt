package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosTotalsViewState : Parcelable {
    data object Loading : WooPosTotalsViewState()

    data class Checkout(
        val totals: Totals,
        val readerStatus: ReaderStatus,
        val isFreeOrder: Boolean,
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

    sealed class ReaderStatus(
        open val title: String,
        open val subtitle: String,
    ) : Parcelable {
        @Parcelize
        data class Preparing(
            override val title: String,
            override val subtitle: String,
        ) : ReaderStatus(
            title = title,
            subtitle = subtitle
        )

        @Parcelize
        data class CheckingOrder(
            override val title: String,
            override val subtitle: String,
        ) : ReaderStatus(
            title = title,
            subtitle = subtitle
        )

        @Parcelize
        data class ReadyForPayment(
            override val title: String,
            override val subtitle: String,
        ) : ReaderStatus(
            title = title,
            subtitle = subtitle
        )

        @Parcelize
        data class Disconnected(
            override val title: String,
            override val subtitle: String,
            val actionButtonLabel: String,
        ) : ReaderStatus(
            title = title,
            subtitle = subtitle
        )
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
