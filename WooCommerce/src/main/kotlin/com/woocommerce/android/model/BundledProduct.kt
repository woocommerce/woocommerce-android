package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.parcelize.Parcelize

@Parcelize
data class BundledProduct(
    val id: Long,
    val parentProductId: Long,
    val bundledProductId: Long,
    val title: String,
    val stockStatus: ProductStockStatus,
    val rules: BundleProductRules,
    val imageUrl: String? = null,
    val sku: String? = null,
    val isVariable: Boolean
) : Parcelable

@Parcelize
data class BundleProductRules(
    val quantityMin: Float? = null,
    val quantityMax: Float? = null,
    val isOptional: Boolean = false,
    val quantityDefault: Float = 0f,
    val attributesDefault: List<VariantOption>? = null,
    val variationIds: List<Long>? = null
) : Parcelable
