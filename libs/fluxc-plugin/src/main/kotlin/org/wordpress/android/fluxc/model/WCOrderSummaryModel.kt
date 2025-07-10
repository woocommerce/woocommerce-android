package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.Ignore
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface

/**
 * Class represents the bare minimum fields needed to determine if an order is outdated and
 * needs to be fetched. It's also important to store this information in the database so it
 * can be used by the order list's implementation of [ListItemDataSourceInterface] to create a list
 * of existing orders in the [OrderEntity] table, as well as a list of orders being fetched by the
 * API because they do not yet exist. Normally we wouldn't need this extra step to work with the
 * [org.wordpress.android.fluxc.store.ListStore], but since we need the `dateCreated` field to group the
 * orders into time-based groups, this extra table is necessary.
 */
@Entity(
    tableName = "OrderSummaryEntity",
    primaryKeys = ["siteId", "orderId"]
)
data class WCOrderSummaryModel(
    val siteId: LocalId = LocalId(0),
    val orderId: RemoteId = RemoteId(0L), // The unique identifier for this order on the server
    val dateCreated: String = "", // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
) {
    @Ignore var dateModified: String = ""
}
