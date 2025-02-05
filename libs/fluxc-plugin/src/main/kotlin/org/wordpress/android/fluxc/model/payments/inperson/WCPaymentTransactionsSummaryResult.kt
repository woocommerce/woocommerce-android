package org.wordpress.android.fluxc.model.payments.inperson

import com.google.gson.annotations.SerializedName

data class WCPaymentTransactionsSummaryResult(
    @SerializedName("count") val transactionsCount: Int,
    val currency: String,
    val total: Int,
    val fees: Int,
    val net: Int,
    @SerializedName("store_currencies") val storeCurrencies: List<String>?,
    @SerializedName("customer_currencies") val customerCurrencies: List<String>?,
)
