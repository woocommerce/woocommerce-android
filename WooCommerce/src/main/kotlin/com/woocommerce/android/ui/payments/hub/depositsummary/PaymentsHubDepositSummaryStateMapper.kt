package com.woocommerce.android.ui.payments.hub.depositsummary

import com.woocommerce.android.extensions.formatToDDMMMYYYY
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import java.util.Date
import javax.inject.Inject

class PaymentsHubDepositSummaryStateMapper @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
    private val dateFormatter: DateToDDMMMYYYYStringFormatter
) {
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
                    availableFunds = formatMoney(
                        amount = availableBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                        currency = currency
                    ),
                    pendingFunds = formatMoney(
                        amount = pendingBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                        currency = currency
                    ),
                    pendingBalanceDepositsCount = pendingBalances.firstOrNull {
                        it.currency == currency
                    }?.depositsCount ?: 0,
                    fundsAvailableInDays = overview.account?.depositsSchedule?.delayDays,
                    fundsDepositInterval = overview.account.fundsAvailableIn(),
                    nextDeposit = nextDeposits.firstOrNull { it.currency == currency }?.let { mapDeposit(it) },
                    lastDeposit = lastPaidDeposits.firstOrNull { it.currency == currency }?.let { mapDeposit(it) }
                )
            }
        )
    }

    private fun mapDeposit(info: WooPaymentsDepositsOverview.Deposit.Info) =
        PaymentsHubDepositSummaryState.Deposit(
            amount = formatMoney(info.amount ?: 0L, info.currency.orEmpty()),
            status = info.status.toDepositStatus(),
            date = if (info.date != null) dateFormatter(Date(info.date!!)) else ""
        )

    private fun formatMoney(amount: Long, currency: String) =
        currencyFormatter.formatCurrencyGivenInTheSmallestCurrencyUnit(
            amount = amount,
            currencyCode = currency,
        )

    @Suppress("ReturnCount")
    private fun WooPaymentsDepositsOverview.Account?.fundsAvailableIn(): PaymentsHubDepositSummaryState.Info.Interval? {
        return when (this?.depositsSchedule?.interval?.lowercase()) {
            "daily" -> PaymentsHubDepositSummaryState.Info.Interval.Daily
            "weekly" -> PaymentsHubDepositSummaryState.Info.Interval.Weekly(
                this.depositsSchedule?.weeklyAnchor ?: return null
            )

            "monthly" -> PaymentsHubDepositSummaryState.Info.Interval.Monthly(
                this.depositsSchedule?.monthlyAnchor ?: return null
            )

            else -> null
        }
    }

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

class DateToDDMMMYYYYStringFormatter @Inject constructor() {
    operator fun invoke(date: Date): String = date.formatToDDMMMYYYY()
}
