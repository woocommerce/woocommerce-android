package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "ProductShippingClassEntity",
    primaryKeys = ["localSiteId", "remoteShippingClassId"],
)
data class WCProductShippingClassModel(
    val localSiteId: LocalId = LocalId(0),
    val remoteShippingClassId: RemoteId = RemoteId(0L), // The unique identifier for this shipping class on the server
    val name: String = "",
    val slug: String = "",
    val description: String = "",
)
