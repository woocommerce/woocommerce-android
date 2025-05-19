package org.wordpress.android.fluxc.model

import androidx.room.Entity
import com.yarolegovich.wellsql.core.annotation.Column
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

// todo: as soon as SiteModel is migrated to Room, add foreign key constraint
@Entity(
    tableName = "ProductShippingClassEntity",
    primaryKeys = ["localSiteId", "remoteShippingClassId"],
)
data class WCProductShippingClassModel(
    @Column val localSiteId: LocalId = LocalId(0),
    @Column val remoteShippingClassId: RemoteId = RemoteId(0L), // The unique identifier for this shipping class on the server
    @Column val name: String = "",
    @Column val slug: String = "",
    @Column val description: String = "",
)
