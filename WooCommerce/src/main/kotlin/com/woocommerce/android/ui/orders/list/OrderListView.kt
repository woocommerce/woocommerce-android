package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderListViewBinding
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.model.WCOrderStatusModel

private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

class OrderListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderListViewBinding.inflate(LayoutInflater.from(ctx), this)

    private lateinit var ordersAdapter: OrderListAdapter
    private lateinit var listener: OrderListListener

    /**
     * Id of a just-created order to reveal at the top of the list, see [scrollToTopWhenOrderAppears].
     */
    private var pendingCreatedOrderId: Long? = null
    private var onPendingCreatedOrderHandled: (() -> Unit)? = null

    val emptyView
        get() = binding.emptyView

    val ordersList
        get() = binding.ordersList

    fun init(
        currencyFormatter: CurrencyFormatter,
        orderListListener: OrderListListener
    ) {
        this.listener = orderListListener
        this.ordersAdapter = OrderListAdapter(orderListListener, currencyFormatter)
        ordersAdapter.onListCommitted = ::handleListCommitted

        binding.ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = ordersAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    // Layout-driven dispatches report a delta of 0, so this only fires for actual scrolling
                    // (touch, mouse, accessibility), which takes priority over revealing a created order.
                    if (dy != 0) {
                        clearPendingCreatedOrder()
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
     * Opens the first order. Used in tablets in a 2 pane layout where the first order needs to be opened by default
     */
    fun openFirstOrder() {
        ordersAdapter.openFirstOrder()
    }

    fun openOrder(orderId: Long, startPaymentsFlow: Boolean = false) {
        ordersAdapter.openOrder(orderId, startPaymentsFlow)
    }

    /**
     * Submit new paged list data to the adapter
     */
    fun submitPagedList(list: PagedList<OrderListItemUIType>?) {
        val recyclerViewState = onFragmentSavedInstanceState()
        val isRevealingCreatedOrder = pendingCreatedOrderId != null
        ordersAdapter.submitList(list)

        post {
            // While a created order is being revealed, [handleListCommitted] owns the scroll position.
            if (isRevealingCreatedOrder) return@post
            (binding.ordersList.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                if (layoutManager.findFirstVisibleItemPosition() < MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION) {
                    layoutManager.onRestoreInstanceState(recyclerViewState)
                }
            }
        }
    }

    /**
     * clear order list adapter data
     */
    fun clearAdapterData() {
        if (::ordersAdapter.isInitialized) {
            ordersAdapter.submitList(null)
        }
    }

    /**
     * scroll to the top of the order list
     */
    fun scrollToTop() {
        binding.ordersList.smoothScrollToPosition(0)
    }

    /**
     * Scrolls the list to the top on each committed list update until [orderId] appears in the data, so a
     * just-created order (inserted at the top, possibly under a new date header) is visible once the refreshed
     * list lands. The scroll runs in the adapter commit callback, after the async diff is applied, so it cannot
     * race the list update. Resolved when the order shows up or the user scrolls the list, at which point
     * [onHandled] is invoked.
     */
    fun scrollToTopWhenOrderAppears(orderId: Long, onHandled: () -> Unit) {
        pendingCreatedOrderId = orderId
        onPendingCreatedOrderHandled = onHandled
    }

    private fun handleListCommitted(list: PagedList<OrderListItemUIType>?) {
        val orderId = pendingCreatedOrderId ?: return
        (binding.ordersList.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
        val orderShown = list?.snapshot()
            ?.any { (it as? OrderListItemUIType.OrderListItemUI)?.orderId == orderId } == true
        if (orderShown) {
            clearPendingCreatedOrder()
        }
    }

    private fun clearPendingCreatedOrder() {
        if (pendingCreatedOrderId == null) return
        pendingCreatedOrderId = null
        onPendingCreatedOrderHandled?.invoke()
        onPendingCreatedOrderHandled = null
    }

    /**
     * save the order list on configuration change
     */
    fun onFragmentSavedInstanceState() = binding.ordersList.layoutManager?.onSaveInstanceState()

    fun setLoadingMoreIndicator(active: Boolean) {
        binding.loadMoreProgressbar.isVisible = active
    }
}
