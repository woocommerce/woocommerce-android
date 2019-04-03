package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.SearchView.OnQueryTextListener
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderListAdapter.OnLoadMoreListener
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import javax.inject.Inject

class OrderListFragment : TopLevelFragment(), OrderListContract.View,
        OrderStatusSelectorDialog.OrderStatusDialogListener, OnQueryTextListener, OnActionExpandListener,
        OnLoadMoreListener {
    companion object {
        val TAG: String = OrderListFragment::class.java.simpleName
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"

        private const val SEARCH_TYPING_DELAY_MS = 500L

        fun newInstance(orderStatusFilter: String? = null): OrderListFragment {
            val fragment = OrderListFragment()
            fragment.orderStatusFilter = orderStatusFilter
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderListContract.Presenter
    @Inject lateinit var ordersAdapter: OrderListAdapter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var ordersDividerDecoration: DividerItemDecoration
    private var orderFilterDialog: OrderStatusSelectorDialog? = null

    override var isRefreshPending = true // If true, the fragment will refresh its orders when its visible
    override var isRefreshing: Boolean
        get() = orderRefreshLayout.isRefreshing
        set(_) {}
    override var isSearching: Boolean = false

    private var listState: Parcelable? = null // Save the state of the recycler view
    private var orderStatusFilter: String? = null // Order status filter
    private var filterMenuItem: MenuItem? = null

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var searchQuery: String = ""
    private val searchHandler = Handler()

    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(STATE_KEY_LIST)
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
            orderStatusFilter = bundle.getString(STATE_KEY_ACTIVE_FILTER, null)
            isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        }

        ordersAdapter.setOnLoadMoreListener(this)
    }

    // region options menu
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_order_list_fragment, menu)

        filterMenuItem = menu?.findItem(R.id.menu_filter)

        searchMenuItem = menu?.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        refreshOptionsMenu()
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (orderStatusFilter != null && orderStatusFilter != ordersAdapter.orderStatusFilter) {
            onOrderStatusSelected(orderStatusFilter)
        }
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_filter -> {
                AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_FILTER_TAPPED)
                showFilterDialog()
                true
            }
            R.id.menu_search -> {
                AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shouldShowFilterMenuItem(): Boolean {
        return when {
            (isShowingAllOrders() && empty_view.visibility == View.VISIBLE) -> false
            (childFragmentManager.backStackEntryCount > 0) -> false
            else -> true
        }
    }

    private fun shouldShowSearchMenuItem(): Boolean {
        return when {
            (childFragmentManager.backStackEntryCount > 0) -> false
            else -> true
        }
    }
    // endregion

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateFragmentView(
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

                    if (!isRefreshPending) {
                        isRefreshPending = true
                        if (isSearching) {
                            presenter.searchOrders(searchQuery)
                        } else {
                            presenter.loadOrders(orderStatusFilter, forceRefresh = true)
                        }
                    }
                }
            }
        }
        return view
    }

    override fun onPause() {
        super.onPause()

        // If the order filter dialog is visible, close it
        orderFilterDialog?.dismiss()
        orderFilterDialog = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the divider decoration for the list
        ordersDividerDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)

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

        if (isActive && !deferInit) {
            presenter.loadOrders(orderStatusFilter, forceRefresh = this.isRefreshPending)
        }

        listState?.let {
            ordersList.layoutManager?.onRestoreInstanceState(listState)
            listState = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState = ordersList.layoutManager?.onSaveInstanceState()

        outState.putParcelable(STATE_KEY_LIST, listState)
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        outState.putString(STATE_KEY_ACTIVE_FILTER, orderStatusFilter)
        outState.putBoolean(STATE_KEY_IS_SEARCHING, isSearching)
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)

        super.onSaveInstanceState(outState)
    }

    override fun onBackStackChanged() {
        super.onBackStackChanged()

        // If this fragment is now visible and we've deferred loading orders due to it not
        // being visible - go ahead and load the orders.
        if (isActive) {
            refreshOptionsMenu()
            if (isSearching) {
                searchMenuItem?.expandActionView()
                searchView?.setQuery(searchQuery, false)
            } else {
                presenter.loadOrders(orderStatusFilter, forceRefresh = this.isRefreshPending)
            }
            enableSearchListeners()
        } else {
            // disable the search listeners until we return to this fragment - otherwise the query text
            // will fire with an empty string and the collapse event will fire as we leave this fragment
            disableSearchListeners()
            refreshOptionsMenu()
        }
    }

    override fun onDestroyView() {
        disableSearchListeners()
        presenter.dropView()
        filterMenuItem = null
        searchView = null
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // silently refresh if this fragment is no longer hidden
        if (!hidden) {
            presenter.fetchAndLoadOrdersFromDb(orderStatusFilter, isForceRefresh = false)
        }
    }

    override fun setLoadingMoreIndicator(active: Boolean) {
        load_more_progressbar.visibility = if (active) View.VISIBLE else View.GONE
    }

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(ordersView, R.layout.skeleton_order_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun showOrders(orders: List<WCOrderModel>, filterByStatus: String?, isFreshData: Boolean) {
        orderStatusFilter = filterByStatus

        if (!ordersAdapter.isSameOrderList(orders)) {
            ordersList?.let {
                if (isFreshData) {
                    ordersList.scrollToPosition(0)
                }
                ordersAdapter.setOrders(orders, orderStatusFilter)
            }
        }

        if (isFreshData) {
            isRefreshPending = false
        }

        // Update the toolbar title
        activity?.title = getFragmentTitle()
    }

    /**
     * User scrolled to the last order and the adapter is requesting us to fetch more orders
     */
    override fun onRequestLoadMore() {
        if (presenter.canLoadMoreOrders() && !presenter.isLoadingOrders()) {
            if (isSearching) {
                presenter.searchMoreOrders(searchQuery)
            } else {
                presenter.loadMoreOrders(orderStatusFilter)
            }
        }
    }

    private fun isShowingAllOrders(): Boolean {
        return !isSearching && orderStatusFilter.isNullOrEmpty()
    }

    /**
     * shows the view that appears for stores that have have no orders matching the current filter
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
            isRefreshPending = false
        } else {
            empty_view.hide()
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.orders)
                .plus(orderStatusFilter.takeIf { !it.isNullOrEmpty() }?.let { filter ->
                    val orderStatusLabel = presenter.getOrderStatusOptions()[filter]?.label
                    getString(R.string.orderlist_filtered, orderStatusLabel)
                } ?: "")
    }

    override fun scrollToTop() {
        ordersList.smoothScrollToPosition(0)
    }

    override fun refreshFragmentState() {
        isRefreshPending = true
        if (isActive) {
            if (isSearching) {
                presenter.searchOrders(searchQuery)
            } else {
                presenter.loadOrders(orderStatusFilter, forceRefresh = true)
            }
        }
    }

    override fun showLoadOrdersError() {
        uiMessageResolver.getSnack(R.string.orderlist_error_fetch_generic).show()
    }

    override fun showNoConnectionError() {
        uiMessageResolver.getSnack(R.string.error_generic_network).show()
    }

    override fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>) {
        ordersAdapter.setOrderStatusOptions(orderStatusOptions)
    }

    // region Filtering
    private fun showFilterDialog() {
        val orderStatusOptions = presenter.getOrderStatusOptions()
        orderFilterDialog = OrderStatusSelectorDialog
                .newInstance(orderStatusOptions, orderStatusFilter, true, listener = this)
                .also { it.show(fragmentManager, OrderStatusSelectorDialog.TAG) }
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        orderStatusFilter = orderStatus

        if (isAdded) {
            AnalyticsTracker.track(
                    Stat.ORDERS_LIST_FILTER,
                    mapOf(AnalyticsTracker.KEY_STATUS to orderStatus.orEmpty())
            )

            clearSearchResults()
            ordersAdapter.clearAdapterData()
            presenter.loadOrders(orderStatusFilter, true)

            activity?.title = getFragmentTitle()
            searchMenuItem?.isVisible = shouldShowSearchMenuItem()
        }
    }
    // endregion

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        submitSearch(query)
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.length > 2) {
            submitSearchDelayed(newText)
        } else {
            ordersAdapter.clearAdapterData()
        }
        showEmptyView(false)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        ordersAdapter.clearAdapterData()
        isSearching = true
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        clearSearchResults()
        return true
    }

    /**
     * Submit the search after a brief delay unless the query has changed - this is used to
     * perform a search while the user is typing
     */
    override fun submitSearchDelayed(query: String) {
        searchHandler.postDelayed({
            searchView?.let {
                // submit the search if the searchView's query still matches the passed query
                if (query == it.query.toString()) submitSearch(query)
            }
        }, SEARCH_TYPING_DELAY_MS)
    }

    /**
     * Submit the search with no delay
     */
    override fun submitSearch(query: String) {
        AnalyticsTracker.track(
                Stat.ORDERS_LIST_FILTER,
                mapOf(AnalyticsTracker.KEY_SEARCH to query))

        searchQuery = query
        presenter.searchOrders(query)
    }

    /**
     * Presenter received search results, show them in the adapter
     */
    override fun showSearchResults(query: String, orders: List<WCOrderModel>) {
        if (query == searchQuery) {
            org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
            ordersAdapter.setOrders(orders)
        }
    }

    /**
     * Presenter received search result with an offset due to infinite scroll, add them to the adapter
     */
    override fun addSearchResults(query: String, orders: List<WCOrderModel>) {
        if (query == searchQuery) {
            ordersAdapter.addOrders(orders)
        }
    }

    /**
     * Return to the non-search order view
     */
    override fun clearSearchResults() {
        if (isSearching) {
            searchQuery = ""
            isSearching = false
            disableSearchListeners()
            activity?.title = getFragmentTitle()
            searchMenuItem?.collapseActionView()
            presenter.fetchAndLoadOrdersFromDb(orderStatusFilter, isForceRefresh = false)
        }
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
