package org.wordpress.android.fluxc.persistence.entity.pos

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * POS-specific product variation table with limited columns for better performance in POS context.
 */
@Entity(
    tableName = "PosVariationEntity",
    primaryKeys = ["localSiteId", "remoteProductId", "remoteVariationId"],
)
data class WCPosVariationModel(
    val localSiteId: LocalId = LocalId(0),
    val remoteProductId: RemoteId = RemoteId(0),
    val remoteVariationId: RemoteId = RemoteId(0),
    val dateModified: String = "",
    val sku: String = "",
    val globalUniqueId: String = "",
    val variationName: String = "",
    val price: String = "",
    val regularPrice: String = "",
    val salePrice: String = "",
    val description: String = "",
    val stockQuantity: Double = 0.0,
    val stockStatus: String = "",
    val manageStock: Boolean = false,
    val backordered: Boolean = false,
    val attributesJson: String = "",
    val imageUrl: String = "",
    val status: String = "",
    val lastUpdated: String = "",
    val downloadable: Boolean = false,
)
