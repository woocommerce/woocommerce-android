package org.wordpress.android.fluxc.persistence.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverviewComposedEntities
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsAccountDepositSummary
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsBalance
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsCurrencyBalances
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsCurrencyDeposits
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDeposit
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsOverviewApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsSchedule
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsManualDeposit
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsSourceTypes
import org.wordpress.android.fluxc.persistence.entity.BalanceType
import org.wordpress.android.fluxc.persistence.entity.DepositType
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsAccountDepositSummaryEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsBalanceEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity
import org.wordpress.android.fluxc.persistence.entity.SourceTypes as WooPaymentsSourceTypesEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsSchedule as WooPaymentsDepositsScheduleEntity

class WooPaymentsDepositsOverviewMapperTest {
    private val mapper = WooPaymentsDepositsOverviewMapper()

    @Suppress("LongMethod")
    @Test
    fun `when mapApiResponseToModel, then api response mapped to model`() {
        // GIVEN
        val apiResponse = WooPaymentsDepositsOverviewApiResponse(
            deposit = WooPaymentsCurrencyDeposits(
                lastPaid = listOf(
                    WooPaymentsDeposit(
                        id = "id",
                        date = 1L,
                        type = "type",
                        amount = 1L,
                        status = "status",
                        bankAccount = "bankAccount",
                        currency = "currency",
                        automatic = true,
                        fee = 1L,
                        feePercentage = 1.0,
                        created = 1L
                    )
                ),
                lastManualDeposits = listOf(
                    WooPaymentsManualDeposit(
                        currency = "currency",
                        date = 1L
                    )
                )
            ),
            balance = WooPaymentsCurrencyBalances(
                pending = listOf(
                    WooPaymentsBalance(
                        amount = 1L,
                        currency = "rub",
                        sourceTypes = WooPaymentsSourceTypes(
                            card = 1
                        ),
                        feePercentage = 1.0,
                        net = 1L,
                        fee = 1L
                    )
                ),
                available = listOf(
                    WooPaymentsBalance(
                        amount = 1L,
                        currency = "usd",
                        sourceTypes = WooPaymentsSourceTypes(
                            card = 1
                        ),
                        feePercentage = 1.0,
                        net = 1L,
                        fee = 1L
                    )
                ),
                instant = listOf(
                    WooPaymentsBalance(
                        amount = 1L,
                        currency = "eur",
                        sourceTypes = WooPaymentsSourceTypes(
                            card = 1
                        ),
                        feePercentage = 1.0,
                        net = 1L,
                        fee = 1L
                    )
                )
            ),
            account = WooPaymentsAccountDepositSummary(
                depositsEnabled = true,
                depositsBlocked = true,
                depositsSchedule = WooPaymentsDepositsSchedule(
                    interval = "interval",
                    weeklyAnchor = "monday",
                    monthlyAnchor = 10,
                    delayDays = 1
                ),
                defaultCurrency = "defaultCurrency"
            )
        )

        // WHEN
        val result = mapper.mapApiResponseToModel(apiResponse)

        // THEN
        assertThat(result.deposit?.lastManualDeposits?.get(0)?.currency).isEqualTo("currency")
        assertThat(result.deposit?.lastManualDeposits?.get(0)?.date).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.automatic).isEqualTo(true)
        assertThat(result.deposit?.lastPaid?.get(0)?.bankAccount).isEqualTo("bankAccount")
        assertThat(result.deposit?.lastPaid?.get(0)?.created).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.currency).isEqualTo("currency")
        assertThat(result.deposit?.lastPaid?.get(0)?.date).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.depositId).isEqualTo("id")
        assertThat(result.deposit?.lastPaid?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.deposit?.lastPaid?.get(0)?.status).isEqualTo("status")
        assertThat(result.deposit?.lastPaid?.get(0)?.type).isEqualTo("type")
        assertThat(result.balance?.available?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.balance?.available?.get(0)?.currency).isEqualTo("usd")
        assertThat(result.balance?.available?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.balance?.available?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.balance?.available?.get(0)?.net).isEqualTo(1L)
        assertThat(result.balance?.available?.get(0)?.sourceTypes?.card).isEqualTo(1)
        assertThat(result.balance?.instant?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.balance?.instant?.get(0)?.currency).isEqualTo("eur")
        assertThat(result.balance?.instant?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.balance?.instant?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.balance?.instant?.get(0)?.net).isEqualTo(1L)
        assertThat(result.balance?.instant?.get(0)?.sourceTypes?.card).isEqualTo(1)
        assertThat(result.balance?.pending?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.balance?.pending?.get(0)?.currency).isEqualTo("rub")
        assertThat(result.balance?.pending?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.balance?.pending?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.balance?.pending?.get(0)?.net).isEqualTo(1L)
        assertThat(result.balance?.pending?.get(0)?.sourceTypes?.card).isEqualTo(1)
        assertThat(result.account?.defaultCurrency).isEqualTo("defaultCurrency")
        assertThat(result.account?.depositsBlocked).isEqualTo(true)
        assertThat(result.account?.depositsEnabled).isEqualTo(true)
        assertThat(result.account?.depositsSchedule?.delayDays).isEqualTo(1)
        assertThat(result.account?.depositsSchedule?.monthlyAnchor).isEqualTo(10)
        assertThat(result.account?.depositsSchedule?.weeklyAnchor).isEqualTo("monday")
        assertThat(result.account?.depositsSchedule?.interval).isEqualTo("interval")
    }

    @Suppress("LongMethod")
    @Test
    fun `when mapEntityToModel, then entity mapped to model`() {
        // GIVEN
        val entity = WooPaymentsDepositsOverviewComposedEntities(
            overview = WooPaymentsDepositsOverviewEntity(
                localSiteId = LocalId(1),
                account = WooPaymentsAccountDepositSummaryEntity(
                    depositsEnabled = true,
                    depositsBlocked = true,
                    depositsSchedule = WooPaymentsDepositsScheduleEntity(
                        delayDays = 1,
                        interval = "interval",
                        monthlyAnchor = null,
                        weeklyAnchor = null,
                    ),
                    defaultCurrency = "defaultCurrency"
                )
            ),
            lastPaidDeposits = listOf(
                WooPaymentsDepositEntity(
                    localSiteId = LocalId(1),
                    depositId = "id",
                    date = 1L,
                    type = "type",
                    amount = 1L,
                    status = "status1",
                    bankAccount = "bankAccount1",
                    currency = "usd",
                    automatic = true,
                    fee = 1L,
                    feePercentage = 1.0,
                    created = 1L,
                    depositType = DepositType.LAST_PAID
                )
            ),
            lastManualDeposits = listOf(
                WooPaymentsManualDepositEntity(
                    localSiteId = LocalId(1),
                    date = 1L,
                    currency = "eur",
                )
            ),
            pendingBalances = listOf(
                WooPaymentsBalanceEntity(
                    localSiteId = LocalId(1),
                    amount = 1L,
                    currency = "rub",
                    sourceTypes = WooPaymentsSourceTypesEntity(
                        card = 1
                    ),
                    feePercentage = 1.0,
                    net = 1L,
                    fee = 1L,
                    balanceType = BalanceType.AVAILABLE,
                )
            ),
            availableBalances = listOf(
                WooPaymentsBalanceEntity(
                    localSiteId = LocalId(1),
                    amount = 1L,
                    currency = "usd",
                    sourceTypes = WooPaymentsSourceTypesEntity(
                        card = 1
                    ),
                    feePercentage = 1.0,
                    net = 1L,
                    fee = 1L,
                    balanceType = BalanceType.AVAILABLE,
                )
            ),
            instantBalances = listOf(
                WooPaymentsBalanceEntity(
                    localSiteId = LocalId(1),
                    amount = 1L,
                    currency = "eur",
                    sourceTypes = WooPaymentsSourceTypesEntity(
                        card = 1
                    ),
                    feePercentage = 1.0,
                    net = 1L,
                    fee = 1L,
                    balanceType = BalanceType.AVAILABLE,
                )
            ),
        )

        // WHEN
        val result = mapper.mapEntityToModel(entity)!!

        // THEN
        assertThat(result.deposit?.lastManualDeposits?.get(0)?.currency).isEqualTo("eur")
        assertThat(result.deposit?.lastManualDeposits?.get(0)?.date).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.automatic).isEqualTo(true)
        assertThat(result.deposit?.lastPaid?.get(0)?.bankAccount).isEqualTo("bankAccount1")
        assertThat(result.deposit?.lastPaid?.get(0)?.created).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.currency).isEqualTo("usd")
        assertThat(result.deposit?.lastPaid?.get(0)?.date).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.depositId).isEqualTo("id")
        assertThat(result.deposit?.lastPaid?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.deposit?.lastPaid?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.deposit?.lastPaid?.get(0)?.status).isEqualTo("status1")
        assertThat(result.deposit?.lastPaid?.get(0)?.type).isEqualTo("type")
        assertThat(result.balance?.available?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.balance?.available?.get(0)?.currency).isEqualTo("usd")
        assertThat(result.balance?.available?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.balance?.available?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.balance?.available?.get(0)?.net).isEqualTo(1L)
        assertThat(result.balance?.available?.get(0)?.sourceTypes?.card).isEqualTo(1)
        assertThat(result.balance?.instant?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.balance?.instant?.get(0)?.currency).isEqualTo("eur")
        assertThat(result.balance?.instant?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.balance?.instant?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.balance?.instant?.get(0)?.net).isEqualTo(1L)
        assertThat(result.balance?.instant?.get(0)?.sourceTypes?.card).isEqualTo(1)
        assertThat(result.balance?.pending?.get(0)?.amount).isEqualTo(1L)
        assertThat(result.balance?.pending?.get(0)?.currency).isEqualTo("rub")
        assertThat(result.balance?.pending?.get(0)?.fee).isEqualTo(1L)
        assertThat(result.balance?.pending?.get(0)?.feePercentage).isEqualTo(1.0)
        assertThat(result.balance?.pending?.get(0)?.net).isEqualTo(1L)
        assertThat(result.balance?.pending?.get(0)?.sourceTypes?.card).isEqualTo(1)
        assertThat(result.account?.defaultCurrency).isEqualTo("defaultCurrency")
        assertThat(result.account?.depositsBlocked).isEqualTo(true)
        assertThat(result.account?.depositsEnabled).isEqualTo(true)
        assertThat(result.account?.depositsSchedule?.delayDays).isEqualTo(1)
        assertThat(result.account?.depositsSchedule?.monthlyAnchor).isNull()
        assertThat(result.account?.depositsSchedule?.weeklyAnchor).isNull()
        assertThat(result.account?.depositsSchedule?.interval).isEqualTo("interval")
    }
}
