package com.woocommerce.android.ui.aiassistant

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.compose.ProductSummaryRow
import com.woocommerce.android.ui.products.compose.ProductSummaryRowInfo
import java.math.BigDecimal

internal class AiAssistantProductCardRenderer(
    private val currencyFormatter: AiAssistantCurrencyFormatter,
) {
    @Composable
    fun Card(
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

internal data class AssistantProductSummaryRowModel(
    val title: String,
    val imageUrl: String,
    val stockStatusPriceText: String,
    val skuText: String?,
)

internal fun AssistantCard.Product.toProductSummaryRowModel(
    context: Context,
    currencyFormatter: AiAssistantCurrencyFormatter,
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
