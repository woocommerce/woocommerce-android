package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto

import com.google.gson.annotations.SerializedName

data class CustomerFromAnalyticsDTO(
    @SerializedName("avg_order_value")
    val avgOrderValue: Double? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("date_last_active")
    val dateLastActive: String? = null,
    @SerializedName("date_last_active_gmt")
    val dateLastActiveGmt: String? = null,
    @SerializedName("date_last_order")
    val dateLastOrder: String? = null,
    @SerializedName("date_registered")
    val dateRegistered: String? = null,
    @SerializedName("date_registered_gmt")
    val dateRegisteredGmt: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("orders_count")
    val ordersCount: Int? = null,
    @SerializedName("postcode")
    val postcode: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("total_spend")
    val totalSpend: Double? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    @SerializedName("username")
    val username: String? = null
)
