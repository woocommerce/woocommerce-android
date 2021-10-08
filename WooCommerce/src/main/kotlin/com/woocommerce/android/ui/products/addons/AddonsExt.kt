package com.woocommerce.android.ui.products.addons

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.*
import java.math.BigDecimal

fun Addon.HasAdjustablePrice.Price.Adjusted.handlePriceType(
    formatCurrencyForDisplay: (BigDecimal) -> String
) = when (priceType) {
    FlatFee -> this.toFormattedPrice(formatCurrencyForDisplay)
    PercentageBased -> "$value%"
    QuantityBased -> value
}

private fun Addon.HasAdjustablePrice.Price.Adjusted.toFormattedPrice(
    formatCurrencyForDisplay: (BigDecimal) -> String
) = value.toBigDecimalOrNull()
    ?.let { formatCurrencyForDisplay(it) }
    ?: value
