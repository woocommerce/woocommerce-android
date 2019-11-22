package com.woocommerce.android.ui.orders.list

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.push.NotificationHandler.NotificationReceivedEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.ThrottleLiveData
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.FETCH_NOTIFICATION
import org.wordpress.android.fluxc.action.NotificationAction.UPDATE_NOTIFICATION
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import java.util.Locale

private const val EMPTY_VIEW_THROTTLE = 250L
typealias PagedOrdersList = PagedList<OrderListItemUIType>

@OpenClassOnDebug
class OrderListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    protected val repository: OrderListRepository,
    private val orderStore: WCOrderStore,
    private val listStore: ListStore,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState, dispatchers), LifecycleOwner {
    protected val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var pagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null

    private val dataSource by lazy {
        OrderListItemDataSource(dispatcher, orderStore, networkStatus, lifecycle)
    }

    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var isStarted = false

    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var isRefreshPending = true

    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var arePaymentGatewaysFetched = false

    protected val _pagedListData = MediatorLiveData<PagedOrdersList>()
    val pagedListData: LiveData<PagedOrdersList> = _pagedListData

    private val _isLoadingMore = MediatorLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isFetchingFirstPage = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _orderStatusOptions = MutableLiveData<Map<String, WCOrderStatusModel>>()
    val orderStatusOptions: LiveData<Map<String, WCOrderStatusModel>> = _orderStatusOptions

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _emptyViewState: ThrottleLiveData<OrderListEmptyUiState> by lazy {
        ThrottleLiveData<OrderListEmptyUiState>(
                offset = EMPTY_VIEW_THROTTLE,
                coroutineScope = this,
                mainDispatcher = dispatchers.main,
                backgroundDispatcher = dispatchers.computation)
    }
    val emptyViewState: LiveData<OrderListEmptyUiState> = _emptyViewState

    var isSearching = false
    var searchQuery = ""
    var orderStatusFilter = ""

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true
        lifecycleRegistry.markState(Lifecycle.State.STARTED)

        EventBus.getDefault().register(this)
        dispatcher.register(this)

        launch {
            // Populate any cached order status options immediately since we use this
            // value in many different places in the order list view.
            _orderStatusOptions.value = repository.getCachedOrderStatusOptions()
        }
    }

    fun loadList(statusFilter: String? = null, searchQuery: String? = null, excludeFutureOrders: Boolean = false) {
        val listDescriptor = WCOrderListDescriptor(selectedSite.get(), statusFilter, searchQuery, excludeFutureOrders)

        // Clear any of the data sources assigned to the current wrapper, then
        // create a new one.
        pagedListWrapper?.let { clearLiveDataSources(it) }
        val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)

        listenToEmptyViewStateLiveData(pagedListWrapper)

        _pagedListData.addSource(pagedListWrapper.data) { pagedList ->
            pagedList?.let {
                if (isSearchResultDeliverable(pagedListWrapper)) {
                    _pagedListData.value = it
                }
            }
        }
        _isFetchingFirstPage.addSource(pagedListWrapper.isFetchingFirstPage) {
            _isFetchingFirstPage.value = it
        }
        _isLoadingMore.addSource(pagedListWrapper.isLoadingMore) {
            _isLoadingMore.value = it
        }
        _isEmpty.addSource(pagedListWrapper.isEmpty) {
            _isEmpty.value = it
        }

        pagedListWrapper.listError.observe(this, Observer {
            it?.let {
                _showSnackbarMessage.value = R.string.orderlist_error_fetch_generic
            }
        })

        this.pagedListWrapper = pagedListWrapper
        fetchOrdersAndOrderDependencies()
    }

    /**
     * Refresh the order list with fresh data from the API as well as refresh order status
     * options and payment gateways if the network is available.
     */
    fun fetchOrdersAndOrderDependencies() {
        if (networkStatus.isConnected()) {
            launch {
                pagedListWrapper?.fetchFirstPage()
                fetchOrderStatusOptions()
                fetchPaymentGateways()
            }
        } else {
            isRefreshPending = true
            showOfflineSnack()
        }
    }

    /**
     * Refresh the order count by order status list with fresh data from the API
     */
    fun fetchOrderStatusOptions() {
        launch {
            // Fetch and load order status options
            when (repository.fetchOrderStatusOptionsFromApi()) {
                SUCCESS -> _orderStatusOptions.value = repository.getCachedOrderStatusOptions()
                else -> { /* do nothing */ }
            }
        }
    }

    /**
     * Fetch payment gateways so they are available for order refunds later
     */
    fun fetchPaymentGateways() {
        launch {
            if (networkStatus.isConnected() && !arePaymentGatewaysFetched) {
                when (repository.fetchPaymentGateways()) {
                    SUCCESS -> arePaymentGatewaysFetched = true
                    else -> { /* do nothing */ }
                }
            }
        }
    }

    /**
     * Reload the orders list with the database available in the database. This is the ideal way to
     * load changes to orders that were initiated from within this app instance. If the change was
     * successfully pushed to the API, then the database would already be updated so there is no
     * need to hit the API again.
     */
    fun reloadListFromCache() {
        pagedListWrapper?.invalidateData()
    }

    /**
     * Used to filter out dataset changes that might trigger an empty view when performing a search.
     *
     * @return True if the user is either not currently in search mode, or if they are there is already data
     * available so the view can safely be updated.
     */
    private fun isSearchResultDeliverable(pagedListWrapper: PagedListWrapper<OrderListItemUIType>): Boolean {
        return !isSearching ||
                (isSearching &&
                        pagedListWrapper.isFetchingFirstPage.value != null &&
                        isFetchingFirstPage.value == false)
    }

    private fun clearLiveDataSources(pagedListWrapper: PagedListWrapper<OrderListItemUIType>) {
        with(pagedListWrapper) {
            _pagedListData.removeSource(data)
            _emptyViewState.removeSource(pagedListData)
            _emptyViewState.removeSource(isEmpty)
            _emptyViewState.removeSource(listError)
            _emptyViewState.removeSource(isFetchingFirstPage)
            _isFetchingFirstPage.removeSource(isFetchingFirstPage)
            _isLoadingMore.removeSource(isLoadingMore)
        }
    }

    /**
     * Builds the function for handling empty view state scenarios and links the various [LiveData] feeds as
     * a source for the [_emptyViewState] LivData object.
     */
    private fun listenToEmptyViewStateLiveData(wrapper: PagedListWrapper<OrderListItemUIType>) {
        _emptyViewState.addSource(wrapper.isEmpty) { createAndPostEmptyUiState(wrapper) }
        _emptyViewState.addSource(wrapper.isFetchingFirstPage) { createAndPostEmptyUiState(wrapper) }
        _emptyViewState.addSource(wrapper.listError) { createAndPostEmptyUiState(wrapper) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createAndPostEmptyUiState(wrapper: PagedListWrapper<OrderListItemUIType>) {
        val listType = when {
            isSearching -> OrderListType.SEARCH
            isShowingProcessingTab() -> OrderListType.PROCESSING
            else -> OrderListType.ALL
        }
        val emptyView = createEmptyUiState(
                orderListType = listType,
                isNetworkAvailable = networkStatus.isConnected(),
                isLoadingData = wrapper.isFetchingFirstPage.value ?: false ||
                        wrapper.data.value == null,
                isListEmpty = wrapper.isEmpty.value ?: true,
                hasOrders = repository.hasCachedOrdersForSite(),
                isError = wrapper.listError.value != null,
                fetchFirstPage = this::fetchOrdersAndOrderDependencies)
        _emptyViewState.postValue(emptyView)
    }

    private fun isShowingProcessingTab() = orderStatusFilter.isNotEmpty() &&
            orderStatusFilter.toLowerCase(Locale.ROOT) == CoreOrderStatus.PROCESSING.value

    private fun showOfflineSnack() {
        // Network is not connected
        _showSnackbarMessage.value = R.string.offline_error
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
        repository.onCleanup()
        super.onCleared()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        when (event.causeOfChange) {
            FETCH_NOTIFICATION, UPDATE_NOTIFICATION -> {
                // A notification was received by the device and the details have been fetched from the API.
                // Refresh the orders list in case that notification was a new order notification.
                if (!event.isError) {
                    pagedListWrapper?.invalidateData()
                }
            }
            else -> {}
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> pagedListWrapper?.fetchFirstPage()
            else -> {}
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            if (isRefreshPending) {
                pagedListWrapper?.fetchFirstPage()
            }
        } else {
            // Invalidate the list data so that orders that have not
            // yet been downloaded (the "loading" items) can be removed
            // from the current list view.
            pagedListWrapper?.invalidateData()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationReceivedEvent) {
        // a new order notification came in so refresh the active order list
        if (event.channel == NEW_ORDER) {
            pagedListWrapper?.fetchFirstPage()
        }
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<OrderListViewModel>
}
