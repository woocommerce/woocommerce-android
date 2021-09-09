package com.woocommerce.android.ui.products.addons

import org.wordpress.android.fluxc.domain.Addon
import java.math.BigDecimal

fun Addon.HasAdjustablePrice.Price.Adjusted.toFormattedPrice(
    formatCurrencyForDisplay: (BigDecimal) -> String
) = value.toBigDecimalOrNull()
    ?.let { formatCurrencyForDisplay(it) }
    ?: value
