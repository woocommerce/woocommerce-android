package com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking

import com.google.gson.annotations.SerializedName

class CustomPackageCreationResponse {
    val custom: List<CustomPackageDTO>? = null
}

data class CustomPackageCreationRequestData(
    val name: String? = null,
    @SerializedName("is_letter") val isLetter: Boolean? = null,
    @SerializedName("inner_dimensions") val innerDimensions: String? = null,
    @SerializedName("box_weight") val boxWeight: Double? = null,
    @SerializedName("is_user_defined") val isUserDefined: Boolean? = null,
    @SerializedName("max_weight") val maxWeight: Double? = null
)

class PackageResponse {
    val storeOptions: PackageStoreOptionsDTO? = null
    val packages: PackagesInfoDTO? = null
}

class PackageStoreOptionsDTO {
    @SerializedName("currency_symbol")
    val currencySymbol: String? = null

    @SerializedName("dimension_unit")
    val dimensionUnit: String? = null

    @SerializedName("weight_unit")
    val weightUnit: String? = null

    @SerializedName("origin_country")
    val originCountry: String? = null
}

class PackagesInfoDTO {
    val saved: SavedPackageInfoDTO? = null
    val predefined: CarrierPredefinedPackagesDTO? = null
}
