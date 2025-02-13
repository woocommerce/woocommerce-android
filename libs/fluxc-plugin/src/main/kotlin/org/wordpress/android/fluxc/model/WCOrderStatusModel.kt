package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderStatusModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    constructor(statusKey: String) : this() {
        this.statusKey = statusKey
    }

    @Column var localSiteId = 0
    @Column var statusKey = ""
    @Column var label = ""
    @Column var statusCount = 0

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
