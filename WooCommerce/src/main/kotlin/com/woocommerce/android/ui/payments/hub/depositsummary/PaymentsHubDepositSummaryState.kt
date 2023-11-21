package com.woocommerce.android.ui.payments.hub.depositsummary

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError

sealed class PaymentsHubDepositSummaryState {
    object Loading : PaymentsHubDepositSummaryState()
    data class Error(val error: WooError) : PaymentsHubDepositSummaryState()
    data class Success(
        val overview: Overview,
        val fromCache: Boolean,
        val onLearnMoreClicked: () -> Unit,
        val onExpandCollapseClicked: (Boolean) -> Unit,
        val onCurrencySelected: (String) -> Unit,
    ) : PaymentsHubDepositSummaryState()

    data class Overview(
        val defaultCurrency: String,
        val infoPerCurrency: Map<String, Info>,
    )

    data class Info(
        val availableFundsFormatted: String,
        val pendingFundsFormatted: String,
        val availableFundsAmount: Long,
        val pendingFundsAmount: Long,
        val pendingBalanceDepositsCount: Int,
        val fundsAvailableInDays: Int?,
        val fundsDepositInterval: Interval?,
        val nextDeposit: Deposit?,
        val lastDeposit: Deposit?,
    ) {
        sealed class Interval {
            object Daily : Interval()
            data class Weekly(val weekDay: String) : Interval()
            data class Monthly(val day: Int) : Interval()
        }
    }

    data class Deposit(
        val amount: String,
        val status: Status,
        val date: String,
    ) {
        enum class Status {
            ESTIMATED, PENDING, IN_TRANSIT, PAID, CANCELED, FAILED, UNKNOWN
        }
    }
}
