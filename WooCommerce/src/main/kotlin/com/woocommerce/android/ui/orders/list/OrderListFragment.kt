package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderStatusListView
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import kotlinx.android.synthetic.main.order_list_view.*
import kotlinx.android.synthetic.main.order_list_view.view.*
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.PROCESSING
import org.wordpress.android.util.DisplayUtils
import java.util.Locale
import javax.inject.Inject
import org.wordpress.android.util.ActivityUtils as WPActivityUtils

class OrderListFragment : TopLevelFragment(),
        OrderStatusListView.OrderStatusListListener, OnQueryTextListener, OnActionExpandListener, OrderListListener {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"
        const val STATE_KEY_IS_FILTER_ENABLED = "is_filter_enabled"
        const val ARG_ORDER_STATUS_FILTER = "args-order-status-filter"

        private const val SEARCH_TYPING_DELAY_MS = 500L
        private const val TAB_INDEX_PROCESSING = 0
        private const val TAB_INDEX_ALL = 1

        fun newInstance(orderStatus: String? = null): OrderListFragment {
            val fragment = OrderListFragment()
            orderStatus?.let {
                val args = Bundle()
                args.putString(ARG_ORDER_STATUS_FILTER, orderStatus)
                fragment.arguments = args
            }
            return fragment
        }
    }

    @Inject internal lateinit var viewModelFactory: ViewModelFactory
    @Inject internal lateinit var uiMessageResolver: UIMessageResolver
    @Inject internal lateinit var selectedSite: SelectedSite
    @Inject internal lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderListViewModel by viewModels { viewModelFactory }
    private var listState: Parcelable? = null // Save the state of the recycler view

    // Alias for interacting with [viewModel.orderStatusFilter] so the value is always
    // identical to the real value on the UI side.
    private var orderStatusFilter: String
        private set(value) { viewModel.orderStatusFilter = value }
        get() = viewModel.orderStatusFilter

    // Alias for interacting with [viewModel.isSearching] so the value is always identical
    // to the real value on the UI side.
    private var isSearching: Boolean
        private set(value) { viewModel.isSearching = value }
        get() = viewModel.isSearching

    private var orderListMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private val searchHandler = Handler()

    // Alias for interacting with [viewModel.searchQuery] so the value is always identical
    // to the real value on the UI side.
    private var searchQuery: String
        private set(value) { viewModel.searchQuery = value }
        get() = viewModel.searchQuery

    /**
     * flag to check if the user selected any order status from the order status list
     * If true, the data in the order list tab is currently visible and will be refreshed
     */
    private var isFilterEnabled: Boolean = false

    private val tabLayout: TabLayout by lazy {
        TabLayout(requireContext(), null, R.attr.tabStyle)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(STATE_KEY_LIST)
            orderStatusFilter = bundle.getString(STATE_KEY_ACTIVE_FILTER, StringUtils.EMPTY)
            isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            isFilterEnabled = bundle.getBoolean(STATE_KEY_IS_FILTER_ENABLED)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        } ?: arguments?.let {
            orderStatusFilter = it.getString(ARG_ORDER_STATUS_FILTER, StringUtils.EMPTY)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_order_list_fragment, menu)

        orderListMenu = menu
        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.orderlist_search_hint)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        refreshOptionsMenu()
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order_list, container, false)
        with(view) {
            orderRefreshLayout?.apply {
                // Set the scrolling view in the custom SwipeRefreshLayout
                scrollUpChild = order_list_view.ordersList
                setOnRefreshListener {
                    AnalyticsTracker.track(Stat.ORDERS_LIST_PULLED_TO_REFRESH)
                    refreshOrders()
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        order_list_view.init(currencyFormatter = currencyFormatter, orderListListener = this)
        order_status_list_view.init(listener = this)
        initializeViewModel()
    }

    override fun onResume() {
        super.onResume()
        addTabLayoutToAppBar(tabLayout)
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Get the english version to use for setting the tab tag.
        val englishTabArray = StringUtils
                .getStringArrayByLocale(requireContext(), R.array.order_list_tabs, "en")

        resources.getStringArray(R.array.order_list_tabs).toList()
                .forEachIndexed { index, title ->
                    val tab = tabLayout.newTab().apply {
                        text = title
                        tag = englishTabArray?.get(index) ?: title
                    }
                    tabLayout.addTab(tab)

                    // If this tab is the one that should be active, select it and load
                    // the appropriate list.
                    if (index == calculateStartupTabPosition()) {
                        orderStatusFilter = calculateOrderStatusFilter(tab)
                        tab.select()
                    }
                }

        listState?.let {
            order_list_view.onFragmentRestoreInstanceState(it)
            listState = null
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                hideEmptyView()

                // Calculate the filter that should be active based on the selected
                // tab and the state of the list.
                val previousOrderStatus = orderStatusFilter
                orderStatusFilter = calculateOrderStatusFilter(tab)

                if (orderStatusFilter != previousOrderStatus) {
                    // store the selected tab in SharedPrefs and clear the adapter data,
                    // then load orders with the calculated filter.
                    AppPrefs.setSelectedOrderListTab(tab.position)
                    order_list_view.clearAdapterData()
                    loadListForActiveTab()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                order_list_view.scrollToTop()
            }
        })

        val filterOrSearchEnabled = isFilterEnabled || isSearching
        showTabs(!filterOrSearchEnabled)

        if (isFilterEnabled) {
            viewModel.submitSearchOrFilter(statusFilter = orderStatusFilter)
        } else if (isSearching) {
            searchHandler.postDelayed({ searchView?.setQuery(searchQuery, true) }, 100)
        } else {
            loadListForActiveTab()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_KEY_LIST, order_list_view.onFragmentSavedInstanceState())
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
        super.onDestroyView()
    }

    /**
     * Gets called when moving between TopLevelFragments
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (isActive) {
            showOptionsMenu(true)
            addTabLayoutToAppBar(tabLayout)

            if (isSearching) {
                clearSearchResults()
            }
        } else {
            removeTabLayoutFromAppBar(tabLayout)
        }
    }

    override fun onReturnedFromChildFragment() {
        showOptionsMenu(true)
        addTabLayoutToAppBar(tabLayout)

        if (isOrderStatusFilterEnabled()) {
            viewModel.reloadListFromCache()
        } else {
            searchHandler.postDelayed({ searchView?.setQuery(searchQuery, true) }, 20)
        }
    }

    override fun onChildFragmentOpened() {
        removeTabLayoutFromAppBar(tabLayout)
    }

    /**
     * This is a replacement for activity?.invalidateOptionsMenu() since that causes the
     * search menu item to collapse
     */
    private fun refreshOptionsMenu() {
        if (!isChildFragmentShowing() && isSearching) {
            enableSearchListeners()
            searchMenuItem?.expandActionView()
            if (isFilterEnabled) displayFilteredList()
        } else {
            val showSearch = shouldShowSearchMenuItem()
            searchMenuItem?.let {
                if (it.isActionViewExpanded) it.collapseActionView()
                if (it.isVisible != showSearch) it.isVisible = showSearch
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
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

    override fun refreshFragmentState() {
        if (isActive) {
            order_list_view?.clearAdapterData()
            refreshOrders() // reload the active list from scratch
        } else {
            // refresh order status options in the background even when order list is hidden
            // This is so that when an order status change takes place, we need to refresh the order
            // status count in the local cache
            viewModel.fetchOrderStatusOptions()
        }
    }

    override fun scrollToTop() {
        order_list_view.scrollToTop()
    }

    private fun initializeViewModel() {
        viewModel.initializeListsForMainTabs()

        // populate views with any existing viewModel data
        viewModel.orderStatusOptions.value?.let { options ->
            // So the order status can be matched to the appropriate label
            order_list_view.setOrderStatusOptions(options)

            updateOrderStatusList(options)
        }

        // setup observers
        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner, Observer {
            orderRefreshLayout?.isRefreshing = it == true
        })

        viewModel.isLoadingMore.observe(viewLifecycleOwner, Observer {
            it?.let { isLoadingMore ->
                order_list_view.setLoadingMoreIndicator(active = isLoadingMore)
            }
        })

        viewModel.orderStatusOptions.observe(viewLifecycleOwner, Observer {
            it?.let { options ->
                // So the order status can be matched to the appropriate label
                order_list_view.setOrderStatusOptions(options)

                updateOrderStatusList(options)
            }
        })

        viewModel.pagedListData.observe(viewLifecycleOwner, Observer {
            updatePagedListData(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowErrorSnack -> {
                    uiMessageResolver.showSnack(event.messageRes)
                    orderRefreshLayout?.isRefreshing = false
                }
                else -> event.isHandled = false
            }
        })

        viewModel.emptyViewType.observe(viewLifecycleOwner, Observer {
            it?.let { emptyViewType ->
                when (emptyViewType) {
                    EmptyViewType.SEARCH_RESULTS -> {
                        empty_view.show(emptyViewType, searchQueryOrFilter = searchQuery)
                    }
                    EmptyViewType.ORDER_LIST -> {
                        empty_view.show(emptyViewType) {
                            ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_ORDERS)
                        }
                    }
                    EmptyViewType.ORDER_LIST_FILTERED -> {
                        empty_view.show(emptyViewType, searchQueryOrFilter = viewModel.orderStatusFilter)
                    }
                    EmptyViewType.NETWORK_OFFLINE, EmptyViewType.NETWORK_ERROR -> {
                        empty_view.show(emptyViewType) {
                            refreshOrders()
                        }
                    }
                    else -> {
                        empty_view.show(emptyViewType)
                    }
                }
            } ?: hideEmptyView()
        })
    }

    private fun hideEmptyView() {
        empty_view?.hide()
    }

    private fun updatePagedListData(pagedListData: PagedList<OrderListItemUIType>?) {
        order_list_view?.submitPagedList(pagedListData)

        if (pagedListData?.size != 0 && isSearching) {
            WPActivityUtils.hideKeyboard(activity)
        }
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
            Stat.ORDER_OPEN, mapOf(
            AnalyticsTracker.KEY_ID to remoteOrderId,
            AnalyticsTracker.KEY_STATUS to orderStatus)
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
        order_list_view_root.visibility = View.VISIBLE
        order_status_list_view.updateOrderStatusListView(orderStatusList.values.toList())
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        orderStatusFilter = orderStatus ?: StringUtils.EMPTY
        if (isAdded) {
            AnalyticsTracker.track(
                    Stat.ORDERS_LIST_FILTER,
                    mapOf(AnalyticsTracker.KEY_STATUS to orderStatus.orEmpty()))

            // Display the filtered list view
            displayFilteredList()

            // Load the filtered list
            order_list_view.clearAdapterData()
            viewModel.submitSearchOrFilter(statusFilter = orderStatus)

            updateActivityTitle()
            searchMenuItem?.isVisible = shouldShowSearchMenuItem()
        }
    }

    /**
     * Calculates the default tab position to display using the following logic:
     * - If no orders for selected store -> "All Orders" tab
     * - If no orders to process -> "All Orders" tab
     * - The last tab the user viewed (saved in SharedPrefs)
     * - Else the "Processing" tab (default)
     *
     * @return the index of the tab to be activated
     */
    private fun calculateStartupTabPosition(): Int {
        val orderStatusOptions = getOrderStatusOptions()
        return if (orderStatusFilter == PROCESSING.value) {
            TAB_INDEX_PROCESSING
        } else if (AppPrefs.hasSelectedOrderListTabPosition()) {
            // If the user has already changed tabs once then select
            // the last tab they had selected.
            AppPrefs.getSelectedOrderListTabPosition()
        } else if (orderStatusOptions.isEmpty() || orderStatusOptions[PROCESSING.value]?.statusCount == 0) {
            // There are no "processing" orders to display, show all.
            TAB_INDEX_ALL
        } else {
            // Default to the "processing" tab if there are orders to
            // process.
            TAB_INDEX_PROCESSING
        }
    }

    private fun getOrderStatusFilterForActiveTab(): String {
        return tabLayout.getTabAt(tabLayout.selectedTabPosition)?.let {
            calculateOrderStatusFilter(it)
        } ?: StringUtils.EMPTY
    }

    /**
     * Calculates the filter to apply based on the state of filtering and which tab is selected.
     *
     * @return If there is an active filter, return that filter. Otherwise, if the "Processing"
     * tab is currently selected, return a filter of "processing", else return null (no filter).
     */
    private fun calculateOrderStatusFilter(tab: TabLayout.Tab): String {
        return when {
            isFilterEnabled -> orderStatusFilter
            tab.position == 0 -> (tab.tag as? String)?.toLowerCase(Locale.getDefault()) ?: StringUtils.EMPTY
            else -> StringUtils.EMPTY
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
        showTabs(false)
        isSearching = true
        checkOrientation()
        onSearchViewActiveChanged(isActive = true)
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
        loadListForActiveTab()
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
        }
    }

    private fun loadListForActiveTab() {
        orderStatusFilter = getOrderStatusFilterForActiveTab()
        getOrderStatusFilterForActiveTab()
        when (tabLayout.selectedTabPosition) {
            TAB_INDEX_PROCESSING -> viewModel.loadProcessingList()
            TAB_INDEX_ALL -> viewModel.loadAllList()
        }
    }

    /**
     * Submit the search after a brief delay unless the query has changed - this is used to
     * perform a search while the user is typing
     */
    private fun submitSearchDelayed(query: String) {
        searchHandler.postDelayed({
            searchView?.let {
                // submit the search if the searchView's query still matches the passed query
                if (query == it.query.toString()) handleNewSearchRequest(query)
            }
        }, SEARCH_TYPING_DELAY_MS)
    }

    /**
     * Only fired while the user is actively typing in the search
     * field.
     */
    private fun handleNewSearchRequest(query: String) {
        AnalyticsTracker.track(
                Stat.ORDERS_LIST_FILTER,
                mapOf(AnalyticsTracker.KEY_SEARCH to query))

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

    private fun isOrderStatusFilterEnabled() = isFilterEnabled || !isSearching

    private fun showTabs(show: Boolean) {
        if (show && tabLayout.visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(tabLayout)
        } else if (!show && tabLayout.visibility != View.GONE) {
            tabLayout.visibility = View.GONE
        }
    }

    private fun refreshOrders() {
        viewModel.fetchOrdersAndOrderDependencies()
    }

    private fun disableSearchListeners() {
        orderListMenu?.findItem(R.id.menu_settings)?.isVisible = true
        order_list_view_root.visibility = View.VISIBLE
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
        hideOrderStatusListView()
        showTabs(true)
        (activity as? MainActivity)?.showBottomNav()

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
                .plus(orderStatusFilter.let { filter ->
                    val orderStatusLabel = getOrderStatusOptions()[filter]?.label
                    getString(R.string.orderlist_filtered, orderStatusLabel)
                })

        searchView?.findViewById<EditText>(R.id.search_src_text)?.also {
            it.setHintTextColor(Color.WHITE)
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
                it.isEnabled = true
            }
            searchView?.queryHint = getString(R.string.orderlist_search_hint)

            (activity as? MainActivity)?.hideBottomNav()
        }
    }

    private fun displayOrderStatusListView() {
        order_status_list_view.visibility = View.VISIBLE
        order_list_view.visibility = View.GONE
        orderRefreshLayout.isEnabled = false
    }

    private fun hideOrderStatusListView() {
        order_status_list_view.visibility = View.GONE
        order_list_view.visibility = View.VISIBLE
        orderRefreshLayout.isEnabled = true
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
            order_list_view.clearAdapterData()
        }
    }
    // endregion

    private fun addTabLayoutToAppBar(tabLayout: TabLayout) {
        (activity?.findViewById<View>(R.id.app_bar_layout) as? AppBarLayout)?.let { appBar ->
            if (isActive && !appBar.children.contains(tabLayout)) {
                appBar.addView(tabLayout)
            }
        }
    }

    private fun removeTabLayoutFromAppBar(tabLayout: TabLayout) {
        (activity?.findViewById<View>(R.id.app_bar_layout) as? AppBarLayout)?.removeView(tabLayout)
    }

    override fun isScrolledToTop() = order_list_view.getCurrentPosition() == 0
}
