package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.util.CurrencyFormatter

fun ShippableItemModel.toUIModel(
    currencyFormatter: CurrencyFormatter,
    storeOptions: StoreOptionsModel
): ShippableItemUI {
    return ShippableItemUI(
        itemId = itemId,
        productId = productId,
        title = title,
        formattedPrice = currencyFormatter.formatCurrency(price, currency),
        quantity = quantity,
        imageUrl = imageUrl,
        formattedSize = getSizeWithUnits(storeOptions.dimensionUnit),
        formattedWeight = getWeightWithUnits(storeOptions.weightUnit)
    )
}
