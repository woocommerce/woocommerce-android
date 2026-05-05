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
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.compose.OrderSummaryRow
import com.woocommerce.android.ui.orders.compose.OrderSummaryRowModel
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.compose.ProductSummaryRow
import com.woocommerce.android.ui.products.compose.ProductSummaryRowInfo
import com.woocommerce.android.util.CurrencyFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Date

class WooAssistantCardRenderer(
    private val currencyFormatter: CurrencyFormatter,
) : AssistantCardRenderer {
    @Composable
    override fun OrderCard(
        card: AssistantCard.Order,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        OrderSummaryRow(
            order = card.toOrderSummaryRowModel(context, currencyFormatter),
            onClick = { onAction(AssistantCardAction.OpenOrder(card.remoteOrderId)) },
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
            onClick = { onAction(AssistantCardAction.OpenProduct(card.remoteProductId)) },
            modifier = modifier,
        ) {
            ProductSummaryRowInfo(rowModel.stockStatusPriceText)
            rowModel.skuText?.let { sku -> ProductSummaryRowInfo(sku) }
        }
    }
}

// ---------- Order card ----------

internal fun AssistantCard.Order.toOrderSummaryRowModel(
    context: Context,
    currencyFormatter: CurrencyFormatter,
): OrderSummaryRowModel {
    fun formatTotalPrice(): String =
        when {
            total.isBlank() -> ""
            currency.isBlank() -> total
            else -> currencyFormatter.formatCurrency(total, currency)
        }

    fun formatDate(): String =
        try {
            Date.from(Instant.parse(date)).formatToMMMdd()
        } catch (_: DateTimeParseException) {
            date
        }

    fun resolveCustomerName(): String =
        customerName.ifBlank { context.getString(R.string.orderdetail_customer_name_default) }

    return OrderSummaryRowModel(
        number = number,
        date = formatDate(),
        customerName = resolveCustomerName(),
        status = status,
        statusColor = resolveOrderStatusColor(status),
        totalPrice = formatTotalPrice(),
        isPosOrder = false,
    )
}

@ColorRes
private fun resolveOrderStatusColor(status: String): Int =
    when (Order.Status.fromValue(status)) {
        is Order.Status.Processing -> R.color.tag_bg_processing
        is Order.Status.Completed -> R.color.tag_bg_completed
        is Order.Status.Failed -> R.color.tag_bg_failed
        is Order.Status.OnHold -> R.color.tag_bg_on_hold
        is Order.Status.Custom -> if (status == CIABOrderStatusMapper.OPEN_KEY) {
            R.color.tag_bg_processing
        } else {
            R.color.tag_bg_other
        }
        else -> R.color.tag_bg_other
    }

// ---------- Product card ----------

internal data class AssistantProductSummaryRowModel(
    val title: String,
    val imageUrl: String,
    val stockStatusPriceText: String,
    val skuText: String?,
)

internal fun AssistantCard.Product.toProductSummaryRowModel(
    context: Context,
    currencyFormatter: CurrencyFormatter,
): AssistantProductSummaryRowModel {
    fun formatProductPrice(): String =
        price.toBigDecimalOrNull()
            ?.let { amount: BigDecimal -> currencyFormatter.buildBigDecimalFormatter()(amount) }
            ?: price

    fun stockStatusPriceText(): String {
        val productStatus = ProductStatus.fromString(status)
            ?.takeIf { it != ProductStatus.PUBLISH }
            ?.toLocalizedString(context)
        val stock = ProductStockStatus.stockStatusToDisplayString(
            context,
            ProductStockStatus.fromString(stockStatus),
        )
        return listOfNotNull(productStatus, stock, formatProductPrice())
            .filter { it.isNotBlank() }
            .joinToString(separator = " \u2022 ")
    }

    return AssistantProductSummaryRowModel(
        title = name,
        imageUrl = imageUrl,
        stockStatusPriceText = stockStatusPriceText(),
        skuText = sku
            .takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.orderdetail_product_lineitem_sku_value, it) },
    )
}
