package com.woocommerce.android.ui.orderlist

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.ParentFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderListFragment : ParentFragment(), OrderListContract.View {
    companion object {
        val TAG: String = OrderListFragment::class.java.simpleName
        fun newInstance() = OrderListFragment()
    }

    @Inject lateinit var presenter: OrderListContract.Presenter
    @Inject lateinit var ordersAdapter: OrderListAdapter
    private lateinit var ordersDividerDecoration: DividerItemDecoration

    override var isActive: Boolean = false
        get() = isAdded

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateFragmentView(inflater: LayoutInflater?,
                                      container: ViewGroup?,
                                      savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_order_list, container, false)
        view?.let {
            with(view) {
                orderRefreshLayout.apply {
                    setColorSchemeColors(
                            ContextCompat.getColor(activity, R.color.colorPrimary),
                            ContextCompat.getColor(activity, R.color.colorAccent),
                            ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                    )
                    // Set the scrolling view in the custom SwipeRefreshLayout
                    scrollUpChild = ordersList
                    setOnRefreshListener { presenter.loadOrders() }
                }
            }
        }
        // Set the title in the action bar
        activity.title = getString(R.string.wc_orders)

        // Set the divider decoration for the list
        ordersDividerDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        ordersDividerDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.list_divider))

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ordersAdapter
            addItemDecoration(ordersDividerDecoration)
        }
        presenter.takeView(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun setLoadingIndicator(active: Boolean) {
        with(orderRefreshLayout) {
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
}
