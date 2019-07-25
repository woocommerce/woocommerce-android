package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.model.TimeGroup
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * This class represents three possible list item view states for the order list page,
 * and contains the data needed to properly populate the view associated with this state.
 *
 * <ul>
 *     <li>Section Headers</li>
 *     <li>Order List Item</li>
 *     <li>Order List Item - Loading</li>
 * </ul>
 */
sealed class OrderListItemUIType {
    /**
     * Data required to populate a section header in the order list view.
     */
    class SectionHeader(val title: TimeGroup) : OrderListItemUIType()

    /**
     * Flag that the data or a single order item is not yet available for
     * display. Signals a loading view for that item should be displayed.
     */
    class LoadingItem(val remoteId: RemoteId) : OrderListItemUIType()

    /**
     * Data required to populate a single order item in the order list view.
     */
    data class OrderListItemUI(
        val remoteOrderId: RemoteId,
        val orderNumber: String,
        val orderName: String,
        val orderTotal: String,
        val status: String,
        val dateCreated: String,
        val currencyCode: String
    ) : OrderListItemUIType()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        if (this is SectionHeader && other is SectionHeader) {
            return this.title == other.title
        }

        if (this is LoadingItem && other is LoadingItem) {
            return this.remoteId == other.remoteId
        }

        if (this is OrderListItemUI && other is OrderListItemUI) {
            if (remoteOrderId != other.remoteOrderId
                    || orderNumber != other.orderNumber
                    || orderName != other.orderName
                    || orderTotal != other.orderTotal
                    || status != other.status
                    || dateCreated != other.dateCreated
                    || currencyCode != other.currencyCode) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
