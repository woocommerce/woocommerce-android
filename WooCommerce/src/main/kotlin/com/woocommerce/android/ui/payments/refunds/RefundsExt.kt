package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.ui.products.ParameterRepository
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

private const val DEFAULT_CURRENCY_DECIMALS = 2

suspend fun ParameterRepository.getCurrencyDecimals(): Int =
    getParameters().currencyFormattingParameters?.currencyDecimalNumber ?: DEFAULT_CURRENCY_DECIMALS

/**
 * Returns a copy of this item refunding [newQuantity] units, with the amounts rounded once per line to [decimals].
 *
 * WooCommerce rejects a line whose refund exceeds what is left to refund on it, so per-unit rounding must never push
 * the line past its balance: refunding the last remaining units refunds exactly what is left on the line.
 */
fun ProductRefundListItem.withRefundQuantity(newQuantity: Int, decimals: Int): ProductRefundListItem {
    if (newQuantity <= 0) {
        return copy(quantity = newQuantity, subtotal = BigDecimal.ZERO, totalTax = BigDecimal.ZERO, taxes = emptyList())
    }

    val isRefundingAllRemaining = newQuantity >= availableRefundQuantity
    val shareOfLine: (BigDecimal) -> BigDecimal = { amount ->
        amount.multiply(newQuantity.toBigDecimal())
            .divide(orderItem.quantity.toBigDecimal(), MathContext.DECIMAL128)
            .roundedTo(decimals)
    }

    val remainingSubtotal = (orderItem.total - refundedTotal).roundedTo(decimals).coerceAtLeast(BigDecimal.ZERO)
    val subtotal = if (isRefundingAllRemaining) {
        remainingSubtotal
    } else {
        shareOfLine(orderItem.total).min(remainingSubtotal)
    }

    val taxes = orderItem.taxes.map { tax ->
        val refundedTax = refundedTaxes.filter { it.rateId == tax.rateId }.sumOf { it.tax }
        val remainingTax = (tax.taxAmount - refundedTax).roundedTo(decimals).coerceAtLeast(BigDecimal.ZERO)
        val taxToRefund = if (isRefundingAllRemaining) remainingTax else shareOfLine(tax.taxAmount).min(remainingTax)
        TaxRefund(rateId = tax.rateId, tax = taxToRefund)
    }

    val remainingLineTotal = (orderItem.total + orderItem.totalTax - refundedTotal - refundedTaxes.sumOf { it.tax })
        .roundedTo(decimals)
        .coerceAtLeast(BigDecimal.ZERO)
    val (clampedSubtotal, clampedTaxes) = clampToLineTotal(subtotal, taxes, remainingLineTotal)

    // Without a per-rate breakdown the tax can't go on the line, so it's only added to the refund amount.
    val taxWithoutRate = when {
        orderItem.taxes.isNotEmpty() -> BigDecimal.ZERO
        isRefundingAllRemaining -> (orderItem.totalTax - refundedTaxes.sumOf { it.tax }).roundedTo(decimals)
        else -> shareOfLine(orderItem.totalTax)
    }.coerceAtLeast(BigDecimal.ZERO)

    return copy(
        quantity = newQuantity,
        subtotal = clampedSubtotal,
        totalTax = clampedTaxes.sumOf { it.tax } + taxWithoutRate,
        taxes = clampedTaxes
    )
}

private fun clampToLineTotal(
    subtotal: BigDecimal,
    taxes: List<TaxRefund>,
    maxLineTotal: BigDecimal
): Pair<BigDecimal, List<TaxRefund>> {
    var excess = subtotal + taxes.sumOf { it.tax } - maxLineTotal
    if (excess <= BigDecimal.ZERO) return subtotal to taxes

    val clampedTaxes = taxes.asReversed().map { tax ->
        val cut = excess.min(tax.tax).coerceAtLeast(BigDecimal.ZERO)
        excess -= cut
        tax.copy(tax = tax.tax - cut)
    }.asReversed()
    return (subtotal - excess).coerceAtLeast(BigDecimal.ZERO) to clampedTaxes
}

private fun BigDecimal.roundedTo(decimals: Int): BigDecimal = setScale(decimals, RoundingMode.HALF_UP)
