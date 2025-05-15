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
class WCProductShippingClassModel(
    @Column var localSiteId: Int = 0,
    @Column var remoteShippingClassId: Long = 0L, // The unique identifier for this shipping class on the server
    @Column var name: String = "",
    @Column var slug: String = "",
    @Column var description: String = "",
)
