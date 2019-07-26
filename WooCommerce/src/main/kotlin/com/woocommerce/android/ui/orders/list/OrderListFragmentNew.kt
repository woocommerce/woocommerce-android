package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
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
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.orderRefreshLayout
import kotlinx.android.synthetic.main.fragment_order_list.ordersList
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import javax.inject.Inject

class OrderListFragmentNew : TopLevelFragment(), OrderListContractNew.View,
        OrderStatusSelectorDialog.OrderStatusDialogListener {
    companion object {
        const val TAG: String = "OrderListFragmentNew"
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"

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
    override var isSearching: Boolean = false

    /**
     * Hack to ignore the first time the `pagedListWrapper.data` event is fired so as
     * not to show the `empty_view` erroneously.
     */
    private var isListDataInit = false
    private var listState: Parcelable? = null // Save the state of the recycler view
    private var orderStatusFilter: String? = null
    private var filterMenuItem: MenuItem? = null

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var searchQuery: String = ""

    private val skeletonView = SkeletonView()

    override fun onAttach(context: Context?) {
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

                    if (isSearching) {
                        // FIXME: Search
                    } else {
                        pagedListWrapper?.fetchFirstPage()
                    }
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

        val orderListDescriptor = presenter.generateListDescriptor(orderStatusFilter, "")
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
        // FIXME: Search

        presenter.dropView()

        // FIXME: Filtering

        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (hidden) {
            // FIXME: SEARCH
        } else {
            // FIXME: Search

            // silently refresh if this fragment is no longer hidden
            if (isSearching) {
                // FIXME: Search
            } else {
                pagedListWrapper?.fetchFirstPage()
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // FIXME: Filtering
    }

    // region Options menu
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
                // FIXME: SEARCH
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
        val orderStatusOptions = presenter.getOrderStatusOptions()
        orderFilterDialog = OrderStatusSelectorDialog
                .newInstance(orderStatusOptions, orderStatusFilter, true, listener = this)
                .also { it.show(fragmentManager, OrderStatusSelectorDialog.TAG) }
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        orderStatusFilter = orderStatus

        // FIXME: Filtering
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
        if (isSearching) {
            // FIXME: Search
//                presenter.searchOrders(searchQuery)
        } else {
            pagedListWrapper?.fetchFirstPage()
        }
    }

    override fun scrollToTop() {
        ordersList.smoothScrollToPosition(0)
    }

    override fun onReturnedFromChildFragment() {
        showOptionsMenu(true)

        if (isSearching) {
            searchMenuItem?.expandActionView()
            searchView?.setQuery(searchQuery, false)
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

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(ordersView, R.layout.skeleton_order_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun showOrderDetail(remoteOrderId: Long) {
        // FIXME: Search
//        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showOrderDetail(selectedSite.get().id, remoteOrderId)
    }

    override fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>) {
        // So the order status can be matched to the appropriate label
        ordersAdapter.setOrderStatusOptions(orderStatusOptions)
    }

    private fun loadList(descriptor: WCOrderListDescriptor) {
        showSkeleton(true)

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
            wrapper.data.observe(this, Observer {
                it?.let { orderListData ->
                    if (orderListData.isNotEmpty()) {
                        showSkeleton(false)
                        ordersAdapter.submitList(orderListData)
                        listState?.let {
                            ordersList.layoutManager?.onRestoreInstanceState(listState)
                            listState = null
                        }
                    }

                    /*
                     * Intentionally skip the first data result before setting up the
                     * listener for the empty event which controls whether or not we
                     * show the empty view. The first time the list is fetched, the data
                     * result is just the total of records in the db. If a new install, it
                     * will be zero.
                     */
                    if (isListDataInit) {
                        wrapper.isEmpty.observe(this, Observer { it2 ->
                            it2?.let { empty ->
                                showEmptyView(empty)
                                showSkeleton(false)
                            }
                        })
                    }

                    isListDataInit = true
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
}
