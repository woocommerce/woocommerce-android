package com.woocommerce.android.ui.payments.hub.depositsummary

import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import java.util.Date
import javax.inject.Inject

class PaymentsHumDepositSummaryStateMapper @Inject constructor() {

    @Suppress("ReturnCount")
    fun mapDepositOverviewToViewModelOverviews(
        overview: WooPaymentsDepositsOverview
    ): PaymentsHubDepositSummaryState.Overview? {
        val pendingBalances = overview.balance?.pending.orEmpty()
        val availableBalances = overview.balance?.available.orEmpty()

        val lastPaidDeposits = overview.deposit?.lastPaid.orEmpty()
        val nextDeposits = overview.deposit?.nextScheduled.orEmpty()

        val defaultCurrency = overview.account?.defaultCurrency.orEmpty()

        if (defaultCurrency.isEmpty()) return null

        val currencies = (
            (pendingBalances + availableBalances).map {
                it.currency
            } + (lastPaidDeposits + nextDeposits).map {
                it.currency
            }
            ).filterNotNull().toSet()
        if (currencies.isEmpty()) return null

        return PaymentsHubDepositSummaryState.Overview(
            defaultCurrency = defaultCurrency,
            infoPerCurrency = currencies.associateWith { currency ->
                PaymentsHubDepositSummaryState.Info(
                    availableFunds = availableBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                    pendingFunds = pendingBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                    pendingBalanceDepositsCount = pendingBalances.firstOrNull {
                        it.currency == currency
                    }?.depositsCount ?: 0,
                    fundsAvailableInDays = overview.account.fundsAvailableIn(),
                    nextDeposit = nextDeposits.firstOrNull { it.currency == currency }?.let { mapDeposit(it) },
                    lastDeposit = lastPaidDeposits.firstOrNull { it.currency == currency }?.let { mapDeposit(it) }
                )
            }
        )
    }

    private fun mapDeposit(it: WooPaymentsDepositsOverview.Deposit.Info) =
        PaymentsHubDepositSummaryState.Deposit(
            amount = it.amount ?: 0,
            status = it.status.toDepositStatus(),
            date = if (it.date != null) Date(it.date!!) else null
        )

    // Proper implementation in the following PRs
    private fun WooPaymentsDepositsOverview.Account?.fundsAvailableIn() =
        PaymentsHubDepositSummaryState.Info.Interval.Days(this?.depositsSchedule?.delayDays ?: 0)

    private fun String?.toDepositStatus() =
        when (this?.uppercase()) {
            "ESTIMATED" -> PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED
            "PENDING" -> PaymentsHubDepositSummaryState.Deposit.Status.PENDING
            "IN_TRANSIT" -> PaymentsHubDepositSummaryState.Deposit.Status.IN_TRANSIT
            "PAID" -> PaymentsHubDepositSummaryState.Deposit.Status.PAID
            "CANCELED" -> PaymentsHubDepositSummaryState.Deposit.Status.CANCELED
            "FAILED" -> PaymentsHubDepositSummaryState.Deposit.Status.FAILED
            else -> PaymentsHubDepositSummaryState.Deposit.Status.UNKNOWN
        }
}
