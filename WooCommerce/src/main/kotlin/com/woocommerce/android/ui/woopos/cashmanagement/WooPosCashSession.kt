package com.woocommerce.android.ui.woopos.cashmanagement

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** Money returned by Core is decimal text in the session currency. */
data class WooPosCashSession(
    val id: Long,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("drawer_id") val drawerId: String?,
    val status: String,
    val revision: Int,
    val currency: String,
    @SerializedName("currency_precision") val currencyPrecision: Int,
    @SerializedName("opening_amount") val openingAmount: String,
    @SerializedName("cash_sales_total") val cashSalesTotal: String,
    @SerializedName("cash_refunds_total") val cashRefundsTotal: String,
    @SerializedName("paid_in_total") val paidInTotal: String,
    @SerializedName("paid_out_total") val paidOutTotal: String,
    @SerializedName("expected_amount") val expectedAmount: String,
    @SerializedName("counted_amount") val countedAmount: String?,
    val variance: String?,
    val note: String?,
    @SerializedName("date_created_gmt") val dateCreatedGmt: String,
    @SerializedName("date_closed_gmt") val dateClosedGmt: String?,
    @SerializedName("opened_by_name") val openedByName: String,
    @SerializedName("closed_by_name") val closedByName: String?,
) {
    val expectedCash: BigDecimal get() = expectedAmount.toBigDecimal()
}

data class WooPosCashMovement(
    val id: Long,
    val type: String,
    val amount: String,
    val reason: String?,
    @SerializedName("order_id") val orderId: Long?,
    @SerializedName("refund_id") val refundId: Long?,
    @SerializedName("occurred_at") val occurredAt: String,
    @SerializedName("created_by_name") val createdByName: String,
)

data class WooPosCashPage<T>(val items: List<T>, val hasMore: Boolean)
