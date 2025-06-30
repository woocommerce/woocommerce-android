package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo

import com.google.gson.annotations.SerializedName

data class WooPaymentsDepositsOverviewApiResponse(
    val deposit: WooPaymentsCurrencyDeposits?,
    val balance: WooPaymentsCurrencyBalances?,
    val account: WooPaymentsAccountDepositSummary?
)

data class WooPaymentsCurrencyDeposits(
    @SerializedName("last_paid") val lastPaid: List<WooPaymentsDeposit>?,
    @SerializedName("last_manual_deposits") val lastManualDeposits: List<WooPaymentsManualDeposit>?
)

data class WooPaymentsDeposit(
    val id: String?,
    val date: Long?,
    val type: String?,
    val amount: Long?,
    val status: String?,
    val bankAccount: String?,
    val currency: String,
    val automatic: Boolean?,
    val fee: Long?,
    @SerializedName("fee_percentage") val feePercentage: Double?,
    val created: Long?
)

data class WooPaymentsManualDeposit(
    val currency: String?,
    val date: Long?
)

data class WooPaymentsCurrencyBalances(
    val pending: List<WooPaymentsBalance>?,
    val available: List<WooPaymentsBalance>?,
    val instant: List<WooPaymentsBalance>?
)

data class WooPaymentsBalance(
    val amount: Long?,
    val currency: String?,
    @SerializedName("source_types") val sourceTypes: WooPaymentsSourceTypes?,
    @SerializedName("fee_percentage") val feePercentage: Double?,
    @SerializedName("net") val net: Long?,
    val fee: Long?,
)

data class WooPaymentsSourceTypes(
    val card: Int?
)

data class WooPaymentsAccountDepositSummary(
    @SerializedName("deposits_enabled") val depositsEnabled: Boolean?,
    @SerializedName("deposits_blocked") val depositsBlocked: Boolean?,
    @SerializedName("deposits_schedule") val depositsSchedule: WooPaymentsDepositsSchedule?,
    @SerializedName("default_currency") val defaultCurrency: String?
)

data class WooPaymentsDepositsSchedule(
    @SerializedName("delay_days") val delayDays: Int?,
    @SerializedName("weekly_anchor") val weeklyAnchor: String?,
    @SerializedName("monthly_anchor") val monthlyAnchor: Int?,
    val interval: String?
)
