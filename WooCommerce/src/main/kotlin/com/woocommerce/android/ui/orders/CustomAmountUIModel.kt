package com.woocommerce.android.ui.orders

import android.os.Parcelable
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.TaxStatus
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CustomAmountUIModel(
    val id: Long,
    val amount: BigDecimal,
    val name: String,
    val isLocked: Boolean = false,
    val taxStatus: TaxStatus = TaxStatus(),
) : Parcelable
