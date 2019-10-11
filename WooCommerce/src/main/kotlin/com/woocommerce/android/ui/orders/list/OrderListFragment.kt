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
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderStatusSelectorDialog
import com.woocommerce.android.util.ActivityUtils
import org.wordpress.android.util.ActivityUtils as WPActivityUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.UiHelpers
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.orderRefreshLayout
import kotlinx.android.synthetic.main.fragment_order_list.ordersList
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import javax.inject.Inject

private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

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

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiMessageResolver: UIMessageResolver
    @Inject internal lateinit var selectedSite: SelectedSite
    @Inject internal lateinit var currencyFormatter: CurrencyFormatter

    private lateinit var viewModel: OrderListViewModel
    private lateinit var ordersAdapter: OrderListAdapter
    private lateinit var ordersDividerDecoration: DividerItemDecoration
    private var orderFilterDialog: OrderStatusSelectorDialog? = null

    private var listState: Parcelable? = null // Save the state of the recycler view
    private var orderStatusFilter: String? = null
    private var filterMenuItem: MenuItem? = null

    // Alias for interacting with [viewModel.isSearching] so the value is always identical
    // to the real value on the UI side.
    private var isSearching: Boolean
        private set(value) { viewModel.isSearching = value }
        get() = viewModel.isSearching

    // Alias for interacting with [viewModel.searchQuery] so the value is always identical
    // to the real value on the UI side.
    private var searchQuery: String
        private set(value) { viewModel.searchQuery = value }
        get() = viewModel.searchQuery

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
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

        viewModel.pagedListData.observe(this, Observer {
            updatePagedListData(it)
        })

        viewModel.showSnackbarMessage.observe(this, Observer { msg ->
            msg?.let { uiMessageResolver.showSnack(it) }
        })

        viewModel.scrollToPosition.observe(this, Observer {
            // TODO AMANDA - needed?
        })

        viewModel.emptyViewState.observe(this, Observer {
            it?.let { emptyViewState -> updateEmptyViewForState(emptyViewState) }
        })

        viewModel.shareStore.observe(this, Observer {
            AnalyticsTracker.track(Stat.ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED)
            selectedSite.getIfExists()?.let { site ->
                context?.let { ctx ->
                    selectedSite.getIfExists()?.let {
                        ActivityUtils.shareStoreUrl(ctx, site.url)
                    }
                }
            }
        })

        viewModel.start()
        viewModel.loadList(orderStatusFilter, searchQuery)
    }

    private fun updatePagedListData(pagedListData: PagedList<OrderListItemUIType>?) {
        val recyclerViewState = ordersList?.layoutManager?.onSaveInstanceState()
        ordersAdapter.submitList(pagedListData)

        if (pagedListData?.size != 0 && isSearching) {
            WPActivityUtils.hideKeyboard(activity)
        }

        ordersList?.post {
            (ordersList?.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                if (layoutManager.findFirstVisibleItemPosition() < MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION) {
                    layoutManager.onRestoreInstanceState(recyclerViewState)
                }
            }
        }
    }

    private fun updateEmptyViewForState(state: OrderListEmptyUiState) {
        empty_view?.let { emptyView ->
            if (state.emptyViewVisible) {
                UiHelpers.setTextOrHide(emptyView.title, state.title)
                UiHelpers.setImageOrHide(emptyView.image, state.imgResId)
                setupButtonOrHide(emptyView.button, state.buttonText, state.onButtonClick)
                emptyView.visibility = View.VISIBLE
            } else {
                emptyView.visibility = View.GONE
            }
        }
    }

    private fun setupButtonOrHide(
        buttonView: Button,
        text: UiString?,
        onButtonClick: (() -> Unit)?
    ) {
        UiHelpers.setTextOrHide(buttonView, text)
        buttonView.setOnClickListener { onButtonClick?.invoke() }
    }

    private fun showOrderDetail(remoteOrderId: Long) {
        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showOrderDetail(selectedSite.get().id, remoteOrderId)
    }

    private fun isShowingAllOrders(): Boolean {
        return !isSearching && orderStatusFilter.isNullOrEmpty()
    }

    private fun clearOrderList() {
        ordersAdapter.submitList(null)
    }

    // region options menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_order_list_fragment, menu)

        filterMenuItem = menu.findItem(R.id.menu_filter)

        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
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

    // region filtering
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

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        handleNewSearchRequest(query)
        WPActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.length > 2) {
            submitSearchDelayed(newText)
        } else if (newText.isEmpty()) {
            clearOrderList()
        }
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
