package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.woocommerce.android.model.IProduct
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
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
) : IProduct, Parcelable
