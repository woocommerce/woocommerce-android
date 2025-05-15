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
    tableName = "ProductShippingClassEntity",
    primaryKeys = ["localSiteId", "remoteShippingClassId"],
)
data class WCProductShippingClassModel(
    @Column val localSiteId: Int = 0,
    @Column val remoteShippingClassId: Long = 0L, // The unique identifier for this shipping class on the server
    @Column val name: String = "",
    @Column val slug: String = "",
    @Column val description: String = "",
)
