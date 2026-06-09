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

internal class AiAssistantVariationCardRenderer(
    private val currencyFormatter: AiAssistantCurrencyFormatter,
) {
    @Composable
    fun Card(
        card: AssistantCard.Variation,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        val rowModel = card.toVariationSummaryRowModel(context, currencyFormatter)
        ProductSummaryRow(
            title = rowModel.title,
            imageUrl = rowModel.imageUrl,
            onClick = { onAction(card.toOpenProductVariationAction()) },
            modifier = modifier,
        ) {
            rowModel.supportingTexts.forEach { text ->
                ProductSummaryRowInfo(text)
            }
        }
    }
}

internal fun AssistantCard.Variation.toOpenProductVariationAction(): AssistantCardAction =
    AssistantCardAction.OpenProductVariation(
        parentProductId = parentProductId,
        variationId = variationId,
    )

internal data class AssistantVariationSummaryRowModel(
    val title: String,
    val imageUrl: String,
    val supportingTexts: List<String>,
)

internal fun AssistantCard.Variation.toVariationSummaryRowModel(
    context: Context,
    currencyFormatter: AiAssistantCurrencyFormatter,
): AssistantVariationSummaryRowModel {
    fun formatVariationPrice(): String =
        price.toBigDecimalOrNull()
            ?.let { amount: BigDecimal -> currencyFormatter.buildBigDecimalFormatter()(amount) }
            ?: price

    fun attributesText(): String? =
        attributes
            .mapNotNull { attribute ->
                val name = attribute.name.takeIf { it.isNotBlank() }
                val option = attribute.option.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (name != null) "$name: $option" else option
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " \u2022 ")

    fun statusStockPriceText(): String? {
        val productStatus = ProductStatus.fromString(status)
            ?.takeIf { it != ProductStatus.PUBLISH }
            ?.toLocalizedString(context)
        val stock = stockStatus
            .takeIf { it.isNotBlank() }
            ?.let {
                ProductStockStatus.stockStatusToDisplayString(
                    context,
                    ProductStockStatus.fromString(it),
                )
            }

        return listOfNotNull(productStatus, stock, formatVariationPrice())
            .filter { it.isNotBlank() }
            .joinToString(separator = " \u2022 ")
            .takeIf { it.isNotBlank() }
    }

    val skuText = sku
        .takeIf { it.isNotBlank() }
        ?.let { context.getString(R.string.orderdetail_product_lineitem_sku_value, it) }
    val attributesTitle = attributesText()
    val title = attributesTitle
        ?: skuText
        ?: context.getString(R.string.ai_assistant_variation_card_id_title, variationId)

    return AssistantVariationSummaryRowModel(
        title = title,
        imageUrl = imageUrl,
        supportingTexts = listOfNotNull(
            parentProductName.takeIf { it.isNotBlank() },
            statusStockPriceText(),
            skuText.takeIf { it != title },
        ),
    )
}
