package com.woocommerce.android.ui.payments.hub.depositsummary

import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import java.util.Date

class PaymentsHubDepositSummaryStateMapperTest {
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(100, "USD") }.thenReturn("100$")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(200, "USD") }.thenReturn("200$")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(300, "USD") }.thenReturn("300$")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(500, "USD") }.thenReturn("500$")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(0, "EUR") }.thenReturn("0€")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(150, "EUR") }.thenReturn("150€")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(250, "EUR") }.thenReturn("250€")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(0, "USD") }.thenReturn("0$")
        on { formatCurrencyGivenInTheSmallestCurrencyUnit(250, "RUB") }.thenReturn("250R")
    }
    private val dateFormatter: DateToDDMMMYYYYStringFormatter = mock()
    private val mapper = PaymentsHubDepositSummaryStateMapper(
        currencyFormatter,
        dateFormatter,
    )

    @Test
    fun `given overview without default currency, when mapDepositOverviewToViewModelOverviews, then return null`() {
        // GIVEN
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 0,
                    interval = "weekly",
                    monthlyAnchor = null,
                    weeklyAnchor = null
                ),
                defaultCurrency = null,
                depositsBlocked = false,
                depositsEnabled = false
            ),
            balance = null,
            deposit = null
        )

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        assertThat(result).isInstanceOf(PaymentsHubDepositSummaryStateMapper.Result.InvalidateInputDate::class.java)
    }

    @Suppress("LongMethod")
    @Test
    fun `given overview with instant balances, when mapDepositOverviewToViewModelOverviews, then return Overview with instant balances`() {
        // GIVEN
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                defaultCurrency = "USD",
                depositsBlocked = false,
                depositsEnabled = true,
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 1,
                    interval = "weekly",
                    monthlyAnchor = null,
                    weeklyAnchor = "monday"
                )
            ),
            balance = WooPaymentsDepositsOverview.Balance(
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
                ),
                instant = listOf(
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
                ),
                pending = listOf(
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
            ),
            deposit = null
        )

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        result as PaymentsHubDepositSummaryStateMapper.Result.Success
        assertThat(result.overview.defaultCurrency).isEqualTo("USD")
        assertThat(result.overview.infoPerCurrency["USD"]?.availableFundsFormatted).isEqualTo("100$")
        assertThat(result.overview.infoPerCurrency["USD"]?.availableFundsAmount).isEqualTo(100)
        assertThat(result.overview.infoPerCurrency["USD"]?.pendingFundsFormatted).isEqualTo("300$")
        assertThat(result.overview.infoPerCurrency["USD"]?.pendingFundsAmount).isEqualTo(300)
        assertThat(result.overview.infoPerCurrency["USD"]?.pendingBalanceDepositsCount).isEqualTo(2)
        assertThat(result.overview.infoPerCurrency["USD"]?.fundsAvailableInDays).isEqualTo(1)
        assertThat(result.overview.infoPerCurrency["USD"]?.fundsDepositInterval).isEqualTo(
            PaymentsHubDepositSummaryState.Info.Interval.Weekly("monday")
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `given overview with multiple currencies and different deposit statuses, when mapDepositOverviewToViewModelOverviews, then return Overview with correct statuses and sorted`() {
        // GIVEN
        val currentDate = System.currentTimeMillis()
        val overview = WooPaymentsDepositsOverview(
            account = WooPaymentsDepositsOverview.Account(
                defaultCurrency = "USD",
                depositsBlocked = false,
                depositsEnabled = true,
                depositsSchedule = WooPaymentsDepositsOverview.Account.DepositsSchedule(
                    delayDays = 30,
                    interval = "monthly",
                    monthlyAnchor = 15,
                    weeklyAnchor = null
                )
            ),
            balance = WooPaymentsDepositsOverview.Balance(
                available = listOf(
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 100,
                        currency = "RUB",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    ),
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 150,
                        currency = "EUR",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    ),
                    WooPaymentsDepositsOverview.Balance.Info(
                        amount = 100,
                        currency = "USD",
                        fee = null,
                        feePercentage = null,
                        net = null,
                        transactionIds = null,
                        sourceTypes = null,
                        depositsCount = null
                    ),
                ),
                instant = null,
                pending = null
            ),
            deposit = WooPaymentsDepositsOverview.Deposit(
                lastPaid = listOf(
                ),
                nextScheduled = listOf(
                    WooPaymentsDepositsOverview.Deposit.Info(
                        amount = 250,
                        automatic = true,
                        bankAccount = "bank_account",
                        created = currentDate,
                        currency = "RUB",
                        date = currentDate,
                        fee = null,
                        feePercentage = null,
                        depositId = "deposit_id",
                        status = "ESTIMATED",
                        type = "type",
                    ),
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
                    ),
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
                ),
                lastManualDeposits = null
            )
        )
        val date = "23 Oct 2024"
        whenever(dateFormatter(Date(currentDate))).thenReturn(date)

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        result as PaymentsHubDepositSummaryStateMapper.Result.Success
        assertThat(result.overview.defaultCurrency).isEqualTo("USD")
        val firstKey = result.overview.infoPerCurrency.keys.elementAt(0)
        val secondKey = result.overview.infoPerCurrency.keys.elementAt(1)
        val thirdKey = result.overview.infoPerCurrency.keys.elementAt(2)
        assertThat(result.overview.infoPerCurrency[firstKey]?.lastDeposit?.status).isEqualTo(
            PaymentsHubDepositSummaryState.Deposit.Status.PAID
        )
        assertThat(result.overview.infoPerCurrency[firstKey]?.lastDeposit?.amount).isEqualTo("200$")
        assertThat(result.overview.infoPerCurrency[firstKey]?.lastDeposit?.date).isEqualTo(date)
        assertThat(result.overview.infoPerCurrency[secondKey]?.nextDeposit?.status).isEqualTo(
            PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED
        )
        assertThat(result.overview.infoPerCurrency[secondKey]?.nextDeposit?.amount).isEqualTo("250€")
        assertThat(result.overview.infoPerCurrency[secondKey]?.nextDeposit?.date).isEqualTo(date)
        assertThat(result.overview.infoPerCurrency[firstKey]?.availableFundsFormatted).isEqualTo("100$")
        assertThat(result.overview.infoPerCurrency[firstKey]?.availableFundsAmount).isEqualTo(100)
        assertThat(result.overview.infoPerCurrency[firstKey]?.pendingFundsFormatted).isEqualTo("0$")
        assertThat(result.overview.infoPerCurrency[firstKey]?.pendingFundsAmount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency[firstKey]?.pendingBalanceDepositsCount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency[firstKey]?.fundsAvailableInDays).isEqualTo(30)
        assertThat(result.overview.infoPerCurrency[firstKey]?.fundsDepositInterval).isEqualTo(
            PaymentsHubDepositSummaryState.Info.Interval.Monthly(15)
        )
        assertThat(result.overview.infoPerCurrency[secondKey]?.availableFundsFormatted).isEqualTo("150€")
        assertThat(result.overview.infoPerCurrency[secondKey]?.availableFundsAmount).isEqualTo(150)
        assertThat(result.overview.infoPerCurrency[secondKey]?.pendingFundsFormatted).isEqualTo("0€")
        assertThat(result.overview.infoPerCurrency[secondKey]?.pendingFundsAmount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency[secondKey]?.pendingBalanceDepositsCount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency[secondKey]?.fundsAvailableInDays).isEqualTo(30)
        assertThat(result.overview.infoPerCurrency[secondKey]?.fundsDepositInterval).isEqualTo(
            PaymentsHubDepositSummaryState.Info.Interval.Monthly(15)
        )

        assertThat(result.overview.infoPerCurrency[thirdKey]?.availableFundsFormatted).isEqualTo("250R")
        assertThat(result.overview.infoPerCurrency[thirdKey]?.availableFundsAmount).isEqualTo(250)
        assertThat(result.overview.infoPerCurrency[thirdKey]?.pendingFundsFormatted).isEqualTo("0R")
        assertThat(result.overview.infoPerCurrency[thirdKey]?.pendingFundsAmount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency[thirdKey]?.pendingBalanceDepositsCount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency[thirdKey]?.fundsAvailableInDays).isEqualTo(30)
        assertThat(result.overview.infoPerCurrency[thirdKey]?.fundsDepositInterval).isEqualTo(
            PaymentsHubDepositSummaryState.Info.Interval.Monthly(15)
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
                    interval = "daily",
                    monthlyAnchor = null,
                    weeklyAnchor = null
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
        val date = "23 Oct 2024"
        whenever(dateFormatter(Date(currentDate))).thenReturn(date)

        // WHEN
        val result = mapper.mapDepositOverviewToViewModelOverviews(overview)

        // THEN
        result as PaymentsHubDepositSummaryStateMapper.Result.Success
        assertThat(result.overview.defaultCurrency).isEqualTo("USD")
        assertThat(result.overview.infoPerCurrency["USD"]?.nextDeposit?.amount).isEqualTo("500$")
        assertThat(result.overview.infoPerCurrency["USD"]?.nextDeposit?.status).isEqualTo(
            PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED
        )
        assertThat(result.overview.infoPerCurrency["USD"]?.nextDeposit?.date).isEqualTo(date)
        assertThat(result.overview.infoPerCurrency["USD"]?.availableFundsFormatted).isEqualTo("0$")
        assertThat(result.overview.infoPerCurrency["USD"]?.availableFundsAmount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency["USD"]?.pendingFundsFormatted).isEqualTo("0$")
        assertThat(result.overview.infoPerCurrency["USD"]?.pendingFundsAmount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency["USD"]?.pendingBalanceDepositsCount).isEqualTo(0)
        assertThat(result.overview.infoPerCurrency["USD"]?.fundsAvailableInDays).isEqualTo(5)
        assertThat(result.overview.infoPerCurrency["USD"]?.fundsDepositInterval).isEqualTo(
            PaymentsHubDepositSummaryState.Info.Interval.Daily
        )
    }
}
