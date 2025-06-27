package com.woocommerce.android.ui.orders.wooshippinglabels.models

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel

data class PurchasedLabelData(
    val labels: List<ShippingLabelModel>,
    val origin: Map<String, OriginShippingAddress>,
    val destination: Map<String, Address>,
    val rates: Map<String, WooShippingRateModel>,
)
