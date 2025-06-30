package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderShipmentTrackingModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column(name = "LOCAL_ORDER_ID") var orderId = 0L // The local db unique identifier for the parent order object
    @Column var remoteTrackingId = ""
    @Column var trackingNumber = ""
    @Column var trackingProvider = ""
    @Column var trackingLink = ""
    @Column var dateShipped = ""

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = this.id
}
