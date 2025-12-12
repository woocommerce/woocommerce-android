package org.wordpress.android.fluxc.model.plugin

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "SitePluginEntity",
    primaryKeys = ["siteId", "slug"]
)
data class SitePluginModel(
    val siteId: LocalId,
    val name: String,
    val version: String,
    val slug: String,
    val authorName: String,
    @ColumnInfo(name = "isActive")
    val isActive: Boolean
)
