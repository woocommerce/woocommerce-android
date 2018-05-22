package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.util.SnackbarUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_fulfillment.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class OrderFulfillmentFragment : Fragment(), OrderFulfillmentContract.View, View.OnClickListener {
    companion object {
        const val TAG = "OrderFulfillmentFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_CONNECTION_ERROR = "connection-error"

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

    private var snackbar: Snackbar? = null // Displays connection errors

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

        savedInstanceState?.let {
            val connectError = it.getBoolean(FIELD_CONNECTION_ERROR, false)
            if (connectError) {
                orderFulfill_btnComplete?.let { btn ->
                    showNetworkConnectivityError(btn)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        snackbar?.let {
            outState.putBoolean(FIELD_CONNECTION_ERROR, true)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        presenter.dropView()
        snackbar?.let {
            it.dismiss()
            snackbar = null
        }
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
        v?.let {
            // Check for network connection, if none, show message.
            if (NetworkUtils.isNetworkAvailable(context)) {
                parentFragment?.let { router ->
                    if (router is OrdersViewRouter) {
                        presenter.orderModel?.let {
                            router.openOrderDetail(it, true)
                        }
                    }
                }
            } else {
                showNetworkConnectivityError(it)
            }
        }
    }

    private fun showNetworkConnectivityError(view: View) {
        snackbar = SnackbarUtils.getRetrySnack(
                view, getString(R.string.order_update_error_no_connection), this)
        snackbar?.show()
    }
}
