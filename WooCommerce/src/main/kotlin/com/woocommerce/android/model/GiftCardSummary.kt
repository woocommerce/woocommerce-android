package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class GiftCardSummary(
    val id: Long,
    val code: String,
    val used: BigDecimal
) : Parcelable
