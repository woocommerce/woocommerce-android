package com.woocommerce.android.ui.payments.hub.depositsummary

import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import javax.inject.Inject

class PaymentsHumDepositSummaryStateMapper @Inject constructor() {
    fun mapDepositOverviewToViewModelOverviews(
        overview: WooPaymentsDepositsOverview
    ): List<PaymentsHubDepositSummaryState.Overview> {
        val pendingBalances = overview.balance?.pending.orEmpty()
        val availableBalances = overview.balance?.available.orEmpty()

        val lastPaidDeposits = overview.deposit?.lastPaid.orEmpty()
        val nextDeposits = overview.deposit?.nextScheduled.orEmpty()

        val defaultCurrency = overview.account?.defaultCurrency.orEmpty()

        if (defaultCurrency.isEmpty()) return emptyList()

        val currencies = ((pendingBalances + availableBalances).map { it.currency } +
            (lastPaidDeposits + nextDeposits).map { it.currency }).toSet()
        if (currencies.isEmpty()) return emptyList()

        currencies.map {currency ->
            PaymentsHubDepositSummaryState.Info(
                availableFunds = availableBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                pendingFunds = pendingBalances.firstOrNull { it.currency == currency }?.amount ?: 0,
                depositsCount = pending.firstOrNull { it.currency == currency }?. ?: 0,
            )
        }

        return PaymentsHubDepositSummaryState.Overview(
            defaultCurrency = defaultCurrency,
            infoPerCurrency =
        )
    }
}
