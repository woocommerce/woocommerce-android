package com.woocommerce.android.ui.orders.wooshippinglabels.models

import com.woocommerce.android.model.IProduct
import java.math.BigDecimal

data class ShippableItemModel(
    val itemId: Long,
    val productId: Long,
    val title: String,
    val price: BigDecimal,
    val quantity: Float,
    val imageUrl: String? = null,
    val currency: String,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float
) : IProduct
