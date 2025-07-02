package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CustomsData(
    val contentType: ContentType,
    val contentDescription: String,
    val restrictionType: RestrictionType,
    val restrictionDescription: String,
    val isReturnToSender: Boolean,
    val itn: String,
    val items: List<CustomsItem>
) : Parcelable

@Parcelize
data class CustomsItem(
    val productID: Long,
    val description: String,
    val quantity: Float,
    val value: BigDecimal,
    val weight: Float,
    val hsTariffNumber: String,
    val originCountry: String,
    val originCountryCode: String = ""
) : Parcelable

enum class ContentType(val resourceId: Int) {
    MERCHANDISE(R.string.woo_shipping_labels_customs_content_merchandise),
    GIFT(R.string.woo_shipping_labels_customs_content_gift),
    RETURNED_GOODS(R.string.woo_shipping_labels_customs_content_returned_goods),
    SAMPLE(R.string.woo_shipping_labels_customs_content_sample),
    DOCUMENTS(R.string.woo_shipping_labels_customs_content_documents),
    OTHER(R.string.woo_shipping_labels_customs_content_other);

    companion object {
        fun fromString(value: String?): ContentType {
            val type = entries.find { it.name.equals(value, ignoreCase = true) }
            if (type == null) {
                WooLog.w(T.SHIPPING_LABELS, "Unexpected ContentType value received: $value")
            }
            return type ?: OTHER
        }
    }
}

enum class RestrictionType(val resourceId: Int) {
    NONE(R.string.woo_shipping_labels_customs_restriction_none),
    QUARANTINE(R.string.woo_shipping_labels_customs_restriction_quarantine),
    SANITARY_INSPECTION(R.string.woo_shipping_labels_customs_restriction_sanitary),
    OTHER(R.string.woo_shipping_labels_customs_restriction_other);

    companion object {
        fun fromString(value: String?): RestrictionType {
            val type = entries.find { it.name.equals(value, ignoreCase = true) }
            if (type == null) {
                WooLog.w(T.SHIPPING_LABELS, "Unexpected RestrictionType value received: $value")
            }
            return type ?: NONE
        }
    }
}
