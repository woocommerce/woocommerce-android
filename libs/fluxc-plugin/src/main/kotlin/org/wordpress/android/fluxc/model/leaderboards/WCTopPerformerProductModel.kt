package org.wordpress.android.fluxc.model.leaderboards

import com.google.gson.Gson
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCTopPerformerProductModel(
    @Column var productInfo: String = "",
    @Column var currency: String = "",
    @Column var quantity: Int = 0,
    @Column var total: Double = 0.0,
    @Column var localSiteId: Int = 0,
    @Column var unit: String = "",
    @PrimaryKey @Column private var id: Int = 0
) : Identifiable {
    val product
        get() = Gson().fromJson(productInfo, WCProductModel::class.java)

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
