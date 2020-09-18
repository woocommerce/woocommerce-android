package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdSet
import org.wordpress.android.fluxc.model.order.toIdSet

@OpenClassOnDebug
class OrderDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()

    private val orderIdSet: OrderIdSet
        get() = navArgs.orderId.toIdSet()

    final val orderDetailViewStateData = LiveDataDelegate(savedState, OrderDetailViewState())
    private var orderDetailViewState by orderDetailViewStateData

    private val _orderNotes = MutableLiveData<List<OrderNote>>()
    val orderNotes: LiveData<List<OrderNote>> = _orderNotes

    override fun onCleared() {
        super.onCleared()
        orderDetailRepository.onCleanup()
    }

    fun loadOrderDetail() {
        launch {
            orderDetailRepository.getOrder(navArgs.orderId)?.let { orderInDb ->
                updateOrderState(orderInDb)
                loadOrderNotes()
            } ?: fetchOrder()
        }
    }

    fun hasVirtualProductsOnly(): Boolean {
        return orderDetailViewState.order?.items?.let { lineItems ->
            val remoteProductIds = lineItems.map { it.productId }
            orderDetailRepository.getProductsByRemoteIds(remoteProductIds).any { it.virtual }
        } ?: false
    }

    private suspend fun fetchOrder() {
        if (networkStatus.isConnected()) {
            orderDetailViewState = orderDetailViewState.copy(isOrderDetailSkeletonShown = true)
            val fetchedOrder = orderDetailRepository.fetchOrder(navArgs.orderId)
            if (fetchedOrder != null) {
                updateOrderState(fetchedOrder)
                loadOrderNotes()
            } else {
                triggerEvent(ShowSnackbar(string.order_error_fetch_generic))
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            orderDetailViewState = orderDetailViewState.copy(isOrderDetailSkeletonShown = false)
        }
    }

    private fun updateOrderState(order: Order) {
        val orderStatus = orderDetailRepository.getOrderStatus(order.status.value)
        orderDetailViewState = orderDetailViewState.copy(
            order = order,
            orderStatus = orderStatus,
            toolbarTitle = resourceProvider.getString(
                string.orderdetail_orderstatus_ordernum, order.number
            )
        )
    }

    private suspend fun loadOrderNotes() {
        orderDetailViewState = orderDetailViewState.copy(isOrderNotesSkeletonShown = true)
        if (!orderDetailRepository.fetchOrderNotes(orderIdSet.id, orderIdSet.remoteOrderId)) {
            triggerEvent(ShowSnackbar(string.order_error_fetch_notes_generic))
        }
        // fetch order notes from the local db and hide the skeleton view
        _orderNotes.value = orderDetailRepository.getOrderNotes(orderIdSet.id)
        orderDetailViewState = orderDetailViewState.copy(isOrderNotesSkeletonShown = false)
    }

    @Parcelize
    data class OrderDetailViewState(
        val order: Order? = null,
        val toolbarTitle: String? = null,
        val orderStatus: OrderStatus? = null,
        val isOrderDetailSkeletonShown: Boolean? = null,
        val isOrderNotesSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<OrderDetailViewModel>
}
