package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductSettingsModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    // note that there are many more product settings than this, but for now these are all we need
    @Column var localSiteId = 0
    @Column var weightUnit = ""
    @Column var dimensionUnit = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
