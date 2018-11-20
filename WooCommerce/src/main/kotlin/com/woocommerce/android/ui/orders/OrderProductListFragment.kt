package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_product_list.*
import kotlinx.android.synthetic.main.order_detail_product_list.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderProductListFragment : BaseFragment(), OrderProductListContract.View {
    companion object {
        const val TAG = "OrderProductListFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"

        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)
            val fragment = OrderProductListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderProductListContract.Presenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_order_product_list, container, false)

        // Set activity title
        arguments?.getString(FIELD_ORDER_NUMBER, "").also {
            activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, it) }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        arguments?.getString(FIELD_ORDER_IDENTIFIER, null)?.let { presenter.loadOrderDetail(it) }

        productList_products.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (dy > 0) onScrollDown() else if (dy < 0) onScrollUp()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderProducts(order: WCOrderModel) {
        orderProducts_list.initView(order, true)
    }
}
