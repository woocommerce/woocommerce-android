package com.woocommerce.android.ui.orders.list

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentOrderListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderStatusListView
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.ui.orders.simplepayments.SimplePaymentsDialog
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.login.util.getColorFromAttribute
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject
import org.wordpress.android.util.ActivityUtils as WPActivityUtils

@AndroidEntryPoint
class OrderListFragment :
    TopLevelFragment(R.layout.fragment_order_list),
    OrderStatusListView.OrderStatusListListener,
    OnQueryTextListener,
    OnActionExpandListener,
    OrderListListener {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"
        const val STATE_KEY_IS_FILTER_ENABLED = "is_filter_enabled"
        const val ORDER_FILTER_RESULT_KEY = "order_filter_result"

        private const val SEARCH_TYPING_DELAY_MS = 500L
    }

    @Inject internal lateinit var uiMessageResolver: UIMessageResolver
    @Inject internal lateinit var selectedSite: SelectedSite
    @Inject internal lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderListViewModel by viewModels()

    // Alias for interacting with [viewModel.orderStatusFilter] so the value is always
    // identical to the real value on the UI side.
    private var orderStatusFilter: String
        private set(value) {
            viewModel.orderStatusFilter = value
        }
        get() = viewModel.orderStatusFilter

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
    private val searchHandler = Handler(Looper.getMainLooper())
    private var simplePaymentMenuItem: MenuItem? = null

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!

    // Alias for interacting with [viewModel.searchQuery] so the value is always identical
    // to the real value on the UI side.
    private var searchQuery: String
        private set(value) {
            viewModel.searchQuery = value
        }
        get() = viewModel.searchQuery

    /**
     * flag to check if the user selected any order status from the order status list
     * If true, the data in the order list tab is currently visible and will be refreshed
     */
    private var isFilterEnabled: Boolean = false

    private val emptyView
        get() = binding.orderListView.emptyView

    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)?.state ?: FeatureFeedbackSettings.FeedbackState.UNANSWERED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { bundle ->
            orderStatusFilter = bundle.getString(STATE_KEY_ACTIVE_FILTER, StringUtils.EMPTY)
            isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            isFilterEnabled = bundle.getBoolean(STATE_KEY_IS_FILTER_ENABLED)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_order_list_fragment, menu)

        orderListMenu = menu
        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.orderlist_search_hint)

        simplePaymentMenuItem = menu.findItem(R.id.menu_add)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        refreshOptionsMenu()
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        _binding = FragmentOrderListBinding.bind(view)
        binding.orderListView.init(currencyFormatter = currencyFormatter, orderListListener = this)
        binding.orderStatusListView.init(listener = this)
        binding.orderRefreshLayout.apply {
            // Set the scrolling view in the custom refresh SwipeRefreshLayout
            scrollUpChild = binding.orderListView.ordersList
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.ORDERS_LIST_PULLED_TO_REFRESH)
                refreshOrders()
            }
        }

        initializeViewModel()
        initializeResultHandlers()

        if (isFilterEnabled) {
            viewModel.submitSearchOrFilter(statusFilter = orderStatusFilter)
        } else if (isSearching) {
            searchHandler.postDelayed({ searchView?.setQuery(searchQuery, true) }, 100)
        }

        setupOrderFilters()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (FeatureFlag.SIMPLE_PAYMENTS.isEnabled()) {
            displaySimplePaymentsWIPCard(true)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_ACTIVE_FILTER, orderStatusFilter)
        outState.putBoolean(STATE_KEY_IS_SEARCHING, isSearching)
        outState.putBoolean(STATE_KEY_IS_FILTER_ENABLED, isFilterEnabled)
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        disableSearchListeners()
        searchView = null
        orderListMenu = null
        searchMenuItem = null
        simplePaymentMenuItem = null
        super.onDestroyView()
        _binding = null
    }

    private fun isSimplePaymentsAvailable(): Boolean {
        return FeatureFlag.SIMPLE_PAYMENTS.isEnabled() &&
            AppPrefs.isSimplePaymentsEnabled &&
            viewModel.isCardReaderOnboardingCompleted()
    }

    /**
     * This is a replacement for activity?.invalidateOptionsMenu() since that causes the
     * search menu item to collapse
     */
    private fun refreshOptionsMenu() {
        if (!isChildFragmentShowing() && isSearching) {
            enableSearchListeners()
            val savedSearchQuery = searchQuery
            searchMenuItem?.expandActionView()
            searchQuery = savedSearchQuery
            if (isFilterEnabled) displayFilteredList()
        } else {
            val showSearch = shouldShowSearchMenuItem()
            searchMenuItem?.let {
                if (it.isActionViewExpanded) it.collapseActionView()
                if (it.isVisible != showSearch) it.isVisible = showSearch
            }
        }

        simplePaymentMenuItem?.isVisible = isSimplePaymentsAvailable()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }
            R.id.menu_add -> {
                showSimplePaymentsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isChildFragmentShowing() = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false

    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = isChildFragmentShowing()
        return when {
            (isChildShowing) -> false
            (isFilterEnabled) -> false
            else -> true
        }
    }

    private fun getOrderStatusOptions() = viewModel.orderStatusOptions.value.orEmpty()

    override fun getFragmentTitle() = if (isFilterEnabled || isSearching) "" else getString(R.string.orders)

    override fun scrollToTop() {
        binding.orderListView.scrollToTop()
    }

    @Suppress("LongMethod")
    private fun initializeViewModel() {
        viewModel.initializeOrdersList()

        // populate views with any existing viewModel data
        viewModel.orderStatusOptions.value?.let { options ->
            // So the order status can be matched to the appropriate label
            binding.orderListView.setOrderStatusOptions(options)

            updateOrderStatusList(options)
        }

        // setup observers
        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner) {
            binding.orderRefreshLayout.isRefreshing = it == true
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) {
            it?.let { isLoadingMore ->
                binding.orderListView.setLoadingMoreIndicator(active = isLoadingMore)
            }
        }

        viewModel.orderStatusOptions.observe(viewLifecycleOwner) {
            it?.let { options ->
                // So the order status can be matched to the appropriate label
                binding.orderListView.setOrderStatusOptions(options)

                updateOrderStatusList(options)
            }
        }

        viewModel.pagedListData.observe(viewLifecycleOwner) {
            updatePagedListData(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowErrorSnack -> {
                    uiMessageResolver.showSnack(event.messageRes)
                    binding.orderRefreshLayout.isRefreshing = false
                }
                is ShowOrderFilters -> showOrderFilters()
                else -> event.isHandled = false
            }
        }

        viewModel.emptyViewType.observe(viewLifecycleOwner) {
            it?.let { emptyViewType ->
                when (emptyViewType) {
                    EmptyViewType.SEARCH_RESULTS -> {
                        binding.orderStatusListView
                        emptyView.show(emptyViewType, searchQueryOrFilter = searchQuery)
                    }
                    EmptyViewType.ORDER_LIST -> {
                        emptyView.show(emptyViewType) {
                            ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_ORDERS)
                        }
                    }
                    EmptyViewType.ORDER_LIST_FILTERED -> {
                        emptyView.show(emptyViewType, searchQueryOrFilter = viewModel.orderStatusFilter)
                    }
                    EmptyViewType.NETWORK_OFFLINE, EmptyViewType.NETWORK_ERROR -> {
                        emptyView.show(emptyViewType) {
                            refreshOrders()
                        }
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
        }
    }

    private fun initializeResultHandlers() {
        handleResult<Order>(SimplePaymentsDialog.KEY_SIMPLE_PAYMENTS_RESULT) { order ->
            binding.orderListView.post {
                openOrderDetail(order.localId.value, order.remoteId, order.status.value)
            }
        }
    }

    private fun showOrderFilters() {
        findNavController().navigate(R.id.action_orderListFragment_to_orderFilterListFragment)
    }

    private fun showSimplePaymentsDialog() {
        AnalyticsTracker.track(Stat.SIMPLE_PAYMENTS_FLOW_STARTED)
        findNavController().navigate(R.id.action_orderListFragment_to_simplePaymentsDialog)
    }

    private fun hideEmptyView() {
        emptyView.hide()
    }

    private fun updatePagedListData(pagedListData: PagedList<OrderListItemUIType>?) {
        binding.orderListView.submitPagedList(pagedListData)
    }

    /**
     * We use this to clear the options menu when navigating to a child destination - otherwise this
     * fragment's menu will continue to appear when the child is shown
     */
    private fun showOptionsMenu(show: Boolean) {
        setHasOptionsMenu(show)
        if (show) {
            refreshOptionsMenu()
        }
    }

    override fun openOrderDetail(localOrderId: Int, remoteOrderId: Long, orderStatus: String) {
        // Track user clicked to open an order and the status of that order
        AnalyticsTracker.track(
            Stat.ORDER_OPEN,
            mapOf(
                AnalyticsTracker.KEY_ID to remoteOrderId,
                AnalyticsTracker.KEY_STATUS to orderStatus
            )
        )

        // if a search is active, we need to collapse the search view so order detail can show it's title and then
        // remember the user was searching (since both searchQuery and isSearching will be reset)
        if (isSearching) {
            val savedSearch = searchQuery
            clearSearchResults()
            updateActivityTitle()
            searchQuery = savedSearch
            isSearching = true
        }

        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showOrderDetail(selectedSite.get().id, localOrderId, remoteOrderId)
    }

    private fun updateOrderStatusList(orderStatusList: Map<String, WCOrderStatusModel>) {
        binding.orderListViewRoot.visibility = View.VISIBLE
        binding.orderStatusListView.updateOrderStatusListView(orderStatusList.values.toList())
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        orderStatusFilter = orderStatus ?: StringUtils.EMPTY
        if (isAdded) {
            AnalyticsTracker.track(
                Stat.ORDERS_LIST_FILTER,
                mapOf(AnalyticsTracker.KEY_STATUS to orderStatus.orEmpty())
            )

            // Display the filtered list view
            displayFilteredList()

            // Load the filtered list
            binding.orderListView.clearAdapterData()
            viewModel.submitSearchOrFilter(statusFilter = orderStatus)

            updateActivityTitle()
            searchMenuItem?.isVisible = shouldShowSearchMenuItem()
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
        when {
            newText.isEmpty() -> {
                displayOrderStatusListView()
                searchQuery = ""
            }
            else -> hideOrderStatusListView()
        }

        if (newText.length > 2) {
            submitSearchDelayed(newText)
        } else {
            clearOrderListData()
        }

        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        clearOrderListData()
        isSearching = true
        checkOrientation()
        onSearchViewActiveChanged(isActive = true)
        simplePaymentMenuItem?.isVisible = false
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        if (isFilterEnabled) {
            closeFilteredList()
            enableSearchListeners()
            searchMenuItem?.isVisible = false
            searchView?.post { searchMenuItem?.expandActionView() }
        } else {
            clearSearchResults()
            searchMenuItem?.isVisible = true
        }
        viewModel.onSearchClosed()
        simplePaymentMenuItem?.isVisible = isSimplePaymentsAvailable()
        onSearchViewActiveChanged(isActive = false)
        return true
    }

    private fun clearSearchResults() {
        if (isSearching) {
            searchQuery = ""
            isSearching = false
            disableSearchListeners()
            updateActivityTitle()
            searchMenuItem?.collapseActionView()
            (activity as? MainActivity)?.showBottomNav()
        }
    }

    /**
     * Submit the search after a brief delay unless the query has changed - this is used to
     * perform a search while the user is typing
     */
    private fun submitSearchDelayed(query: String) {
        searchHandler.postDelayed(
            {
                searchView?.let {
                    // submit the search if the searchView's query still matches the passed query
                    if (query == it.query.toString()) handleNewSearchRequest(query)
                }
            },
            SEARCH_TYPING_DELAY_MS
        )
    }

    /**
     * Only fired while the user is actively typing in the search
     * field.
     */
    private fun handleNewSearchRequest(query: String) {
        AnalyticsTracker.track(
            Stat.ORDERS_LIST_FILTER,
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
        orderListMenu?.findItem(R.id.menu_settings)?.isVisible = true
        binding.orderListViewRoot.visibility = View.VISIBLE
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
        hideOrderStatusListView()

        if (isFilterEnabled) closeFilteredList()
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

        orderListMenu?.findItem(R.id.menu_settings)?.isVisible = false
        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)
        displayOrderStatusListView()

        (activity as? MainActivity)?.hideBottomNav()
    }

    /**
     * Method called when user clicks on an order status from [OrderStatusListView]
     *
     * 1. Hide the order status view
     * 2. Disable search
     * 3. Display the order status selected in the search query text area
     * 4. Set [isFilterEnabled] flag to true.
     *    This is because once an order status is selected and the order list for that status is displayed,
     *    when back is clicked, the order list needs to be refreshed again from the api,
     *    since we only store the orders for a particular status in local cache.
     */
    private fun displayFilteredList() {
        isFilterEnabled = true
        hideOrderStatusListView()
        searchView?.queryHint = getString(R.string.orders)
            .plus(
                orderStatusFilter.let { filter ->
                    val orderStatusLabel = getOrderStatusOptions()[filter]?.label
                    getString(R.string.orderlist_filtered, orderStatusLabel)
                }
            )

        searchView?.findViewById<EditText>(R.id.search_src_text)?.also {
            it.setHintTextColor(requireContext().getColorFromAttribute(R.attr.colorOnSurface))
            it.isEnabled = false
        }
        (activity as? MainActivity)?.showBottomNav()
    }

    /**
     * Method called when user clicks on the back button after selecting an order status.
     *
     * 1. Hide the order status view
     * 2. Enable search again and update the hint query
     */
    private fun closeFilteredList() {
        if (isFilterEnabled) {
            isFilterEnabled = false
            searchView?.findViewById<EditText>(R.id.search_src_text)?.also {
                it.setHintTextColor(requireContext().getColorFromAttribute(android.R.attr.textColorHint))
                it.isEnabled = true
            }
            searchView?.queryHint = getString(R.string.orderlist_search_hint)

            (activity as? MainActivity)?.hideBottomNav()
        }
    }

    private fun displayOrderStatusListView() {
        binding.orderStatusListView.visibility = View.VISIBLE
        binding.orderListView.visibility = View.GONE
        binding.orderRefreshLayout.isEnabled = false
    }

    private fun hideOrderStatusListView() {
        binding.orderStatusListView.visibility = View.GONE
        binding.orderListView.visibility = View.VISIBLE
        binding.orderRefreshLayout.isEnabled = true
    }

    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        if (isLandscape && isSearching) {
            searchView?.post { searchView?.clearFocus() }
        }
    }

    /**
     * Method to clear adapter data only if order filter is not enabled.
     * This is to prevent the order filter list data from being cleared when fragment state is restored
     */
    private fun clearOrderListData() {
        if (!isFilterEnabled) {
            binding.orderListView.clearAdapterData()
        }
    }
    // endregion

    override fun shouldExpandToolbar(): Boolean {
        return binding.orderListView.ordersList.computeVerticalScrollOffset() == 0 && !isSearching
    }

    private fun setupOrderFilters() {
        binding.orderFiltersCard.isVisible = FeatureFlag.ORDER_FILTERS.isEnabled()
        if (FeatureFlag.ORDER_FILTERS.isEnabled()) {
            binding.orderFiltersCard.setClickListener { viewModel.onFiltersButtonTapped() }
            handleResult<Boolean>(ORDER_FILTER_RESULT_KEY) {
                viewModel.onFiltersChanged(it)
            }
        }
    }

    private fun displaySimplePaymentsWIPCard(show: Boolean) {
        if (!show ||
            feedbackState == FeatureFeedbackSettings.FeedbackState.DISMISSED ||
            !viewModel.isCardReaderOnboardingCompleted()
        ) {
            binding.simplePaymentsWIPcard.isVisible = false
            return
        }

        val isEnabled = AppPrefs.isSimplePaymentsEnabled
        @StringRes val messageId = if (isEnabled) {
            R.string.orderlist_simple_payments_wip_message_enabled
        } else {
            R.string.orderlist_simple_payments_wip_message_disabled
        }

        binding.simplePaymentsWIPcard.isVisible = true
        binding.simplePaymentsWIPcard.initView(
            getString(R.string.orderlist_simple_payments_wip_title),
            getString(messageId),
            onGiveFeedbackClick = { onGiveFeedbackClicked() },
            onDismissClick = { onDismissWIPCardClicked() },
            showFeedbackButton = isEnabled
        )
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            Stat.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.SIMPLE_PAYMENTS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissWIPCardClicked() {
        AnalyticsTracker.track(
            Stat.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.DISMISSED)
        displaySimplePaymentsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeatureFeedbackSettings.FeedbackState) {
        FeatureFeedbackSettings(
            FeatureFeedbackSettings.Feature.SIMPLE_PAYMENTS.name,
            state
        ).registerItselfWith(TAG)
    }
}
