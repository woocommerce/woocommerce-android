package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.blaze.BlazeBillingSummary
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Date

class BlazeBillingSummaryResponse(
    @SerializedName("debt") val debt: String?,
    @SerializedName("payment_links") val paymentLinks: List<PaymentLinkResponse?>?
) {
    class PaymentLinkResponse(
        @SerializedName("date") val date: String?,
        @SerializedName("amount") val amount: String?,
        @SerializedName("url") val url: String?
    ) {
        fun toDomainModel(): BlazeBillingSummary.PaymentLink? = url?.let {
            BlazeBillingSummary.PaymentLink(
                date = date?.toDateOrNull(),
                amount = amount.toAmount(),
                url = it
            )
        }
    }

    fun toDomainModel() = BlazeBillingSummary(
        debt = debt.toAmount(),
        paymentLinks = paymentLinks.orEmpty().mapNotNull { it?.toDomainModel() }
    )
}

private fun String?.toAmount() = this?.toDoubleOrNull() ?: 0.0

private fun String.toDateOrNull(): Date? = try {
    Date.from(OffsetDateTime.parse(this).toInstant())
} catch (e: DateTimeParseException) {
    null
}
