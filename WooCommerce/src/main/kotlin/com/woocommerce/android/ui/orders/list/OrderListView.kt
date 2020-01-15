package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import kotlinx.android.synthetic.main.order_list_view.view.*
import org.wordpress.android.fluxc.model.WCOrderStatusModel

private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

class OrderListView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_list_view, this)
    }

    private lateinit var ordersAdapter: OrderListAdapter
    private lateinit var listener: OrderListListener

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
            layoutManager = LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            addItemDecoration(ordersDividerDecoration)
            adapter = ordersAdapter
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

    // TODO remove this code
    /*fun updateEmptyViewForState(state: OrderListEmptyUiState, fmtArgs: String? = null) {
        when (state) {
            is DataShown -> { hideEmptyView() }
            is EmptyList -> { showEmptyView(state.title, fmtArgs, state.imgResId) }
            is Loading -> { showEmptyView(state.title) }
            is ErrorWithRetry -> {
                showEmptyView(
                        state.title,
                        imgResId = state.imgResId,
                        buttonText = state.buttonText,
                        onButtonClick = state.onButtonClick()
                )
            }
        }
    }

    private fun showEmptyView(
        title: UiString? = null,
        fmtArgs: String? = null,
        @DrawableRes imgResId: Int? = null,
        buttonText: UiString? = null,
        onButtonClick: (() -> Unit)? = null
    ) {
        empty_view?.let { emptyView ->
            UiHelpers.setTextOrHide(emptyView.title, title, fmtArgs)
            UiHelpers.setImageOrHide(emptyView.image, imgResId)
            UiHelpers.setTextOrHide(emptyView.button, buttonText)
            emptyView.button.setOnClickListener { onButtonClick?.invoke() }
            if (emptyView.visibility == View.GONE) {
                WooAnimUtils.fadeIn(emptyView, Duration.MEDIUM)
            }
        }
    }*/
}
