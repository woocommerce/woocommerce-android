package com.woocommerce.android.ui.orders

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.AddOrderNoteActivity.Companion.FIELD_IS_CUSTOMER_NOTE
import com.woocommerce.android.ui.orders.AddOrderNoteActivity.Companion.FIELD_NOTE_TEXT
import com.woocommerce.android.ui.orders.OrderDetailOrderNoteListView.OrderDetailNoteListener
import com.woocommerce.android.ui.orders.OrderDetailPaymentView.OrderDetailPaymentViewListener
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View, OrderDetailNoteListener,
        OrderDetailPaymentViewListener {
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
    @Inject lateinit var networkStatus: NetworkStatus

    private var changeOrderStatusCanceled: Boolean = false
    private var changeOrderStatusSnackbar: Snackbar? = null
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
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK && data != null) {
            val noteText = data.getStringExtra(FIELD_NOTE_TEXT)
            val isCustomerNote = data.getBooleanExtra(FIELD_IS_CUSTOMER_NOTE, false)
            orderDetail_noteList.addTransientNote(noteText, isCustomerNote)
            presenter.pushOrderNote(noteText, isCustomerNote)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        changeOrderStatusSnackbar?.dismiss()
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
            orderDetail_paymentInfo.initView(order, this)

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

    override fun updateOrderStatus(newStatus: String) {
        orderDetail_orderStatus.updateStatus(newStatus)
        presenter.orderModel?.let {
            orderDetail_productList.updateView(it, false, this)
            orderDetail_paymentInfo.initView(it, this)
        }
    }

    override fun showChangeOrderStatusSnackbar(newStatus: String) {
        changeOrderStatusCanceled = false

        presenter.orderModel?.let {
            previousOrderStatus = it.status
            it.status = newStatus

            // artificially set order status
            updateOrderStatus(newStatus)

            // Listener for the UNDO button in the snackbar
            val actionListener = View.OnClickListener {
                // User canceled the action to change the order status
                changeOrderStatusCanceled = true

                presenter.orderModel?.let { order ->
                    previousOrderStatus?.let { status ->
                        order.status = status
                        updateOrderStatus(status)
                    }
                    previousOrderStatus = null
                }
            }

            // Callback listens for the snackbar to be dismissed. If the swiped to dismiss, or it
            // timed out, then process the request to change the order status
            val callback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (pendingNotesError) {
                        notesSnack?.show()
                    }
                    if (!changeOrderStatusCanceled) {
                        presenter.doChangeOrderStatus(newStatus)
                    }
                }
            }

            @StringRes val idRes = if (newStatus == CoreOrderStatus.COMPLETED.value) {
                R.string.order_fulfill_marked_complete
            } else {
                R.string.order_fulfill_payment_cleared
            }
            changeOrderStatusSnackbar = uiMessageResolver
                    .getUndoSnack(idRes, actionListener = actionListener)
                    .also {
                        it.addCallback(callback)
                        it.show()
                    }
        }
    }

    override fun onRequestAddNote() {
        if (!networkStatus.isConnected()) {
            // If offline, show generic offline message and exit without opening add note screen
            uiMessageResolver.showOfflineSnack()
            return
        }

        showAddOrderNoteScreen()
    }

    override fun showAddOrderNoteScreen() {
        val intent = Intent(activity, AddOrderNoteActivity::class.java)
        intent.putExtra(AddOrderNoteActivity.FIELD_ORDER_IDENTIFIER, presenter.orderModel?.getIdentifier())
        intent.putExtra(AddOrderNoteActivity.FIELD_ORDER_NUMBER, presenter.orderModel?.number)
        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
    }

    override fun showAddOrderNoteSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_added).show()
    }

    override fun showAddOrderNoteErrorSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_error).show()
    }

    /**
     * user tapped "Payment Cleared" on the payment view
     */
    override fun onRequestPaymentCleared() {
        showChangeOrderStatusSnackbar(CoreOrderStatus.PROCESSING.value)
    }

    override fun markOrderStatusChangedSuccess() {
        previousOrderStatus = null
    }

    override fun markOrderStatusChangedFailed() {
        // Set the order status back to the previous status
        previousOrderStatus?.let {
            orderDetail_orderStatus.updateStatus(it)
            previousOrderStatus = null
        }
    }

    override fun showNotesErrorSnack() {
        notesSnack = uiMessageResolver.getSnack(R.string.order_error_fetch_notes_generic)

        if ((changeOrderStatusSnackbar?.isShownOrQueued) == true) {
            pendingNotesError = true
        } else {
            notesSnack?.show()
        }
    }

    override fun showOrderStatusChangedError() {
        uiMessageResolver.getSnack(R.string.order_error_update_general).show()
        previousOrderStatus?.let { status ->
            updateOrderStatus(status)
        }
        previousOrderStatus = null
    }
}
