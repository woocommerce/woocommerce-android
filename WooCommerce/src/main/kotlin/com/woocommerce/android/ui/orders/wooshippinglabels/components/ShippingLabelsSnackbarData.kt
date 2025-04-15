package com.woocommerce.android.ui.orders.wooshippinglabels.components

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration

data class ShippingLabelsSnackbarData(
    @StringRes val message: Int,
    val messageParameters: List<Int> = emptyList(),
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: Int,
    val dismissAction: () -> Unit = {},
    val action: () -> Unit
)
