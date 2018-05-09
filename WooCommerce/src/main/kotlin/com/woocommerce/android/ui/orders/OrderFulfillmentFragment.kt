package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R.layout
import com.woocommerce.android.R.string
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderFulfillmentFragment : Fragment(), OrderFulfillmentContract.View {
    companion object {
        const val FIELD_ORDER_ID = "order-id"
        const val FIELD_ORDER_NUMBER = "order-number"
        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putInt(FIELD_ORDER_ID, order.id)

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)

            val fragment = OrderFulfillmentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout.fragment_order_fulfillment, container, false)

        arguments?.let { arguments ->
            // set activity title
            val orderNumber = arguments.getString(FIELD_ORDER_NUMBER, "")
            activity?.title = getString(string.orderdetail_order_fulfillment, orderNumber.toString())
        }
        return view
    }

    override fun showOrderDetail(order: WCOrderModel?) {
        // TODO Populate order fulfillment view
    }
}
