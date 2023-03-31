package com.woocommerce.android.model

import com.woocommerce.android.network.giftcard.GiftCardRestClient
import java.math.BigDecimal

data class GiftCardSummary(
    val id: Long,
    val code: String,
    val used: BigDecimal
)

fun GiftCardRestClient.GiftCardSummaryDto.toAppModel(): GiftCardSummary {
    return GiftCardSummary(
        id = this.id ?: 0,
        code = this.code.orEmpty(),
        used = this.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    )
}
