package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainUIMessageResolver
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View {
    companion object {
        const val TAG = "OrderDetailFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_ORIGINAL_ORDER_STATUS = "previous-order-status"
        const val STATE_KEY_PENDING_COMPLETE_ERROR = "active-mark-order-complete-error"

        fun newInstance(order: WCOrderModel, originalOrderStatus: String?): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)

            // If order recently completed, this was the order status prior to that change
            args.putString(FIELD_ORIGINAL_ORDER_STATUS, originalOrderStatus)

            val fragment = OrderDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderDetailContract.Presenter
    @Inject lateinit var uiResolver: MainUIMessageResolver

    private var originalOrderStatus: String? = null
    private var errorUpdateStatusSnackbar: Snackbar? = null
    private var errorFetchNotesSnackbar: Snackbar? = null

    private val retryFetchNotesListener = View.OnClickListener { v ->
        // Handler for the RETRY button to retry fetching notes after a connection error.
        v?.let {
            context?.let { context ->
                errorFetchNotesSnackbar?.dismiss()
                errorFetchNotesSnackbar = null
                presenter.loadOrderNotes(context)
            }
        }
    }

    private val retryUpdateOrderStatusListener = View.OnClickListener { v ->
        // Handler for the RETRY button to retry submitting order to mark it complete after a
        // connection error.
        v?.let {
            context?.let { context ->
                errorUpdateStatusSnackbar?.dismiss()
                errorUpdateStatusSnackbar = null
                pendingUndoOrderComplete()
            }
        }
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

        // Parse out original order status if the user just fulfilled the order
        originalOrderStatus = arguments?.getString(FIELD_ORIGINAL_ORDER_STATUS, null)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        arguments?.getString(FIELD_ORDER_IDENTIFIER, null)?.let {
            context?.let { context ->
                presenter.loadOrderDetail(context, it)
            }

            // If order was fulfilled, show message to allow user to undo
            originalOrderStatus?.let {
                pendingUndoOrderComplete()
            }
        }

        savedInstanceState?.let {
            val isMarkCompleteError = it.getBoolean(STATE_KEY_PENDING_COMPLETE_ERROR, false)
            if (isMarkCompleteError) {
                showNetworkErrorForUpdateOrderStatus()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        errorUpdateStatusSnackbar?.let {
            outState.putBoolean(STATE_KEY_PENDING_COMPLETE_ERROR, true)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        errorUpdateStatusSnackbar?.let {
            it.dismiss()
            errorUpdateStatusSnackbar = null
        }
        errorFetchNotesSnackbar?.let {
            it.dismiss()
            errorFetchNotesSnackbar = null
        }
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

    override fun showNetworkErrorForNotes() {
        errorFetchNotesSnackbar = uiResolver.getRetrySnack(
                R.string.order_error_fetch_notes_network, null, retryFetchNotesListener)
        errorFetchNotesSnackbar?.show()
    }

    override fun showNetworkErrorForUpdateOrderStatus() {
        errorUpdateStatusSnackbar = uiResolver.getRetrySnack(
                R.string.order_error_update_no_connection, null, retryUpdateOrderStatusListener)
        errorUpdateStatusSnackbar?.show()
    }

    // User has clicked the "UNDO" button to undo order fulfillment.
    // Submit request to change the order status back to the original value.
    private fun pendingUndoOrderComplete() {
        originalOrderStatus?.let { status ->
            // Listener for the UNDO button in the errorUpdateStatusSnackbar
            val actionListener = View.OnClickListener {
                context?.let {
                    // User canceled the action to mark the order complete.
                    presenter.updateOrderStatus(it, status)
                }
            }

            val callback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    // Remove the original order status from arguments
                    context?.let {
                        arguments?.remove(FIELD_ORIGINAL_ORDER_STATUS)
                    }
                }
            }

            errorUpdateStatusSnackbar = uiResolver.getUndoSnack(
                    R.string.order_fulfill_marked_complete,
                    null,
                    actionListener)
            errorUpdateStatusSnackbar?.addCallback(callback)
            errorUpdateStatusSnackbar?.show()
        }
    }

    override fun orderStatusUpdateSuccess(order: WCOrderModel) {
        // Set the order status back to the previous status
        originalOrderStatus?.let {
            orderDetail_orderStatus.updateStatus(it)
            originalOrderStatus = null
        }

        // Update the product list view to display the option
        // to fulfill order
        orderDetail_productList.updateView(order, false, this)

        // Display success snack message
        uiResolver.showSnack(R.string.order_fulfill_undo_success)
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
}
