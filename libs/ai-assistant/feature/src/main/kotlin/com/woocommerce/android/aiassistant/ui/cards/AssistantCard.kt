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

    data class Customer(
        val remoteCustomerId: Long,
        val name: String,
        val email: String,
    ) : AssistantCard

    data class Stats(
        val id: String,
        val kind: Kind,
        val after: String,
        val before: String,
        val currency: String,
        val metrics: List<Metric>,
    ) : AssistantCard {
        enum class Kind {
            Revenue,
            Orders
        }

        enum class MetricType {
            TotalSales,
            NetSales,
            TotalOrders,
            AverageOrderValue
        }

        data class Metric(
            val type: MetricType,
            val value: String,
            val chartPoints: List<ChartPoint>,
        )

        data class ChartPoint(
            val date: String,
            val value: Double,
        )
    }
}
