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
import com.woocommerce.android.ui.base.UIMessageResolver
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
        const val STATE_KEY_CONNECTION_ERROR = "connection-error"
        const val STATE_KEY_PREVIOUS_STATUS = "previous-order-status"

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
    @Inject lateinit var uiResolver: UIMessageResolver

    private var connectErrorSnackbar: Snackbar? = null // Displays connection errors
    private var originalOrderStatus: String? = null

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
            val connectError = it.getBoolean(STATE_KEY_CONNECTION_ERROR, false)
            if (connectError) { showNetworkConnectivityError() }

            originalOrderStatus = it.getString(STATE_KEY_PREVIOUS_STATUS, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        connectErrorSnackbar?.takeIf { it.isShownOrQueued }?.let {
            outState.putBoolean(STATE_KEY_CONNECTION_ERROR, true)
        }
        originalOrderStatus?.let { outState.putString(STATE_KEY_PREVIOUS_STATUS, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        presenter.dropView()
        connectErrorSnackbar?.dismiss()
        connectErrorSnackbar = null
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
        context?.let {
            presenter.orderModel?.let { order ->
                originalOrderStatus = order.status
                presenter.markOrderComplete()
            }
        }
    }

    override fun toggleCompleteButton(isEnabled: Boolean) {
        orderFulfill_btnComplete.isEnabled = isEnabled
    }

    override fun orderFulfilled() {
        parentFragment?.let { router ->
            if (router is OrdersViewRouter) {
                presenter.orderModel?.let {
                    router.openOrderDetail(it, originalOrderStatus)
                }
            }
        }
    }

    override fun isNetworkConnected() = NetworkUtils.isNetworkAvailable(context)

    override fun showNetworkConnectivityError() {
        connectErrorSnackbar = uiResolver.getRetrySnack(
                R.string.order_error_update_no_connection, null, this)
        connectErrorSnackbar?.show()
    }
}
