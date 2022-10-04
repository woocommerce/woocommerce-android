@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.orders.list

import android.os.Parcelable
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.filters.domain.GetSelectedOrderFiltersCount
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.OpenPurchaseCardReaderLink
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.ui.payments.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.ui.payments.banner.BannerState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.LandscapeChecker
import com.woocommerce.android.util.ThrottleLiveData
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okio.utf8Size
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderSummariesFetched
import org.wordpress.android.mediapicker.util.filter
import javax.inject.Inject

private const val EMPTY_VIEW_THROTTLE = 250L

// Small delay before triggering the glance animation event
private const val DELAY_GLANCE_DURATION = 500L
typealias PagedOrdersList = PagedList<OrderListItemUIType>

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LeakingThis")
@HiltViewModel
class OrderListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderListRepository: OrderListRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderStore: WCOrderStore,
    private val listStore: ListStore,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val fetcher: WCOrderFetcher,
    private val resourceProvider: ResourceProvider,
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
    private val getSelectedOrderFiltersCount: GetSelectedOrderFiltersCount,
    private val bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker,
    private val orderListTransactionLauncher: OrderListTransactionLauncher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val landscapeChecker: LandscapeChecker,
) : ScopedViewModel(savedState), LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    val performanceObserver: LifecycleObserver = orderListTransactionLauncher

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    internal var ordersPagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null
    internal var activePagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null

    private val dataSource by lazy {
        OrderListItemDataSource(dispatcher, orderStore, networkStatus, fetcher, resourceProvider)
    }

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState(filterCount = getSelectedOrderFiltersCount()))
    internal var viewState by viewStateLiveData

    private val _pagedListData = MediatorLiveData<PagedOrdersList>()
    val pagedListData: LiveData<PagedOrdersList>
        get() {
            return _pagedListData.map {
                viewModelScope.launch {
                    updateBannerState(landscapeChecker, _pagedListData.value.isNullOrEmpty())
                }
                it
            }
        }

    private val _isLoadingMore = MediatorLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isFetchingFirstPage = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage.map {
        if (it == false) {
            orderListTransactionLauncher.onListFetched()
        }
        it
    }

    private val _orderStatusOptions = MutableLiveData<Map<String, WCOrderStatusModel>>()
    val orderStatusOptions: LiveData<Map<String, WCOrderStatusModel>> = _orderStatusOptions

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _emptyViewType: ThrottleLiveData<EmptyViewType?> by lazy {
        ThrottleLiveData(
            offset = EMPTY_VIEW_THROTTLE,
            coroutineScope = this,
            mainDispatcher = dispatchers.main,
            backgroundDispatcher = dispatchers.computation
        )
    }
    val emptyViewType: LiveData<EmptyViewType?> = _emptyViewType

    val shouldShowUpsellCardReaderDismissDialog: MutableLiveData<Boolean> = MutableLiveData(false)
    val bannerState: MutableLiveData<BannerState> = MutableLiveData()

    var isSearching = false
    private var dismissListErrors = false
    var searchQuery = ""

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        EventBus.getDefault().register(this)
        dispatcher.register(this)

        launch {
            // Populate any cached order status options immediately since we use this
            // value in many different places in the order list view.
            _orderStatusOptions.value = orderListRepository.getCachedOrderStatusOptions()

            _emptyViewType.postValue(EmptyViewType.ORDER_LIST_LOADING)
            if (selectedSite.exists()) {
                loadOrders()
            } else {
                WooLog.w(
                    WooLog.T.ORDERS,
                    "Order list can't fetch site plugins, no selected site " +
                        "- siteId ${selectedSite.getSelectedSiteId()}$"
                )
            }
        }

        shouldGlanceFirstSwipeAbleItem()
    }

    private fun shouldGlanceFirstSwipeAbleItem() {
        isFetchingFirstPage.observe(this) { isFetching ->
            if (!isFetching && InAppLifecycleMemory.shouldGlanceFirstSwipeAbleItem) {
                launch {
                    // Wait for the list to be draw
                    delay(DELAY_GLANCE_DURATION)
                    InAppLifecycleMemory.shouldGlanceFirstSwipeAbleItem = false
                    triggerEvent(OrderListEvent.GlanceFirstSwipeAbleItem)
                }
            }
        }
    }

    private suspend fun shouldDisplayBanner(isLandScape: Boolean, isOrderListEmpty: Boolean): Boolean {
        return bannerDisplayEligibilityChecker.isEligibleForInPersonPayments() &&
            canShowCardReaderUpsellBanner(System.currentTimeMillis()) &&
            !isLandScape &&
            !isOrderListEmpty
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun updateBannerState(landscapeChecker: LandscapeChecker, isOrdersListEmpty: Boolean) {
        bannerState.value = BannerState(
            shouldDisplayBanner = shouldDisplayBanner(landscapeChecker.isLandscape(), isOrdersListEmpty),
            onPrimaryActionClicked = {
                onCtaClicked(AnalyticsTracker.KEY_BANNER_ORDER_LIST)
            },
            onDismissClicked = { onDismissClicked() },
            title = R.string.card_reader_upsell_card_reader_banner_title,
            description = R.string.card_reader_upsell_card_reader_banner_description,
            primaryActionLabel = R.string.card_reader_upsell_card_reader_banner_cta,
            chipLabel = R.string.card_reader_upsell_card_reader_banner_new
        )
        trackBannerShownIfDisplayed()
    }

    private fun trackBannerShownIfDisplayed() {
        if (bannerState.value?.shouldDisplayBanner == true) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.FEATURE_CARD_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_BANNER_SOURCE to AnalyticsTracker.KEY_BANNER_ORDER_LIST,
                    AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS
                )
            )
        }
    }

    fun loadOrders() {
        ordersPagedListWrapper = listStore.getList(getWCOrderListDescriptorWithFilters(), dataSource, lifecycle)
        viewState = viewState.copy(filterCount = getSelectedOrderFiltersCount())
        activatePagedListWrapper(ordersPagedListWrapper!!)
        fetchOrdersAndOrderDependencies()
    }

    /**
     * Creates and activates a new list with the search and filter params provided. This should only be used
     * by the search component portion of the order list view.
     *
     * NOTE: Although technically the "PROCESSING" tab is a filtered list, it should not use this method. The
     * processing list will always use the same [processingPagedListWrapper].
     */
    fun submitSearchOrFilter(searchQuery: String) {
        val listDescriptor = WCOrderListDescriptor(
            selectedSite.get(),
            searchQuery = sanitizeSearchQuery(searchQuery)
        )
        val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)
        activatePagedListWrapper(pagedListWrapper, isFirstInit = true)
    }

    /**
     * Removes the `#` from the start of the search keyword, if present.
     *
     *  This allows searching for an order with `#123` and getting the results for order `123`.
     *  See https://github.com/woocommerce/woocommerce-android/issues/2621
     *
     */
    private fun sanitizeSearchQuery(searchQuery: String): String {
        if (searchQuery.startsWith("#")) {
            return searchQuery.drop(1)
        }
        return searchQuery
    }

    /**
     * Refresh the active order list with fresh data from the API as well as refresh order status
     * options and payment gateways if the network is available.
     */
    fun fetchOrdersAndOrderDependencies() {
        if (networkStatus.isConnected()) {
            launch(dispatchers.main) {
                activePagedListWrapper?.fetchFirstPage()
                fetchOrderStatusOptions()
                fetchPaymentGateways()
            }
        } else {
            viewState = viewState.copy(isRefreshPending = true)
            showOfflineSnack()
        }
    }

    /**
     * Fetch payment gateways so they are available for order refunds later
     */
    suspend fun fetchPaymentGateways() {
        if (networkStatus.isConnected() && !viewState.arePaymentGatewaysFetched) {
            when (orderListRepository.fetchPaymentGateways()) {
                SUCCESS -> {
                    viewState = viewState.copy(arePaymentGatewaysFetched = true)
                }
                else -> {
                    /* do nothing */
                }
            }
        }
    }

    /**
     * Refresh the order count by order status list with fresh data from the API
     */
    fun fetchOrderStatusOptions() {
        launch(dispatchers.main) {
            // Fetch and load order status options
            when (orderListRepository.fetchOrderStatusOptionsFromApi()) {
                SUCCESS -> _orderStatusOptions.value = orderListRepository.getCachedOrderStatusOptions()
                else -> { /* do nothing */
                }
            }
        }
    }

    /**
     * Track user clicked to open an order and the status of that order, along with some
     * data about the order custom fields
     */
    fun trackOrderClickEvent(orderId: Long, orderStatus: String) = launch {
        val (customFieldsCount, customFieldsSize) =
            orderDetailRepository.getOrderMetadata(orderId)
                .map { it.value.utf8Size() }
                .let {
                    Pair(
                        // amount of custom fields in the order
                        it.size,
                        // total size in bytes of all custom fields in the order
                        if (it.isEmpty()) 0 else it.reduce(Long::plus)
                    )
                }

        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_OPEN,
            mapOf(
                AnalyticsTracker.KEY_ID to orderId,
                AnalyticsTracker.KEY_STATUS to orderStatus,
                AnalyticsTracker.KEY_CUSTOM_FIELDS_COUNT to customFieldsCount,
                AnalyticsTracker.KEY_CUSTOM_FIELDS_SIZE to customFieldsSize
            )
        )
    }

    /**
     * Activates the provided list by first removing the LiveData sources for the active list,
     * then creating new LiveData sources for the provided [pagedListWrapper] and setting it as
     * the active list. If [isFirstInit] is true, then this [pagedListWrapper] is freshly created
     * so we'll need to call `fetchOrdersAndOrderDependencies` to initialize it.
     */
    private fun activatePagedListWrapper(
        pagedListWrapper: PagedListWrapper<OrderListItemUIType>,
        isFirstInit: Boolean = false
    ) {
        // Clear any of the data sources assigned to the current wrapper, then
        // create a new one.
        clearLiveDataSources(this.activePagedListWrapper)

        listenToEmptyViewStateLiveData(pagedListWrapper)

        _pagedListData.addSource(pagedListWrapper.data) { pagedList ->
            pagedList?.let {

                _pagedListData.value = it
            }
        }
        _isFetchingFirstPage.addSource(pagedListWrapper.isFetchingFirstPage) {
            _isFetchingFirstPage.value = it
        }
        _isEmpty.addSource(pagedListWrapper.isEmpty) {
            _isEmpty.value = it
        }
        _isLoadingMore.addSource(pagedListWrapper.isLoadingMore) {
            _isLoadingMore.value = it
        }

        pagedListWrapper.listError.filter { error ->
            !dismissListErrors && error != null
        }.observe(this) {
            triggerEvent(ShowErrorSnack(R.string.orderlist_error_fetch_generic))
        }
        this.activePagedListWrapper = pagedListWrapper

        if (isFirstInit) {
            fetchOrdersAndOrderDependencies()
        } else {
            pagedListWrapper.invalidateData()
        }
    }

    private fun clearLiveDataSources(pagedListWrapper: PagedListWrapper<OrderListItemUIType>?) {
        pagedListWrapper?.apply {
            _pagedListData.removeSource(data)
            _emptyViewType.removeSource(pagedListData)
            _emptyViewType.removeSource(isEmpty)
            _emptyViewType.removeSource(listError)
            _emptyViewType.removeSource(isFetchingFirstPage)
            _isEmpty.removeSource(isEmpty)
            _isFetchingFirstPage.removeSource(isFetchingFirstPage)
            _isLoadingMore.removeSource(isLoadingMore)
        }
    }

    /**
     * Builds the function for handling empty view state scenarios and links the various [LiveData] feeds as
     * a source for the [_emptyViewType] LivData object.
     */
    private fun listenToEmptyViewStateLiveData(wrapper: PagedListWrapper<OrderListItemUIType>) {
        _emptyViewType.addSource(wrapper.isEmpty) { createAndPostEmptyViewType(wrapper) }
        _emptyViewType.addSource(wrapper.isFetchingFirstPage) { createAndPostEmptyViewType(wrapper) }
        _emptyViewType.addSource(wrapper.listError) { createAndPostEmptyViewType(wrapper) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createAndPostEmptyViewType(wrapper: PagedListWrapper<OrderListItemUIType>) {
        val isLoadingData = wrapper.isFetchingFirstPage.value ?: false ||
            wrapper.data.value == null
        val isListEmpty = wrapper.isEmpty.value ?: true
        val isError = wrapper.listError.value != null

        val newEmptyViewType: EmptyViewType? = if (isListEmpty) {
            when {
                isError -> EmptyViewType.NETWORK_ERROR
                isLoadingData -> {
                    // don't show intermediate screen when loading search results
                    if (isSearching) {
                        null
                    } else {
                        EmptyViewType.ORDER_LIST_LOADING
                    }
                }
                isSearching && searchQuery.isNotEmpty() -> EmptyViewType.SEARCH_RESULTS
                viewState.filterCount > 0 -> EmptyViewType.ORDER_LIST_FILTERED
                else -> {
                    if (networkStatus.isConnected()) {
                        EmptyViewType.ORDER_LIST
                    } else {
                        EmptyViewType.NETWORK_OFFLINE
                    }
                }
            }
        } else {
            null
        }
        _emptyViewType.postValue(newEmptyViewType)
    }

    private fun showOfflineSnack() {
        // Network is not connected
        triggerEvent(ShowErrorSnack(R.string.offline_error))
    }

    override fun onCleared() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        clearLiveDataSources(activePagedListWrapper)
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
        orderListRepository.onCleanup()
        orderListTransactionLauncher.clear()
        super.onCleared()
    }

    @Suppress("unused", "DEPRECATION")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> activePagedListWrapper?.fetchFirstPage()
            else -> {
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            if (viewState.isRefreshPending) {
                if (isSearching) {
                    activePagedListWrapper?.fetchFirstPage()
                }
                ordersPagedListWrapper?.fetchFirstPage()
            }
        } else {
            // Invalidate the list data so that orders that have not
            // yet been downloaded (the "loading" items) can be removed
            // from the current list view.
            if (isSearching) {
                activePagedListWrapper?.invalidateData()
            }
            ordersPagedListWrapper?.invalidateData()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationReceived(event: NotificationReceivedEvent) {
        if (event.channel == NotificationChannelType.NEW_ORDER && isSearching) {
            activePagedListWrapper?.fetchFirstPage()
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderSummariesFetched(event: OnOrderSummariesFetched) {
        // Only track if this is not from a search query
        if (!event.listDescriptor.searchQuery.isNullOrEmpty()) {
            return
        }

        val totalDurationInSeconds = event.duration.toDouble() / 1_000
        AnalyticsTracker.track(
            AnalyticsEvent.ORDERS_LIST_LOADED,
            mapOf(
                AnalyticsTracker.KEY_TOTAL_DURATION to totalDurationInSeconds,
                AnalyticsTracker.KEY_STATUS to event.listDescriptor.statusFilter
            )
        )
    }

    fun onFiltersButtonTapped() {
        AnalyticsTracker.track(AnalyticsEvent.ORDERS_LIST_VIEW_FILTER_OPTIONS_TAPPED)
        triggerEvent(ShowOrderFilters)
    }

    fun onSearchClosed() {
        loadOrders()
    }

    private fun onCtaClicked(source: String) {
        launch {
            triggerEvent(
                OpenPurchaseCardReaderLink(bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(source))
            )
        }
    }

    private fun onDismissClicked() {
        shouldShowUpsellCardReaderDismissDialog.value = true
        triggerEvent(OrderListEvent.DismissCardReaderUpsellBanner)
    }

    fun onRemindLaterClicked(currentTimeInMillis: Long, source: String) {
        shouldShowUpsellCardReaderDismissDialog.value = false
        bannerDisplayEligibilityChecker.onRemindLaterClicked(currentTimeInMillis, source)
        triggerEvent(OrderListEvent.DismissCardReaderUpsellBannerViaRemindMeLater)
    }

    fun onDontShowAgainClicked(source: String) {
        shouldShowUpsellCardReaderDismissDialog.value = false
        bannerDisplayEligibilityChecker.onDontShowAgainClicked(source)
        triggerEvent(OrderListEvent.DismissCardReaderUpsellBannerViaDontShowAgain)
    }

    fun onBannerAlertDismiss() {
        shouldShowUpsellCardReaderDismissDialog.value = false
    }

    private fun canShowCardReaderUpsellBanner(currentTimeInMillis: Long): Boolean {
        return bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(currentTimeInMillis)
    }

    fun shouldDisplaySimplePaymentsWIPCard(): Boolean {
        return !canShowCardReaderUpsellBanner(System.currentTimeMillis())
    }

    private fun updateOrderDisplayedStatus(position: Int, status: String) {
        val pagedList = _pagedListData.value ?: return
        (pagedList[position] as OrderListItemUIType.OrderListItemUI).status = status
        triggerEvent(OrderListEvent.NotifyOrderChanged(position))
    }

    fun onSwipeStatusUpdate(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture) {
        dismissListErrors = true

        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_STATUS_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_ID to gestureSource.orderId,
                AnalyticsTracker.KEY_FROM to gestureSource.oldStatus,
                AnalyticsTracker.KEY_TO to gestureSource.newStatus,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_LIST
            )
        )

        optimisticUpdateOrderStatus(
            orderId = gestureSource.orderId,
            status = gestureSource.newStatus,
            onOptimisticSuccess = { swipeStatusUpdateOptimisticSuccess(gestureSource) },
            onFail = { swipeStatusUpdateFails(gestureSource) }
        )
    }

    private fun swipeStatusUpdateOptimisticSuccess(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture) {
        val dismissAction = object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (event != DISMISS_EVENT_ACTION && event != DISMISS_EVENT_CONSECUTIVE) {
                    dismissListErrors = false
                }
            }
        }

        updateOrderDisplayedStatus(gestureSource.orderPosition, gestureSource.newStatus)
        triggerEvent(
            Event.ShowUndoSnackbar(
                message = resourceProvider.getString(
                    R.string.orderlist_mark_completed_success,
                    gestureSource.orderId
                ),
                undoAction = { undoSwipeStatusUpdate(gestureSource) },
                dismissAction = dismissAction
            )
        )
    }

    private fun swipeStatusUpdateFails(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture) {
        triggerEvent(OrderListEvent.NotifyOrderChanged(gestureSource.orderPosition))
        triggerEvent(
            OrderListEvent.ShowRetryErrorSnack(
                message = resourceProvider.getString(
                    R.string.orderlist_updating_order_error,
                    gestureSource.orderId
                ),
                retry = { onSwipeStatusUpdate(gestureSource) }
            )
        )
    }

    private fun undoSwipeStatusUpdate(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture) {
        dismissListErrors = true
        optimisticUpdateOrderStatus(
            orderId = gestureSource.orderId,
            status = gestureSource.oldStatus,
            onOptimisticSuccess = {
                updateOrderDisplayedStatus(gestureSource.orderPosition, gestureSource.oldStatus)
            },
            onFail = {
                triggerEvent(OrderListEvent.NotifyOrderChanged(gestureSource.orderPosition))
                triggerEvent(
                    OrderListEvent.ShowRetryErrorSnack(
                        message = resourceProvider.getString(
                            R.string.orderlist_updating_order_error,
                            gestureSource.orderId
                        ),
                        retry = { undoSwipeStatusUpdate(gestureSource) }
                    )
                )
            },
            onSuccess = {
                dismissListErrors = false
            }
        )
    }

    private fun optimisticUpdateOrderStatus(
        orderId: Long,
        status: String,
        onOptimisticSuccess: () -> Unit = {},
        onFail: () -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        launch {
            orderDetailRepository.updateOrderStatus(orderId, status).collect { result ->
                when {
                    result is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> onOptimisticSuccess()
                    result is WCOrderStore.UpdateOrderResult.RemoteUpdateResult && result.event.isError -> onFail()
                    result is WCOrderStore.UpdateOrderResult.RemoteUpdateResult && !result.event.isError -> onSuccess()
                }
            }
        }
    }

    sealed class OrderListEvent : Event() {
        data class ShowErrorSnack(@StringRes val messageRes: Int) : OrderListEvent()
        object ShowOrderFilters : OrderListEvent()
        object DismissCardReaderUpsellBanner : OrderListEvent()
        object DismissCardReaderUpsellBannerViaRemindMeLater : OrderListEvent()
        object DismissCardReaderUpsellBannerViaDontShowAgain : OrderListEvent()
        data class OpenPurchaseCardReaderLink(val url: String) : OrderListEvent()
        data class ShowRetryErrorSnack(
            val message: String,
            val retry: View.OnClickListener
        ) : OrderListEvent()

        data class NotifyOrderChanged(val position: Int) : OrderListEvent()
        object GlanceFirstSwipeAbleItem : OrderListEvent()
    }

    @Parcelize
    data class ViewState(
        val isRefreshPending: Boolean = false,
        val arePaymentGatewaysFetched: Boolean = false,
        val filterCount: Int = 0
    ) : Parcelable
}

object InAppLifecycleMemory {
    var shouldGlanceFirstSwipeAbleItem = true
}
