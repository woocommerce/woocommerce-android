package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.model.TimeGroup
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
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
    data class SectionHeader(val title: TimeGroup) : OrderListItemUIType()

    /**
     * Flag that the data or a single order item is not yet available for
     * display. Signals a loading view for that item should be displayed.
     */
    data class LoadingItem(val remoteId: RemoteId) : OrderListItemUIType()

    /**
     * Data required to populate a single order item in the order list view.
     */
    data class OrderListItemUI(
        val localOrderId: LocalId,
        val remoteOrderId: RemoteId,
        val orderNumber: String,
        val orderName: String,
        val orderTotal: String,
        val status: String,
        val dateCreated: String,
        val currencyCode: String,
        val isLastItemInSection: Boolean = false
    ) : OrderListItemUIType()

    override fun equals(other: Any?) = other?.let { this::class == other::class } ?: false

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
