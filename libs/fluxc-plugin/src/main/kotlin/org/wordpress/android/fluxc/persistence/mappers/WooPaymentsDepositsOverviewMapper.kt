package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Account
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Account.DepositsSchedule
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Balance
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Balance.SourceTypes
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Deposit
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Deposit.Info
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview.Deposit.ManualDeposit
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverviewComposedEntities
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsBalance
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDeposit
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsOverviewApiResponse
import org.wordpress.android.fluxc.persistence.entity.BalanceType
import org.wordpress.android.fluxc.persistence.entity.DepositType
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsAccountDepositSummaryEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsBalanceEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsSchedule
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity
import javax.inject.Inject

class WooPaymentsDepositsOverviewMapper @Inject constructor() {
    fun mapApiResponseToModel(apiResponse: WooPaymentsDepositsOverviewApiResponse) =
        WooPaymentsDepositsOverview(
            account = apiResponse.account?.let { summary ->
                Account(
                    defaultCurrency = summary.defaultCurrency,
                    depositsBlocked = summary.depositsBlocked,
                    depositsEnabled = summary.depositsEnabled,
                    depositsSchedule = summary.depositsSchedule?.let {
                        DepositsSchedule(
                            delayDays = it.delayDays,
                            weeklyAnchor = it.weeklyAnchor,
                            monthlyAnchor = it.monthlyAnchor,
                            interval = it.interval
                        )
                    }
                )
            },
            balance = apiResponse.balance?.let {
                Balance(
                    available = it.available?.map { available -> mapApiResponseBalanceToModel(available) },
                    instant = it.instant?.map { instant -> mapApiResponseBalanceToModel(instant) },
                    pending = it.pending?.map { pending -> mapApiResponseBalanceToModel(pending) }
                )
            },
            deposit = apiResponse.deposit?.let {
                Deposit(
                    lastManualDeposits = it.lastManualDeposits?.map { manualDeposit ->
                        ManualDeposit(
                            currency = manualDeposit.currency,
                            date = manualDeposit.date
                        )
                    },
                    lastPaid = it.lastPaid?.map {
                        info -> mapApiDepositToModel(info)
                    },
                )
            }
        )

    fun mapEntityToModel(entity: WooPaymentsDepositsOverviewComposedEntities?) =
        entity?.let { entity ->
            WooPaymentsDepositsOverview(
                account = mapEntityAccountToModel(entity.overview.account),
                balance = Balance(
                    available = entity.availableBalances?.map { mapBonusToEntity(it) },
                    instant = entity.instantBalances?.map { mapBonusToEntity(it) },
                    pending = entity.pendingBalances?.map { mapBonusToEntity(it) },
                ),
                deposit = Deposit(
                    lastManualDeposits = entity.lastManualDeposits?.map {
                        ManualDeposit(
                            currency = it.currency,
                            date = it.date
                        )
                    },
                    lastPaid = entity.lastPaidDeposits?.map {
                        Info(
                            amount = it.amount,
                            automatic = it.automatic,
                            bankAccount = it.bankAccount,
                            created = it.created,
                            currency = it.currency,
                            date = it.date,
                            fee = it.fee,
                            feePercentage = it.feePercentage,
                            status = it.status,
                            type = it.type,
                            depositId = it.depositId
                        )
                    },
                )
            )
        }

    private fun mapBonusToEntity(it: WooPaymentsBalanceEntity) =
        Balance.Info(
            amount = it.amount,
            currency = it.currency,
            sourceTypes = it.sourceTypes?.let { sourceTypes ->
                SourceTypes(
                    card = sourceTypes.card
                )
            },
            fee = it.fee,
            feePercentage = it.feePercentage,
            net = it.net,
        )

    fun mapModelDepositToEntity(
        deposit: Info,
        site: SiteModel,
        depositType: DepositType
    ): WooPaymentsDepositEntity =
        WooPaymentsDepositEntity(
            localSiteId = site.localId(),
            date = deposit.date,
            type = deposit.type,
            amount = deposit.amount,
            status = deposit.status,
            bankAccount = deposit.bankAccount,
            currency = deposit.currency,
            automatic = deposit.automatic,
            fee = deposit.fee,
            feePercentage = deposit.feePercentage,
            created = deposit.created,
            depositId = deposit.depositId,
            depositType = depositType,
        )

    fun mapModelManualDepositToEntity(
        manualDeposit: ManualDeposit,
        site: SiteModel
    ): WooPaymentsManualDepositEntity =
        WooPaymentsManualDepositEntity(
            localSiteId = site.localId(),
            currency = manualDeposit.currency,
            date = manualDeposit.date
        )

    fun mapModelBalanceToEntity(
        balance: Balance.Info,
        site: SiteModel,
        balanceType: BalanceType,
    ): WooPaymentsBalanceEntity =
        WooPaymentsBalanceEntity(
            localSiteId = site.localId(),
            amount = balance.amount,
            currency = balance.currency,
            sourceTypes = balance.sourceTypes?.let { sourceTypes ->
                org.wordpress.android.fluxc.persistence.entity.SourceTypes(
                    card = sourceTypes.card
                )
            },
            fee = balance.fee,
            feePercentage = balance.feePercentage,
            net = balance.net,
            balanceType = balanceType,
        )

    fun mapModelToEntity(
        model: WooPaymentsDepositsOverview,
        site: SiteModel
    ) = WooPaymentsDepositsOverviewEntity(
        localSiteId = site.localId(),
        account = mapModelAccountToEntity(model.account),
    )

    private fun mapApiResponseBalanceToModel(available: WooPaymentsBalance) =
        Balance.Info(
            amount = available.amount,
            currency = available.currency,
            sourceTypes = available.sourceTypes?.let { sourceTypes ->
                SourceTypes(
                    card = sourceTypes.card
                )
            },
            fee = available.fee,
            feePercentage = available.feePercentage,
            net = available.net,
        )

    private fun mapEntityAccountToModel(account: WooPaymentsAccountDepositSummaryEntity?) =
        account?.let {
            Account(
                defaultCurrency = it.defaultCurrency,
                depositsBlocked = it.depositsBlocked,
                depositsEnabled = it.depositsEnabled,
                depositsSchedule = mapEntityDepositsScheduleToModel(it.depositsSchedule)
            )
        }

    private fun mapEntityDepositsScheduleToModel(depositsSchedule: WooPaymentsDepositsSchedule?) =
        depositsSchedule?.let {
            DepositsSchedule(
                delayDays = it.delayDays,
                weeklyAnchor = it.weeklyAnchor,
                monthlyAnchor = it.monthlyAnchor,
                interval = it.interval
            )
        }

    private fun mapModelAccountToEntity(account: Account?) =
        account?.let {
            WooPaymentsAccountDepositSummaryEntity(
                depositsEnabled = it.depositsEnabled,
                depositsBlocked = it.depositsBlocked,
                depositsSchedule = mapModelDepositsScheduleToEntity(it.depositsSchedule),
                defaultCurrency = it.defaultCurrency
            )
        }

    private fun mapModelDepositsScheduleToEntity(depositsSchedule: DepositsSchedule?) =
        depositsSchedule?.let {
            WooPaymentsDepositsSchedule(
                delayDays = it.delayDays,
                weeklyAnchor = it.weeklyAnchor,
                monthlyAnchor = it.monthlyAnchor,
                interval = it.interval
            )
        }

    private fun mapApiDepositToModel(info: WooPaymentsDeposit) =
        Info(
            amount = info.amount,
            automatic = info.automatic,
            bankAccount = info.bankAccount,
            created = info.created,
            currency = info.currency,
            date = info.date,
            fee = info.fee,
            feePercentage = info.feePercentage,
            status = info.status,
            type = info.type,
            depositId = info.id
        )
}
