package org.wordpress.android.fluxc.persistence.entity.pos

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * POS-specific product table with limited columns for better performance in POS context
 */
@Entity(
    tableName = "PosProductEntity",
    primaryKeys = ["localSiteId", "remoteId"],
)
data class WCPosProductModel(
    val localSiteId: LocalId = LocalId(0),
    val remoteId: RemoteId = RemoteId(0),
    val name: String = "",
    val sku: String = "",
    val globalUniqueId: String = "",
    val type: String = "",
    val price: String = "",
    val downloadable: Boolean = false,
    val images: String = "",
    val attributes: String = "",
    val parentId: Long = 0L,
    val status: String = "",
    val regularPrice: String = "",
    val salePrice: String = "",
    val onSale: Boolean = false,
    val description: String = "",
    val shortDescription: String = "",
    val manageStock: Boolean = false,
    val stockQuantity: Double = 0.0,
    val stockStatus: String = "",
    val backordersAllowed: Boolean = false,
    val backordered: Boolean = false,
    val categories: String = "",
    val tags: String = "",
    val dateModified: String = ""
)
