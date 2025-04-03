package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel.Companion.SINGLE_QUANTITY
import com.woocommerce.android.ui.orders.wooshippinglabels.split.SelectableShippableItemUI
import com.woocommerce.android.ui.orders.wooshippinglabels.split.SelectableShippableItemsUI
import com.woocommerce.android.util.CurrencyFormatter

fun ShippableItemModel.toUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String
): ShippableItemUI {
    return ShippableItemUI(
        itemId = itemId,
        productId = productId,
        title = title,
        formattedPrice = currencyFormatter.formatCurrency(price, currency),
        quantity = quantity,
        imageUrl = imageUrl,
        formattedSize = getSizeWithUnits(dimensionUnit),
        formattedWeight = getWeightWithUnits(weightUnit)
    )
}

fun List<ShippableItemModel>.toUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String
): ShippableItemsUI {
    val shippableItemsUI = this.map { item -> item.toUIModel(currencyFormatter, dimensionUnit, weightUnit) }
    val formattedTotalPrice = this.getFormattedTotalPrice(currencyFormatter)
    val formattedTotalWeight = this.getFormattedTotalWeight(weightUnit)

    return ShippableItemsUI(
        shippableItems = shippableItemsUI,
        formattedTotalWeight = formattedTotalWeight,
        formattedTotalPrice = formattedTotalPrice
    )
}

fun ShippableItemModel.toSelectableUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String
): SelectableShippableItemUI {
    val shippableItemUI = ShippableItemUI(
        itemId = itemId,
        productId = productId,
        title = title,
        formattedPrice = currencyFormatter.formatCurrency(shippingTotalValue, currency),
        quantity = quantity,
        imageUrl = imageUrl,
        formattedSize = getSizeWithUnits(dimensionUnit),
        formattedWeight = getWeightWithUnits(weightUnit)
    )
    return if (quantity > SINGLE_QUANTITY) {
        val innerShippableItemUI = ShippableItemUI(
            itemId = itemId,
            productId = productId,
            title = title,
            formattedPrice = currencyFormatter.formatCurrency(price, currency),
            quantity = 1f,
            imageUrl = imageUrl,
            formattedSize = getSizeWithUnits(dimensionUnit),
            formattedWeight = getWeightWithUnits(weightUnit)
        )
        SelectableShippableItemUI.ExpandableSelectableShippableItemUI(shippableItemUI, innerShippableItemUI)
    } else {
        SelectableShippableItemUI.SingleSelectableShippableItemUI(shippableItemUI)
    }
}

fun List<ShippableItemModel>.toSelectableUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String
): SelectableShippableItemsUI {
    val shippableItemsUI = this.map { item -> item.toSelectableUIModel(currencyFormatter, dimensionUnit, weightUnit) }
    val formattedTotalPrice = this.getFormattedTotalPrice(currencyFormatter)
    val formattedTotalWeight = this.getFormattedTotalWeight(weightUnit)

    return SelectableShippableItemsUI(
        shippableItems = shippableItemsUI,
        formattedTotalWeight = formattedTotalWeight,
        formattedTotalPrice = formattedTotalPrice
    )
}

fun List<ShippableItemModel>.getFormattedTotalPrice(currencyFormatter: CurrencyFormatter): String {
    val totalPrice = this.sumOf { it.price }
    val formattedTotalPrice = this.firstOrNull()?.currency?.let {
        currencyFormatter.formatCurrency(totalPrice, it)
    } ?: currencyFormatter.formatCurrency(totalPrice)
    return formattedTotalPrice
}

fun List<ShippableItemModel>.getFormattedTotalWeight(weightUnit: String): String {
    val totalWeight = this.sumByFloat { it.weight * it.quantity }
    return "${totalWeight.formatToString()} $weightUnit"
}

fun Order.getShippingLinesSummary(
    currencyFormatter: CurrencyFormatter
): List<ShippingLineSummaryUI> {
    return this.shippingLines.map {
        ShippingLineSummaryUI(
            title = it.methodTitle,
            amount = currencyFormatter.formatCurrency(it.total, this.currency)
        )
    }
}
