package com.woocommerce.android.aiassistant.ui.cards

sealed interface AssistantCard {
    data class Order(
        val remoteOrderId: Long,
        val number: String,
        val status: String,
        val total: String,
        val currency: String,
        val customerName: String,
        val date: String,
    ) : AssistantCard

    data class Product(
        val remoteProductId: Long,
        val name: String,
        val sku: String,
        val price: String,
        val stockStatus: String,
        val status: String,
        val imageUrl: String,
    ) : AssistantCard

    data class Stats(
        val after: String,
        val before: String,
        val revenueTotal: String,
        val revenueCurrency: String,
        val orderCount: String,
        val revenueChartPoints: List<ChartPoint>,
        val orderChartPoints: List<ChartPoint>,
    ) : AssistantCard {
        data class ChartPoint(
            val date: String,
            val value: Double,
        )
    }
}
