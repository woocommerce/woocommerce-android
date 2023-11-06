package com.woocommerce.android.ui.payments.hub.depositsummary

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview

class PaymentsHumDepositSummaryStateMapperTest {
    private val mapper = PaymentsHumDepositSummaryStateMapper()

    @Test
    fun `given overview without default currency, when mapDepositOverviewToViewModelOverviews, then return null`() {
        // GIVEN
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 0, interval = ""
                ), defaultCurrency = null, depositsBlocked = false, depositsEnabled = false
            ), balance = null, deposit = null
        )

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given overview with instant balances, when mapDepositOverviewToViewModelOverviews, then return Overview with instant balances`() {
        // GIVEN
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                defaultCurrency = "USD",
                depositsBlocked = false,
                depositsEnabled = true,
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 1, interval = "DAILY"
                )
            ), balance = WooPaymentsDepositsOverview.Balance(
                available = listOf(
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 100,
                        currency = "USD",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    )
                ), instant = listOf(
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 200,
                        currency = "USD",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    )
                ), pending = listOf(
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 300,
                        currency = "USD",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = 2
                    )
                )
            ), deposit = null
        )

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        assertThat(result).isNotNull
        assertThat(result?.defaultCurrency).isEqualTo("USD")
        assertThat(result?.infoPerCurrency?.get("USD")?.availableFunds).isEqualTo(100)
        assertThat(result?.infoPerCurrency?.get("USD")?.pendingFunds).isEqualTo(300)
        assertThat(result?.infoPerCurrency?.get("USD")?.pendingBalanceDepositsCount).isEqualTo(2)
        assertThat(result?.infoPerCurrency?.get("USD")?.fundsAvailableInDays).isEqualTo(
            PaymentsHubDepositSummaryState.Info.Interval.Days(
                1
            )
        )
    }

    @Test
    fun `given overview with multiple currencies and different deposit statuses, when mapDepositOverviewToViewModelOverviews, then return Overview with correct statuses`() {
        // GIVEN
        val currentDate = System.currentTimeMillis()
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                defaultCurrency = "USD",
                depositsBlocked = false,
                depositsEnabled = true,
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 30, interval = "MONTHLY"
                )
            ), balance = WooPaymentsDepositsOverview.Balance(
                available = listOf(
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 100,
                        currency = "USD",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    ), WooPaymentsDepositsOverview.Balance.Info(
                        amount = 150,
                        currency = "EUR",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    )
                ), instant = null, pending = null
            ), deposit = WooPaymentsDepositsOverview.Deposit(
                lastPaid = listOf(
                    WooPaymentsDepositsOverview.Deposit.Info(
                        amount = 200,
                        automatic = true,
                        bankAccount = "bank_account",
                        created = currentDate,
                        currency = "USD",
                        date = currentDate,
                        fee = null,
                        feePercentage = null,
                        depositId = "deposit_id",
                        status = "PAID",
                        type = "type"
                    )
                ), nextScheduled = listOf(
                    WooPaymentsDepositsOverview.Deposit.Info(
                        amount = 250,
                        automatic = true,
                        bankAccount = "bank_account",
                        created = currentDate,
                        currency = "EUR",
                        date = currentDate,
                        fee = null,
                        feePercentage = null,
                        depositId = "deposit_id",
                        status = "ESTIMATED",
                        type = "type",
                    )
                ), lastManualDeposits = null
            )
        )

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        assertThat(result).isNotNull
        assertThat(result?.defaultCurrency).isEqualTo("USD")
        assertThat(result?.infoPerCurrency?.get("USD")?.lastDeposit?.status).isEqualTo(
            PaymentsHubDepositSummaryState.Deposit.Status.PAID
        )
        assertThat(result?.infoPerCurrency?.get("EUR")?.nextDeposit?.status).isEqualTo(
            PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED
        )
    }

    @Test
    fun `given overview with deposits scheduled and no balances, when mapDepositOverviewToViewModelOverviews, then return Overview with scheduled deposits only`() {
        // GIVEN
        val currentDate = System.currentTimeMillis()
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                defaultCurrency = "USD",
                depositsBlocked = false,
                depositsEnabled = true,
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 5,
                    interval = "DAILY"
                )
            ),
            balance = null,
            deposit = WooPaymentsDepositsOverview.Deposit(
                lastPaid = null,
                nextScheduled = listOf(
                    WooPaymentsDepositsOverview.Deposit.Info(
                        amount = 500,
                        automatic = true,
                        bankAccount = "bank_account",
                        created = currentDate,
                        currency = "USD",
                        date = currentDate,
                        fee = null,
                        feePercentage = null,
                        depositId = "deposit_id_next",
                        status = "ESTIMATED",
                        type = "type"
                    )
                ),
                lastManualDeposits = null
            )
        )

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        assertThat(result).isNotNull
        assertThat(result?.defaultCurrency).isEqualTo("USD")
        assertThat(result?.infoPerCurrency?.get("USD")?.nextDeposit?.amount).isEqualTo(500)
        assertThat(result?.infoPerCurrency?.get("USD")?.nextDeposit?.status).isEqualTo(PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED)
        assertThat(result?.infoPerCurrency?.get("USD")?.availableFunds).isEqualTo(0)
        assertThat(result?.infoPerCurrency?.get("USD")?.pendingFunds).isEqualTo(0)
    }
}
