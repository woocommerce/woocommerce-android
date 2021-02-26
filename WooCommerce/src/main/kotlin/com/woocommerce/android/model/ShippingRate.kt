package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class ShippingRate(
    val id: String,
    val title: String,
    val deliveryEstimate: Int,
    val deliveryDate: Date?,
    val price: String,
    val carrier: ShippingCarrier,
    val isTrackingAvailable: Boolean,
    val isFreePickupAvailable: Boolean,
    val isInsuranceAvailable: Boolean,
    val insuranceCoverage: String?,
    val isSignatureRequired: Boolean,
    val isSignatureAvailable: Boolean,
    val signaturePrice: String?,
    val isAdultSignatureAvailable: Boolean,
    val adultSignaturePrice: String?,
    val extraOptionSelected: ExtraOption,
    val isSelected: Boolean
) : Parcelable {
    enum class ShippingCarrier(val title: String) {
        FEDEX("Fedex"),
        USPS("USPS"),
        UPS("UPS")
    }
    enum class ExtraOption {
        SIGNATURE,
        ADULT_SIGNATURE,
        NONE
    }
}
