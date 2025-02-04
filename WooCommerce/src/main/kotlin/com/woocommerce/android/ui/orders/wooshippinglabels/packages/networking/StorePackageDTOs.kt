package com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking

import com.google.gson.annotations.SerializedName

class SavedPackageInfoDTO {
    val custom: List<CustomPackageDTO>? = null
}

class CustomPackageDTO {
    val id: String? = null
    val name: String? = null
    val dimensions: String? = null
    val length: Double? = null
    val width: Double? = null
    val height: Double? = null
    val type: String? = null

    @SerializedName("box_weight")
    val boxWeight: Double? = null

    @SerializedName("is_letter")
    val isLetter: Boolean? = null

    @SerializedName("is_user_defined")
    val isUserDefined: Boolean? = null
}
