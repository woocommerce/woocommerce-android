package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
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
    val originAddress: Address? = null,
    val destinationAddress: Address? = null
) : Parcelable {
    @IgnoredOnParcel
    val trackingLink: String
        get() = ShipmentTrackingUrls.fromCarrier(carrierId, tracking) ?: ""

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

/**
 * This fill ShippingLabelModel with products
 *
 * The [ShippingLabelModel.productIds] field is only available from the API after WCS 1.24.1 or in the new Woo Shipping
 * plugin.
 * Adding a check in this method to fetch products by productIds only if it is available.
 * Otherwise default to using [ShippingLabel.productNames]
 */
fun List<ShippingLabelModel>.fillProducts(products: List<Order.Item>): List<ShippingLabelModel> = map { shippingLabel ->
    val products = if (shippingLabel.productIds.isNullOrEmpty()) {
        // This case can be removed when the old shipping label flow is removed from the codebase since productIds
        // will always be available in the new flow.
        products.filter { it.name in shippingLabel.productNames }
    } else {
        products.filter { it.uniqueId in shippingLabel.productIds }
    }
    shippingLabel.copy(products = products)
}
