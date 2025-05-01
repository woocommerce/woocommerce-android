package org.wordpress.android.fluxc.model

import androidx.room.Entity

//todo: as soon as SiteModel is migrated to Room, add foreign key constraint
@Entity(
    tableName = "ProductCategoryEntity",
    primaryKeys = ["localSiteId", "remoteCategoryId"],
)
data class WCProductCategoryModel(
    val localSiteId: Int = 0,
    val remoteCategoryId: Long = 0L, // The unique identifier for this category on the server
    val name: String = "",
    val slug: String = "",
    val parent: Long = 0L,
)
