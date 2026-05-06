package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import java.math.BigDecimal

@Entity(
    tableName = "WooPushNotificationPreferences",
    primaryKeys = ["localSiteId"]
)
data class WooPushNotificationPreferencesEntity(
    val localSiteId: LocalId,
    val storeOrderEnabled: Boolean? = null,
    val storeOrderMinAmount: BigDecimal? = null,
    val storeReviewEnabled: Boolean? = null,
    val storeReviewMaxRating: Int? = null,
    val storeStockEnabled: Boolean? = null,
    val storeStockLowStock: Boolean? = null,
    val storeStockOutOfStock: Boolean? = null,
    val storeStockOnBackorder: Boolean? = null,
)

internal fun WooPushNotificationPreferencesEntity.toModel(): WooPushNotificationPreferences =
    WooPushNotificationPreferences(
        storeOrder = toStoreOrderPreferences(),
        storeReview = toStoreReviewPreferences(),
        storeStock = toStoreStockPreferences()
    )

private fun WooPushNotificationPreferencesEntity.toStoreOrderPreferences() =
    if (storeOrderEnabled == null && storeOrderMinAmount == null) {
        null
    } else {
        WooPushNotificationPreferences.StoreOrderPreferences(
            enabled = storeOrderEnabled,
            minAmount = storeOrderMinAmount
        )
    }

private fun WooPushNotificationPreferencesEntity.toStoreReviewPreferences() =
    if (storeReviewEnabled == null && storeReviewMaxRating == null) {
        null
    } else {
        WooPushNotificationPreferences.StoreReviewPreferences(
            enabled = storeReviewEnabled,
            maxRating = storeReviewMaxRating
        )
    }

private fun WooPushNotificationPreferencesEntity.toStoreStockPreferences() =
    listOf(
        storeStockEnabled,
        storeStockLowStock,
        storeStockOutOfStock,
        storeStockOnBackorder
    ).takeIf { values -> values.any { it != null } }?.let {
        WooPushNotificationPreferences.StoreStockPreferences(
            enabled = storeStockEnabled,
            lowStock = storeStockLowStock,
            outOfStock = storeStockOutOfStock,
            onBackorder = storeStockOnBackorder
        )
    }

internal fun WooPushNotificationPreferences.toEntity(localSiteId: LocalId): WooPushNotificationPreferencesEntity =
    WooPushNotificationPreferencesEntity(
        localSiteId = localSiteId,
        storeOrderEnabled = storeOrder?.enabled,
        storeOrderMinAmount = storeOrder?.minAmount,
        storeReviewEnabled = storeReview?.enabled,
        storeReviewMaxRating = storeReview?.maxRating,
        storeStockEnabled = storeStock?.enabled,
        storeStockLowStock = storeStock?.lowStock,
        storeStockOutOfStock = storeStock?.outOfStock,
        storeStockOnBackorder = storeStock?.onBackorder
    )
