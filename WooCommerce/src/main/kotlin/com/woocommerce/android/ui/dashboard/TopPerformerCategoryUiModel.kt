package com.woocommerce.android.ui.dashboard

data class TopPerformerCategoryUiModel(
    val categoryId: Long,
    val name: String,
    val timesOrdered: String,
    val netSales: String,
    val onClick: (Long) -> Unit
)
