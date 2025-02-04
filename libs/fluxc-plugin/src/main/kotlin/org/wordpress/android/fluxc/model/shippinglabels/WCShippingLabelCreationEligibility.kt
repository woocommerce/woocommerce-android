package org.wordpress.android.fluxc.model.shippinglabels

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
class WCShippingLabelCreationEligibility(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteOrderId = 0L
    @Column var canCreatePackage = false
    @Column var canCreatePaymentMethod = false
    @Column var canCreateCustomsForm = false
    @Column var isEligible = false
        @JvmName("setIsEligible")
        set
    @Column var reason: String? = null

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId(): Int = id
}
