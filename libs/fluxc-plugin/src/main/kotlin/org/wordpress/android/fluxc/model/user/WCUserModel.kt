package org.wordpress.android.fluxc.model.user

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "UserEntity",
    primaryKeys = ["localSiteId", "remoteUserId"],
)
data class WCUserModel(
    val localSiteId: LocalId,
    val remoteUserId: RemoteId,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val roles: String = ""
)
