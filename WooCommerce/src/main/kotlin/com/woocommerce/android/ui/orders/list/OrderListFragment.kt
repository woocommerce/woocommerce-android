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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
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
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.pinFabAboveBottomNavigationBar
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment.Companion.KEY_BARCODE_SCANNING_SCAN_STATUS
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.DefaultOrderListItemLookup
import com.woocommerce.android.ui.orders.OrderSelectionItemKeyProvider
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.OrderStatusSelectorDialog
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.ui.products.MutableMultipleSelectionPredicate
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject
import org.wordpress.android.util.ActivityUtils as WPActivityUtils

@AndroidEntryPoint
@Suppress("LargeClass")
class OrderListFragment :
    TopLevelFragment(R.layout.fragment_order_list),
    OnQueryTextListener,
    OnActionExpandListener,
    OrderListListener,
    SwipeToComplete.OnSwipeListener,
    ActionMode.Callback {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"
        const val FILTER_CHANGE_NOTICE_KEY = "filters_changed_notice"

        private const val JITM_FRAGMENT_TAG = "jitm_orders_fragment"
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.3f
        private const val LAST_WINDOW_SIZE_WAS_LARGER_THAN_COMPACT = "last_window_size_was_larger_than_compact"
        private const val HANDLER_DELAY = 200L
        private const val TOP_OFFSET_PROGRESS_WITH_ACTION_MODE = 200
    }

    @Inject
    internal lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    internal lateinit var selectedSite: SelectedSite

    @Inject
    internal lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var feedbackPrefs: FeedbackPrefs

    private var tracker: SelectionTracker<Long>? = null
    private var actionMode: ActionMode? = null
    private val selectionPredicate = MutableMultipleSelectionPredicate<Long>(
        maxSelectionCount = OrderListViewModel.BULK_UPDATE_COUNT_LIMIT
    )
    private val viewModel: OrderListViewModel by viewModels()
    private val communicationViewModel: OrdersCommunicationViewModel by activityViewModels()
    private var snackBar: Snackbar? = null

    override fun onStop() {
        snackBar?.dismiss()
        super.onStop()
    }

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
            viewModel.isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // We discovered cases where the callback was being invoked multiple times.
                    // Most likely due to lagging fragment transition, it was not removed from the
                    // onBackPressedDispatcher before next back press event.
                    // The check below ensures that the callback is only called once to prevent crashes.
                    if (findNavController().currentDestination?.id != R.id.orders) return

                    selectedOrder.selectOrder(-1L)
                    if (requireContext().isTwoPanesShouldBeUsed) {
                        if (!binding.detailPaneContainer.findNavController().popBackStack()) {
                            findNavController().popBackStack()
                        }
                    } else if (viewModel.isSearching) {
                        handleSearchViewCollapse()
                    } else {
                        val result =
                            _binding?.detailPaneContainer?.findNavController()?.navigateUp() ?: false
                        val isCompactScreen = !requireContext().isTwoPanesShouldBeUsed
                        if (!result && _binding?.listPaneContainer?.isVisible != true && isCompactScreen) {
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
        // When Action Mode bar appears, the progress bar can be covered up by it, so we're pushing it down
        // to make sure it stays visible.
        binding.listPaneContainer.setProgressViewOffset(false, 0, TOP_OFFSET_PROGRESS_WITH_ACTION_MODE)

        initObservers()
        initializeResultHandlers()
        adjustUiForDeviceType(savedInstanceState)
        binding.orderFiltersCard.setClickListener { viewModel.onFiltersButtonTapped() }
        initCreateOrderFAB(binding.createOrderButton)
        initSwipeBehaviour()

        if (FeatureFlag.BULK_UPDATE_ORDERS_STATUS.isEnabled()) {
            addSelectionTracker()
        }
    }

    private fun addSelectionTracker() {
        tracker = SelectionTracker.Builder(
            "orderSelection", // a string to identify our selection in the context of this fragment
            binding.orderListView.ordersList, // the RecyclerView where we will apply the tracker
            OrderSelectionItemKeyProvider(binding.orderListView.ordersList), // the source of selection keys
            DefaultOrderListItemLookup(
                binding.orderListView.ordersList
            ), // the source of information about recycler items
            StorageStrategy.createLongStorage() // strategy for type-safe storage of the selection state
        ).withSelectionPredicate(selectionPredicate)
            .build()

        (binding.orderListView.ordersList.adapter as? OrderListAdapter)?.tracker = tracker

        tracker?.addObserver(
            object : SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    val selectionCount = tracker?.selection?.size() ?: 0
                    selectionPredicate.currentSelectionCount = selectionCount

                    viewModel.onSelectionChanged(selectionCount)
                }
            }
        )
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
        viewModel.isSearching = true
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
                binding.twoPaneLayoutGuideline.setGuidelinePercent(TABLET_LANDSCAPE_WIDTH_RATIO)
            }
        }
        binding.listPaneContainer.visibility = View.VISIBLE
        binding.detailPaneContainer.visibility = View.VISIBLE
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
        viewModel.loadOrders()
        if (requireContext().isTwoPanesShouldBeUsed) {
            refreshOrders()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_IS_SEARCHING, viewModel.isSearching)
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)
        if (findNavController().currentDestination?.id == R.id.orders) {
            // We want to check if [OrderListFragment] is the current destination (at the top of the backstack),
            // because onSaveInstanceState hook is called in all the fragments in the back stack on config change,
            // even if they are not being recreated.
            if (requireContext().isTwoPanesShouldBeUsed) {
                outState.putBoolean(LAST_WINDOW_SIZE_WAS_LARGER_THAN_COMPACT, true)
            }
        }
        tracker?.onSaveInstanceState(outState)

        _binding?.let { binding ->
            val adapter = binding.orderListView.ordersList.adapter as? OrderListAdapter
            adapter?.let {
                viewModel.orderIdAndPositionBackup = it.orderIdAndPosition
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        tracker?.run {
            onRestoreInstanceState(savedInstanceState)
            _binding?.let { binding ->
                restoreAdapterBulkSelectionState(binding)
                if (hasSelection()) {
                    setItemsSelected(selection.toList(), true)
                }
            }
        }

        super.onViewStateRestored(savedInstanceState)
    }

    private fun restoreAdapterBulkSelectionState(binding: FragmentOrderListBinding) {
        val adapter = binding.orderListView.ordersList.adapter as? OrderListAdapter
        if (adapter != null) {
            adapter.orderIdAndPosition = viewModel.orderIdAndPositionBackup
        }
    }

    override fun onDestroyView() {
        disableSearchListeners()
        handler.removeCallbacksAndMessages(null)
        searchView = null
        orderListMenu = null
        searchMenuItem = null
        tracker = null
        actionMode = null
        super.onDestroyView()
        _binding = null
    }

    /**
     * This is a replacement for activity?.invalidateOptionsMenu() since that causes the
     * search menu item to collapse
     */
    private fun refreshOptionsMenu() {
        if (!isChildFragmentShowing() && viewModel.isSearching) {
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

    override fun getFragmentTitle() = if (viewModel.isSearching) "" else getString(R.string.orders)

    override fun scrollToTop() {
        binding.orderListView.scrollToTop()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun initObservers() {
        // setup observers
        selectedOrder.selectedOrderId.observe(viewLifecycleOwner) {
            viewModel.updateOrderSelectedStatus(
                orderId = selectedOrder.selectedOrderId.value ?: -1,
                requireContext().isTwoPanesShouldBeUsed
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
            updateOrderSelectedStatus()
            updatePagedListData(it)
            if (requireContext().isTwoPanesShouldBeUsed) {
                when {
                    communicationViewModel.event.value is
                    OrdersCommunicationViewModel.CommunicationEvent.OrdersLoaded -> {
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
                        selectedOrder.selectedOrderId.value == null ||
                        selectedOrder.selectedOrderId.value == -1L -> {
                        handler.postDelayed({
                            val firstOrder = it
                                .filterIsInstance<OrderListItemUIType.OrderListItemUI>()
                                .firstOrNull()

                            firstOrder?.let { firstOrder ->
                                openFirstOrder()
                                selectedOrder.selectOrder(firstOrder.orderId)
                            }
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
                            openFirstOrder()
                        }, HANDLER_DELAY)
                    }
                }
            }
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
                        NavGraphMainDirections.actionGlobalAuthenticatedWebViewFragment(
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
                is MultiLiveEvent.Event.ShowActionStringSnackbar -> uiMessageResolver.showActionSnack(
                    message = event.message,
                    actionText = event.actionText,
                    action = event.action
                )

                is OrderListViewModel.OrderListEvent.RetryLoadingOrders -> refreshOrders()
                is OrderListViewModel.OrderListEvent.OpenOrderCreationWithSimplePaymentsMigration ->
                    openOrderCreationFragment(indicateSimplePaymentsMigration = true)

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
            new.isErrorFetchingDataBannerVisible.takeIfNotEqualTo(old?.isErrorFetchingDataBannerVisible) {
                displayErrorParsingOrdersCard(it)
            }
            new.shouldDisplayTroubleshootingBanner.takeIfNotEqualTo(old?.shouldDisplayTroubleshootingBanner) {
                displayTimeoutErrorCard(it)
            }
            new.orderListState?.takeIfNotEqualTo(old?.orderListState) {
                handleListState(it)
            }
            new.selectionCount?.takeIfNotEqualTo(old?.selectionCount) { count ->
                actionMode?.title = StringUtils.getQuantityString(
                    context = requireContext(),
                    quantity = count,
                    default = R.string.orderlist_selection_count,
                    one = R.string.orderlist_selection_count_single
                )
            }
            new.isBottomNavBarVisible.takeIfNotEqualTo(old?.isBottomNavBarVisible) { isBottomNavBarVisible ->
                showBottomNavBar(isVisible = isBottomNavBarVisible)
            }
            new.isAddOrderButtonVisible.takeIfNotEqualTo(old?.isAddOrderButtonVisible) { isVisible ->
                showAddOrderButton(show = isVisible)
            }
            new.isBulkUpdating.takeIfNotEqualTo(old?.isBulkUpdating) { isBulkUpdating ->
                // Instead of recreating a new progress bar, re-use the progress bar in the SwipeRefreshLayout
                binding.listPaneContainer.isRefreshing = isBulkUpdating
            }
        }
        viewModel.lastUpdateOrdersList.observe(viewLifecycleOwner) { lastUpdate ->
            binding.orderFiltersCard.updateLastUpdate(lastUpdate)
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

    private fun handleListState(orderListState: OrderListViewModel.ViewState.OrderListState) {
        when (orderListState) {
            OrderListViewModel.ViewState.OrderListState.Selecting -> {
                actionMode = (requireActivity() as AppCompatActivity)
                    .startSupportActionMode(this@OrderListFragment)
                delayMultiSelection()
                enableFiltersCard(false)
                enableOrdersRefresh(false)
            }

            OrderListViewModel.ViewState.OrderListState.Browsing -> {
                actionMode?.finish()
                enableFiltersCard(true)
                enableOrdersRefresh(true)
            }
        }
    }

    private fun delayMultiSelection() {
        selectionPredicate.selectMultiple = false
        binding.orderListView.ordersList.post {
            selectionPredicate.selectMultiple = true
        }
    }

    private fun enableFiltersCard(enable: Boolean) {
        binding.orderFiltersCard.isEnabled(enable)
    }

    private fun enableOrdersRefresh(enable: Boolean) {
        binding.listPaneContainer.isEnabled = enable
    }

    private fun showBottomNavBar(isVisible: Boolean) {
        if (!isVisible) {
            (activity as? MainActivity)?.hideBottomNav()
        } else {
            (activity as? MainActivity)?.showBottomNav()
        }
    }

    private fun showAddOrderButton(show: Boolean) {
        if (show) {
            uiMessageResolver.anchorViewId = binding.createOrderButton.id
            binding.createOrderButton.show()
        } else {
            uiMessageResolver.anchorViewId = null
            binding.createOrderButton.hide()
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
            requireContext().isTwoPanesShouldBeUsed
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
            viewModel.onBulkOrderStatusChanged(
                orderIds = tracker?.selection?.toList() ?: emptyList(),
                newStatus = Order.Status.fromValue(it.newStatus)
            )
        }
    }

    private fun showOrderFilters() {
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrderFilterListFragment()
        )
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
            requireContext().isTwoPanesShouldBeUsed
        )

        if (requireContext().isTwoPanesShouldBeUsed) {
            _binding?.createOrderButton?.show()
        } else {
            _binding?.createOrderButton?.hide()
        }

        // if a search is active, we need to collapse the search view so order detail can show it's title and then
        // remember the user was searching (since both searchQuery and isSearching will be reset)
        if (viewModel.isSearching) {
            val savedSearch = searchQuery
            clearSearchResults()
            updateActivityTitle()
            searchQuery = savedSearch
            viewModel.isSearching = true
        }
        (activity as? MainNavigationRouter)?.run {
            val navHostFragment = if (requireContext().isTwoPanesShouldBeUsed) {
                childFragmentManager.findFragmentById(R.id.detailPaneContainer) as NavHostFragment
            } else {
                null
            }
            selectedOrder.selectOrder(orderId)
            viewModel.updateOrderSelectedStatus(orderId, requireContext().isTwoPanesShouldBeUsed)
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
        viewModel.isSearching = true
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
        if (viewModel.isSearching) {
            if (!requireContext().isTwoPanesShouldBeUsed) {
                searchQuery = ""
                viewModel.isSearching = false
                disableSearchListeners()
                updateActivityTitle()
                searchMenuItem?.collapseActionView()
                (activity as? MainActivity)?.showBottomNav()
            }
            viewModel.isSearching = false
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
        if (!requireContext().isTwoPanesShouldBeUsed) {
            handler.postDelayed({
                (activity as? MainActivity)?.hideBottomNav()
            }, HANDLER_DELAY)
        }
    }

    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        if (isLandscape && viewModel.isSearching) {
            searchView?.post { searchView?.clearFocus() }
        }
    }
    // endregion

    override fun shouldExpandToolbar(): Boolean {
        return binding.orderListView.ordersList.computeVerticalScrollOffset() == 0 && !viewModel.isSearching
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

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_action_mode_orders_list, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_orderlist_update_status -> {
                viewModel.onBulkUpdateStatusClicked()
                true
            }

            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        tracker?.clearSelection()
        actionMode = null
    }
}
