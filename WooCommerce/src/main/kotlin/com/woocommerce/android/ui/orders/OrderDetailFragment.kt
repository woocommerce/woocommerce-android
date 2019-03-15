package com.woocommerce.android.ui.orders

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.NestedScrollView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SNACK_ORDER_MARKED_COMPLETE_UNDO_BUTTON_TAPPED
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.AddOrderNoteActivity.Companion.FIELD_IS_CUSTOMER_NOTE
import com.woocommerce.android.ui.orders.AddOrderNoteActivity.Companion.FIELD_NOTE_TEXT
import com.woocommerce.android.ui.orders.OrderDetailOrderNoteListView.OrderDetailNoteListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.widgets.AppRatingDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View, OrderDetailNoteListener,
        OrderStatusSelectorDialog.OrderStatusDialogListener {
    companion object {
        const val TAG = "OrderDetailFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_MARK_COMPLETE = "mark-order-complete"
        const val FIELD_REMOTE_NOTE_ID = "remote-notification-id"
        const val REQUEST_CODE_ADD_NOTE = 100

        fun newInstance(
            orderId: OrderIdentifier,
            remoteNoteId: Long? = null,
            markComplete: Boolean = false
        ): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, orderId)

            // True if order fulfillment requested, else false
            args.putBoolean(FIELD_MARK_COMPLETE, markComplete)

            // If opened from a notification, add the remote_note_id
            remoteNoteId?.let { args.putLong(FIELD_REMOTE_NOTE_ID, it) }

            val fragment = OrderDetailFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            localSiteId: Int,
            remoteOrderId: Long,
            remoteNoteId: Long? = null,
            markComplete: Boolean = false
        ): Fragment {
            val orderIdentifier = OrderIdentifier(localSiteId, remoteOrderId)
            return newInstance(orderIdentifier, remoteNoteId, markComplete)
        }
    }

    @Inject lateinit var presenter: OrderDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var productImageMap: ProductImageMap

    private var changeOrderStatusCanceled: Boolean = false
    private var changeOrderStatusSnackbar: Snackbar? = null
    private var previousOrderStatus: String? = null
    private var notesSnack: Snackbar? = null
    private var pendingNotesError = false
    private var runOnStartFunc: (() -> Unit)? = null

    private var orderStatusSelector: OrderStatusSelectorDialog? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        arguments?.let {
            val markComplete = it.getBoolean(FIELD_MARK_COMPLETE, false)
            it.remove(FIELD_MARK_COMPLETE)

            val orderIdentifier = it.getString(FIELD_ORDER_IDENTIFIER) as OrderIdentifier
            presenter.loadOrderDetail(orderIdentifier, markComplete)

            val remoteNoteId = it.getLong(FIELD_REMOTE_NOTE_ID, 0)
            activity?.let { ctx ->
                if (remoteNoteId > 0) {
                    presenter.markOrderNotificationRead(ctx, remoteNoteId)
                }
            }
        }

        scrollView.setOnScrollChangeListener {
            _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            if (scrollY > oldScrollY) onScrollDown() else if (scrollY < oldScrollY) onScrollUp()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK && data != null) {
            val noteText = data.getStringExtra(FIELD_NOTE_TEXT)
            val isCustomerNote = data.getBooleanExtra(FIELD_IS_CUSTOMER_NOTE, false)
            orderDetail_noteList.addTransientNote(noteText, isCustomerNote)
            presenter.pushOrderNote(noteText, isCustomerNote)
            AppRatingDialog.incrementInteractions()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()

        runOnStartFunc?.let {
            it.invoke()
            runOnStartFunc = null
        }
    }

    override fun onPause() {
        super.onPause()
        orderStatusSelector?.let {
            it.dismiss()
            orderStatusSelector = null
        }
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
            // set the title to the order number
            activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, it.number)

            // Populate the Order Status Card
            val orderStatus = presenter.getOrderStatusForStatusKey(order.status)
            orderDetail_orderStatus
                    .initView(order, orderStatus, object : OrderDetailOrderStatusView.OrderStatusListener {
                override fun openOrderStatusSelector() {
                    showOrderStatusSelector()
                }
            })

            // Populate the Order Product List Card
            orderDetail_productList.initView(
                    order,
                    productImageMap,
                    false,
                    currencyFormatter.buildFormatter(order.currency),
                    this
            )

            // Populate the Customer Information Card
            orderDetail_customerInfo.initView(order, false)

            // Populate the Payment Information Card
            orderDetail_paymentInfo.initView(order, currencyFormatter.buildFormatter(order.currency))

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

    override fun showOrderNotesSkeleton(show: Boolean) {
        orderDetail_noteList.showSkeleton(show)
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

    override fun setOrderStatus(newStatus: String) {
        val orderStatus = presenter.getOrderStatusForStatusKey(newStatus)
        orderDetail_orderStatus.updateStatus(orderStatus)
        presenter.orderModel?.let {
            orderDetail_productList.updateView(it, this)
            orderDetail_paymentInfo.initView(it, currencyFormatter.buildFormatter(it.currency))
        }
    }

    override fun refreshOrderStatus() {
        presenter.orderModel?.let {
            setOrderStatus(it.status)
        }
    }

    override fun showChangeOrderStatusSnackbar(newStatus: String) {
        changeOrderStatusCanceled = false

        presenter.orderModel?.let { order ->
            AnalyticsTracker.track(Stat.ORDER_STATUS_CHANGE, mapOf(
                    AnalyticsTracker.KEY_ID to order.remoteOrderId,
                    AnalyticsTracker.KEY_FROM to order.status,
                    AnalyticsTracker.KEY_TO to newStatus))

            previousOrderStatus = order.status
            order.status = newStatus

            // artificially set order status
            setOrderStatus(newStatus)

            // Listener for the UNDO button in the snackbar
            val actionListener = View.OnClickListener {
                AnalyticsTracker.track(
                        Stat.ORDER_STATUS_CHANGE_UNDO,
                        mapOf(AnalyticsTracker.KEY_ID to order.remoteOrderId))

                when (newStatus) {
                    CoreOrderStatus.COMPLETED.value ->
                        AnalyticsTracker.track(SNACK_ORDER_MARKED_COMPLETE_UNDO_BUTTON_TAPPED)
                    else -> {}
                }

                // User canceled the action to change the order status
                changeOrderStatusCanceled = true

                presenter.orderModel?.let { order ->
                    previousOrderStatus?.let { status ->
                        order.status = status
                        setOrderStatus(status)
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

            // Select the appropriate snack message based on the new status
            val snackMsg = when (newStatus) {
                CoreOrderStatus.COMPLETED.value -> R.string.order_fulfill_marked_complete
                else -> R.string.order_status_changed_to
            }
            changeOrderStatusSnackbar = uiMessageResolver
                    .getUndoSnack(snackMsg, newStatus, actionListener = actionListener)
                    .also {
                        it.addCallback(callback)
                        it.show()
                    }
        }
    }

    override fun refreshProductImages() {
        if (isAdded) {
            orderDetail_productList.refreshProductImages()
        }
    }

    // TODO: replace progress bar with a skeleton
    override fun showLoadOrderProgress(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        orderDetail_container.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun showLoadOrderError() {
        loadingProgress.visibility = View.GONE
        uiMessageResolver.showSnack(R.string.order_error_fetch_generic)

        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
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
        presenter.orderModel?.let {
            val intent = Intent(activity, AddOrderNoteActivity::class.java)
            intent.putExtra(AddOrderNoteActivity.FIELD_ORDER_IDENTIFIER, it.getIdentifier())
            intent.putExtra(AddOrderNoteActivity.FIELD_ORDER_NUMBER, it.number)
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
        }
    }

    override fun showAddOrderNoteSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_added).show()
    }

    override fun showAddOrderNoteErrorSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_error).show()
    }

    override fun markOrderStatusChangedSuccess() {
        previousOrderStatus = null
    }

    override fun markOrderStatusChangedFailed() {
        // Set the order status back to the previous status
        previousOrderStatus?.let {
            val orderStatus = presenter.getOrderStatusForStatusKey(it)
            orderDetail_orderStatus.updateStatus(orderStatus)
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
            setOrderStatus(status)
        }
        previousOrderStatus = null
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        orderStatus?.let {
            showChangeOrderStatusSnackbar(it)
        }
    }

    private fun showOrderStatusSelector() {
        presenter.orderModel?.let { order ->
            val orderStatusOptions = presenter.getOrderStatusOptions()
            val orderStatus = order.status
            orderStatusSelector = OrderStatusSelectorDialog
                    .newInstance(
                            orderStatusOptions,
                            orderStatus,
                            false,
                            listener = this)
                    .also { it.show(fragmentManager, OrderStatusSelectorDialog.TAG) }
        }
    }
}
