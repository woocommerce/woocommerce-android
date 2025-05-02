package org.wordpress.android.fluxc.model

import androidx.room.Entity
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

//todo: as soon as SiteModel is migrated to Room, add foreign key constraint
@Entity(
    tableName = "ProductTagEntity",
    primaryKeys = ["localSiteId", "remoteTagId"],
)
data class WCProductTagModel(
    val localSiteId: Int = 0,
    val remoteTagId: Long = 0L, // The unique identifier for this tag on the server
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val count: Int = 0,
)
