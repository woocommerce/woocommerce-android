package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * Class represents the bare minimum fields needed to determine if an order is outdated and
 * needs to be fetched. It's also important to store this information in the database so it
 * can be used by the order list's implementation of [ListItemDataSourceInterface] to create a list
 * of existing orders in the [OrderEntity] table, as well as a list of orders being fetched by the
 * API because they do not yet exist. Normally we wouldn't need this extra step to work with the
 * [org.wordpress.android.fluxc.store.ListStore], but since we need the `dateCreated` field to group the
 * orders into time-based groups, this extra table is necessary.
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE (REMOTE_ORDER_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE"
)
data class WCOrderSummaryModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column(name = "REMOTE_ORDER_ID") var orderId = 0L // The unique identifier for this order on the server
    @Column var dateCreated = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    var dateModified = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
