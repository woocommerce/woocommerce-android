package org.wordpress.android.fluxc.model.blaze

import java.util.Date

/**
 * Blaze billing summary of the current WordPress.com user.
 *
 * @param debt amount the user failed to pay for previous campaigns, in USD.
 * @param paymentLinks unpaid orders that the user can pay to clear the debt.
 */
data class BlazeBillingSummary(
    val debt: Double,
    val paymentLinks: List<PaymentLink>
) {
    data class PaymentLink(
        val date: Date?,
        val amount: Double,
        val url: String
    )
}
