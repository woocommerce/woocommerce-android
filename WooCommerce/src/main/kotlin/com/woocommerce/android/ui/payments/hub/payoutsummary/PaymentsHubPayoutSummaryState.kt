package com.woocommerce.android.ui.payments.hub.payoutsummary

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import java.util.SortedMap

sealed class PaymentsHubPayoutSummaryState {
    object Loading : PaymentsHubPayoutSummaryState()
    data class Error(val error: WooError) : PaymentsHubPayoutSummaryState()
    data class Success(
        val overview: Overview,
        val fromCache: Boolean,
        val onLearnMoreClicked: () -> Unit,
        val onExpandCollapseClicked: (Boolean) -> Unit,
        val onCurrencySelected: (String) -> Unit,
    ) : PaymentsHubPayoutSummaryState()

    data class Overview(
        val defaultCurrency: String,
        val infoPerCurrency: SortedMap<String, Info>,
    )

    data class Info(
        val availableFundsFormatted: String,
        val pendingFundsFormatted: String,
        val availableFundsAmount: Long,
        val pendingFundsAmount: Long,
        val fundsAvailableInDays: Int?,
        val fundsPayoutInterval: Interval?,
        val lastPayout: Payout?,
    ) {
        sealed class Interval {
            object Daily : Interval()
            data class Weekly(val weekDay: String) : Interval()
            data class Monthly(val day: Int) : Interval()
        }
    }

    data class Payout(
        val amount: String,
        val status: Status,
        val date: String,
    ) {
        enum class Status {
            ESTIMATED, PENDING, IN_TRANSIT, PAID, CANCELED, FAILED, UNKNOWN
        }
    }
}
