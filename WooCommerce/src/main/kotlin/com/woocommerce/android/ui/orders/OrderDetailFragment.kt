package com.woocommerce.android.ui.orders

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderDetailOrderNoteListView.OrderDetailNoteListener
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View, OrderDetailNoteListener {
    companion object {
        const val TAG = "OrderDetailFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_MARK_COMPLETE = "mark-order-complete"
        const val REQUEST_CODE_ADD_NOTE = 100

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
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var markCompleteCanceled: Boolean = false
    private var undoMarkCompleteSnackbar: Snackbar? = null
    private var previousOrderStatus: String? = null
    private var notesSnack: Snackbar? = null
    private var pendingNotesError = false

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
        arguments?.remove(FIELD_MARK_COMPLETE)

        context?.let {
            arguments?.getString(FIELD_ORDER_IDENTIFIER, null)?.let {
                presenter.loadOrderDetail(it, markComplete)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            presenter.loadOrderNotes()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        undoMarkCompleteSnackbar?.dismiss()
        notesSnack?.dismiss()
        super.onStop()
    }

    override fun onDestroyView() {
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
        orderDetail_noteList.initView(notes, this)
    }

    override fun updateOrderNotes(notes: List<WCOrderNoteModel>) {
        // Update the notes in the notes card
        orderDetail_noteList.updateView(notes)
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

    override fun updateOrderStatus(status: String) {
        orderDetail_orderStatus.updateStatus(status)
        presenter.orderModel?.let {
            orderDetail_productList.updateView(it, false, this)
        }
    }

    override fun showUndoOrderCompleteSnackbar() {
        markCompleteCanceled = false

        presenter.orderModel?.let {
            previousOrderStatus = it.status
            it.status = CoreOrderStatus.COMPLETED.value

            // artificially set order status to Complete
            updateOrderStatus(CoreOrderStatus.COMPLETED.value)

            // Listener for the UNDO button in the snackbar
            val actionListener = View.OnClickListener {
                // User canceled the action to mark the order complete.
                markCompleteCanceled = true

                presenter.orderModel?.let { order ->
                    previousOrderStatus?.let { status ->
                        order.status = status
                        updateOrderStatus(status)
                    }
                    previousOrderStatus = null
                }
            }

            // Callback listens for the snackbar to be dismissed. If the swiped to dismiss, or it
            // timed out, then process the request to mark this order complete.
            val callback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (pendingNotesError) {
                        notesSnack?.show()
                    }
                    if (!markCompleteCanceled) {
                        presenter.doMarkOrderComplete()
                    }
                }
            }
            undoMarkCompleteSnackbar = uiMessageResolver
                    .getUndoSnack(R.string.order_fulfill_marked_complete, actionListener = actionListener)
                    .also {
                        it.addCallback(callback)
                        it.show()
                    }
        }
    }

    override fun onRequestAddNote() {
        showAddOrderNoteScreen()
    }

    override fun showAddOrderNoteScreen() {
        val intent = Intent(activity, AddOrderNoteActivity::class.java)
        intent.putExtra(AddOrderNoteActivity.FIELD_ORDER_IDENTIFIER, presenter.orderModel?.getIdentifier())
        intent.putExtra(AddOrderNoteActivity.FIELD_ORDER_NUMBER, presenter.orderModel?.number)
        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
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

    override fun showNotesErrorSnack() {
        notesSnack = uiMessageResolver.getSnack(R.string.order_error_fetch_notes_generic)

        if ((undoMarkCompleteSnackbar?.isShownOrQueued) == true) {
            pendingNotesError = true
        } else {
            notesSnack?.show()
        }
    }

    override fun showCompleteOrderError() {
        uiMessageResolver.getSnack(R.string.order_error_update_general).show()
        previousOrderStatus?.let { status ->
            updateOrderStatus(status)
        }
        previousOrderStatus = null
    }
}
