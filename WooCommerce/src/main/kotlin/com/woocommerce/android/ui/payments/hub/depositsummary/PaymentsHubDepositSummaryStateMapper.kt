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
    fun mapDepositOverviewToViewModelOverviews(
        overview: WooPaymentsDepositsOverview
    ): Result {
        val pendingBalances = overview.balance?.pending.orEmpty()
        val availableBalances = overview.balance?.available.orEmpty()

        val lastPaidDeposits = overview.deposit?.lastPaid.orEmpty()

        val defaultCurrency = overview.account?.defaultCurrency.orEmpty()

        if (defaultCurrency.isEmpty()) return Result.InvalidInputData

        val pendingAndAvailableBalancesCurrencies = (pendingBalances + availableBalances).map { it.currency }
        val lastPaidDepositsCurrencies = lastPaidDeposits.map { it.currency }
        val currencies = (pendingAndAvailableBalancesCurrencies + lastPaidDepositsCurrencies).filterNotNull().toSet()

        return if (currencies.isEmpty()) {
            Result.InvalidInputData
        } else {
            Result.Success(
                PaymentsHubDepositSummaryState.Overview(
                    defaultCurrency = defaultCurrency,
                    infoPerCurrency = currencies.associateWith { currency ->
                        PaymentsHubDepositSummaryState.Info(
                            availableFundsFormatted = formatMoney(
                                amount = availableBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                                currency = currency
                            ),
                            pendingFundsFormatted = formatMoney(
                                amount = pendingBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                                currency = currency
                            ),
                            availableFundsAmount = availableBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                            pendingFundsAmount = pendingBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                            fundsAvailableInDays = overview.account?.depositsSchedule?.delayDays,
                            fundsDepositInterval = overview.account.fundsAvailableIn(),
                            lastDeposit = lastPaidDeposits.firstOrNull { it.currency == currency }?.let {
                                mapDeposit(it)
                            }
                        )
                    }.toSortedMap(
                        compareBy<String> { it != defaultCurrency }.thenBy { it }
                    )
                )
            )
        }
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

    sealed class Result {
        data class Success(val overview: PaymentsHubDepositSummaryState.Overview) : Result()
        object InvalidInputData : Result()
    }
}

class DateToDDMMMYYYYStringFormatter @Inject constructor() {
    operator fun invoke(date: Date): String = date.formatToDDMMMYYYY()
}
