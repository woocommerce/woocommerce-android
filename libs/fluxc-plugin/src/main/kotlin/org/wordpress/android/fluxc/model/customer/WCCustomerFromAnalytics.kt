package org.wordpress.android.fluxc.model.customer

data class WCCustomerFromAnalytics(
    val avgOrderValue: Double,
    val city: String,
    val country: String,
    val dateLastActive: String,
    val dateLastActiveGmt: String,
    val dateLastOrder: String,
    val dateRegistered: String,
    val dateRegisteredGmt: String,
    val email: String,
    val id: Long,
    val name: String,
    val ordersCount: Int,
    val postcode: String,
    val state: String,
    val totalSpend: Double,
    val userId: Long,
    val username: String
)
