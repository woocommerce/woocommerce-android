package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View {
    companion object {
        const val TAG = "OrderDetailFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_MARK_COMPLETE = "mark-order-complete"

        fun newInstance(order: WCOrderModel, markComplete: Boolean = false): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)

            // True if order fulfillment requested, else false
            args.putBoolean(FIELD_MARK_COMPLETE, markComplete)

            val fragment = OrderDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderDetailContract.Presenter
    @Inject lateinit var uiResolver: UIMessageResolver

    private var markCompleteCanceled: Boolean = false
    private var previousOrderStatus: String? = null
    private var connectErrorSnackbar: Snackbar? = null
    private var undoMarkCompleteSnackbar: Snackbar? = null

    // Handler for the RETRY button to retry fetching notes after a connection error.
    private val retryFetchNotesListener: View.OnClickListener by lazy {
        View.OnClickListener { v -> v?.let { context?.let { presenter.loadOrderNotes() } } }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_order_detail, container, false)

        // Set activity title
        arguments?.getString(FIELD_ORDER_NUMBER, "").also {
            activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, it)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        val markComplete = arguments?.getBoolean(FIELD_MARK_COMPLETE, false) ?: false

        context?.let {
            arguments?.getString(FIELD_ORDER_IDENTIFIER, null)?.let {
                presenter.loadOrderDetail(it, markComplete)
            }
        }
    }

    override fun onStop() {
        undoMarkCompleteSnackbar?.dismiss()
        super.onStop()
    }

    override fun onDestroyView() {
        connectErrorSnackbar?.dismiss()
        connectErrorSnackbar = null

        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderDetail(order: WCOrderModel?) {
        order?.let {
            // Populate the Order Status Card
            orderDetail_orderStatus.initView(order)

            // Populate the Order Product List Card
            orderDetail_productList.initView(order, false, this)

            // Populate the Customer Information Card
            if (parentFragment is OrderCustomerActionListener) {
                orderDetail_customerInfo.initView(order, false, parentFragment as OrderCustomerActionListener)
            } else {
                orderDetail_customerInfo.initView(order, false)
            }

            // Populate the Payment Information Card
            orderDetail_paymentInfo.initView(order)

            // Check for customer note, show if available
            if (order.customerNote.isEmpty()) {
                orderDetail_customerNote.visibility = View.GONE
            } else {
                orderDetail_customerNote.visibility = View.VISIBLE
                orderDetail_customerNote.initView(order)
            }
        }
    }

    override fun showOrderNotes(notes: List<WCOrderNoteModel>) {
        // Populate order notes card
        orderDetail_noteList.initView(notes)
    }

    override fun updateOrderNotes(notes: List<WCOrderNoteModel>) {
        // Update the notes in the notes card
        orderDetail_noteList.updateView(notes)
    }

    override fun showUndoOrderCompleteSnackbar() {
        markCompleteCanceled = false

        presenter.orderModel?.let {
            previousOrderStatus = it.status
            orderDetail_orderStatus.updateStatus(OrderStatus.COMPLETED)

            // Listener for the UNDO button in the snackbar
            val actionListener = View.OnClickListener {
                // User canceled the action to mark the order complete.
                markCompleteCanceled = true
                arguments?.remove(FIELD_MARK_COMPLETE)
                previousOrderStatus?.let {
                    orderDetail_orderStatus.updateStatus(it)
                }
                previousOrderStatus = null
            }

            // Callback listens for the snackbar to be dismissed. If the swiped to dismiss, or it
            // timed out, then process the request to mark this order complete.
            val callback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    markOrderComplete()
                }
            }
            undoMarkCompleteSnackbar = uiResolver
                .getUndoSnack(R.string.order_fulfill_marked_complete, null, actionListener).also {
                    it.addCallback(callback).show()
                }
        }
    }

    private fun markOrderComplete() {
        if (!markCompleteCanceled) {
            arguments?.remove(FIELD_MARK_COMPLETE)
            presenter.doMarkOrderComplete()
        }
    }

    override fun markOrderCompleteSuccess() {
        previousOrderStatus = null
    }

    override fun markOrderCompleteFailed() {
        // Set the order status back to the previous status
        previousOrderStatus?.let {
            orderDetail_orderStatus.updateStatus(it)
            previousOrderStatus = null
        }
    }

    override fun openOrderFulfillment(order: WCOrderModel) {
        parentFragment?.let { router ->
            if (router is OrdersViewRouter) {
                router.openOrderFulfillment(order)
            }
        }
    }

    override fun openOrderProductList(order: WCOrderModel) {
        parentFragment?.let { router ->
            if (router is OrdersViewRouter) {
                router.openOrderProductList(order)
            }
        }
    }

    override fun isNetworkConnected() = NetworkUtils.isNetworkAvailable(context)

    override fun showNetworkConnectivityError() {
        if (connectErrorSnackbar == null) {
            connectErrorSnackbar = uiResolver.getRetrySnack(
                    R.string.order_error_fetch_notes_network, null, retryFetchNotesListener)
        }
        connectErrorSnackbar?.show()
    }
}
