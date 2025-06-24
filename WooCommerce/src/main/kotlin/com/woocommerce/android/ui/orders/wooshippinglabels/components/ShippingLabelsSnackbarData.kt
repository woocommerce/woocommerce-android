package com.woocommerce.android.ui.orders.wooshippinglabels.components

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

data class ShippingLabelsSnackbarData(
    @StringRes val message: Int,
    val messageParameters: List<Int> = emptyList(),
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: Int,
    val isSuccessSnackbar: Boolean = false,
    val dismissAction: () -> Unit = {},
    val action: () -> Unit
)

data class ShippingLabelsSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val withDismissAction: Boolean = false,
    val isSuccessSnackbar: Boolean
) : SnackbarVisuals
