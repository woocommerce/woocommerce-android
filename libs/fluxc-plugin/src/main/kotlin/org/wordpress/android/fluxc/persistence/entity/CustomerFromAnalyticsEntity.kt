package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId

@Entity(tableName = "CustomerFromAnalytics", primaryKeys = ["localSiteId", "id"])
data class CustomerFromAnalyticsEntity (
    val localSiteId: LocalOrRemoteId.LocalId,
    val id: Long,
    val userId: Long,
    val avgOrderValue: Double,
    val city: String,
    val country: String,
    val dateLastActive: String,
    val dateLastActiveGmt: String,
    val dateLastOrder: String,
    val dateRegistered: String,
    val dateRegisteredGmt: String,
    val email: String,
    val name: String,
    val ordersCount: Int,
    val postcode: String,
    val state: String,
    val totalSpend: Double,
    val username: String
)
