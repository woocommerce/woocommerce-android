package org.wordpress.android.fluxc.model.attribute

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "GlobalAttributeEntity",
    primaryKeys = ["siteId", "remoteId"]
)
data class WCGlobalAttributeModel(
    val siteId: LocalId,
    val remoteId: RemoteId,
    var name: String,
    var slug: String,
    var type: String,
    var orderBy: String,
    var hasArchives: Boolean,
)
