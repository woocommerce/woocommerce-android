package org.wordpress.android.fluxc.model.payments.woo

data class WooPaymentsDepositsOverview(
    val account: Account?,
    val balance: Balance?,
    val deposit: Deposit?
) {
    data class Account(
        val defaultCurrency: String?,
        val depositsBlocked: Boolean?,
        val depositsEnabled: Boolean?,
        val depositsSchedule: DepositsSchedule?
    ) {
        data class DepositsSchedule(
            val delayDays: Int?,
            val weeklyAnchor: String?,
            val monthlyAnchor: Int?,
            val interval: String?
        )
    }

    data class Balance(
        val available: List<Info>?,
        val instant: List<Info>?,
        val pending: List<Info>?
    ) {
        data class Info(
            val amount: Long?,
            val currency: String?,
            val fee: Long?,
            val feePercentage: Double?,
            val net: Long?,
            val sourceTypes: SourceTypes?,
        )

        data class SourceTypes(
            val card: Int?
        )
    }

    data class Deposit(
        val lastManualDeposits: List<ManualDeposit>?,
        val lastPaid: List<Info>?,
    ) {
        data class Info(
            val amount: Long?,
            val automatic: Boolean?,
            val bankAccount: String?,
            val created: Long?,
            val currency: String?,
            val date: Long?,
            val fee: Long?,
            val feePercentage: Double?,
            val depositId: String?,
            val status: String?,
            val type: String?
        )

        data class ManualDeposit(
            val currency: String?,
            val date: Long?
        )
    }
}
