package com.woocommerce.android.ui.woopos.common.data

import java.math.BigDecimal

/**
 * POS-specific product variation model containing only the fields needed for POS functionality.
 * This model provides better performance and cleaner separation compared to the general ProductVariation model.
 */
data class WooPosVariation(
    val remoteVariationId: Long,
    val remoteProductId: Long,
    val globalUniqueId: String,
    val type: WooPosVariationType,
    val price: BigDecimal?,
    val image: WooPosVariationImage?,
    val attributes: List<WooPosVariationAttribute>,
    val isVisible: Boolean,
    val isDownloadable: Boolean
) {

    enum class WooPosVariationType(val value: String) {
        VARIATION("variation"),
        SUBSCRIPTION_VARIATION("subscription_variation"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String): WooPosVariationType =
                entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }

    data class WooPosVariationImage(
        val source: String
    )

    data class WooPosVariationAttribute(
        val id: Long?,
        val name: String?,
        val option: String?
    )
}
