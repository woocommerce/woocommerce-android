package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesDatasourceMapper.Companion.CARRIER_DHL_EXPRESS_KEY
import kotlinx.parcelize.IgnoredOnParcel
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
    val commercialInvoiceUrl: String?,
    val isCommercialInvoiceSubmittedElectronically: Boolean,
    val packageName: String,
    val isLetter: Boolean,
    val productNames: List<String>,
    val productIds: List<Long>?,
    val shipmentId: String?,
    val receiptItemId: Long,
    val createdDate: Date?,
    val mainReceiptId: Long,
    val rate: BigDecimal,
    val currency: String,
    val expiryDate: Long,
    val usedDate: Long?,
    val refund: Refund?,
    val products: List<Order.Item> = emptyList(),
    val hazmatCategory: ShippingLabelHazmatCategory? = null,
    val originAddress: Address? = null,
    val destinationAddress: Address? = null,
    val error: String? = null,
) : Parcelable {
    @IgnoredOnParcel
    val trackingLink: String
        get() = ShipmentTrackingUrls.fromCarrier(carrierId, tracking) ?: ""

    @IgnoredOnParcel
    val refundDuration: Int
        get() = if (carrierId == CARRIER_DHL_EXPRESS_KEY) {
            REFUND_DURATION_DHL_EXPRESS
        } else {
            REFUND_DURATION_DEFAULT
        }

    @IgnoredOnParcel
    val isRefundAvailable: Boolean
        get() {
            val createdDate = createdDate?.time ?: return true
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(REFUND_EXPIRY_DAYS)

            return createdDate >= thirtyDaysAgo &&
                !hasLabelExpired &&
                carrierId != CARRIER_DHL_EXPRESS_KEY &&
                tracking.isNotEmpty()
        }

    @IgnoredOnParcel
    val isPurchased: Boolean
        get() = status == ShippingLabelStatus.PURCHASED && refund == null

    @IgnoredOnParcel
    private val hasLabelExpired: Boolean
        get() = status == ShippingLabelStatus.ANONYMIZED || usedDate != null || expiryDate < System.currentTimeMillis()

    @Parcelize
    data class Refund(val status: String?, val requestDate: Date?) : Parcelable

    companion object {
        private const val REFUND_EXPIRY_DAYS = 30L
        private const val REFUND_DURATION_DHL_EXPRESS = 31
        const val REFUND_DURATION_DEFAULT = 14
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
