package com.woocommerce.android.ui.orders.list

import android.content.Context
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
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderStatusSelectorDialog
import com.woocommerce.android.util.CurrencyFormatter
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.orderRefreshLayout
import kotlinx.android.synthetic.main.fragment_order_list.ordersList
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class OrderListFragment : TopLevelFragment(),
        OrderStatusSelectorDialog.OrderStatusDialogListener, OnQueryTextListener, OnActionExpandListener {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"

        private const val SEARCH_TYPING_DELAY_MS = 500L

        fun newInstance(orderStatusFilter: String? = null) =
            OrderListFragment().apply { this.orderStatusFilter = orderStatusFilter }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private lateinit var viewModel: OrderListViewModel
    private lateinit var ordersAdapter: OrderListAdapter
    private lateinit var ordersDividerDecoration: DividerItemDecoration
    private var orderFilterDialog: OrderStatusSelectorDialog? = null

    private var listState: Parcelable? = null // Save the state of the recycler view
    private var orderStatusFilter: String? = null
    private var filterMenuItem: MenuItem? = null

    private var isSearching: Boolean = false
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var searchQuery: String = ""
    private val searchHandler = Handler()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(STATE_KEY_LIST)
            orderStatusFilter = bundle.getString(STATE_KEY_ACTIVE_FILTER, null)
            isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order_list, container, false)
        with(view) {
            orderRefreshLayout?.apply {
                activity?.let { activity ->
                    setColorSchemeColors(
                            ContextCompat.getColor(activity, R.color.colorPrimary),
                            ContextCompat.getColor(activity, R.color.colorAccent),
                            ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                    )
                }
                // Set the scrolling view in the custom SwipeRefreshLayout
                scrollUpChild = ordersList
                setOnRefreshListener {
                    AnalyticsTracker.track(Stat.ORDERS_LIST_PULLED_TO_REFRESH)

                    orderRefreshLayout.isRefreshing = false
                    viewModel.fetchFirstPage()
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the divider decoration for the list
        ordersDividerDecoration = DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
        )

        // Get cached order status options and prime the adapter
        ordersAdapter = OrderListAdapter(currencyFormatter) {
            showOrderDetail(it)
        }

        ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            addItemDecoration(ordersDividerDecoration)
            adapter = ordersAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        onScrollDown()
                    } else if (dy < 0) {
                        onScrollUp()
                    }
                }
            })
        }

        empty_view.setSiteToShare(selectedSite.get(), Stat.ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED)

        if (isSearching) {
            rebuildSearchView()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onPause() {
        super.onPause()

        // If the order filter dialog is visible, close it
        orderFilterDialog?.dismiss()
        orderFilterDialog = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState = ordersList.layoutManager?.onSaveInstanceState()

        outState.putParcelable(STATE_KEY_LIST, listState)
        outState.putString(STATE_KEY_ACTIVE_FILTER, orderStatusFilter)
        outState.putBoolean(STATE_KEY_IS_SEARCHING, isSearching)
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        disableSearchListeners()
        searchView = null
        filterMenuItem = null

        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (hidden) {
            disableSearchListeners()
        } else {
            enableSearchListeners()
            viewModel.reloadListFromCache()
        }
    }

    // region Options menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_order_list_fragment, menu)

        filterMenuItem = menu.findItem(R.id.menu_filter)

        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
    }

    /**
     * This is a replacement for activity?.invalidateOptionsMenu() since that causes the
     * search menu item to collapse
     */
    private fun refreshOptionsMenu() {
        val showFilter = shouldShowFilterMenuItem()
        filterMenuItem?.let {
            if (it.isVisible != showFilter) it.isVisible = showFilter
        }

        val showSearch = shouldShowFilterMenuItem()
        searchMenuItem?.let {
            if (it.isActionViewExpanded && !showFilter) it.collapseActionView()
            if (it.isVisible != showSearch) it.isVisible = showSearch
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_filter -> {
                AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_FILTER_TAPPED)
                showFilterDialog()
                true
            }
            R.id.menu_search -> {
                AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_SEARCH_TAPPED)
                clearOrderList()
                enableSearchListeners()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shouldShowFilterMenuItem(): Boolean {
        val isChildShowing = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        return when {
            !isActive -> false
            (isShowingAllOrders() && empty_view.visibility == View.VISIBLE) -> false
            (isChildShowing) -> false
            else -> true
        }
    }

    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        return when {
            (isChildShowing) -> false
            else -> true
        }
    }
    // endregion

    // region Filtering
    private fun showFilterDialog() {
        fragmentManager?.let { fm ->
            val orderStatusOptions = viewModel.orderStatusOptions.value ?: emptyMap()
            orderFilterDialog = OrderStatusSelectorDialog
                    .newInstance(orderStatusOptions, orderStatusFilter, true, listener = this)
                    .also { it.show(fm, OrderStatusSelectorDialog.TAG) }
        }
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        if (orderStatusFilter == orderStatus) {
            // Filter has not changed. Exit.
            return
        }

        orderStatusFilter = orderStatus
        if (isAdded) {
            AnalyticsTracker.track(
                    Stat.ORDERS_LIST_FILTER,
                    mapOf(AnalyticsTracker.KEY_STATUS to orderStatus.orEmpty())
            )

            clearOrderList()
            closeSearchView()

            viewModel.loadList(statusFilter = orderStatus)

            updateActivityTitle()
            searchMenuItem?.isVisible = shouldShowSearchMenuItem()
        }
    }
    // endregion

    override fun getFragmentTitle(): String {
        return getString(R.string.orders)
                .plus(orderStatusFilter.takeIf { !it.isNullOrEmpty() }?.let { filter ->
                    val orderStatusLabel = viewModel.orderStatusOptions.value?.let {
                        it[filter]?.label
                    }
                    getString(R.string.orderlist_filtered, orderStatusLabel)
                } ?: "")
    }

    override fun refreshFragmentState() {
        clearOrderList()
        viewModel.fetchFirstPage() // reload the active list from scratch
    }

    override fun scrollToTop() {
        ordersList.smoothScrollToPosition(0)
    }

    override fun onReturnedFromChildFragment() {
        showOptionsMenu(true)

        if (isSearching) {
            rebuildSearchView()
        }
    }

    /**
     * Shows the appropriate "empty view" when no orders are available for display:
     * <ul>
     *     <li>If searching -> show the "No matching orders" view</li>
     *     <li>If filtering -> show the "No orders" view</li>
     *     <li>Else -> show "Customers waiting" view</li>
     * </ul>
     */
    fun showEmptyView(show: Boolean) {
        if (show) {
            @StringRes val messageId: Int
            val showImage: Boolean
            val showShareButton: Boolean
            when {
                isSearching -> {
                    showImage = false
                    showShareButton = false
                    messageId = R.string.orders_empty_message_with_search
                }
                isShowingAllOrders() -> {
                    showImage = true
                    showShareButton = true
                    messageId = R.string.waiting_for_customers
                }
                else -> {
                    showImage = true
                    showShareButton = true
                    messageId = R.string.orders_empty_message_with_filter
                }
            }
            empty_view.show(messageId, showImage, showShareButton)
        } else {
            empty_view.hide()
        }
    }

    fun showOrderDetail(remoteOrderId: Long) {
        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showOrderDetail(selectedSite.get().id, remoteOrderId)
    }

    private fun isShowingAllOrders(): Boolean {
        return !isSearching && orderStatusFilter.isNullOrEmpty()
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

    private fun clearOrderList() {
        ordersAdapter.submitList(null)
    }

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        handleNewSearchRequest(query)
        ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.length > 2) {
            submitSearchDelayed(newText)
        } else if (newText.isEmpty()) {
            clearOrderList()
        }
        showEmptyView(false)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        isSearching = true
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        closeSearchView()
        viewModel.loadList(statusFilter = orderStatusFilter)
        return true
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
        viewModel.loadList(searchQuery = query)
    }

    /**
     * Return to the non-search order view
     */
    private fun closeSearchView() {
        if (isSearching) {
            showEmptyView(false)

            searchQuery = ""
            isSearching = false
            disableSearchListeners()
            searchMenuItem?.collapseActionView()
            updateActivityTitle()
        }
    }

    private fun rebuildSearchView() {
        // To prevent a timing issue that's causing the search bar
        // to not be expanded when returning from order detail.
        searchHandler.postDelayed({
            val expanded = searchMenuItem?.expandActionView() ?: false
            if (expanded) {
                searchView?.setQuery(searchQuery, false)
            }
            enableSearchListeners()
        }, 200L)
    }

    private fun disableSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
    }

    private fun enableSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)
    }
    // endregion

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(OrderListViewModel::class.java)

        // setup observers
        viewModel.isFetchingFirstPage.observe(this, Observer {
            orderRefreshLayout?.isRefreshing = it == true
        })

        viewModel.isLoadingMore.observe(this, Observer {
            it?.let { loadingMore ->
                load_more_progressbar?.visibility = if (loadingMore) View.VISIBLE else View.GONE
            }
        })

        viewModel.orderStatusOptions.observe(this, Observer {
            it?.let { options ->
                // So the order status can be matched to the appropriate label
                ordersAdapter.setOrderStatusOptions(options)
            }
        })

        var firstRunComplete = false
        var dataObserverAdded = false
        viewModel.pagedListData.observe(this, Observer {
            it?.let { orderListData ->
                if (orderListData.isNotEmpty()) {
                    ordersAdapter.submitList(orderListData)
                    listState?.let {
                        ordersList.layoutManager?.onRestoreInstanceState(listState)
                        listState = null
                    }

                    if (isSearching) {
                        ActivityUtils.hideKeyboard(activity)
                    }
                }

                /*
                 * Intentionally skip the first data result before setting up the
                 * listener for the empty event which controls whether or not we
                 * show the empty view. The first time the list is fetched, the data
                 * result is just the total of records in the db. If a new install, it
                 * will be zero.
                 */
                if (firstRunComplete && !dataObserverAdded) {
                    viewModel.isEmpty.observe(this, Observer { it2 ->
                        it2?.let { empty ->
                            showEmptyView(empty)
                        }
                    })
                    dataObserverAdded = true
                }

                firstRunComplete = true
            }
        })

        viewModel.showSnackbarMessage.observe(this, Observer { msg ->
            msg?.let { uiMessageResolver.showSnack(it) }
        })

        viewModel.scrollToPosition.observe(this, Observer {
            // TODO
        })

        viewModel.start()
        viewModel.loadList(orderStatusFilter, searchQuery)
    }
}
