package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper

interface OrderListContractNew {
    interface Presenter : BasePresenter<View> {
        /**
         * Generates and returns the [PagedListWrapper] used for fetching and syncing
         * the order list data to the [OrderListAdapterNew].
         *
         * @param descriptor A [WCOrderListDescriptor] that describes the parameters used
         * for fetching and displaying orders.
         * @param lifecycle The [Lifecycle] of the view this wrapper should conform to
         */
        fun generatePageWrapper(
            descriptor: WCOrderListDescriptor,
            lifecycle: Lifecycle
        ): PagedListWrapper<OrderListItemUIType>

        /**
         * Generates and returns a [WCOrderListDescriptor] with the parameters to be used for
         * fetching and displaying orders.
         *
         * @param orderStatusFilter The order status filter to filter orders by for display
         * @param orderSearchQuery The search query to use to find matching orders for display
         */
        fun generateListDescriptor(orderStatusFilter: String?, orderSearchQuery: String? = ""): WCOrderListDescriptor

        // Order status methods
        fun getOrderStatusOptions(): Map<String, WCOrderStatusModel>
        fun refreshOrderStatusOptions()
        fun isOrderStatusOptionsRefreshing(): Boolean

        // Shipment tracking
        var isShipmentTrackingProviderFetched: Boolean
        fun loadShipmentTrackingProviders()
    }

    interface View : BaseView<Presenter> {
        var isRefreshPending: Boolean
        var isSearching: Boolean
        var isRefreshing: Boolean

        fun showEmptyView(show: Boolean)
        fun refreshFragmentState()
        fun showOrderDetail(remoteOrderId: Long)

        fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>)
    }
}
