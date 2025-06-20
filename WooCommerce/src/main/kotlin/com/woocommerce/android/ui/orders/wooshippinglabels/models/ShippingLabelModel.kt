package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesDatasourceMapper.Companion.CARRIER_DHL_EXPRESS_KEY
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.TimeUnit

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
    val usedDate: Long?,
) : Parcelable {
    val refundDuration: Int
        get() = if (carrierId == CARRIER_DHL_EXPRESS_KEY) {
            REFUND_DURATION_DHL_EXPRESS
        } else {
            REFUND_DURATION_DEFAULT
        }

    val isRefundAvailable: Boolean
        get() {
            val createdDate = createdDate?.time ?: return true
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(REFUND_EXPIRY_DAYS)

            return createdDate >= thirtyDaysAgo &&
                !hasLabelExpired &&
                carrierId != CARRIER_DHL_EXPRESS_KEY &&
                tracking.isNotEmpty()
        }

    private val hasLabelExpired: Boolean
        get() = status == ShippingLabelStatus.ANONYMIZED || usedDate != null || expiryDate < System.currentTimeMillis()

    companion object {
        private const val REFUND_EXPIRY_DAYS = 30L
        private const val REFUND_DURATION_DHL_EXPRESS = 14
        const val REFUND_DURATION_DEFAULT = 31
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
