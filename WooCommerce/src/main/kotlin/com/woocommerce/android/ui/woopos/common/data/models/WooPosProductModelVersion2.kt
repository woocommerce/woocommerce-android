package com.woocommerce.android.ui.woopos.common.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

/**
 * This model provides a clean separation between the data layer (WCPosProductEntity)
 * and the view layer, ensuring all required data is present and properly typed.
 */
@Parcelize
data class WooPosProductModelVersion2(
    val remoteId: Long,
    val parentId: Long?,
    val name: String,
    val sku: String,
    val globalUniqueId: String,
    val type: WooPosProductType,
    val status: WooPosProductStatus,
    val pricing: WooPosPricing,
    val description: String,
    val shortDescription: String,
    val isDownloadable: Boolean,
    val lastModified: String,
    val images: List<WooPosProductImage> = emptyList(),
    val attributes: List<WooPosProductAttribute> = emptyList(),
    val categories: List<WooPosProductCategory> = emptyList(),
    val tags: List<WooPosProductTag> = emptyList(),
    val variationIds: List<Long> = emptyList(),
) : Parcelable {

    sealed class WooPosPricing : Parcelable {
        @Parcelize
        data object NoPricing : WooPosPricing()

        @Parcelize
        data class RegularPricing(
            val price: BigDecimal
        ) : WooPosPricing()

        @Parcelize
        data class SalePricing(
            val regularPrice: BigDecimal,
            val salePrice: BigDecimal
        ) : WooPosPricing()

        val displayPrice: BigDecimal?
            get() = when (this) {
                is NoPricing -> null
                is RegularPricing -> price
                is SalePricing -> salePrice
            }

        val isOnSale: Boolean
            get() = this is SalePricing

        val hasPrice: Boolean
            get() = this != NoPricing

        val formattedPrice: String
            get() = displayPrice?.toPlainString() ?: ""
    }

    enum class WooPosProductType {
        SIMPLE,
        VARIABLE,
        GROUPED,
        EXTERNAL,
        VARIATION,
        SUBSCRIPTION,
        VARIABLE_SUBSCRIPTION,
        CUSTOM,
        BUNDLE,
        COMPOSITE
    }

    enum class WooPosProductStatus {
        PUBLISH,
        DRAFT,
        PENDING,
        PRIVATE,
        TRASH,
        UNKNOWN
    }

    @Parcelize
    data class WooPosProductImage(
        val id: Long,
        val url: String,
        val name: String,
        val alt: String?
    ) : Parcelable

    @Parcelize
    data class WooPosProductAttribute(
        val id: Long,
        val name: String,
        val options: List<String>,
        val isVisible: Boolean,
        val isVariation: Boolean
    ) : Parcelable

    @Parcelize
    data class WooPosProductCategory(
        val id: Long,
        val name: String,
        val slug: String
    ) : Parcelable

    @Parcelize
    data class WooPosProductTag(
        val id: Long,
        val name: String,
        val slug: String
    ) : Parcelable

    val firstImageUrl: String?
        get() = images.firstOrNull()?.url
}
