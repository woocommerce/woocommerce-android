package com.woocommerce.android.ui.orderlist

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainActivity
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.order_list_fragment.*
import kotlinx.android.synthetic.main.order_list_fragment.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderListFragment : Fragment(), OrderListContract.View {
    @Inject lateinit var presenter: OrderListContract.Presenter

    override var isActive: Boolean = false
        get() = isAdded

    /**
     * Listener for clicks on individual orders.
     */
    internal var itemListener = object : OrderItemListener {
        override fun onOrderItemClicked(order: WCOrderModel) {
            // Todo add itemListener to list items
        }
    }

    private lateinit var ordersAdapter: OrderListAdapter
    private lateinit var ordersDividerDecoration: DividerItemDecoration

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.order_list_fragment, container, false)

        ordersDividerDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        ordersDividerDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.list_divider))

        with(root) {
            orderRefreshLayout.apply {
                setColorSchemeColors(
                    ContextCompat.getColor(activity, R.color.color_primary),
                    ContextCompat.getColor(activity, R.color.color_accent),
                    ContextCompat.getColor(activity, R.color.color_primary_dark)
                )
                // Set the scrolling view in the custom SwipeRefreshLayout
                scrollUpChild = ordersList
                setOnRefreshListener { presenter.loadOrders() }
            }
        }

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ordersAdapter = OrderListAdapter()
        ordersAdapter.setOrders(ArrayList())
        ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ordersAdapter
            addItemDecoration(ordersDividerDecoration)
        }

        presenter.takeView(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.dropView()
    }

    override fun setLoadingIndicator(active: Boolean) {
        val root = view ?: return
        with(root as SwipeRefreshLayout) {
            // Make sure this is called after the layout is done with everything else.
            post { isRefreshing = active }
        }
    }

    override fun showOrders(orders: List<WCOrderModel>) {
        ordersAdapter.setOrders(orders)
        ordersView.visibility = View.VISIBLE
        noOrdersView.visibility = View.GONE
        setLoadingIndicator(false)
    }

    override fun showNoOrders() {
        ordersView.visibility = View.GONE
        noOrdersView.visibility = View.VISIBLE
    }

    override fun getSelectedSite() = (activity as? MainActivity)?.getSite()

    interface OrderItemListener {
        fun onOrderItemClicked(order: WCOrderModel)
    }
}
