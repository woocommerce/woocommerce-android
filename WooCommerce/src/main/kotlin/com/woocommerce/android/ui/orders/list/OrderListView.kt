package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
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

    fun init(
        currencyFormatter: CurrencyFormatter,
        orderListListener: OrderListListener
    ) {
        this.listener = orderListListener
        this.ordersAdapter = OrderListAdapter(orderListListener, currencyFormatter)

        binding.ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = ordersAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
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
     * Submit new paged list data to the adapter
     */
    fun submitPagedList(list: PagedList<OrderListItemUIType>?) {
        val recyclerViewState = onFragmentSavedInstanceState()
        ordersAdapter.submitList(list)

        post {
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
        ordersAdapter.submitList(null)
    }

    /**
     * scroll to the top of the order list
     */
    fun scrollToTop() {
        binding.ordersList.smoothScrollToPosition(0)
    }

    fun getCurrentPosition() = (binding.ordersList.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0

    /**
     * save the order list on configuration change
     */
    fun onFragmentSavedInstanceState() = binding.ordersList.layoutManager?.onSaveInstanceState()

    /**
     * restore the order list on configuration change
     */
    fun onFragmentRestoreInstanceState(listState: Parcelable) {
        binding.ordersList.layoutManager?.onRestoreInstanceState(listState)
    }

    fun setLoadingMoreIndicator(active: Boolean) {
        binding.loadMoreProgressbar.isVisible = active
    }
}
