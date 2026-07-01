package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.network.giftcard.GiftCardRestClient
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class GiftCardSummary(
    val id: Long,
    val code: String,
    val used: BigDecimal
) : Parcelable

fun GiftCardRestClient.GiftCardSummaryDto.toAppModel(): GiftCardSummary {
    return GiftCardSummary(
        id = this.id ?: 0,
        code = this.code.orEmpty(),
        used = this.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    )
}
