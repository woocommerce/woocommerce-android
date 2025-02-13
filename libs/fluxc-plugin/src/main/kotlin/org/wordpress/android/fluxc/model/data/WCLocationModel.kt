package org.wordpress.android.fluxc.model.data

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(name = "WCLocations", addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCLocationModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var parentCode: String = ""
    @Column var code = ""
    @Column var name = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
