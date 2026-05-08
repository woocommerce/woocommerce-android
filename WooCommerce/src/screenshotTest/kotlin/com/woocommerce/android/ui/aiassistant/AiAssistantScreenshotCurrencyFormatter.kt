package com.woocommerce.android.ui.aiassistant

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal fun screenshotWooAssistantCardRenderer(): WooAssistantCardRenderer =
    WooAssistantCardRenderer(ScreenshotAiAssistantCurrencyFormatter)

private object ScreenshotAiAssistantCurrencyFormatter : AiAssistantCurrencyFormatter {
    override fun formatCurrency(
        rawValue: String,
        currencyCode: String,
        applyDecimalFormatting: Boolean,
    ): String {
        val amount = rawValue.toBigDecimalOrNull() ?: return rawValue
        return formatAmount(amount, currencyCode)
    }

    override fun buildBigDecimalFormatter(): (BigDecimal) -> String = { amount ->
        formatAmount(amount, DEFAULT_CURRENCY_CODE)
    }

    private fun formatAmount(amount: BigDecimal, currencyCode: String): String {
        val resolvedCurrencyCode = currencyCode.ifBlank { DEFAULT_CURRENCY_CODE }
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        formatter.currency = runCatching { Currency.getInstance(resolvedCurrencyCode) }
            .getOrDefault(Currency.getInstance(DEFAULT_CURRENCY_CODE))
        return formatter.format(amount)
    }
}

private const val DEFAULT_CURRENCY_CODE = "USD"
