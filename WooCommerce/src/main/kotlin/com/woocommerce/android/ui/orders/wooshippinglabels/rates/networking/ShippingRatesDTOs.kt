package com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking

import com.google.gson.annotations.SerializedName
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.CustomsItemDTO
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.HazmatCategory
import java.math.BigDecimal

sealed class PackageDTO {
    data class CommonPackageDTO(
        val id: String = "custom_box",
        @SerializedName("box_id") val boxId: String = "0",
        val length: Double,
        val width: Double,
        val height: Double,
        @SerializedName("is_letter") val isLetter: Boolean,
        val weight: Double,
        @SerializedName("hazmat") val hazmatCategory: HazmatCategory? = null
    ) : PackageDTO()

    data class PackageWithCustomsDTO(
        val id: String = "custom_box",
        @SerializedName("box_id") val boxId: String = "0",
        val length: Double,
        val width: Double,
        val height: Double,
        @SerializedName("is_letter") val isLetter: Boolean,
        val weight: Double,
        @SerializedName("contents_type") val contentsType: String,
        @SerializedName("contents_explanation") val contentExplanation: String,
        @SerializedName("restriction_type") val restrictionType: String,
        @SerializedName("restriction_comments") val restrictionComments: String,
        @SerializedName("non_delivery_option") val isReturnToSender: String,
        val itn: String,
        val items: List<CustomsItemDTO>,
        @SerializedName("hazmat") val hazmatCategory: HazmatCategory? = null,
    ) : PackageDTO()
}

data class DestinationAddressDTO(
    val company: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val phone: String? = null,
    val name: String? = null,
    val address: String? = null,
    val address2: String? = null,
    val email: String? = null
)

data class OriginAddressDTO(
    @SerializedName("address_1") val address: String? = null,
    @SerializedName("address_2") val address2: String? = null,
    val city: String? = null,
    val company: String? = null,
    val country: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val postcode: String? = null,
    val state: String? = null
)

data class ShippingRateDTO(
    @SerializedName("rate_id") val rateId: String,
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("carrier_id") val carrierId: String?,
    val title: String,
    val rate: BigDecimal,
    @SerializedName("retail_rate") val retailRate: BigDecimal? = null,
    @SerializedName("list_rate") val listRate: BigDecimal? = null,
    @SerializedName("is_selected") val isSelected: Boolean,
    val tracking: Boolean = false,
    val insurance: String?,
    @SerializedName("free_pickup") val freePickup: Boolean,
    @SerializedName("shipment_id") val shipmentId: String?,
    @SerializedName("delivery_days") val deliveryDays: Int,
    @SerializedName("delivery_date_guaranteed") val deliveryDateGuaranteed: Boolean,
    @SerializedName("delivery_date") val deliveryDate: String?
)

data class WooShippingRatesDTO(
    val rates: List<ShippingRateDTO>
)
