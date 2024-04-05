package com.woocommerce.android.ui.dashboard

data class JetpackBenefitsBannerUiModel(
    val show: Boolean = false,
    val onDismiss: () -> Unit = {}
)

data class TopPerformerProductUiModel(
    val productId: Long,
    val name: String,
    val timesOrdered: String,
    val netSales: String,
    val imageUrl: String?,
    val onClick: (Long) -> Unit
)
