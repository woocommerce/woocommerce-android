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
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_LIST_PRODUCT_BARCODE_SCANNING_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IPP_BANNER_CAMPAIGN_NAME
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IPP_BANNER_REMIND_LATER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IPP_BANNER_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_SOURCE_ORDER_LIST
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.filter
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningTracker
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.ScanningSource
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.filters.domain.GetSelectedOrderFiltersCount
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFiltersAndSearchQuery
import com.woocommerce.android.ui.orders.filters.domain.ShouldShowCreateTestOrderScreen
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData
import com.woocommerce.android.ui.payments.feedback.ipp.MarkFeedbackBannerAsDismissed
import com.woocommerce.android.ui.payments.feedback.ipp.MarkFeedbackBannerAsDismissedForever
import com.woocommerce.android.ui.payments.feedback.ipp.MarkIPPFeedbackSurveyAsCompleted
import com.woocommerce.android.ui.payments.feedback.ipp.ShouldShowFeedbackBanner
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.ThrottleLiveData
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import okio.utf8Size
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderSummariesFetched
import javax.inject.Inject

private const val EMPTY_VIEW_THROTTLE = 250L

typealias PagedOrdersList = PagedList<OrderListItemUIType>

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
    private val fetcher: FetchOrdersRepository,
    private val resourceProvider: ResourceProvider,
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
    private val getWCOrderListDescriptorWithFiltersAndSearchQuery: GetWCOrderListDescriptorWithFiltersAndSearchQuery,
    private val getSelectedOrderFiltersCount: GetSelectedOrderFiltersCount,
    private val orderListTransactionLauncher: OrderListTransactionLauncher,
    private val getIPPFeedbackBannerData: GetIPPFeedbackBannerData,
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner,
    private val shouldShowCreateTestOrderScreen: ShouldShowCreateTestOrderScreen,
    private val markFeedbackBannerAsDismissed: MarkFeedbackBannerAsDismissed,
    private val markFeedbackBannerAsDismissedForever: MarkFeedbackBannerAsDismissedForever,
    private val markFeedbackBannerAsCompleted: MarkIPPFeedbackSurveyAsCompleted,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val feedbackPrefs: FeedbackPrefs,
    private val barcodeScanningTracker: BarcodeScanningTracker,
) : ScopedViewModel(savedState), LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val simplePaymentsAndOrderCreationFeedbackState
        get() = feedbackPrefs.getFeatureFeedbackSettings(
            FeatureFeedbackSettings.Feature.SIMPLE_PAYMENTS_AND_ORDER_CREATION
        )?.feedbackState ?: FeatureFeedbackSettings.FeedbackState.UNANSWERED

    val performanceObserver: LifecycleObserver = orderListTransactionLauncher

    internal var ordersPagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null
    internal var activePagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null

    private val dataSource by lazy {
        OrderListItemDataSource(dispatcher, orderStore, networkStatus, fetcher, resourceProvider)
    }

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState(filterCount = getSelectedOrderFiltersCount()))
    internal var viewState by viewStateLiveData

    private val _pagedListData = MediatorLiveData<PagedOrdersList>()
    val pagedListData: LiveData<PagedOrdersList> = _pagedListData

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

    var isSearching = false
    private var dismissListErrors = false
    var searchQuery = ""

    private val isSimplePaymentsAndOrderCreationFeedbackVisible: Boolean
        get() {
            val simplePaymentsAndOrderFeedbackDismissed =
                simplePaymentsAndOrderCreationFeedbackState == FeatureFeedbackSettings.FeedbackState.DISMISSED
            val isIPPSurveyFeedbackHidden = viewState.ippFeedbackBannerState is IPPSurveyFeedbackBannerState.Hidden
            return isIPPSurveyFeedbackHidden && !simplePaymentsAndOrderFeedbackDismissed
        }

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

        displayIPPFeedbackOrOrdersBannerOrJitm()
    }

    fun loadOrders() {
        ordersPagedListWrapper = listStore.getList(getWCOrderListDescriptorWithFilters(), dataSource, lifecycle)
        viewState = viewState.copy(
            filterCount = getSelectedOrderFiltersCount(),
            isErrorFetchingDataBannerVisible = false
        )
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
        val listDescriptor = getWCOrderListDescriptorWithFiltersAndSearchQuery(sanitizeSearchQuery(searchQuery))
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
            viewState = viewState.copy(isErrorFetchingDataBannerVisible = false)
            launch(dispatchers.main) {
                activePagedListWrapper?.fetchFirstPage()
                fetchOrderStatusOptions()
                fetchPaymentGateways()
            }
        } else {
            viewState = viewState.copy(isRefreshPending = true, isErrorFetchingDataBannerVisible = false)
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

    fun onScanClicked() {
        trackScanClickedEvent()
        triggerEvent(OrderListEvent.OpenBarcodeScanningFragment)
    }

    private fun trackScanClickedEvent() {
        analyticsTracker.track(ORDER_LIST_PRODUCT_BARCODE_SCANNING_TAPPED)
    }

    fun handleBarcodeScannedStatus(status: CodeScannerStatus) {
        when (status) {
            is CodeScannerStatus.Failure -> {
                barcodeScanningTracker.trackScanFailure(
                    ScanningSource.ORDER_LIST,
                    status.type
                )
                triggerEvent(
                    OrderListEvent.OnAddingProductViaScanningFailed(
                        R.string.order_list_barcode_scanning_scanning_failed
                    ) {
                        triggerEvent(OrderListEvent.OpenBarcodeScanningFragment)
                    }
                )
            }
            is CodeScannerStatus.Success -> {
                barcodeScanningTracker.trackSuccess(ScanningSource.ORDER_LIST)
                triggerEvent(
                    OrderListEvent.OnBarcodeScanned(status.code, status.format)
                )
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
                // We are re-checking the ipp feedback survey banner logic on every order fetch
                // from the API. This is primarily being done because when the app migrates
                // WCDatabase from v22 to v23, we clear all the orders in the database and fetch
                // it freshly. After the update, on the very first launch, the ipp feedback survey banner logic
                // returns that there are no IPP orders since the database is not filled with any orders
                // (The API call happens after the database check). After the API returns, we re-check the logic
                // so that the database is populated by now and we can show the correct banner.
                // This also helps in updating the feedback survey banner according to the order changes
                // on pull-to-refresh.
                displayIPPFeedbackOrOrdersBannerOrJitm()
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

        pagedListWrapper.listError
            .filter { !dismissListErrors }
            .filterNotNull()
            .observe(this) { error ->
                if (error.type == ListStore.ListErrorType.PARSE_ERROR) {
                    viewState = viewState.copy(
                        isErrorFetchingDataBannerVisible = true,
                        ippFeedbackBannerState = IPPSurveyFeedbackBannerState.Hidden,
                        isSimplePaymentsAndOrderCreationFeedbackVisible = false
                    )
                } else {
                    triggerEvent(ShowErrorSnack(R.string.orderlist_error_fetch_generic))
                }
            }
        this.activePagedListWrapper = pagedListWrapper

        if (isFirstInit) {
            fetchOrdersAndOrderDependencies()
        } else {
            pagedListWrapper.invalidateData()
        }
    }

    private fun displayIPPFeedbackOrOrdersBannerOrJitm() {
        viewModelScope.launch {
            val bannerData = getIPPFeedbackBannerData()
            when {
                shouldShowFeedbackBanner() && bannerData != null -> {
                    viewState = viewState.copy(
                        ippFeedbackBannerState = IPPSurveyFeedbackBannerState.Visible(bannerData)
                    )
                    trackIPPBannerEvent(AnalyticsEvent.IPP_FEEDBACK_BANNER_SHOWN)
                }
                !isSimplePaymentsAndOrderCreationFeedbackVisible -> {
                    viewState = viewState.copy(
                        ippFeedbackBannerState = IPPSurveyFeedbackBannerState.Hidden,
                        jitmEnabled = true
                    )
                }
            }
            refreshOrdersBannerVisibility()
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
    @Suppress("NestedBlockDepth", "ComplexMethod")
    fun createAndPostEmptyViewType(wrapper: PagedListWrapper<OrderListItemUIType>) {
        val isLoadingData = wrapper.isFetchingFirstPage.value ?: false ||
            wrapper.data.value == null
        val isListEmpty = wrapper.isEmpty.value ?: true
        val isError = wrapper.listError.value != null

        viewModelScope.launch {
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
                    else -> when {
                        !networkStatus.isConnected() -> EmptyViewType.NETWORK_OFFLINE
                        shouldShowCreateTestOrderScreen() -> EmptyViewType.ORDER_LIST_CREATE_TEST_ORDER
                        else -> EmptyViewType.ORDER_LIST
                    }
                }
            } else {
                null
            }
            _emptyViewType.postValue(newEmptyViewType)
        }
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

        launch {
            val totalDurationInSeconds = event.duration.toDouble() / 1_000
            val totalCompletedOrders = orderListRepository
                .getCachedOrderStatusOptions()[CoreOrderStatus.COMPLETED.value]?.statusCount
            AnalyticsTracker.track(
                AnalyticsEvent.ORDERS_LIST_LOADED,
                mapOf(
                    AnalyticsTracker.KEY_TOTAL_DURATION to totalDurationInSeconds,
                    AnalyticsTracker.KEY_STATUS to event.listDescriptor.statusFilter,
                    AnalyticsTracker.KEY_TOTAL_COMPLETED_ORDERS to totalCompletedOrders
                )
            )
        }
    }

    fun onFiltersButtonTapped() {
        AnalyticsTracker.track(AnalyticsEvent.ORDERS_LIST_VIEW_FILTER_OPTIONS_TAPPED)
        triggerEvent(ShowOrderFilters)
    }

    fun onSearchClosed() {
        loadOrders()
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

    fun onDismissIPPFeedbackBannerClicked() {
        _event.postValue(OrderListEvent.ShowIPPDismissConfirmationDialog)
    }

    fun onIPPFeedbackBannerCTAClicked() {
        trackIPPBannerEvent(AnalyticsEvent.IPP_FEEDBACK_BANNER_CTA_TAPPED)

        val bannerState = viewState.ippFeedbackBannerState as IPPSurveyFeedbackBannerState.Visible
        _event.postValue(OrderListEvent.OpenIPPFeedbackSurveyLink(bannerState.bannerData.url))
        markFeedbackBannerAsCompleted()
        viewState = viewState.copy(ippFeedbackBannerState = IPPSurveyFeedbackBannerState.Hidden)

        refreshOrdersBannerVisibility()
    }

    fun onIPPFeedbackBannerDismissedForever() {
        trackIPPBannerEvent(
            AnalyticsEvent.IPP_FEEDBACK_BANNER_DISMISSED,
            KEY_IPP_BANNER_REMIND_LATER to false
        )

        markFeedbackBannerAsDismissedForever()
        viewState = viewState.copy(ippFeedbackBannerState = IPPSurveyFeedbackBannerState.Hidden)

        refreshOrdersBannerVisibility()
    }

    fun onIPPFeedbackBannerDismissedShowLater() {
        trackIPPBannerEvent(
            AnalyticsEvent.IPP_FEEDBACK_BANNER_DISMISSED,
            KEY_IPP_BANNER_REMIND_LATER to true
        )

        markFeedbackBannerAsDismissed()
        viewState = viewState.copy(ippFeedbackBannerState = IPPSurveyFeedbackBannerState.Hidden)

        refreshOrdersBannerVisibility()
    }

    private fun refreshOrdersBannerVisibility() {
        viewState = viewState.copy(
            isSimplePaymentsAndOrderCreationFeedbackVisible = isSimplePaymentsAndOrderCreationFeedbackVisible
        )
    }

    private fun trackIPPBannerEvent(event: AnalyticsEvent, vararg customProps: Pair<String, Any>) {
        analyticsTracker.track(event, getIPPBannerEventProps(*customProps))
    }

    private fun getIPPBannerEventProps(vararg customArgs: Pair<String, Any>): Map<String, Any> {
        val bannerData = (viewState.ippFeedbackBannerState as IPPSurveyFeedbackBannerState.Visible).bannerData

        return customArgs.toMap() + mapOf(
            KEY_IPP_BANNER_SOURCE to VALUE_IPP_BANNER_SOURCE_ORDER_LIST,
            KEY_IPP_BANNER_CAMPAIGN_NAME to bannerData.campaignName
        )
    }

    fun onDismissOrderCreationSimplePaymentsFeedback() {
        analyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        refreshOrdersBannerVisibility()
    }

    sealed class OrderListEvent : Event() {
        data class ShowErrorSnack(@StringRes val messageRes: Int) : OrderListEvent()
        object ShowOrderFilters : OrderListEvent()
        data class OpenPurchaseCardReaderLink(
            val url: String,
            @StringRes val titleRes: Int,
        ) : OrderListEvent()

        data class ShowRetryErrorSnack(
            val message: String,
            val retry: View.OnClickListener
        ) : OrderListEvent()

        data class NotifyOrderChanged(val position: Int) : OrderListEvent()

        object ShowIPPDismissConfirmationDialog : OrderListEvent()

        data class OpenIPPFeedbackSurveyLink(val url: String) : OrderListEvent()

        object OpenBarcodeScanningFragment : OrderListEvent()

        data class OnBarcodeScanned(
            val code: String,
            val barcodeFormat: BarcodeFormat
        ) : OrderListEvent()

        data class OnAddingProductViaScanningFailed(
            val message: Int,
            val retry: View.OnClickListener,
        ) : Event()

        data class VMKilledWhenScanningInProgress(@StringRes val message: Int) : Event()
    }

    @Parcelize
    data class ViewState(
        val isRefreshPending: Boolean = false,
        val arePaymentGatewaysFetched: Boolean = false,
        val filterCount: Int = 0,
        val ippFeedbackBannerState: IPPSurveyFeedbackBannerState = IPPSurveyFeedbackBannerState.Hidden,
        val isSimplePaymentsAndOrderCreationFeedbackVisible: Boolean = false,
        val jitmEnabled: Boolean = false,
        val isErrorFetchingDataBannerVisible: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val isFilteringActive = filterCount > 0
    }

    sealed class IPPSurveyFeedbackBannerState : Parcelable {
        @Parcelize
        object Hidden : IPPSurveyFeedbackBannerState()

        @Parcelize
        data class Visible(
            val bannerData: GetIPPFeedbackBannerData.IPPFeedbackBanner,
        ) : IPPSurveyFeedbackBannerState()
    }
}
