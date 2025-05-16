package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

// todo: as soon as SiteModel is migrated to Room, add foreign key constraint
@Entity(
    tableName = "ProductCategoryEntity",
    primaryKeys = ["localSiteId", "remoteCategoryId"],
)
data class WCProductCategoryModel(
    val localSiteId: LocalId = LocalId(0),
    val remoteCategoryId: RemoteId = RemoteId(0L), // The unique identifier for this category on the server
    val name: String = "",
    val slug: String = "",
    val parent: Long = 0L,
)
