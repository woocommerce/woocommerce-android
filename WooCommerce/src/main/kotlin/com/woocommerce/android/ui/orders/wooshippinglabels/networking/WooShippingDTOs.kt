package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class PackagePurchaseDTO(
    val id: String = "0",
    @SerializedName("box_id") val boxId: String = "custom_box",
    val length: Float,
    val width: Float,
    val height: Float,
    @SerializedName("is_letter") val isLetter: Boolean,
    val weight: Float,
    @SerializedName("shipment_id") val shipmentId: String,
    val products: List<Long>,
    @SerializedName("rate_id") val rateId: String,
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("carrier_id") val carrierId: String,
    @SerializedName("service_name") val serviceName: String
)

data class OriginAddressPurchaseDTO(
    val id: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("address_2") val address2: String? = null,
    val city: String? = null,
    val company: String? = null,
    val country: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val postcode: String? = null,
    val state: String? = null,
    val email: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
)

data class RateDTO(
    val rateId: String,
    val serviceId: String,
    val carrierId: String,
    val title: String,
    val rate: BigDecimal,
    val retailRate: BigDecimal,
    val listRate: BigDecimal,
    val isSelected: Boolean,
    val tracking: Boolean,
    val insurance: BigDecimal?,
    val freePickup: Boolean,
    val shipmentId: String,
    val deliveryDays: Int,
    val deliveryDateGuaranteed: Boolean,
    val deliveryDate: String?
)

data class HazmatDTO(
    val isHazmat: Boolean = false,
    val category: String = "",
)
