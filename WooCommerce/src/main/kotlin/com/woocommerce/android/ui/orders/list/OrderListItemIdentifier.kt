package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.model.TimeGroup
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * Identifies the two main types of view items in the order list view while providing a
 * common interface for managing them as a single type:
 * <ul>
 *     <li>Section Header</li>
 *     <li>Order Item</li>
 * </ul>
 *
 * Mostly used by [OrderListItemDataSource] for properly syncing these two
 * types of views against the data being fetched and populated.
 */
sealed class OrderListItemIdentifier {
    class SectionHeaderIdentifier(val title: TimeGroup) : OrderListItemIdentifier()
    class OrderIdentifier(val remoteId: RemoteId, var isLastItemInSection: Boolean = false) : OrderListItemIdentifier()
}
