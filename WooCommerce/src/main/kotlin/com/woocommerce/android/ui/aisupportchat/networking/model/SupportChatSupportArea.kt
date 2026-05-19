package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatSupportArea(
    @SerializedName("area") val area: String? = null,
    @SerializedName("topic") val topic: String? = null,
    @SerializedName("confidence") val confidence: String? = null
) {
    val areaType: SupportAreaType
        get() = SupportAreaType.fromWireValue(area)

    val confidenceLevel: SupportAreaConfidence
        get() = SupportAreaConfidence.fromWireValue(confidence)

    val isHighConfidence: Boolean
        get() = confidenceLevel == SupportAreaConfidence.HIGH
}

enum class SupportAreaType(val wireValue: String) {
    MOBILE_APP("mobile-app"),
    CARD_READER("card-reader"),
    WOO_PAYMENTS("woopayments"),
    WOO_COMMERCE_PLUGIN("woocommerce-plugin"),
    OTHER_EXTENSION_PLUGIN("other-extension-plugin");

    companion object {
        fun fromWireValue(value: String?): SupportAreaType =
            entries.firstOrNull { it.wireValue == value?.lowercase() } ?: MOBILE_APP
    }
}

enum class SupportAreaConfidence(val wireValue: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    companion object {
        fun fromWireValue(value: String?): SupportAreaConfidence =
            entries.firstOrNull { it.wireValue == value?.lowercase() } ?: LOW
    }
}
