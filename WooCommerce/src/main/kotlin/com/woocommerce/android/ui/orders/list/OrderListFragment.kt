package com.woocommerce.android.ui.orders.list

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppUrls
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ORDER_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_START_PAYMENT_FLOW
import com.woocommerce.android.databinding.FragmentOrderListBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Order
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment.Companion.KEY_BARCODE_SCANNING_SCAN_STATUS
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.designSystemComposeView
import com.woocommerce.android.ui.jitm.JitmViewModel
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.OrderStatusSelectorDialog
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("LargeClass")
class OrderListFragment : TopLevelFragment(R.layout.fragment_order_list) {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val FILTER_CHANGE_NOTICE_KEY = "filters_changed_notice"

        private const val TABLET_PORTRAIT_WIDTH_RATIO = 0.4f
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.3f
        private const val LAST_WINDOW_SIZE_WAS_LARGER_THAN_COMPACT = "last_window_size_was_larger_than_compact"
        private const val HANDLER_DELAY = 200L
    }

    @Inject
    internal lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    internal lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    internal lateinit var orderFiltersRepository: OrderFiltersRepository

    private val viewModel: OrderListViewModel by viewModels()
    private val jitmViewModel: JitmViewModel by viewModels()
    private val communicationViewModel: OrdersCommunicationViewModel by activityViewModels()
    private var snackBar: Snackbar? = null
    private val scrollToTopRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var isListAtTop = true
    private var isJitmEventObserverInitialized = false

    override fun onStop() {
        snackBar?.dismiss()
        super.onStop()
    }

    private val handler = Handler(Looper.getMainLooper())

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val selectedOrder: SelectedOrderTrackerViewModel by activityViewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(viewModel.performanceObserver)
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // We discovered cases where the callback was being invoked multiple times.
                    // Most likely due to lagging fragment transition, it was not removed from the
                    // onBackPressedDispatcher before next back press event.
                    // The check below ensures that the callback is only called once to prevent crashes.
                    if (findNavController().currentDestination?.id != R.id.orders) return

                    if (viewModel.selectedOrderIds.value.isNotEmpty()) {
                        viewModel.clearOrderSelection()
                        return
                    }

                    if (viewModel.viewState.isSearching) {
                        viewModel.onSearchClosed()
                        return
                    }

                    selectedOrder.selectOrder(-1L)
                    if (requireContext().isTwoPanesShouldBeUsed) {
                        if (!binding.detailPaneContainer.findNavController().popBackStack()) {
                            findNavController().popBackStack()
                        }
                    } else {
                        val result =
                            _binding?.detailPaneContainer?.findNavController()?.navigateUp() ?: false
                        val isCompactScreen = !requireContext().isTwoPanesShouldBeUsed
                        if (
                            !result &&
                            _binding?.orderListComposeContainer?.isVisible != true &&
                            isCompactScreen
                        ) {
                            // There are no more fragments in the back stack, UI used to be a two pane layout (tablet)
                            // and now it's a single pane layout (phone), e.g. due to a configuration change.
                            // In this case we need to switch panes – show the list pane instead of details pane.
                            adjustUiForDeviceType(savedInstanceState)
                        } else {
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderListBinding.bind(view)
        isJitmEventObserverInitialized = false
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        uiMessageResolver.anchorViewId = null

        ViewGroupCompat.setTransitionGroup(binding.orderListComposeContainer, true)
        binding.orderListComposeContainer.addView(
            designSystemComposeView {
                val highlightedOrderId by selectedOrder.selectedOrderId.observeAsState()
                OrderListScreen(
                    viewModel = viewModel,
                    communicationViewModel = communicationViewModel,
                    currencyFormatter = currencyFormatter,
                    detailHighlightedOrderId = highlightedOrderId
                        ?.takeIf { requireContext().isTwoPanesShouldBeUsed },
                    scrollToTopRequests = scrollToTopRequests,
                    jitmViewModelProvider = { jitmViewModel },
                    onOrderTapped = ::openOrderDetail,
                    onLearnMoreClicked = {
                        ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_ORDERS)
                    },
                    onCreateOrderClicked = ::openOrderCreationFragment,
                    onTroubleshootingClicked = ::onTroubleshootingClicked,
                    onContactSupportClicked = ::onContactSupportClicked,
                    onListAtTopChanged = { isListAtTop = it },
                )
            }.apply {
                id = R.id.order_list_compose_view
            },
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        initObservers()
        initSelectionObserver()
        initializeResultHandlers()
        adjustUiForDeviceType(savedInstanceState)
        updateSnackbarAnchor()
    }

    private fun adjustUiForDeviceType(savedInstanceState: Bundle?) {
        if (requireContext().isTwoPanesShouldBeUsed) {
            adjustLayoutForTablet()
        } else {
            adjustLayoutForNonTablet(savedInstanceState)
            savedInstanceState?.putBoolean(LAST_WINDOW_SIZE_WAS_LARGER_THAN_COMPACT, false)
        }
    }

    private fun adjustLayoutForTablet() {
        when (requireContext().isTwoPanesShouldBeUsed) {
            false -> return
            true -> {
                setListDetailsLayoutWidthRatio()
            }
        }
        binding.orderListComposeContainer.visibility = View.VISIBLE
        binding.detailPaneContainer.visibility = View.VISIBLE
        updateSnackbarAnchor()
    }

    private fun setListDetailsLayoutWidthRatio() {
        if (!requireContext().isTwoPanesShouldBeUsed) return

        val ratio = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> TABLET_LANDSCAPE_WIDTH_RATIO
            else -> TABLET_PORTRAIT_WIDTH_RATIO
        }
        binding.twoPaneLayoutGuideline.setGuidelinePercent(ratio)
    }

    private fun adjustLayoutForNonTablet(savedInstanceState: Bundle?) {
        if (wasLastWindowSizeLargerThanCompact(savedInstanceState)) {
            if (viewModel.isSelecting()) {
                displayListPaneOnly()
            } else {
                displayDetailPaneOnly()
            }
        } else {
            displayListPaneOnly()
        }
    }

    private fun wasLastWindowSizeLargerThanCompact(savedInstanceState: Bundle?) =
        savedInstanceState != null && savedInstanceState.getBoolean(
            LAST_WINDOW_SIZE_WAS_LARGER_THAN_COMPACT,
            false
        )

    private fun displayListPaneOnly() {
        _binding?.detailPaneContainer?.visibility = View.GONE
        _binding?.orderListComposeContainer?.visibility = View.VISIBLE
        _binding?.twoPaneLayoutGuideline?.setGuidelinePercent(1f)
        updateSnackbarAnchor()
    }

    private fun displayDetailPaneOnly() {
        _binding?.detailPaneContainer?.visibility = View.VISIBLE
        _binding?.twoPaneLayoutGuideline?.setGuidelinePercent(0.0f)
        _binding?.orderListComposeContainer?.visibility = View.GONE
        updateSnackbarAnchor()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        viewModel.loadOrders()
        updateSnackbarAnchor()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (findNavController().currentDestination?.id == R.id.orders) {
            // We want to check if [OrderListFragment] is the current destination (at the top of the backstack),
            // because onSaveInstanceState hook is called in all the fragments in the back stack on config change,
            // even if they are not being recreated.
            if (requireContext().isTwoPanesShouldBeUsed) {
                outState.putBoolean(LAST_WINDOW_SIZE_WAS_LARGER_THAN_COMPACT, true)
            }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        uiMessageResolver.anchorViewId = null
        isJitmEventObserverInitialized = false
        super.onDestroyView()
        _binding = null
    }

    private fun openBarcodeScanningFragment() {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToBarcodeScanningFragment()
        )
    }

    override fun scrollToTop() {
        scrollToTopRequests.tryEmit(Unit)
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun initObservers() {
        viewModel.pagedListData.observe(viewLifecycleOwner) { pagedList ->
            if (pagedList != null && requireContext().isTwoPanesShouldBeUsed) {
                when {
                    communicationViewModel.event.value is
                        OrdersCommunicationViewModel.CommunicationEvent.OrdersLoaded &&
                        viewModel.orderId.value == -1L -> {
                        // Prevents unintended navigation issues when opening an order list/detail in tablets.
                        // When navigating from order creation to order details via the "Collect Payment" option,
                        // the app correctly opens the Select Payment fragment inside the order details flow.
                        // However, if the above condition is not present, this navigation is undone,
                        // and only the order details screen is shown (skipping the Select Payment fragment).
                        // This no-op block ensures the app doesn't mistakenly re-trigger the order details screen.
                    }

                    // A specific order is set to be opened
                    viewModel.orderId.value != -1L -> {
                        openSpecificOrder(viewModel.orderId.value)
                        clearSelectedOrderIdInViewModel()
                    }
                    // Open the first order when filtering is active, but only if no order is explicitly selected by
                    // the user. If a user enables filtering, selects an order, and then pulls to refresh, we should
                    // retain the selected order instead of automatically selecting the first order.
                    viewModel.viewState.isFilteringActive &&
                        (
                            selectedOrder.selectedOrderId.value == null ||
                                selectedOrder.selectedOrderId.value == -1L
                            ) -> {
                        handler.postDelayed({
                            openFirstOrder(pagedList)
                        }, HANDLER_DELAY)
                    }

                    selectedOrder.selectedOrderId.value != null &&
                        selectedOrder.selectedOrderId.value != -1L -> {
                        openSpecificOrder(selectedOrder.selectedOrderId.value)
                    }
                    // No order selected and no specific order to open, or no specific condition met
                    selectedOrder.selectedOrderId.value == null || selectedOrder.selectedOrderId.value == -1L -> {
                        // The first time the user logs in, we need to add some delay
                        // before opening the first order.
                        handler.postDelayed({
                            openFirstOrder(pagedList)
                        }, HANDLER_DELAY)
                    }
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowErrorSnack -> {
                    uiMessageResolver.showSnack(event.messageRes)
                }

                is ShowOrderFilters -> showOrderFilters()
                is OrderListViewModel.OrderListEvent.OpenPurchaseCardReaderLink -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalAuthenticatedWebViewFragment(
                            urlToLoad = event.url,
                            title = resources.getString(event.titleRes)
                        )
                    )
                }

                is MultiLiveEvent.Event.ShowUndoSnackbar -> {
                    snackBar = uiMessageResolver.getUndoSnack(
                        message = event.message,
                        actionListener = event.undoAction
                    ).also {
                        it.addCallback(event.dismissAction)
                        it.show()
                    }
                }

                is OrderListViewModel.OrderListEvent.ShowRetryErrorSnack -> {
                    snackBar = uiMessageResolver.getRetrySnack(
                        message = event.message,
                        actionListener = event.retry
                    ).also {
                        it.show()
                    }
                }

                is OrderListViewModel.OrderListEvent.OnBarcodeScanned -> {
                    openOrderCreationFragment(event.code, event.barcodeFormat)
                }

                is OrderListViewModel.OrderListEvent.OnAddingProductViaScanningFailed -> {
                    uiMessageResolver.getRetrySnack(
                        stringResId = event.message,
                        isIndefinite = false,
                        actionListener = event.retry
                    ).show()
                }

                is OrderListViewModel.OrderListEvent.VMKilledWhenScanningInProgress -> {
                    ToastUtils.showToast(
                        context,
                        event.message
                    )
                }

                is OrderListViewModel.OrderListEvent.OpenBarcodeScanningFragment -> {
                    openBarcodeScanningFragment()
                }

                is MultiLiveEvent.Event.ShowDialog -> event.showDialog()
                is MultiLiveEvent.Event.ShowActionStringSnackbar -> uiMessageResolver.showActionSnack(
                    message = event.message,
                    actionText = event.actionText,
                    action = event.action
                )

                is OrderListViewModel.OrderListEvent.RetryLoadingOrders ->
                    viewModel.fetchOrdersAndOrderDependencies()
                is OrderListViewModel.OrderListEvent.ShowUpdateStatusDialog -> {
                    showBulkUpdateStatusDialog(event.currentStatus, event.orderStatusList)
                }

                is OrderListViewModel.OrderListEvent.ShowSnackbarString -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)

                else -> event.isHandled = false
            }
        }

        communicationViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OrdersCommunicationViewModel.CommunicationEvent.OrderTrashed -> {
                    viewModel.trashOrder(event.orderId)
                    selectedOrder.selectOrder(-1L)
                }

                is OrdersCommunicationViewModel.CommunicationEvent.CustomerFilterRequested -> {
                    applyCustomerFilter(event.customerId)
                }

                else -> event.isHandled = false
            }
        }

        viewModel.emptyViewType.observe(viewLifecycleOwner) {
            when (it) {
                EmptyViewType.ORDER_LIST,
                EmptyViewType.ORDER_LIST_FILTERED -> communicationViewModel.notifyOrdersEmpty()
                EmptyViewType.ORDER_LIST_LOADING -> communicationViewModel.notifyOrdersLoading()
                null -> communicationViewModel.notifyOrdersLoaded()
                else -> Unit
            }
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { _, new ->
            if (new.jitmEnabled) {
                initializeJitmEventObserver()
            }
            updateBottomNavVisibility(
                isSearchActive = new.isSearching,
                isSelecting = viewModel.selectedOrderIds.value.isNotEmpty(),
            )
            updateSnackbarAnchor()
        }
    }

    private fun initSelectionObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedOrderIds.collect { selectedOrderIds ->
                    updateBottomNavVisibility(
                        isSearchActive = viewModel.viewState.isSearching,
                        isSelecting = selectedOrderIds.isNotEmpty(),
                    )
                    updateSnackbarAnchor()
                }
            }
        }
    }

    private fun showBulkUpdateStatusDialog(
        currentStatus: String,
        orderStatusList: Array<Order.OrderStatus>
    ) {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrdersListStatusSelectorDialog(
                currentStatus,
                orderStatusList,
                R.string.dialog_ok
            )
        )
    }

    private fun updateBottomNavVisibility(isSearchActive: Boolean, isSelecting: Boolean) {
        showBottomNavBar(isVisible = !isSearchActive && !isSelecting)
    }

    private fun showBottomNavBar(isVisible: Boolean) {
        if (!isVisible) {
            (activity as? MainActivity)?.hideBottomNav()
        } else {
            (activity as? MainActivity)?.showBottomNav()
        }
    }

    private fun updateSnackbarAnchor() {
        val currentBinding = _binding
        val isComposeFabAvailable =
            currentBinding?.orderListComposeContainer?.isVisible == true &&
                viewModel.selectedOrderIds.value.isEmpty()
        uiMessageResolver.anchorViewId = currentBinding
            ?.createOrderSnackbarAnchor
            ?.id
            ?.takeIf { isComposeFabAvailable }
    }

    private fun openFirstOrder(orderList: PagedOrdersList) {
        if (orderList !== viewModel.pagedListData.value) return

        val orders = orderList.snapshot()
            .filterIsInstance<OrderListItemUIType.OrderListItemUI>()
        val firstOrder = orders.firstOrNull()

        firstOrder?.let { firstOrder ->
            if (firstOrder.orderId != selectedOrder.selectedOrderId.value) {
                openOrderDetail(
                    OrderListNavigationTarget(
                        orderId = firstOrder.orderId,
                        loadedOrderIds = orders.map { it.orderId },
                        status = firstOrder.status,
                    )
                )
            }
        }
    }

    private fun openSpecificOrder(orderId: Long?, startPaymentsFlow: Boolean = false) {
        val currentSelectedId = selectedOrder.selectedOrderId.value
        val targetOrderId = orderId ?: -1L
        if (targetOrderId == currentSelectedId) return

        val orders = viewModel.pagedListData.value
            ?.snapshot()
            ?.filterIsInstance<OrderListItemUIType.OrderListItemUI>()
            .orEmpty()
        val loadedOrderIds = orders.map { it.orderId }.let { loadedIds ->
            if (targetOrderId in loadedIds) loadedIds else listOf(targetOrderId) + loadedIds
        }
        openOrderDetail(
            target = OrderListNavigationTarget(
                orderId = targetOrderId,
                loadedOrderIds = loadedOrderIds,
                status = orders.firstOrNull { it.orderId == targetOrderId }?.status.orEmpty(),
            ),
            startPaymentsFlow = startPaymentsFlow,
        )
    }

    private fun clearSelectedOrderIdInViewModel() {
        viewModel.clearOrderId()
    }

    private fun initializeJitmEventObserver() {
        if (isJitmEventObserverInitialized) return

        isJitmEventObserverInitialized = true
        jitmViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is JitmViewModel.CtaClick -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun initializeResultHandlers() {
        handleResult<String>(FILTER_CHANGE_NOTICE_KEY, R.id.orders) {
            selectedOrder.selectOrder(-1L)
            viewModel.loadOrders()
        }
        handleResult<CodeScannerStatus>(KEY_BARCODE_SCANNING_SCAN_STATUS) { status ->
            viewModel.handleBarcodeScannedStatus(status)
        }
        handleResult<Long>(KEY_ORDER_ID) {
            if (requireContext().isTwoPanesShouldBeUsed) {
                openSpecificOrder(it)
            }
        }
        handleResult<Long>(KEY_START_PAYMENT_FLOW) {
            if (requireContext().isTwoPanesShouldBeUsed) {
                openSpecificOrder(it, true)
            }
        }
        handleDialogResult<OrderStatusUpdateSource>(
            key = OrderStatusSelectorDialog.KEY_ORDER_STATUS_RESULT,
            entryId = R.id.orders,
        ) {
            viewModel.onBulkOrderStatusChanged(Order.Status.fromValue(it.newStatus))
        }
    }

    fun applyCustomerFilter(customerId: Long) {
        orderFiltersRepository.setSelectedFilters(
            OrderListFilterCategory.CUSTOMER,
            listOf(customerId.toString())
        )
        orderFiltersRepository.loadCustomerInfoIfNeeded(customerId)
        if (viewModel.viewState.isSearching) {
            viewModel.onSearchQueryChanged("")
        } else {
            viewModel.loadOrders()
        }
        uiMessageResolver.showSnack(R.string.order_list_customer_filter_applied)
    }

    private fun showOrderFilters() {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrderFilterListFragment()
        )
    }

    private fun openOrderCreationFragment(
        code: String? = null,
        barcodeFormat: BarcodeFormat? = null,
    ) {
        OrderDurationRecorder.startRecording()
        AnalyticsTracker.track(AnalyticsEvent.ORDERS_ADD_NEW)
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrderCreationFragment(
                OrderCreateEditViewModel.Mode.Creation,
                code,
                barcodeFormat,
            )
        )
    }

    private fun openTroubleshootConnection() {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToTroubleshootConnectionFragment()
        )
    }

    private fun openOrderDetail(
        target: OrderListNavigationTarget,
        startPaymentsFlow: Boolean = false,
    ) {
        val isTwoPanes = requireContext().isTwoPanesShouldBeUsed
        if (
            !shouldOpenOrderDetail(
                isOrdersDestination = findNavController().currentDestination?.id == R.id.orders,
                isTwoPanes = isTwoPanes,
                selectedOrderId = selectedOrder.selectedOrderId.value,
                targetOrderId = target.orderId,
            )
        ) {
            return
        }

        viewModel.trackOrderClickEvent(
            target.orderId,
            target.status,
            isTwoPanes
        )

        if (!isTwoPanes) {
            uiMessageResolver.anchorViewId = null
        }
        (activity as? MainNavigationRouter)?.run {
            val navHostFragment = if (isTwoPanes) {
                childFragmentManager.findFragmentById(R.id.detailPaneContainer) as NavHostFragment
            } else {
                null
            }
            selectedOrder.selectOrder(target.orderId)
            showOrderDetail(
                orderId = target.orderId,
                allOrderIds = target.loadedOrderIds,
                navHostFragment = navHostFragment,
                startPaymentsFlow = startPaymentsFlow,
            )
        }
    }

    override fun shouldExpandToolbar(): Boolean {
        return isListAtTop && !viewModel.viewState.isSearching && !viewModel.isSelecting()
    }

    private fun onTroubleshootingClicked(type: OrderListTroubleshootingType) {
        when (type) {
            OrderListTroubleshootingType.ParsingError -> openTroubleshootConnection()
            OrderListTroubleshootingType.Timeout -> {
                viewModel.changeTroubleshootingBannerVisibility(show = false)
                viewModel.trackConnectivityTroubleshootClicked()
                openTroubleshootConnection()
            }
        }
    }

    private fun onContactSupportClicked(type: OrderListTroubleshootingType) {
        if (type == OrderListTroubleshootingType.Timeout) {
            viewModel.changeTroubleshootingBannerVisibility(show = false)
        }
        openSupportRequestScreen()
    }

    private fun openSupportRequestScreen() {
        SupportRequestFormActivity.createIntent(
            context = requireContext(),
            origin = HelpOrigin.ORDERS_LIST,
            extraTags = ArrayList()
        ).let { activity?.startActivity(it) }
    }
}

internal fun shouldOpenOrderDetail(
    isOrdersDestination: Boolean,
    isTwoPanes: Boolean,
    selectedOrderId: Long?,
    targetOrderId: Long,
): Boolean {
    return isOrdersDestination && (!isTwoPanes || selectedOrderId != targetOrderId)
}
