package com.woocommerce.android.ui.orders

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CustomAmountUIModel(
    val id: Long,
    val amount: BigDecimal,
    val name: String,
    val isLocked: Boolean = false
) : Parcelable
