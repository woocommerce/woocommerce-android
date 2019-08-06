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
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class OrderListFragmentNew : TopLevelFragment(), OrderListContractNew.View,
        OrderStatusSelectorDialog.OrderStatusDialogListener, OnQueryTextListener, OnActionExpandListener {
    companion object {
        const val TAG: String = "OrderListFragmentNew"
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"

        private const val SEARCH_TYPING_DELAY_MS = 500L

        fun newInstance(orderStatusFilter: String? = null) =
            OrderListFragmentNew().apply { this.orderStatusFilter = orderStatusFilter }
    }

    @Inject lateinit var presenter: OrderListContractNew.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var pagedListWrapper: PagedListWrapper<OrderListItemUIType>? = null
    private lateinit var ordersAdapter: OrderListAdapterNew
    private lateinit var ordersDividerDecoration: DividerItemDecoration
    private var orderFilterDialog: OrderStatusSelectorDialog? = null

    override var isRefreshPending = false // not used.
    override var isRefreshing = false // not used.

    private var listState: Parcelable? = null // Save the state of the recycler view
    private var orderStatusFilter: String? = null
    private var filterMenuItem: MenuItem? = null

    override var isSearching: Boolean = false
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
                    pagedListWrapper?.fetchFirstPage()
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the divider decoration for the list
        ordersDividerDecoration = DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
        )

        // Get cached order status options and prime the adapter
        val orderStatusOptions = presenter.getOrderStatusOptions()
        ordersAdapter = OrderListAdapterNew(currencyFormatter, orderStatusOptions) {
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

        presenter.takeView(this)
        empty_view.setSiteToShare(selectedSite.get(), Stat.ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED)

        val orderListDescriptor = WCOrderListDescriptor(
                site = selectedSite.get(),
                statusFilter = orderStatusFilter,
                searchQuery = searchQuery)
        if (isSearching) {
            rebuildSearchView()
        }
        loadList(orderListDescriptor)
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
        presenter.dropView()

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
            pagedListWrapper?.invalidateData()
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
            val orderStatusOptions = presenter.getOrderStatusOptions()
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

            val descriptor = WCOrderListDescriptor(
                    site = selectedSite.get(),
                    statusFilter = orderStatus)
            loadList(descriptor)

            updateActivityTitle()
            searchMenuItem?.isVisible = shouldShowSearchMenuItem()
        }
    }
    // endregion

    override fun getFragmentTitle(): String {
        return getString(R.string.orders)
                .plus(orderStatusFilter.takeIf { !it.isNullOrEmpty() }?.let { filter ->
                    val orderStatusLabel = presenter.getOrderStatusOptions()[filter]?.label
                    getString(R.string.orderlist_filtered, orderStatusLabel)
                } ?: "")
    }

    override fun refreshFragmentState() {
        pagedListWrapper?.invalidateData()
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
     * Shows the view that appears for stores that have no orders matching the current filter
     */
    override fun showEmptyView(show: Boolean) {
        if (show) {
            // if the user is searching we show a simple "No matching orders" TextView, otherwise if
            // there isn't a filter (ie: we're showing All orders and there aren't any), then we want
            // to show the full "customers waiting" view, otherwise we show a simple textView stating
            // there aren't any orders
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

    override fun showOrderDetail(remoteOrderId: Long) {
        // Load order shipment tracking providers if it hasn't been done already.
        presenter.loadShipmentTrackingProviders()

        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showOrderDetail(selectedSite.get().id, remoteOrderId)
    }

    override fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>) {
        // So the order status can be matched to the appropriate label
        ordersAdapter.setOrderStatusOptions(orderStatusOptions)
    }

    private fun loadList(descriptor: WCOrderListDescriptor) {
        pagedListWrapper?.apply {
            val lifecycleOwner = this@OrderListFragmentNew
            data.removeObservers(lifecycleOwner)
            isLoadingMore.removeObservers(lifecycleOwner)
            isFetchingFirstPage.removeObservers(lifecycleOwner)
            listError.removeObservers(lifecycleOwner)
            isEmpty.removeObservers(lifecycleOwner)
        }

        pagedListWrapper = presenter.generatePageWrapper(descriptor, lifecycle).also { wrapper ->
            /*
             * Set observers for various changes in state
             */
            wrapper.fetchFirstPage()
            wrapper.isLoadingMore.observe(this, Observer {
                it?.let { isLoadingMore ->
                    load_more_progressbar?.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
                }
            })
            wrapper.isFetchingFirstPage.observe(this, Observer { isFetchingFirstPage ->
                orderRefreshLayout?.isRefreshing = isFetchingFirstPage == true

                // Fetch order status options as well
                if (isFetchingFirstPage) {
                    presenter.refreshOrderStatusOptions()
                }
            })
            var firstRunComplete = false
            var dataObserverAdded = false
            wrapper.data.observe(this, Observer {
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
                        wrapper.isEmpty.observe(this, Observer { it2 ->
                            it2?.let { empty ->
                                showEmptyView(empty)
                            }
                        })
                        dataObserverAdded = true
                    }

                    firstRunComplete = true
                }
            })
            wrapper.listError.observe(this, Observer {
                it?.let {
                    // Display an error message
                    showLoadOrdersError()
                }
            })
        }
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

    private fun showLoadOrdersError() {
        uiMessageResolver.getSnack(R.string.orderlist_error_fetch_generic).show()
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
        val descriptor = WCOrderListDescriptor(site = selectedSite.get())
        loadList(descriptor)
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
        val descriptor = WCOrderListDescriptor(
                site = selectedSite.get(),
                statusFilter = null,
                searchQuery = query)
        loadList(descriptor)
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
}
