package org.wordpress.android.fluxc.model.attribute

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCGlobalAttributeModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var localSiteId: Int = 0,
    @Column var name: String = "",
    @Column var slug: String = "",
    @Column var type: String = "",
    @Column var orderBy: String = "",
    @Column var hasArchives: Boolean = false,
    @Column var remoteId: Int = 0
) : Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
