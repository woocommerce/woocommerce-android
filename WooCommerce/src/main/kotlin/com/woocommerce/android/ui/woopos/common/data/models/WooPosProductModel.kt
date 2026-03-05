package com.woocommerce.android.ui.woopos.common.data.models

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import java.math.BigDecimal

/**
 * This model provides a clean separation between the data layer (WooPosProductEntity)
 * and the view layer, ensuring all required data is present and properly typed.
 */
@Parcelize
data class WooPosProductModel(
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
    val parentProductName: String? = null,
) : Parcelable {

    @IgnoredOnParcel
    val variationEnabledAttributes by lazy {
        attributes.filter { it.isVariation }
    }

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

    sealed class WooPosProductType(val value: String) : Parcelable {
        @Parcelize data object Simple : WooPosProductType("simple")

        @Parcelize data object Variable : WooPosProductType("variable")

        @Parcelize data object Grouped : WooPosProductType("grouped")

        @Parcelize data object External : WooPosProductType("external")

        @Parcelize data object Variation : WooPosProductType("variation")

        @Parcelize data object Subscription : WooPosProductType("subscription")

        @Parcelize data object VariableSubscription : WooPosProductType("variable-subscription")

        @Parcelize data object Custom : WooPosProductType("custom")

        @Parcelize data object Bundle : WooPosProductType("bundle")

        @Parcelize data object Composite : WooPosProductType("composite")
    }

    enum class WooPosProductStatus(val value: String) {
        PUBLISH(CoreProductStatus.PUBLISH.value),
        DRAFT(CoreProductStatus.DRAFT.value),
        PENDING(CoreProductStatus.PENDING.value),
        PRIVATE(CoreProductStatus.PRIVATE.value),
        TRASH(CoreProductStatus.TRASH.value),
        UNKNOWN("unknown")
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
