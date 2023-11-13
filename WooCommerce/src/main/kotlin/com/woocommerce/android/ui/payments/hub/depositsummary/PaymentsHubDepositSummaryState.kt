package com.woocommerce.android.ui.payments.hub.depositsummary

sealed class PaymentsHubDepositSummaryState {
    object Loading : PaymentsHubDepositSummaryState()
    data class Error(val errorMessage: String) : PaymentsHubDepositSummaryState()
    data class Success(val overview: Overview) : PaymentsHubDepositSummaryState()

    data class Overview(
        val defaultCurrency: String,
        val infoPerCurrency: Map<String, Info>,
    )

    data class Info(
        val availableFunds: String,
        val pendingFunds: String,
        val pendingBalanceDepositsCount: Int,
        val fundsAvailableInDays: Interval,
        val nextDeposit: Deposit?,
        val lastDeposit: Deposit?,
    ) {
        sealed class Interval {
            data class Days(val days: Int) : Interval()
            data class Weekly(val days: Int) : Interval()
            data class Monthly(val nameOfTheDay: String) : Interval()
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
