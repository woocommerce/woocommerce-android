package com.woocommerce.android.ui.aiassistant

import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardRenderer
import com.woocommerce.android.ciab.CIABOrderStatusMapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.compose.OrderSummaryRow
import com.woocommerce.android.ui.orders.compose.OrderSummaryRowModel
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.compose.ProductSummaryRow
import com.woocommerce.android.ui.products.compose.ProductSummaryRowInfo
import com.woocommerce.android.util.CurrencyFormatter
import java.math.BigDecimal

class WooAssistantCardRenderer(
    private val currencyFormatter: CurrencyFormatter,
) : AssistantCardRenderer {
    @Composable
    override fun OrderCard(
        card: AssistantCard.Order,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        OrderSummaryRow(
            order = card.toOrderSummaryRowModel(currencyFormatter),
            onClick = { onAction(card.toOpenOrderAction()) },
            modifier = modifier,
        )
    }

    @Composable
    override fun ProductCard(
        card: AssistantCard.Product,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        val rowModel = card.toProductSummaryRowModel(context, currencyFormatter)
        ProductSummaryRow(
            title = rowModel.title,
            imageUrl = rowModel.imageUrl,
            onClick = card.toProductCardClick(onAction),
            modifier = modifier,
        ) {
            ProductSummaryRowInfo(rowModel.stockStatusPriceText)
            rowModel.skuText?.let { sku -> ProductSummaryRowInfo(sku) }
        }
    }
}

internal data class AssistantProductSummaryRowModel(
    val title: String,
    val imageUrl: String,
    val stockStatusPriceText: String,
    val skuText: String?,
)

internal fun AssistantCard.Order.toOpenOrderAction() = AssistantCardAction.OpenOrder(remoteOrderId)

internal fun AssistantCard.Product.toOpenProductAction() = AssistantCardAction.OpenProduct(remoteProductId)

internal fun AssistantCard.Product.toProductCardClick(
    onAction: (AssistantCardAction) -> Unit,
): () -> Unit = {
    onAction(toOpenProductAction())
}

internal fun AssistantCard.Product.toProductSummaryRowModel(
    context: Context,
    currencyFormatter: CurrencyFormatter,
) = AssistantProductSummaryRowModel(
    title = name,
    imageUrl = imageUrl,
    stockStatusPriceText = toStockStatusPriceText(context, currencyFormatter),
    skuText = sku
        .takeIf { it.isNotBlank() }
        ?.let { context.getString(R.string.orderdetail_product_lineitem_sku_value, it) },
)

internal fun AssistantCard.Order.toOrderSummaryRowModel(currencyFormatter: CurrencyFormatter) = OrderSummaryRowModel(
    number = number,
    date = date,
    customerName = customerName,
    status = status,
    statusColor = status.toOrderStatusColor(),
    totalPrice = formatTotalPrice(currencyFormatter),
    isPosOrder = false,
)

private fun AssistantCard.Order.formatTotalPrice(currencyFormatter: CurrencyFormatter): String =
    when {
        total.isBlank() -> ""
        currency.isBlank() -> total
        else -> currencyFormatter.formatCurrency(total, currency)
    }

private fun AssistantCard.Product.toStockStatusPriceText(
    context: Context,
    currencyFormatter: CurrencyFormatter,
): String {
    val productStatus = ProductStatus.fromString(status)
        ?.takeIf { it != ProductStatus.PUBLISH }
        ?.toLocalizedString(context)
    val stock = ProductStockStatus.stockStatusToDisplayString(
        context,
        ProductStockStatus.fromString(stockStatus),
    )
    val formattedPrice = formatProductPrice(currencyFormatter)

    return listOfNotNull(productStatus, stock, formattedPrice)
        .filter { it.isNotBlank() }
        .joinToString(separator = " \u2022 ")
}

internal fun AssistantCard.Product.formatProductPrice(currencyFormatter: CurrencyFormatter): String =
    price.toBigDecimalOrNull()
        ?.let { amount: BigDecimal -> currencyFormatter.buildBigDecimalFormatter()(amount) }
        ?: price

@ColorRes
private fun String.toOrderStatusColor(): Int =
    when (Order.Status.fromValue(this)) {
        is Order.Status.Processing -> R.color.tag_bg_processing
        is Order.Status.Completed -> R.color.tag_bg_completed
        is Order.Status.Failed -> R.color.tag_bg_failed
        is Order.Status.OnHold -> R.color.tag_bg_on_hold
        is Order.Status.Custom -> if (this == CIABOrderStatusMapper.OPEN_KEY) {
            R.color.tag_bg_processing
        } else {
            R.color.tag_bg_other
        }
        else -> R.color.tag_bg_other
    }
