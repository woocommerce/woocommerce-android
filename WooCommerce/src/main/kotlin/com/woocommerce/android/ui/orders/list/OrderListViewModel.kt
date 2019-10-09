package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
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
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class OrderListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val repository: OrderListRepository,
    private val orderStore: WCOrderStore,
    private val listStore: ListStore,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) : ScopedViewModel(mainDispatcher), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private var pagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null
    private val dataSource by lazy {
        OrderListItemDataSource(dispatcher, orderStore, lifecycle)
    }

    private var isStarted = false
    private var isRefreshPending = true

    private val _pagedListData = MediatorLiveData<PagedList<OrderListItemUIType>>()
    val pagedListData: LiveData<PagedList<OrderListItemUIType>> = _pagedListData

    private val _isLoadingMore = MediatorLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isFetchingFirstPage = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _orderStatusOptions = MutableLiveData<Map<String, WCOrderStatusModel>>()
    val orderStatusOptions: LiveData<Map<String, WCOrderStatusModel>> = _orderStatusOptions

    private val _scrollToPosition = SingleLiveEvent<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // TODO AMANDA: Add empty UI State live data

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

    fun loadList(statusFilter: String? = null, searchQuery: String? = null) {
        val listDescriptor = WCOrderListDescriptor(selectedSite.get(), statusFilter, searchQuery)

        clearLiveDataSources()
        // TODO AMANDA - listen for empty state live data

        val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)

        _pagedListData.addSource(pagedListWrapper.data) { pagedList ->
            pagedList?.let {
                if (isDataDeliverable()) {
                    _pagedListData.value = it
                }
            }
        }
        _isFetchingFirstPage.addSource(pagedListWrapper.isFetchingFirstPage) {
            // TODO AMANDA - add search logic
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
        fetchFirstPage()
    }

    /**
     * Refresh the order list with fresh data from the API as well as refresh order status
     * options if the network is available.
     */
    fun fetchFirstPage() {
        if (networkStatus.isConnected()) {
            launch {
                pagedListWrapper?.fetchFirstPage()

                // Fetch and load order status options
                when (repository.fetchOrderStatusOptionsFromApi()) {
                    RequestResult.SUCCESS -> _orderStatusOptions.value = repository.getCachedOrderStatusOptions()
                    else -> { /* do nothing */ }
                }
            }
        } else {
            isRefreshPending = true
            showOfflineSnack()
        }
    }

    fun reloadListFromCache() {
        pagedListWrapper?.invalidateData()
    }

    private fun isDataDeliverable(): Boolean {
        // FIXME AMANDA: if searching, make sure isFetchingFirstPage.value != null && is false
        return true
    }

    private fun clearLiveDataSources() {
        pagedListWrapper?.let {
            _pagedListData.removeSource(it.data)
            _isFetchingFirstPage.removeSource(it.isFetchingFirstPage)
            _isLoadingMore.removeSource(it.isLoadingMore)
        }
    }

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
            UPDATE_ORDER_STATUS -> pagedListWrapper?.invalidateData()
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
        }
    }
}
