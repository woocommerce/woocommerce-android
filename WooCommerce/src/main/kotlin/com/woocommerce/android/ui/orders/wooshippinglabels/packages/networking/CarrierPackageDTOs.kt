package com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking

import com.google.gson.annotations.SerializedName

class CarrierPredefinedPackagesDTO {
    val usps: USPSPackageDTO? = null

    @SerializedName("dhlexpress")
    val dhlExpress: DHLPackageDTO? = null
}

class USPSPackageDTO {
    @SerializedName("pri_flat_boxes")
    val flatBoxes: CarrierPackageGroupDTO? = null

    @SerializedName("pri_boxes")
    val boxes: CarrierPackageGroupDTO? = null

    @SerializedName("pri_express_boxes")
    val expressBoxes: CarrierPackageGroupDTO? = null

    @SerializedName("pri_envelopes")
    val envelopes: CarrierPackageGroupDTO? = null

    @SerializedName("pri_express_envelopes")
    val expressEnvelopes: CarrierPackageGroupDTO? = null
}

class DHLPackageDTO {
    @SerializedName("domestic_and_international")
    val domesticAndInternationalPackages: CarrierPackageGroupDTO? = null
}

class CarrierPackageGroupDTO {
    val title: String? = null
    val definitions: List<PredefinedPackageDTO>? = null
}

class PredefinedPackageDTO {
    val id: String? = null
    val name: String? = null

    @SerializedName("inner_dimensions")
    val innerDimensions: String? = null

    @SerializedName("outer_dimensions")
    val outerDimensions: String? = null

    @SerializedName("box_weight")
    val boxWeight: Double? = null

    @SerializedName("is_flat_rate")
    val isFlatRate: Boolean? = null

    @SerializedName("max_weight")
    val maxWeight: Double? = null

    @SerializedName("is_letter")
    val isLetter: Boolean? = null

    @SerializedName("group_id")
    val groupId: String? = null

    @SerializedName("can_ship_international")
    val canShipInternational: Boolean? = null
}
