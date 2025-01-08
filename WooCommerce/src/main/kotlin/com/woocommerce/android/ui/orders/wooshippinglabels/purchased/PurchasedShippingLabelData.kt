package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import android.os.Parcelable
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippableItemsUI
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLineSummaryUI
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingRateSummaryUI
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import kotlinx.parcelize.Parcelize

@Parcelize
data class PurchasedShippingLabelData(
    val labelId: Long,
    val orderId: Long,
    val carrierId: String,
    val trackingNumber: String,
    val items: ShippableItemsUI,
    val addresses: WooShippingAddresses,
    val rateSummary: ShippingRateSummaryUI,
    val shippingLines: List<ShippingLineSummaryUI>,
) : Parcelable

@Parcelize
data class ShippableItem(
    val itemId: Long,
    val productId: Long,
    val title: String,
    val dimensions: String,
    val weight: String,
    val formattedPrice: String,
    val quantity: Float,
    val dimensionUnit: String,
    val weightUnit: String,
    val imageUrl: String? = null
) : Parcelable
