package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CustomsPackage(
    val id: String,
    val box: ShippingPackage,
    val returnToSender: Boolean,
    val contentsType: ContentsType,
    val restrictionType: RestrictionType,
    val itn: String,
    val lines: List<CustomsLine>
) : Parcelable

@Parcelize
data class CustomsLine(
    val itemDescription: String,
    val hsTariffNumber: String,
    val weight: Float,
    val value: BigDecimal,
    val originCountry: String
) : Parcelable

enum class ContentsType(@StringRes val title: Int) {
    Merchandise(R.string.shipping_label_customs_contents_type_merchandise),
    Documents(R.string.shipping_label_customs_contents_type_documents),
    Gift(R.string.shipping_label_customs_contents_type_gifts),
    Sample(R.string.shipping_label_customs_contents_type_sample),
    Other(R.string.shipping_label_customs_contents_type_other);
}

enum class RestrictionType(@StringRes val title: Int) {
    None(R.string.shipping_label_customs_restriction_type_none),
    Quarantine(R.string.shipping_label_customs_restriction_type_quarantine),
    SanitaryInspection(R.string.shipping_label_customs_restriction_type_sanitary_inspection),
    Other(R.string.shipping_label_customs_restriction_type_other);
}
