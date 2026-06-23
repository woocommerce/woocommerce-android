package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.ShippingRatesState
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel.Companion.SINGLE_QUANTITY
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOption
import com.woocommerce.android.ui.orders.wooshippinglabels.split.SelectableShippableItemUI
import com.woocommerce.android.ui.orders.wooshippinglabels.split.SelectableShippableItemsUI
import com.woocommerce.android.util.CurrencyFormatter
import java.math.BigDecimal

fun ShippableItemModel.toUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String
): ShippableItemUI {
    return ShippableItemUI(
        itemId = itemId,
        productId = productId,
        title = title,
        formattedPrice = currencyFormatter.formatCurrency(shippingTotalValue, currency),
        quantity = quantity,
        imageUrl = imageUrl,
        formattedSize = getSizeWithUnits(dimensionUnit),
        formattedWeight = getWeightWithUnits(weightUnit)
    )
}

@Suppress("LongParameterList")
fun List<ShippableItemModel>.toUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String,
    shipmentUIModel: ShipmentUIModel,
    hazmatCategory: ShippingLabelHazmatCategory?,
    packageSelectionState: PackageSelectionState,
    shippingRates: ShippingRatesState,
    customsState: CustomsState,
): ShipmentUI {
    val shippableItemsUI = map { item -> item.toUIModel(currencyFormatter, dimensionUnit, weightUnit) }
    val formattedTotalPrice = getFormattedTotalPrice(currencyFormatter)
    val formattedTotalWeight = getFormattedTotalWeight(weightUnit)
    val shipmentCostUI = getShipmentCostUI(
        shipmentUIModel = shipmentUIModel,
        ratesState = shippingRates,
        currencyFormatter = { currencyFormatter.formatCurrency(it, firstOrNull()?.currency.orEmpty()) }
    )

    return ShipmentUI(
        shippableItems = shippableItemsUI,
        formattedTotalWeight = formattedTotalWeight,
        formattedTotalPrice = formattedTotalPrice,
        packageSelectionState = packageSelectionState,
        customsState = customsState,
        hazmatState = hazmatCategory?.let { WooShippingLabelCreationViewModel.HazmatState.Declared(it) }
            ?: WooShippingLabelCreationViewModel.HazmatState.NoSelection,
        shippingRatesState = shippingRates,
        shipmentCostUI = shipmentCostUI,
        isPurchaseAPILoading = shipmentUIModel.isPurchaseAPILoading,
        labelPurchaseStatus = shipmentUIModel.label?.toPurchaseStatus() ?: LabelPurchaseStatus.Idle
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

fun ShipmentUIModel.toSelectableUIModel(
    currencyFormatter: CurrencyFormatter,
    dimensionUnit: String,
    weightUnit: String,
    purchased: Boolean
): SelectableShippableItemsUI {
    val shippableItemsUI = items.map { item -> item.toSelectableUIModel(currencyFormatter, dimensionUnit, weightUnit) }
    val formattedTotalPrice = items.getFormattedTotalPrice(currencyFormatter)
    val formattedTotalWeight = items.getFormattedTotalWeight(weightUnit)

    return SelectableShippableItemsUI(
        shippableItems = shippableItemsUI,
        formattedTotalWeight = formattedTotalWeight,
        formattedTotalPrice = formattedTotalPrice,
        purchased = purchased
    )
}

fun List<ShippableItemModel>.getFormattedTotalPrice(currencyFormatter: CurrencyFormatter): String {
    val totalPrice = sumOf { it.shippingTotalValue }
    val formattedTotalPrice = firstOrNull()?.currency?.let {
        currencyFormatter.formatCurrency(totalPrice, it)
    } ?: currencyFormatter.formatCurrency(totalPrice)
    return formattedTotalPrice
}

fun List<ShippableItemModel>.getFormattedTotalWeight(weightUnit: String): String {
    val totalWeight = sumByFloat { it.weight * it.quantity }
    return "${totalWeight.formatToString()} $weightUnit"
}

fun Order.getShippingLinesSummary(
    currencyFormatter: CurrencyFormatter
): List<ShippingLineSummaryUI> {
    return shippingLines.map {
        ShippingLineSummaryUI(
            title = it.methodTitle,
            amount = currencyFormatter.formatCurrency(it.total, currency)
        )
    }
}

private fun getShipmentCostUI(
    shipmentUIModel: ShipmentUIModel,
    ratesState: ShippingRatesState,
    currencyFormatter: (BigDecimal) -> String
): ShipmentCostUI? {
    return when {
        shipmentUIModel.isPurchasedOrInProgress -> {
            requireNotNull(shipmentUIModel.label)
            ShipmentCostUI(
                serviceName = shipmentUIModel.label.serviceName,
                formattedBasePrice = currencyFormatter(shipmentUIModel.label.rate),
                formattedTotalPrice = currencyFormatter(shipmentUIModel.label.rate),
                optionsWithFees = emptyMap()
            )
        }

        ratesState is ShippingRatesState.DataState -> {
            val selectedRate = ratesState.selectedRate ?: return null
            val totalPrice = selectedRate.selectedRateOption.rate.price +
                selectedRate.additionalSelectedOptions.sumOf { selectedRate.options.getValue(it).fee }

            @Suppress("SpreadOperator")
            ShipmentCostUI(
                serviceName = selectedRate.title,
                formattedBasePrice = selectedRate.formattedBasePrice,
                formattedTotalPrice = currencyFormatter(totalPrice),
                optionsWithFees = mapOf(
                    if (selectedRate.selectedOption != ShippingRateOption.DEFAULT) {
                        selectedRate.selectedRateOption.optionName to selectedRate.selectedRateOption.formattedFee
                    } else {
                        "" to null
                    },
                    *selectedRate.additionalSelectedOptions.map { option ->
                        val rateOption = selectedRate.options.getValue(option)
                        rateOption.optionName to rateOption.formattedFee
                    }.toTypedArray()
                ).filterNotNull()
            )
        }

        else -> null
    }
}

private fun ShippingLabelModel.toPurchaseStatus(): LabelPurchaseStatus {
    fun getPaperSizes(countryCode: String?): List<WooShippingLabelPaperSize> =
        if (countryCode.isNullOrEmpty() || countryCode.uppercase() in listOf("US", "CA", "MX", "DO")) {
            WooShippingLabelPaperSize.entries.minus(WooShippingLabelPaperSize.A4)
        } else {
            WooShippingLabelPaperSize.entries
        }

    return when {
        status == ShippingLabelStatus.PURCHASE_IN_PROGRESS -> LabelPurchaseStatus.PurchaseInProgress
        status == ShippingLabelStatus.PURCHASED && refund == null -> {
            LabelPurchaseStatus.Purchased(
                availablePrintSizes = getPaperSizes(originAddress?.country?.code),
                isRefundAvailable = isRefundAvailable,
                isCustomsFormAvailable = commercialInvoiceUrl.isNotNullOrEmpty()
            )
        }
        status == ShippingLabelStatus.PURCHASE_ERROR -> LabelPurchaseStatus.Failed(
            errorMessage = error
        )
        else -> LabelPurchaseStatus.Idle
    }
}
