package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.annotations.SerializedName

data class OrderReceiptResponse(
    @SerializedName("receipt_url") val receiptUrl: String,
    @SerializedName("expiration_date") val expirationDate: String,
)
