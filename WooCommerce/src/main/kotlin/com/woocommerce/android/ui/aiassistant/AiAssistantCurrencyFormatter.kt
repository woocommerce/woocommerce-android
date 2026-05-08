package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.util.CurrencyFormatter
import java.math.BigDecimal

internal interface AiAssistantCurrencyFormatter {
    fun formatCurrency(
        rawValue: String,
        currencyCode: String,
        applyDecimalFormatting: Boolean = true,
    ): String

    fun buildBigDecimalFormatter(): (BigDecimal) -> String
}

internal class WooAiAssistantCurrencyFormatter(
    private val currencyFormatter: CurrencyFormatter,
) : AiAssistantCurrencyFormatter {
    override fun formatCurrency(
        rawValue: String,
        currencyCode: String,
        applyDecimalFormatting: Boolean,
    ): String = currencyFormatter.formatCurrency(
        rawValue = rawValue,
        currencyCode = currencyCode,
        applyDecimalFormatting = applyDecimalFormatting,
    )

    override fun buildBigDecimalFormatter(): (BigDecimal) -> String =
        currencyFormatter.buildBigDecimalFormatter()
}
