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
    val imageUrl: String? = null,
    val sku: String? = null
) : Parcelable
