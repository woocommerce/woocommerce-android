package com.woocommerce.android.ui.payments.hub.depositsummary

import java.util.Date

sealed class PaymentsHubDepositSummaryState {
    object Loading : PaymentsHubDepositSummaryState()
    data class Error(val errorMessage: String) : PaymentsHubDepositSummaryState()
    data class Success(val overviews: List<Overview>) : PaymentsHubDepositSummaryState()

    data class Overview(
        val currency: String,
        val availableFunds: Int,
        val pendingFunds: Int,
        val depositsCount: Int,
        val fundsAvailableInDays: Interval,
        val nextDeposit: Deposit,
        val lastDeposit: Deposit,
    ) {
        sealed class Interval {
            data class Days(val days: Int) : Interval()
            data class Weekly(val days: Int) : Interval()
            data class Monthly(val nameOfTheDay: String) : Interval()
        }
    }

    data class Deposit(
        val amount: Int,
        val status: Status,
        val date: Date,
    ) {
        enum class Status {
            ESTIMATED, PENDING, IN_TRANSIT, PAID, CANCELED, FAILED
        }
    }
}
