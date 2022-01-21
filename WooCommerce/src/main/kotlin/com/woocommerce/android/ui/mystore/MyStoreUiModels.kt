package com.woocommerce.android.ui.mystore

data class BenefitsBannerUiModel(
    val show: Boolean = false,
    val onDismiss: () -> Unit = {}
)

data class RevenueStatsUiModel(
    val intervalList: List<StatsIntervalUiModel> = emptyList(),
    val totalOrdersCount: Int? = null,
    val totalSales: Double? = null,
    val currencyCode: String?
)

data class StatsIntervalUiModel(
    val interval: String? = null,
    val ordersCount: Long? = null,
    val sales: Double? = null
)

data class TopPerformerProductUiModel(
    val productId: Long,
    val name: String,
    val timesOrdered: String,
    val netSales: String,
    val imageUrl: String?,
    val onClick: (Long) -> Unit
)
