package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.list.OrderListAdapter
import com.woocommerce.android.ui.orders.list.OrderListEmptyUiState
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.order_list_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

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
        orderListListener: com.woocommerce.android.ui.orders.list.OrderListListener
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
     * Submit new paged list data to the adapter
     */
    fun submitPagedList(list: PagedList<OrderListItemUIType>?) {
        val recyclerViewState = onFragmentSavedInstanceState()
        ordersAdapter.submitList(list)

        post {
            (ordersList?.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
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

    fun initEmptyView(siteModel: SiteModel) {
        // FIXME AMANDA - empty view
//        empty_view.setSiteToShare(siteModel, Stat.ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED)
    }

    fun updateEmptyViewForState(state: OrderListEmptyUiState) {
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

    fun showEmptyView(
        @StringRes messageId: Int,
        showImage: Boolean,
        showShareButton: Boolean,
        @DrawableRes imageId: Int? = null
    ) {
        // FIXME AMANDA - empty view
//        empty_view.show(messageId, showImage, showShareButton, imageId = imageId)
    }

    fun hideEmptyView() {
        // FIXME AMANDA - empty view
//        empty_view.hide()
    }

    fun isEmptyViewVisible() = empty_view.visibility == View.VISIBLE
}
