package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCContentType
import org.wordpress.android.fluxc.model.shippinglabels.WCCustomsItem
import org.wordpress.android.fluxc.model.shippinglabels.WCNonDeliveryOption
import org.wordpress.android.fluxc.model.shippinglabels.WCRestrictionType
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingPackageCustoms
import java.math.BigDecimal

@Parcelize
data class CustomsPackage(
    val id: String,
    val labelPackage: ShippingLabelPackage,
    val returnToSender: Boolean,
    val contentsType: ContentsType,
    val contentsDescription: String? = null,
    val restrictionType: RestrictionType,
    val restrictionDescription: String? = null,
    val itn: String,
    val lines: List<CustomsLine>
) : Parcelable {
    fun toDataModel(): WCShippingPackageCustoms {
        return WCShippingPackageCustoms(
            id = id,
            contentsType = contentsType.toDataModel(),
            contentsExplanation = contentsDescription,
            restrictionType = restrictionType.toDataModel(),
            restrictionComments = restrictionDescription,
            nonDeliveryOption = if (returnToSender) WCNonDeliveryOption.Return else WCNonDeliveryOption.Abandon,
            itn = itn,
            customsItems = lines.map { it.toDataModel() }
        )
    }
}

@Parcelize
data class CustomsLine(
    val productId: Long,
    val itemDescription: String,
    val hsTariffNumber: String,
    val quantity: Int,
    val weight: Float?,
    val value: BigDecimal?,
    val originCountry: Location
) : Parcelable {
    fun toDataModel(): WCCustomsItem {
        return WCCustomsItem(
            productId = productId,
            description = itemDescription,
            hsTariffNumber = hsTariffNumber,
            quantity = quantity,
            value = value!!,
            weight = weight!!,
            originCountry = originCountry.code
        )
    }
}

enum class ContentsType(@StringRes val title: Int) {
    Merchandise(R.string.shipping_label_customs_contents_type_merchandise),
    Documents(R.string.shipping_label_customs_contents_type_documents),
    Gift(R.string.shipping_label_customs_contents_type_gifts),
    Sample(R.string.shipping_label_customs_contents_type_sample),
    Other(R.string.shipping_label_customs_contents_type_other);

    fun toDataModel(): WCContentType {
        return when (this) {
            Merchandise -> WCContentType.Merchandise
            Documents -> WCContentType.Documents
            Gift -> WCContentType.Gift
            Sample -> WCContentType.Sample
            Other -> WCContentType.Other
        }
    }
}

enum class RestrictionType(@StringRes val title: Int) {
    None(R.string.shipping_label_customs_restriction_type_none),
    Quarantine(R.string.shipping_label_customs_restriction_type_quarantine),
    SanitaryInspection(R.string.shipping_label_customs_restriction_type_sanitary_inspection),
    Other(R.string.shipping_label_customs_restriction_type_other);

    fun toDataModel(): WCRestrictionType {
        return when (this) {
            None -> WCRestrictionType.None
            Quarantine -> WCRestrictionType.Quarantine
            SanitaryInspection -> WCRestrictionType.SanitaryInspection
            Other -> WCRestrictionType.Other
        }
    }
}
