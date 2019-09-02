package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.order_list_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

class OrderListView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_list_view, this)
    }

    lateinit var ordersAdapter: OrderListAdapter
    private lateinit var listener: OrderListListener

    private val skeletonView = SkeletonView()

    fun init(
        currencyFormatter: CurrencyFormatter,
        orderListListener: OrderListListener
    ) {
        this.listener = orderListListener
        this.ordersAdapter = OrderListAdapter(orderListListener, currencyFormatter)

        // Set the divider decoration for the list
        val ordersDividerDecoration = androidx.recyclerview.widget.DividerItemDecoration(
                context,
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
        )

        ordersList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            addItemDecoration(ordersDividerDecoration)
            adapter = ordersAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        listener.onFragmentScrollDown()
                    } else if (dy < 0) {
                        listener.onFragmentScrollUp()
                    }
                }
            })
        }
    }

    /**
     * order list adapter method
     * set order status options to the order list adapter
     */
    fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>) {
        ordersAdapter.setOrderStatusOptions(orderStatusOptions)
    }

    /**
     * order list adapter method
     * get order status options from the order list adapter
     */
    fun getOrderListStatusFilter() = ordersAdapter.orderStatusFilter

    /**
     * order list adapter method
     * get order list item count from the order list adapter
     */
    fun getOrderListItemCount() = ordersAdapter.itemCount

    /**
     * order list adapter method
     * set orders to the order list adapter
     */
    fun setOrders(orders: List<WCOrderModel>) {
        ordersAdapter.setOrders(orders)
    }

    /**
     * order list adapter method
     * add orders to the order list adapter
     */
    fun addOrders(orders: List<WCOrderModel>) {
        ordersAdapter.addOrders(orders)
    }

    /**
     * clear order list adapter data
     */
    fun clearAdapterData() {
        ordersAdapter.clearAdapterData()
    }

    /**
     * scroll to the top of the order list
     */
    fun scrollToTop() {
        ordersList.smoothScrollToPosition(0)
    }

    /**
     * save the order list on configuration change
     */
    fun onFragmentSavedInstanceState() = ordersList.layoutManager?.onSaveInstanceState()

    /**
     * restore the order list on configuration change
     */
    fun onFragmentRestoreInstanceState(listState: Parcelable) {
        ordersList.layoutManager?.onRestoreInstanceState(listState)
    }

    fun setLoadingMoreIndicator(active: Boolean) {
        load_more_progressbar.visibility = if (active) View.VISIBLE else View.GONE
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(ordersView, R.layout.skeleton_order_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    fun showOrders(orders: List<WCOrderModel>, filterByStatus: String?, isFreshData: Boolean) {
        ordersList?.let {
            if (isFreshData) {
                ordersList.scrollToPosition(0)
            }
            ordersAdapter.setOrders(orders, filterByStatus)
        }
    }

    fun initEmptyView(siteModel: SiteModel) {
        empty_view.setSiteToShare(siteModel, Stat.ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED)
    }

    fun showEmptyView(@StringRes messageId: Int, showImage: Boolean, showShareButton: Boolean) {
        empty_view.show(messageId, showImage, showShareButton)
    }

    fun hideEmptyView() {
        empty_view.hide()
    }

    fun isEmptyViewDisplayed() = empty_view.visibility == View.VISIBLE
}
