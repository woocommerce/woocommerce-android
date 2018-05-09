package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_product_list.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderProductListFragment : Fragment(), OrderProductListContract.View {
    companion object {
        const val FIELD_ORDER_ID = "order-id"
        const val FIELD_ORDER_NUMBER = "order-number"

        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putInt(FIELD_ORDER_ID, order.id)

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

        arguments?.let { arguments ->
            // Set activity title
            val orderNumber = arguments.getString(FIELD_ORDER_NUMBER, "")
            activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, orderNumber.toString())
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        val orderId = arguments?.getInt(FIELD_ORDER_ID, 0)
        orderId?.let {
            presenter.loadOrderDetail(it)
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderProducts(order: WCOrderModel) {
        orderProducts_list.initView(order, true)
    }
}
