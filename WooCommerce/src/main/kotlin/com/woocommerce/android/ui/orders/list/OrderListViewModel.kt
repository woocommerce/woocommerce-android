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
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDERS_LIST_AUTOMATIC_TIMEOUT_RETRY
import com.woocommerce.android.analytics.AnalyticsEvent.ORDERS_LIST_TOP_BANNER_TROUBLESHOOT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_LIST_PRODUCT_BARCODE_SCANNING_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HORIZONTAL_SIZE_CLASS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IsScreenLargerThanCompactValue
import com.woocommerce.android.analytics.deviceTypeToAnalyticsString
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.filter
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.ShowTestNotification
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
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.RetryLoadingOrders
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.ThrottleLiveData
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
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
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.TIMEOUT_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderSummariesFetched
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private const val EMPTY_VIEW_THROTTLE = 250L

typealias PagedOrdersList = PagedList<OrderListItemUIType>

@Suppress("LargeClass")
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
    private val shouldShowCreateTestOrderScreen: ShouldShowCreateTestOrderScreen,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val feedbackPrefs: FeedbackPrefs,
    private val barcodeScanningTracker: BarcodeScanningTracker,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val appPrefs: AppPrefsWrapper,
    private val showTestNotification: ShowTestNotification,
    private val dateUtils: DateUtils
) : ScopedViewModel(savedState), LifecycleOwner {
    private val navArgs: OrderListFragmentArgs by savedState.navArgs()

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
        OrderListItemDataSource(
            dispatcher,
            orderStore,
            networkStatus,
            fetcher,
            resourceProvider,
            dateUtils
        )
    }

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState(filterCount = getSelectedOrderFiltersCount()))
    internal var viewState by viewStateLiveData

    private val _pagedListData = MediatorLiveData<PagedOrdersList>()
    val pagedListData: LiveData<PagedOrdersList> = _pagedListData

    private val _isLoadingMore = MediatorLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isFetchingFirstPage = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private val _orderStatusOptions = MutableLiveData<Map<String, WCOrderStatusModel>>()
    val orderStatusOptions: LiveData<Map<String, WCOrderStatusModel>> = _orderStatusOptions

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    val orderId: LiveData<Long> = savedState.getLiveData<Long>("orderId")

    private val _emptyViewType: ThrottleLiveData<EmptyViewType?> by lazy {
        ThrottleLiveData(
            offset = EMPTY_VIEW_THROTTLE,
            coroutineScope = this,
            mainDispatcher = dispatchers.main,
            backgroundDispatcher = dispatchers.computation
        )
    }
    val emptyViewType: LiveData<EmptyViewType?> = _emptyViewType

    private var activeWCOrderListDescriptor: WCOrderListDescriptor? = null

    var isSearching = false
    private var dismissListErrors = false
    var searchQuery = ""

    private val isSimplePaymentsAndOrderCreationFeedbackVisible: Boolean
        get() {
            val simplePaymentsAndOrderFeedbackDismissed =
                simplePaymentsAndOrderCreationFeedbackState == FeatureFeedbackSettings.FeedbackState.DISMISSED
            val isTroubleshootingBannerVisible = viewState.shouldDisplayTroubleshootingBanner
            return !simplePaymentsAndOrderFeedbackDismissed && !isTroubleshootingBannerVisible
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

        displayOrdersBannerOrJitm()

        isFetchingFirstPage.filter { !it }
            .observeForever {
                // When first page is fetched
                orderListTransactionLauncher.onListFetched()
                checkChaChingSoundSettings()
            }

        when (navArgs.mode) {
            Mode.START_ORDER_CREATION_WITH_SIMPLE_PAYMENTS_MIGRATION -> {
                triggerEvent(OrderListEvent.OpenOrderCreationWithSimplePaymentsMigration)
            }
            Mode.STANDARD -> {
                // stay on the screen
            }
        }
    }

    fun loadOrders() {
        activeWCOrderListDescriptor = getWCOrderListDescriptorWithFilters()
        ordersPagedListWrapper = listStore.getList(getWCOrderListDescriptorWithFilters(), dataSource, lifecycle)
        viewState = viewState.copy(
            filterCount = getSelectedOrderFiltersCount(),
            isErrorFetchingDataBannerVisible = false
        )
        activatePagedListWrapper(
            pagedListWrapper = ordersPagedListWrapper!!,
            shouldRetry = true
        )
        fetchOrdersAndOrderDependencies()
    }

    /**
     * Creates and activates a new list with the search and filter params provided. This should only be used
     * by the search component portion of the order list view.
     */
    fun submitSearchOrFilter(searchQuery: String) {
        val listDescriptor = getWCOrderListDescriptorWithFiltersAndSearchQuery(sanitizeSearchQuery(searchQuery))
        activeWCOrderListDescriptor = listDescriptor
        val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)
        activatePagedListWrapper(pagedListWrapper, isFirstInit = true)
    }

    fun changeTroubleshootingBannerVisibility(show: Boolean) {
        viewState = viewState.copy(
            shouldDisplayTroubleshootingBanner = show,
            isSimplePaymentsAndOrderCreationFeedbackVisible = !show
        )
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
                else -> {
                    /* do nothing */
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

    fun trackConnectivityTroubleshootClicked() {
        analyticsTracker.track(ORDERS_LIST_TOP_BANNER_TROUBLESHOOT_TAPPED)
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

            CodeScannerStatus.NotFound -> {
                // do nothing
            }
        }
    }

    /**
     * Track user clicked to open an order and the status of that order, along with some
     * data about the order custom fields
     */
    fun trackOrderClickEvent(orderId: Long, orderStatus: String, windowSize: WindowSizeClass) = launch {
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
                AnalyticsTracker.KEY_CUSTOM_FIELDS_SIZE to customFieldsSize,
                KEY_HORIZONTAL_SIZE_CLASS to getScreenSizeClassNameForAnalytics(windowSize)
            )
        )
    }

    private fun getScreenSizeClassNameForAnalytics(windowSize: WindowSizeClass) =
        IsScreenLargerThanCompactValue(windowSize != WindowSizeClass.Compact).deviceTypeToAnalyticsString

    /**
     * Activates the provided list by first removing the LiveData sources for the active list,
     * then creating new LiveData sources for the provided [pagedListWrapper] and setting it as
     * the active list. If [isFirstInit] is true, then this [pagedListWrapper] is freshly created
     * so we'll need to call `fetchOrdersAndOrderDependencies` to initialize it.
     */
    private fun activatePagedListWrapper(
        pagedListWrapper: PagedListWrapper<OrderListItemUIType>,
        isFirstInit: Boolean = false,
        shouldRetry: Boolean = false
    ) {
        // This flag is used to ensure that we only retry the first time a timeout happens
        var noTimeoutHappened = true

        // Clear any of the data sources assigned to the current wrapper, then
        // create a new one.
        clearLiveDataSources(this.activePagedListWrapper)

        listenToEmptyViewStateLiveData(pagedListWrapper)

        _pagedListData.addSource(pagedListWrapper.data) { pagedList ->
            pagedList?.let {
                displayOrdersBannerOrJitm()
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
                when (error.type) {
                    PARSE_ERROR -> {
                        viewState = viewState.copy(
                            isErrorFetchingDataBannerVisible = true,
                            isSimplePaymentsAndOrderCreationFeedbackVisible = false
                        )
                    }

                    TIMEOUT_ERROR -> {
                        when {
                            shouldRetry && noTimeoutHappened -> {
                                analyticsTracker.track(ORDERS_LIST_AUTOMATIC_TIMEOUT_RETRY)
                                triggerEvent(RetryLoadingOrders)
                            }

                            else -> changeTroubleshootingBannerVisibility(show = true)
                        }
                        noTimeoutHappened = false
                    }

                    else -> triggerEvent(ShowErrorSnack(R.string.orderlist_error_fetch_generic))
                }
            }
        this.activePagedListWrapper = pagedListWrapper

        if (isFirstInit) {
            fetchOrdersAndOrderDependencies()
        } else {
            pagedListWrapper.invalidateData()
        }
    }

    private fun displayOrdersBannerOrJitm() {
        viewModelScope.launch {
            when {
                !isSimplePaymentsAndOrderCreationFeedbackVisible -> {
                    viewState = viewState.copy(
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

    fun updateOrderSelectedStatus(orderId: Long, isTablet: Boolean = true) {
        val pagedList = _pagedListData.value ?: return
        if (isTablet) {
            pagedList.map { orderItem ->
                if (orderItem is OrderListItemUIType.OrderListItemUI) {
                    orderItem.isSelected = orderItem.orderId == orderId
                }
            }
        } else {
            pagedList.map { orderItem ->
                if (orderItem is OrderListItemUIType.OrderListItemUI) {
                    orderItem.isSelected = false
                }
            }
        }
        triggerEvent(OrderListEvent.NotifyOrderSelectionChanged)
    }

    fun clearOrderId() {
        savedState["orderId"] = -1L
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

    private fun refreshOrdersBannerVisibility() {
        viewState = viewState.copy(
            isSimplePaymentsAndOrderCreationFeedbackVisible = isSimplePaymentsAndOrderCreationFeedbackVisible
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

    private fun checkChaChingSoundSettings() {
        fun recreateNotificationChannel() {
            notificationChannelsHandler.recreateNotificationChannel(NotificationChannelType.NEW_ORDER)
            triggerEvent(
                Event.ShowActionSnackbar(
                    message = resourceProvider.getString(R.string.cha_ching_sound_succcess_snackbar),
                    actionText = resourceProvider.getString(R.string.cha_ching_sound_succcess_snackbar_action),
                    action = {
                        launch {
                            showTestNotification(
                                title = resourceProvider.getString(R.string.cha_ching_sound_test_notification_title),
                                message = resourceProvider.getString(
                                    R.string.cha_ching_sound_test_notification_message
                                ),
                                channelType = NotificationChannelType.NEW_ORDER,
                                dismissDelay = 10.seconds
                            )
                        }
                    }
                )
            )
        }

        if (notificationChannelsHandler.checkNewOrderNotificationSound() == NewOrderNotificationSoundStatus.DISABLED &&
            !appPrefs.chaChingSoundIssueDialogDismissed
        ) {
            analyticsTracker.track(AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_FIX_SHOWN)
            triggerEvent(
                Event.ShowDialog(
                    titleId = R.string.cha_ching_sound_issue_dialog_title,
                    messageId = R.string.cha_ching_sound_issue_dialog_message,
                    positiveButtonId = R.string.cha_ching_sound_issue_dialog_turn_on_sound,
                    negativeButtonId = R.string.cha_ching_sound_issue_dialog_keep_silent,
                    positiveBtnAction = { _, _ ->
                        analyticsTracker.track(
                            AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_FIX_TAPPED,
                            mapOf(AnalyticsTracker.KEY_SOURCE to "order_list")
                        )
                        recreateNotificationChannel()
                    },
                    negativeBtnAction = { _, _ ->
                        analyticsTracker.track(AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_FIX_DISMISSED)
                        appPrefs.chaChingSoundIssueDialogDismissed = true
                    },
                    cancelable = false
                )
            )
        }
    }

    fun trashOrder(orderId: Long) {
        fun updateExcludedOrders(excludedOrderIds: List<Long>?) {
            val listDescriptor = activeWCOrderListDescriptor?.copy(
                excludedIds = excludedOrderIds?.takeIf { it.isNotEmpty() }
            ) ?: return
            activeWCOrderListDescriptor = listDescriptor
            val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)
            activatePagedListWrapper(pagedListWrapper)
        }

        fun excludeOrder() = updateExcludedOrders(
            excludedOrderIds = (activeWCOrderListDescriptor?.excludedIds ?: emptyList()) + orderId
        )

        fun cancelExcludingOrder() = updateExcludedOrders(activeWCOrderListDescriptor?.excludedIds?.minus(orderId))

        fun handleTrashing() {
            launch {
                orderListRepository
                    .trashOrder(orderId)
                    .onFailure { triggerEvent(ShowErrorSnack(R.string.orderlist_order_trashed_error)) }

                cancelExcludingOrder()
            }
        }

        excludeOrder()

        triggerEvent(
            Event.ShowUndoSnackbar(
                message = resourceProvider.getString(R.string.orderlist_order_trashed, orderId),
                undoAction = { cancelExcludingOrder() },
                dismissAction = object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            handleTrashing()
                        }
                    }
                }
            )
        )
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

        object NotifyOrderSelectionChanged : OrderListEvent()

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

        data object RetryLoadingOrders : OrderListEvent()

        data object OpenOrderCreationWithSimplePaymentsMigration : OrderListEvent()
    }

    @Parcelize
    data class ViewState(
        val isRefreshPending: Boolean = false,
        val arePaymentGatewaysFetched: Boolean = false,
        val filterCount: Int = 0,
        val isSimplePaymentsAndOrderCreationFeedbackVisible: Boolean = false,
        val jitmEnabled: Boolean = false,
        val isErrorFetchingDataBannerVisible: Boolean = false,
        val shouldDisplayTroubleshootingBanner: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val isFilteringActive = filterCount > 0
    }

    enum class Mode {
        STANDARD, START_ORDER_CREATION_WITH_SIMPLE_PAYMENTS_MIGRATION
    }
}
