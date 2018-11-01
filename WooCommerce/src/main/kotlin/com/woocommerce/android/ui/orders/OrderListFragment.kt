package com.woocommerce.android.ui.orders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class OrderListFragment : TopLevelFragment(), OrderListContract.View, OrderStatusFilterDialog.OrderListFilterListener {
    companion object {
        val TAG: String = OrderListFragment::class.java.simpleName
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        const val STATE_KEY_ACTIVE_FILTER = "active-order-status-filter"

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

    override var isRefreshPending = true // If true, the fragment will refresh its orders when its visible
    private var listState: Parcelable? = null // Save the state of the recycler view
    private var orderStatusFilter: String? = null // Order status filter
    private var filterMenuButton: MenuItem? = null

    private val skeletonView = SkeletonView()

    override var isActive: Boolean = false
        get() = childFragmentManager.backStackEntryCount == 0 && !isHidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(STATE_KEY_LIST)
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
            orderStatusFilter = bundle.getString(STATE_KEY_ACTIVE_FILTER, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_order_list_fragment, menu)
        filterMenuButton = menu?.findItem(R.id.menu_filter)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        // Hide filter menu item when we're showing all orders and there aren't any or we're showing order detail.
        val hideFilterMenu = (isShowingAllOrders() && noOrdersView.visibility == View.VISIBLE) || isShowingOrderDetail()
        menu?.findItem(R.id.menu_filter)?.isVisible = !hideFilterMenu
        super.onPrepareOptionsMenu(menu)
    }

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
                        presenter.loadOrders(orderStatusFilter, forceRefresh = true)
                    }
                }
            }
        }
        return view
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
        }

        presenter.takeView(this)

        if (isActive && !deferInit) {
            presenter.loadOrders(orderStatusFilter, forceRefresh = this.isRefreshPending)
        }

        listState?.let {
            ordersList.layoutManager.onRestoreInstanceState(listState)
            listState = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.menu_filter -> {
            AnalyticsTracker.track(Stat.ORDERS_LIST_MENU_FILTER_TAPPED)

            showFilterDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState = ordersList.layoutManager.onSaveInstanceState()

        outState.putParcelable(STATE_KEY_LIST, listState)
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        outState.putString(STATE_KEY_ACTIVE_FILTER, orderStatusFilter)
        super.onSaveInstanceState(outState)
    }

    override fun onBackStackChanged() {
        super.onBackStackChanged()

        // If this fragment is now visible and we've deferred loading orders due to it not
        // being visible - go ahead and load the orders.
        if (isActive) {
            presenter.loadOrders(orderStatusFilter, forceRefresh = this.isRefreshPending)
            filterMenuButton?.isVisible = true
        } else {
            filterMenuButton?.isVisible = false
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        filterMenuButton = null
        super.onDestroyView()
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
            ordersList?.let { _ ->
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

    private fun isShowingAllOrders(): Boolean {
        return orderStatusFilter.isNullOrEmpty()
    }

    private fun isShowingOrderDetail(): Boolean {
        val fragment = childFragmentManager.findFragmentByTag(OrderDetailFragment.TAG)
        return fragment?.isVisible ?: false
    }

    /**
     * shows the view that appears for stores that have have no orders matching the current filter
     */
    override fun showNoOrdersView(show: Boolean) {
        if (show && noOrdersView.visibility != View.VISIBLE) {
            // if there isn't a filter (ie: we're showing All orders and there aren't any), then we want
            // to show the full "customers waiting" view, otherwise we show a simple textView stating
            // there aren't any orders
            if (isShowingAllOrders()) {
                no_orders_image.visibility = View.VISIBLE
                no_orders_share_button.visibility = View.VISIBLE
                no_orders_text.setText(R.string.dashboard_no_orders)
            } else {
                no_orders_image.visibility = View.GONE
                no_orders_share_button.visibility = View.GONE
                no_orders_text.setText(R.string.dashboard_no_orders_with_filter)
            }

            WooAnimUtils.fadeIn(noOrdersView, Duration.LONG)
            WooAnimUtils.fadeOut(ordersView, Duration.LONG)
            no_orders_share_button.setOnClickListener {
                AnalyticsTracker.track(Stat.ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED)
                ActivityUtils.shareStoreUrl(activity!!, selectedSite.get().url)
            }
            isRefreshPending = false
        } else if (!show && noOrdersView.visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(noOrdersView, Duration.LONG)
            WooAnimUtils.fadeIn(ordersView, Duration.LONG)
        }

        activity?.invalidateOptionsMenu()
    }

    /**
     * Only open the order detail if the list is not actively being refreshed.
     */
    override fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderDetailFragment.TAG
            getFragmentFromBackStack(tag)?.let {
                val args = it.arguments ?: Bundle()
                args.putString(OrderDetailFragment.FIELD_ORDER_IDENTIFIER, order.getIdentifier())
                args.putString(OrderDetailFragment.FIELD_ORDER_NUMBER, order.number)
                args.putBoolean(OrderDetailFragment.FIELD_MARK_COMPLETE, markOrderComplete)
                it.arguments = args
                popToState(tag)
            } ?: loadChildFragment(OrderDetailFragment.newInstance(order, markOrderComplete), tag)
        }
    }

    override fun openOrderFulfillment(order: WCOrderModel) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderFulfillmentFragment.TAG
            if (!popToState(tag)) {
                loadChildFragment(OrderFulfillmentFragment.newInstance(order), tag)
            }
        }
    }

    override fun openOrderProductList(order: WCOrderModel) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderProductListFragment.TAG
            if (!popToState(tag)) {
                loadChildFragment(OrderProductListFragment.newInstance(order), tag)
            }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.orders)
                .plus(orderStatusFilter.takeIf { !it.isNullOrEmpty() }?.let { filter ->
                    getString(R.string.orderlist_filtered, CoreOrderStatus.fromValue(filter)?.label)
                } ?: "")
    }

    override fun refreshFragmentState() {
        isRefreshPending = true
        if (isActive) {
            ordersList.smoothScrollToPosition(0)
            presenter.loadOrders(orderStatusFilter, forceRefresh = true)
        }
    }

    override fun showLoadOrdersError() {
        uiMessageResolver.getSnack(R.string.orderlist_error_fetch_generic).show()
    }

    // region OrderCustomerActionListener
    override fun dialPhone(order: WCOrderModel, phone: String) {
        AnalyticsTracker.track(Stat.ORDER_CONTACT_ACTION, mapOf(
                AnalyticsTracker.KEY_ID to order.remoteOrderId,
                AnalyticsTracker.KEY_STATUS to order.status,
                AnalyticsTracker.KEY_TYPE to OrderCustomerActionListener.Action.CALL.name.toLowerCase()))

        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AnalyticsTracker.track(
                    Stat.ORDER_CONTACT_ACTION_FAILED,
                    this.javaClass.simpleName,
                    e.javaClass.simpleName, "No phone app was found")

            ToastUtils.showToast(context, R.string.error_no_phone_app)
        }
    }

    override fun createEmail(order: WCOrderModel, emailAddr: String) {
        AnalyticsTracker.track(Stat.ORDER_CONTACT_ACTION, mapOf(
                AnalyticsTracker.KEY_ID to order.remoteOrderId,
                AnalyticsTracker.KEY_STATUS to order.status,
                AnalyticsTracker.KEY_TYPE to OrderCustomerActionListener.Action.EMAIL.name.toLowerCase()))

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$emailAddr") // only email apps should handle this
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AnalyticsTracker.track(
                    Stat.ORDER_CONTACT_ACTION_FAILED,
                    this.javaClass.simpleName,
                    e.javaClass.simpleName, "No e-mail app was found")

            ToastUtils.showToast(context, R.string.error_no_email_app)
        }
    }

    override fun sendSms(order: WCOrderModel, phone: String) {
        AnalyticsTracker.track(Stat.ORDER_CONTACT_ACTION, mapOf(
                AnalyticsTracker.KEY_ID to order.remoteOrderId,
                AnalyticsTracker.KEY_STATUS to order.status,
                AnalyticsTracker.KEY_TYPE to OrderCustomerActionListener.Action.SMS.name.toLowerCase()))

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phone")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AnalyticsTracker.track(
                    Stat.ORDER_CONTACT_ACTION_FAILED,
                    this.javaClass.simpleName,
                    e.javaClass.simpleName, "No SMS app was found")

            ToastUtils.showToast(context, R.string.error_no_sms_app)
        }
    }
    // endregion

    // region Filtering
    private fun showFilterDialog() {
        val orderStatus = orderStatusFilter?.let {
            CoreOrderStatus.fromValue(it)
        }
        OrderStatusFilterDialog.newInstance(orderStatus, listener = this)
                .show(fragmentManager, OrderStatusFilterDialog.TAG)
    }

    override fun onFilterSelected(orderStatus: String?) {
        AnalyticsTracker.track(
                Stat.ORDERS_LIST_FILTER,
                mapOf(AnalyticsTracker.KEY_IS_LOADING_MORE to orderStatus.orEmpty()))

        orderStatusFilter = orderStatus
        ordersAdapter.clearAdapterData()
        presenter.loadOrders(orderStatusFilter, true)

        // Reset the toolbar title
        activity?.title = getFragmentTitle()
    }
    // endregion
}
