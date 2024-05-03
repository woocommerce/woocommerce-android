package com.woocommerce.android.ui.orders.list

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppUrls
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ORDER_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_START_PAYMENT_FLOW
import com.woocommerce.android.databinding.FragmentOrderListBinding
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.pinFabAboveBottomNavigationBar
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.SIMPLE_PAYMENTS_AND_ORDER_CREATION
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment.Companion.KEY_BARCODE_SCANNING_SCAN_STATUS
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderNavigationLogger
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.TabletLayoutSetupHelper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.wordpress.android.util.ActivityUtils as WPActivityUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
@Suppress("LargeClass")
class OrderListFragment :
    TopLevelFragment(R.layout.fragment_order_list),
    OnQueryTextListener,
    OnActionExpandListener,
    OrderListListener,
    SwipeToComplete.OnSwipeListener,
    TabletLayoutSetupHelper.Screen {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"
        const val FILTER_CHANGE_NOTICE_KEY = "filters_changed_notice"
        private const val TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY = "non_root_navigation_in_detail_pane"

        private const val JITM_FRAGMENT_TAG = "jitm_orders_fragment"
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.3f
        private const val TABLET_PORTRAIT_WIDTH_RATIO = 0.40f
        private const val CURRENT_NAV_DESTINATION = "current_nav_destination"
        private const val HANDLER_DELAY = 200L
    }

    @Inject
    internal lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    internal lateinit var selectedSite: SelectedSite

    @Inject
    internal lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var feedbackPrefs: FeedbackPrefs

    @Inject
    lateinit var orderNavigationLogger: OrderNavigationLogger

    private val viewModel: OrderListViewModel by viewModels()
    private val communicationViewModel: OrdersCommunicationViewModel by activityViewModels()
    private var snackBar: Snackbar? = null

    override val twoPaneLayoutGuideline: Guideline
        get() = binding.twoPaneLayoutGuideline
    override val listPaneContainer: View
        get() = binding.listPaneContainer
    override val detailPaneContainer: View
        get() = binding.detailPaneContainer
    override val navigation: TabletLayoutSetupHelper.Screen.Navigation
        get() = TabletLayoutSetupHelper.Screen.Navigation(
            detailsNavGraphId = R.navigation.nav_graph_orders,
            detailsInitialBundle = null
        )
    override var twoPanesWereShownBeforeConfigChange: Boolean = false

    override val listFragment: Fragment
        get() = this

    override val detailPaneContainerId: Int
        get() = R.id.detailPaneContainer

    @Inject
    lateinit var tabletLayoutSetupHelper: TabletLayoutSetupHelper

    override fun onStop() {
        snackBar?.dismiss()
        super.onStop()
    }

    // Alias for interacting with [viewModel.isSearching] so the value is always identical
    // to the real value on the UI side.
    private var isSearching: Boolean
        private set(value) {
            viewModel.isSearching = value
        }
        get() = viewModel.isSearching

    private var orderListMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!

    // Alias for interacting with [viewModel.searchQuery] so the value is always identical
    // to the real value on the UI side.
    private var searchQuery: String
        private set(value) {
            viewModel.searchQuery = value
        }
        get() = viewModel.searchQuery

    private val emptyView
        get() = binding.orderListView.emptyView

    private val selectedOrder: SelectedOrderTrackerViewModel by activityViewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(viewModel.performanceObserver)
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { bundle ->
            isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    orderNavigationLogger.logBackStack(findNavController(), "Before navigating back from OrderList")
                    WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Start")
                    selectedOrder.selectOrder(-1L)
                    if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                        WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Location #1")
                        if (!binding.detailPaneContainer.findNavController().popBackStack()) {
                            WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Location #2")
                            findNavController().popBackStack()
                        }
                    } else if (isSearching) {
                        WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Location #3")
                        handleSearchViewCollapse()
                    } else {
                        WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Location #4")
                        val result =
                            _binding?.detailPaneContainer?.findNavController()?.navigateUp() ?: false
                        val isCompactScreen = requireContext().windowSizeClass == WindowSizeClass.Compact
                        if (!result && _binding?.listPaneContainer?.isVisible != true && isCompactScreen) {
                            WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Location #5")
                            // There are no more fragments in the back stack, UI used to be a two pane layout (tablet)
                            // and now it's a single pane layout (phone), e.g. due to a configuration change.
                            // In this case we need to switch panes – show the list pane instead of details pane.
                            adjustUiForDeviceType(savedInstanceState)
                        } else {
                            WooLog.d(WooLog.T.ORDERS, "Before navigating back from OrderList: Location #6")
                            findNavController().popBackStack()
                        }
                    }
                    WooLog.d(WooLog.T.ORDERS, "After navigating back from OrderList: End")
                }
            }
        )

        twoPanesWereShownBeforeConfigChange = savedInstanceState?.getBoolean(
            TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY,
            false
        ) ?: false
        tabletLayoutSetupHelper.onRootFragmentCreated(this)

        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val fadeThroughTransition = MaterialFadeThrough().apply { duration = transitionDuration }
        enterTransition = fadeThroughTransition
        exitTransition = fadeThroughTransition
        reenterTransition = fadeThroughTransition
    }

    private fun getSearchQueryHint(): String {
        return if (viewModel.viewState.isFilteringActive) {
            getString(R.string.orderlist_search_hint_active_filters)
        } else {
            getString(R.string.orderlist_search_hint)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setupToolbar()
        view.doOnPreDraw { startPostponedEnterTransition() }

        uiMessageResolver.anchorViewId = binding.createOrderButton.id

        binding.orderListView.init(currencyFormatter = currencyFormatter, orderListListener = this)
        ViewGroupCompat.setTransitionGroup(binding.listPaneContainer, true)
        binding.listPaneContainer.apply {
            // Set the scrolling view in the custom refresh SwipeRefreshLayout
            scrollUpChild = binding.orderListView.ordersList
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.ORDERS_LIST_PULLED_TO_REFRESH)
                refreshOrders()
            }
        }

        initObservers()
        initializeResultHandlers()
        adjustUiForDeviceType(savedInstanceState)
        binding.orderFiltersCard.setClickListener { viewModel.onFiltersButtonTapped() }
        initCreateOrderFAB(binding.createOrderButton)
        initSwipeBehaviour()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.orders)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemSelected(menuItem)
        }
        binding.toolbar.inflateMenu(R.menu.menu_order_list_fragment)
        binding.toolbar.navigationIcon = null
        setupToolbarMenu(binding.toolbar.menu)
    }

    private fun setupToolbarMenu(menu: Menu) {
        orderListMenu = menu
        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getSearchQueryHint()

        // Set listeners for search view expansion and collapse
        searchMenuItem?.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return handleSearchViewExpand()
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                return handleSearchViewCollapse()
            }
        })
        refreshOptionsMenu()
    }

    private fun handleSearchViewExpand(): Boolean {
        isSearching = true
        refreshOptionsMenu()
        checkOrientation()
        onSearchViewActiveChanged(isActive = true)
        binding.orderFiltersCard.isVisible = false
        handler.postDelayed({
            binding.orderListView.clearAdapterData()
        }, 100)
        return true // Return true to expand the action view
    }

    private fun handleSearchViewCollapse(): Boolean {
        clearSearchResults()
        searchMenuItem?.isVisible = true
        viewModel.onSearchClosed()
        binding.orderFiltersCard.isVisible = true
        onSearchViewActiveChanged(isActive = false)
        return true // Return true to collapse the action view
    }

    private fun adjustUiForDeviceType(savedInstanceState: Bundle?) {
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            adjustLayoutForTablet()
        } else {
            adjustLayoutForNonTablet(savedInstanceState)
            savedInstanceState?.putInt(CURRENT_NAV_DESTINATION, -1)
        }
    }

    private fun adjustLayoutForTablet() {
        when (requireContext().windowSizeClass) {
            WindowSizeClass.Compact -> return
            WindowSizeClass.Medium -> {
                binding.twoPaneLayoutGuideline.setGuidelinePercent(TABLET_PORTRAIT_WIDTH_RATIO)
            }
            WindowSizeClass.ExpandedAndBigger -> {
                binding.twoPaneLayoutGuideline.setGuidelinePercent(TABLET_LANDSCAPE_WIDTH_RATIO)
            }
        }
        binding.listPaneContainer.visibility = View.VISIBLE
        binding.detailPaneContainer.visibility = View.VISIBLE
    }

    private fun adjustLayoutForNonTablet(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.getInt(CURRENT_NAV_DESTINATION, -1) != -1) {
            displayDetailPaneOnly()
        } else {
            displayListPaneOnly()
        }
    }

    private fun displayListPaneOnly() {
        _binding?.detailPaneContainer?.visibility = View.GONE
        _binding?.listPaneContainer?.visibility = View.VISIBLE
        _binding?.twoPaneLayoutGuideline?.setGuidelinePercent(1f)
    }

    private fun displayDetailPaneOnly() {
        _binding?.detailPaneContainer?.visibility = View.VISIBLE
        _binding?.twoPaneLayoutGuideline?.setGuidelinePercent(0.0f)
        _binding?.listPaneContainer?.visibility = View.GONE
    }

    private fun initSwipeBehaviour() {
        val swipeToComplete = SwipeToComplete(requireContext(), this)
        val swipeHelper = ItemTouchHelper(swipeToComplete)
        swipeHelper.attachToRecyclerView(binding.orderListView.ordersList)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            refreshOrders()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_IS_SEARCHING, isSearching)
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            val navHostFragment = childFragmentManager.findFragmentById(R.id.detailPaneContainer) as? NavHostFragment
            val currentDestinationId = navHostFragment?.navController?.currentDestination?.id
            outState.putInt(CURRENT_NAV_DESTINATION, currentDestinationId ?: -1)
        }
        outState.putBoolean(
            TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY,
            _binding?.detailPaneContainer?.isVisible == true && _binding?.listPaneContainer?.isVisible == true
        )
    }

    override fun onDestroyView() {
        disableSearchListeners()
        handler.removeCallbacksAndMessages(null)
        searchView = null
        orderListMenu = null
        searchMenuItem = null
        super.onDestroyView()
        _binding = null
    }

    /**
     * This is a replacement for activity?.invalidateOptionsMenu() since that causes the
     * search menu item to collapse
     */
    private fun refreshOptionsMenu() {
        if (!isChildFragmentShowing() && isSearching) {
            val savedSearchQuery = searchQuery
            enableSearchListeners()
            handler.postDelayed({
                searchMenuItem?.expandActionView()
                searchQuery = savedSearchQuery
                searchView?.setQuery(searchQuery, false)
                if (searchQuery.isEmpty()) binding.orderListView.clearAdapterData()
            }, HANDLER_DELAY)
        } else {
            val showSearch = shouldShowSearchMenuItem()
            searchMenuItem?.let {
                if (it.isActionViewExpanded) it.collapseActionView()
                if (it.isVisible != showSearch) it.isVisible = showSearch
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(AnalyticsEvent.ORDERS_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }

            R.id.menu_barcode -> {
                viewModel.onScanClicked()
                true
            }

            else -> false
        }
    }

    private fun openBarcodeScanningFragment() {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToBarcodeScanningFragment()
        )
    }

    private fun initCreateOrderFAB(fabButton: FloatingActionButton) {
        fabButton.setOnClickListener { openOrderCreationFragment() }
        pinFabAboveBottomNavigationBar(fabButton)
    }

    private fun isChildFragmentShowing() = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false

    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = isChildFragmentShowing()
        return when {
            (isChildShowing) -> false
            else -> true
        }
    }

    override fun getFragmentTitle() = if (isSearching) "" else getString(R.string.orders)

    override fun scrollToTop() {
        binding.orderListView.scrollToTop()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun initObservers() {
        // setup observers
        selectedOrder.selectedOrderId.observe(viewLifecycleOwner) {
            viewModel.updateOrderSelectedStatus(
                orderId = selectedOrder.selectedOrderId.value ?: -1,
                requireContext().windowSizeClass != WindowSizeClass.Compact
            )
        }

        viewModel.orderStatusOptions.observe(viewLifecycleOwner) {
            it?.let { options ->
                // So the order status can be matched to the appropriate label
                binding.orderListView.setOrderStatusOptions(options)
            }
        }

        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner) {
            binding.listPaneContainer.isRefreshing = it == true
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) {
            it?.let { isLoadingMore ->
                binding.orderListView.setLoadingMoreIndicator(active = isLoadingMore)
            }
        }

        viewModel.pagedListData.observe(viewLifecycleOwner) {
            if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                when {
                    // A specific order is set to be opened
                    viewModel.orderId.value != -1L -> {
                        openSpecificOrder(viewModel.orderId.value)
                        clearSelectedOrderIdInViewModel()
                    }
                    // No order selected and no specific order to open, or no specific condition met
                    selectedOrder.selectedOrderId.value == null || selectedOrder.selectedOrderId.value == -1L -> {
                        // The first time the user logs in, we need to add some delay
                        // before opening the first order.
                        handler.postDelayed({
                            openFirstOrder()
                        }, HANDLER_DELAY)
                    }
                }
            }
            updateOrderSelectedStatus()
            updatePagedListData(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowErrorSnack -> {
                    uiMessageResolver.showSnack(event.messageRes)
                    binding.listPaneContainer.isRefreshing = false
                }

                is ShowOrderFilters -> showOrderFilters()
                is OrderListViewModel.OrderListEvent.OpenPurchaseCardReaderLink -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            title = resources.getString(event.titleRes)
                        )
                    )
                }

                is OrderListViewModel.OrderListEvent.NotifyOrderChanged -> {
                    binding.orderListView.ordersList.adapter?.notifyItemChanged(event.position)
                }

                OrderListViewModel.OrderListEvent.NotifyOrderSelectionChanged -> {
                    binding.orderListView.ordersList.adapter?.notifyDataSetChanged()
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
                    binding.listPaneContainer.isRefreshing = false
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
                is MultiLiveEvent.Event.ShowActionSnackbar -> uiMessageResolver.showActionSnack(
                    message = event.message,
                    actionText = event.actionText,
                    action = event.action
                )
                is OrderListViewModel.OrderListEvent.RetryLoadingOrders -> refreshOrders()
                is OrderListViewModel.OrderListEvent.OpenOrderCreationWithSimplePaymentsMigration ->
                    openOrderCreationFragment(indicateSimplePaymentsMigration = true)
                else -> event.isHandled = false
            }
        }

        communicationViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OrdersCommunicationViewModel.CommunicationEvent.OrderTrashed -> {
                    viewModel.trashOrder(event.orderId)
                    selectedOrder.selectOrder(-1L)
                }
                else -> event.isHandled = false
            }
        }

        viewModel.emptyViewType.observe(viewLifecycleOwner) {
            it?.let { emptyViewType ->
                when (emptyViewType) {
                    EmptyViewType.SEARCH_RESULTS -> {
                        emptyView.show(emptyViewType, searchQueryOrFilter = searchQuery)
                    }

                    EmptyViewType.ORDER_LIST -> {
                        communicationViewModel.notifyOrdersEmpty()

                        emptyView.show(emptyViewType) {
                            ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_ORDERS)
                        }
                    }

                    EmptyViewType.ORDER_LIST_FILTERED -> {
                        emptyView.show(emptyViewType)
                    }

                    EmptyViewType.NETWORK_OFFLINE, EmptyViewType.NETWORK_ERROR -> {
                        emptyView.show(emptyViewType) {
                            refreshOrders()
                        }
                    }

                    EmptyViewType.ORDER_LIST_CREATE_TEST_ORDER -> {
                        AnalyticsTracker.track(AnalyticsEvent.ORDER_LIST_TEST_ORDER_DISPLAYED)
                        emptyView.show(emptyViewType) {
                            navigateToTryTestOrderScreen()
                        }
                    }

                    EmptyViewType.ORDER_LIST_LOADING -> {
                        communicationViewModel.notifyOrdersLoading()
                        emptyView.show(emptyViewType)
                    }

                    else -> {
                        emptyView.show(emptyViewType)
                    }
                }
            } ?: hideEmptyView()
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.filterCount.takeIfNotEqualTo(old?.filterCount) { filterCount ->
                binding.orderFiltersCard.updateFilterSelection(filterCount)
            }
            new.jitmEnabled.takeIfNotEqualTo(old?.jitmEnabled) { jitmEnabled ->
                initJitm(jitmEnabled)
            }
            new.isSimplePaymentsAndOrderCreationFeedbackVisible.takeIfNotEqualTo(
                old?.isSimplePaymentsAndOrderCreationFeedbackVisible
            ) {
                displaySimplePaymentsWIPCard(it)
            }
            new.isErrorFetchingDataBannerVisible.takeIfNotEqualTo(old?.isErrorFetchingDataBannerVisible) {
                displayErrorParsingOrdersCard(it)
            }
            new.shouldDisplayTroubleshootingBanner.takeIfNotEqualTo(old?.shouldDisplayTroubleshootingBanner) {
                displayTimeoutErrorCard(it)
            }
        }
    }

    private fun openFirstOrder() {
        binding.orderListView.openFirstOrder()
    }

    private fun openSpecificOrder(orderId: Long?, startPaymentsFlow: Boolean = false) {
        binding.orderListView.openOrder(orderId ?: -1L, startPaymentsFlow)
    }

    private fun clearSelectedOrderIdInViewModel() {
        viewModel.clearOrderId()
    }

    private fun updateOrderSelectedStatus() {
        val selectedOrderId = selectedOrder.selectedOrderId.value ?: -1
        viewModel.updateOrderSelectedStatus(
            orderId = selectedOrderId,
            requireContext().windowSizeClass != WindowSizeClass.Compact
        )
    }

    private fun initJitm(jitmEnabled: Boolean) {
        if (jitmEnabled) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.jitmOrdersFragment,
                    JitmFragment.newInstance(JitmMessagePathsProvider.ORDER_LIST),
                    JITM_FRAGMENT_TAG
                )
                .commit()
        } else {
            childFragmentManager.findFragmentByTag(JITM_FRAGMENT_TAG)?.let {
                childFragmentManager.beginTransaction().remove(it).commit()
            }
        }
    }

    private fun initializeResultHandlers() {
        handleResult<String>(FILTER_CHANGE_NOTICE_KEY) {
            viewModel.loadOrders()
        }
        handleResult<CodeScannerStatus>(KEY_BARCODE_SCANNING_SCAN_STATUS) { status ->
            viewModel.handleBarcodeScannedStatus(status)
        }
        handleResult<Long>(KEY_ORDER_ID) {
            if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                openSpecificOrder(it)
            }
        }
        handleResult<Long>(KEY_START_PAYMENT_FLOW) {
            if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                openSpecificOrder(it, true)
            }
        }
    }

    private fun showOrderFilters() {
        findNavController().navigateSafely(R.id.action_orderListFragment_to_orderFilterListFragment)
    }

    private fun navigateToTryTestOrderScreen() {
        AnalyticsTracker.track(AnalyticsEvent.ORDER_LIST_TRY_TEST_ORDER_TAPPED)
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToCreateTestOrderDialogFragment(
                siteUrl = selectedSite.get().url
            )
        )
    }

    private fun openOrderCreationFragment(
        code: String? = null,
        barcodeFormat: BarcodeFormat? = null,
        indicateSimplePaymentsMigration: Boolean = false,
    ) {
        OrderDurationRecorder.startRecording()
        AnalyticsTracker.track(AnalyticsEvent.ORDERS_ADD_NEW)
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrderCreationFragment(
                OrderCreateEditViewModel.Mode.Creation(indicateSimplePaymentsMigration),
                code,
                barcodeFormat,
            )
        )
    }

    private fun openConnectivityTool() {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrderConnectivityToolFragment()
        )
    }

    private fun hideEmptyView() {
        emptyView.hide()
    }

    private fun updatePagedListData(pagedListData: PagedList<OrderListItemUIType>?) {
        binding.orderListView.submitPagedList(pagedListData)
    }

    override fun openOrderDetail(
        orderId: Long,
        allOrderIds: List<Long>,
        orderStatus: String,
        sharedView: View?,
        startPaymentsFlow: Boolean,
    ) {
        viewModel.trackOrderClickEvent(
            orderId,
            orderStatus,
            requireContext().windowSizeClass
        )

        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            _binding?.createOrderButton?.show()
        } else {
            _binding?.createOrderButton?.hide()
        }

        // if a search is active, we need to collapse the search view so order detail can show it's title and then
        // remember the user was searching (since both searchQuery and isSearching will be reset)
        if (isSearching) {
            val savedSearch = searchQuery
            clearSearchResults()
            updateActivityTitle()
            searchQuery = savedSearch
            isSearching = true
        }
        (activity as? MainNavigationRouter)?.run {
            val navHostFragment = if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                childFragmentManager.findFragmentById(R.id.detailPaneContainer) as NavHostFragment
            } else {
                null
            }
            selectedOrder.selectOrder(orderId)
            viewModel.updateOrderSelectedStatus(orderId, requireContext().windowSizeClass != WindowSizeClass.Compact)
            navHostFragment?.let {
                showOrderDetail(orderId, it, startPaymentsFlow = startPaymentsFlow)
            } ?: run {
                // Phone layout
                if (sharedView != null) {
                    showOrderDetailWithSharedTransition(
                        orderId = orderId,
                        allOrderIds = allOrderIds,
                        sharedView = sharedView
                    )
                } else {
                    showOrderDetail(orderId)
                }
            }
        }
    }

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        handleNewSearchRequest(query)
        WPActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        // only display the order status list if the search query is empty
        if (newText.isEmpty()) {
            if (searchQuery.isNotEmpty()) {
                viewModel.loadOrders()
            }

            searchQuery = ""
        }

        if (newText.length > 2) {
            submitSearchDelayed(newText)
        } else {
            binding.orderListView.clearAdapterData()
            hideEmptyView()
        }
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        isSearching = true
        checkOrientation()
        onSearchViewActiveChanged(isActive = true)
        binding.orderFiltersCard.isVisible = false
        binding.orderListView.clearAdapterData()
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        clearSearchResults()
        searchMenuItem?.isVisible = true
        viewModel.onSearchClosed()
        binding.orderFiltersCard.isVisible = true
        onSearchViewActiveChanged(isActive = false)
        return true
    }

    private fun clearSearchResults() {
        if (isSearching) {
            if (requireContext().windowSizeClass == WindowSizeClass.Compact) {
                searchQuery = ""
                isSearching = false
                disableSearchListeners()
                updateActivityTitle()
                searchMenuItem?.collapseActionView()
                (activity as? MainActivity)?.showBottomNav()
            }
            isSearching = false
        }
    }

    /**
     * Submit the search after a brief delay unless the query has changed - this is used to
     * perform a search while the user is typing
     */
    private fun submitSearchDelayed(query: String) {
        handler.postDelayed(
            {
                searchView?.let {
                    // submit the search if the searchView's query still matches the passed query
                    if (query == it.query.toString()) handleNewSearchRequest(query)
                }
            },
            AppConstants.SEARCH_TYPING_DELAY_MS
        )
    }

    /**
     * Only fired while the user is actively typing in the search
     * field.
     */
    private fun handleNewSearchRequest(query: String) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDERS_LIST_SEARCH,
            mapOf(AnalyticsTracker.KEY_SEARCH to query)
        )

        searchQuery = query
        submitSearchQuery(searchQuery)
    }

    /**
     * Loads a new list with the search query. This can be called while the
     * user is interacting with the search component, or to reload the
     * view state.
     */
    private fun submitSearchQuery(query: String) {
        viewModel.submitSearchOrFilter(searchQuery = query)
    }

    private fun refreshOrders() {
        viewModel.fetchOrdersAndOrderDependencies()
    }

    private fun disableSearchListeners() {
        binding.orderListViewRoot.visibility = View.VISIBLE
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
    }

    /**
     * Method called when user clicks on the search menu icon.
     *
     * 1. The settings menu is hidden when the search filter is active to prevent the search view
     *    getting collapsed if the settings menu from the [MainActivity] is clicked.
     * 2. The order status list view is displayed by default
     */
    private fun enableSearchListeners() {
        hideEmptyView()

        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)
        if (requireContext().windowSizeClass == WindowSizeClass.Compact) {
            handler.postDelayed({
                (activity as? MainActivity)?.hideBottomNav()
            }, HANDLER_DELAY)
        }
    }

    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        if (isLandscape && isSearching) {
            searchView?.post { searchView?.clearFocus() }
        }
    }
    // endregion

    override fun shouldExpandToolbar(): Boolean {
        return binding.orderListView.ordersList.computeVerticalScrollOffset() == 0 && !isSearching
    }

    private fun displaySimplePaymentsWIPCard(show: Boolean) {
        if (!show) {
            binding.simplePaymentsWIPcard.isVisible = false
            return
        }

        binding.simplePaymentsWIPcard.isVisible = true
        binding.simplePaymentsWIPcard.initView(
            getString(R.string.orderlist_simple_payments_wip_title),
            getString(R.string.orderlist_simple_payments_wip_message_enabled),
            onGiveFeedbackClick = { onGiveFeedbackClicked() },
            onDismissClick = {
                FeatureFeedbackSettings(
                    FeatureFeedbackSettings.Feature.SIMPLE_PAYMENTS_AND_ORDER_CREATION,
                    FeedbackState.DISMISSED
                ).registerItself(feedbackPrefs)
                viewModel.onDismissOrderCreationSimplePaymentsFeedback()
            },
            showFeedbackButton = true
        )
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        FeatureFeedbackSettings(
            SIMPLE_PAYMENTS_AND_ORDER_CREATION,
            FeedbackState.GIVEN
        ).registerItself(feedbackPrefs)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.ORDER_CREATION)
            .apply { findNavController().navigateSafely(this) }
    }

    override fun onSwiped(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture) {
        viewModel.onSwipeStatusUpdate(gestureSource)
    }

    private fun displayErrorParsingOrdersCard(show: Boolean) {
        displayErrorTroubleshootingCard(
            show = show,
            title = getString(R.string.orderlist_parsing_error_title),
            message = getString(R.string.orderlist_parsing_error_message),
            troubleshootingClick = { ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.ORDERS_TROUBLESHOOTING) },
            supportContactClick = { openSupportRequestScreen() }
        )
    }

    private fun displayTimeoutErrorCard(show: Boolean) {
        if (FeatureFlag.CONNECTIVITY_TOOL.isEnabled()) {
            displayErrorTroubleshootingCard(
                show = show,
                title = getString(R.string.orderlist_timeout_error_title),
                message = getString(R.string.orderlist_timeout_error_message),
                supportContactClick = {
                    viewModel.changeTroubleshootingBannerVisibility(show = false)
                    openSupportRequestScreen()
                },
                troubleshootingClick = {
                    viewModel.changeTroubleshootingBannerVisibility(show = false)
                    viewModel.trackConnectivityTroubleshootClicked()
                    openConnectivityTool()
                }
            )
        }
    }

    private fun displayErrorTroubleshootingCard(
        show: Boolean,
        title: String,
        message: String,
        troubleshootingClick: () -> Unit,
        supportContactClick: () -> Unit
    ) {
        TransitionManager.beginDelayedTransition(binding.orderListViewRoot)
        if (!show) {
            binding.errorTroubleshootingCard.isVisible = false
            return
        }

        binding.errorTroubleshootingCard.isVisible = true
        binding.errorTroubleshootingCard.initView(
            title = title,
            message = message,
            isExpanded = true,
            mainActionText = getString(R.string.error_troubleshooting),
            secondaryActionText = getString(R.string.support_contact),
            mainActionClick = troubleshootingClick,
            secondaryActionClick = supportContactClick
        )
    }

    private fun openSupportRequestScreen() {
        SupportRequestFormActivity.createIntent(
            context = requireContext(),
            origin = HelpOrigin.ORDERS_LIST,
            extraTags = ArrayList()
        ).let { activity?.startActivity(it) }
    }
}
