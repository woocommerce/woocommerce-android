package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class ShippingLabelModel(
    val labelId: Long,
    val tracking: String,
    val refundableAmount: BigDecimal,
    val status: ShippingLabelStatus,
    val created: Date?,
    val carrierId: String,
    val serviceName: String,
    val commercialInvoiceUrl: String,
    val isCommercialInvoiceSubmittedElectronically: Boolean,
    val packageName: String,
    val isLetter: Boolean,
    val productNames: List<String>,
    val productIds: List<Long>,
    val shipmentId: String?,
    val receiptItemId: Long,
    val createdDate: Date?,
    val mainReceiptId: Long,
    val rate: BigDecimal,
    val currency: String,
    val expiryDate: Long,
) : Parcelable {
    companion object {
        private const val REFUND_EXPIRY_DAYS = 30L
    }
}

enum class ShippingLabelStatus {
    @SerializedName("UNKNOWN")
    UNKNOWN,

    @SerializedName("PURCHASE_IN_PROGRESS")
    PURCHASE_IN_PROGRESS,

    @SerializedName("PURCHASED")
    PURCHASED,

    @SerializedName("PURCHASE_ERROR")
    PURCHASE_ERROR,

    @SerializedName("ANONYMIZED")
    ANONYMIZED
}
