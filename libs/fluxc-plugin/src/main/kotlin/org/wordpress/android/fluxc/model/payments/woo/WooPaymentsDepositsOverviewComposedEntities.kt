package org.wordpress.android.fluxc.model.payments.woo

import org.wordpress.android.fluxc.persistence.entity.WooPaymentsBalanceEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity

data class WooPaymentsDepositsOverviewComposedEntities(
    val overview: WooPaymentsDepositsOverviewEntity,

    val lastPaidDeposits: List<WooPaymentsDepositEntity>?,
    val lastManualDeposits: List<WooPaymentsManualDepositEntity>?,

    val pendingBalances: List<WooPaymentsBalanceEntity>?,
    val availableBalances: List<WooPaymentsBalanceEntity>?,
    val instantBalances: List<WooPaymentsBalanceEntity>?,
)
