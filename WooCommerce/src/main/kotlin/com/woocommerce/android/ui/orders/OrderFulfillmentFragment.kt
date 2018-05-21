package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_fulfillment.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderFulfillmentFragment : Fragment(), OrderFulfillmentContract.View, View.OnClickListener {
    companion object {
        const val TAG = "OrderFulfillmentFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"

        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)

            val fragment = OrderFulfillmentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderFulfillmentContract.Presenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout.fragment_order_fulfillment, container, false)

        // Set activity title
        arguments?.getString(FIELD_ORDER_NUMBER, "").also {
            activity?.title = getString(R.string.orderdetail_order_fulfillment, it) }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        arguments?.getString(FIELD_ORDER_IDENTIFIER, null)?.let { presenter.loadOrderDetail(it) }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderDetail(order: WCOrderModel) {
        // Populate the Order Product List Card
        orderFulfill_products.initView(order, true, null)

        // Check for customer provided note, show if available
        if (order.customerNote.isEmpty()) {
            orderFulfill_customerNote.visibility = View.GONE
        } else {
            orderFulfill_customerNote.visibility = View.VISIBLE
            orderFulfill_customerNote.initView(order)
        }

        // Populate the Customer Information Card
        orderFulfill_customerInfo.initView(order, true)

        orderFulfill_btnComplete.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        // User has clicked the button to mark this order complete.

        // todo - check network connectivity - if not connected, display No connection: the order can’t be updated [ RETRY ]

        // todo - if connected, send request to open the orderDetail with mark complete as true
        parentFragment?.let { router ->
            if (router is OrdersViewRouter) {
                presenter.orderModel?.let {
                    router.openOrderDetail(it, true)
                }
            }
        }
    }
}
