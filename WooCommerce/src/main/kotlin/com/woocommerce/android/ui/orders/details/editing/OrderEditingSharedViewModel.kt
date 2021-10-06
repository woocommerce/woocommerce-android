package com.woocommerce.android.ui.orders.details.editing

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@HiltViewModel
class OrderEditingSharedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    orderStore: WCOrderStore,
    selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()
    private val order: WCOrderModel

    private val orderId: Long
        get() = navArgs.orderId.toLong()

    val customerOrderNote: String
        get() = order.customerNote

    init {
        order = orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))!!
    }
}
