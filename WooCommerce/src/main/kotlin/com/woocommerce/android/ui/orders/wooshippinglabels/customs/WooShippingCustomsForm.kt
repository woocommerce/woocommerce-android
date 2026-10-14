package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import android.os.Parcelable
import com.woocommerce.android.R
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
    val items: List<CustomsItem>,
    val euProductIdentifiersRequirement: EuProductIdentifiersRequirement = EuProductIdentifiersRequirement.PENDING
) : Parcelable {
    val asksForEuProductIdentifiers: Boolean
        get() = euProductIdentifiersRequirement == EuProductIdentifiersRequirement.REQUIRED &&
            contentType != ContentType.DOCUMENTS
}

@Parcelize
data class CustomsItem(
    val productID: Long,
    val description: String,
    val quantity: Float,
    val value: BigDecimal,
    val weight: Float,
    val hsTariffNumber: String,
    val originCountry: String,
    val originCountryCode: String,
    val euProductIdentifiers: EuProductIdentifiers = EuProductIdentifiers()
) : Parcelable {
    val totalValue: BigDecimal
        get() = value * quantity.toBigDecimal()
}

@Parcelize
data class EuProductIdentifiers(
    val sku: String = "",
    val manufacturerPartNumber: String = "",
    val gtin: String = "",
    val hasNoBarcode: Boolean = false
) : Parcelable {
    companion object {
        const val NO_BARCODE_STANDARDIZED_PRODUCT_ID = "Y081"
    }
}

enum class EuProductIdentifiersRequirement {
    PENDING,
    REQUIRED,
    NOT_REQUIRED
}

enum class ContentType(val resourceId: Int) {
    MERCHANDISE(R.string.woo_shipping_labels_customs_content_merchandise),
    GIFT(R.string.woo_shipping_labels_customs_content_gift),
    RETURNED_GOODS(R.string.woo_shipping_labels_customs_content_returned_goods),
    SAMPLE(R.string.woo_shipping_labels_customs_content_sample),
    DOCUMENTS(R.string.woo_shipping_labels_customs_content_documents),
    OTHER(R.string.woo_shipping_labels_customs_content_other)
}

enum class RestrictionType(val resourceId: Int) {
    NONE(R.string.woo_shipping_labels_customs_restriction_none),
    QUARANTINE(R.string.woo_shipping_labels_customs_restriction_quarantine),
    SANITARY_INSPECTION(R.string.woo_shipping_labels_customs_restriction_sanitary),
    OTHER(R.string.woo_shipping_labels_customs_restriction_other)
}
