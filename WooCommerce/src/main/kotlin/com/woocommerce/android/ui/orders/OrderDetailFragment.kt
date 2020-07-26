package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_ISSUE_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_TRACKING_ADD_TRACKING_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_TRACKING_DELETE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_VIEW_REFUND_DETAILS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SNACK_ORDER_MARKED_COMPLETE_UNDO_BUTTON_TAPPED
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.fetchTrackingLinks
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.notes.OrderDetailOrderNoteListView.OrderDetailNoteListener
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelActionListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class OrderDetailFragment : BaseFragment(), OrderDetailContract.View, OrderDetailNoteListener,
        OrderStatusSelectorDialog.OrderStatusDialogListener, NavigationResult, ShippingLabelActionListener {
    companion object {
        const val ARG_DID_MARK_COMPLETE = "did_mark_complete"
        const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        private const val REFUNDS_REFRESH_DELAY = 2000L
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

    override var isRefreshPending: Boolean = false
    private val skeletonView = SkeletonView()

    /**
     * Keep track of the deleted [WCOrderShipmentTrackingModel] in case
     * the request to server fails, we need to display an error message
     * and add the deleted [WCOrderShipmentTrackingModel] back to the list
     */
    private var deleteOrderShipmentTrackingSnackbar: Snackbar? = null
    private var deleteOrderShipmentTrackingResponseSnackbar: Snackbar? = null
    private var deleteOrderShipmentTrackingSet = mutableSetOf<WCOrderShipmentTrackingModel>()

    private val navArgs: OrderDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        // The navArgs tell us if we should mark the order complete, but we only want to do that once. We can't
        // change the navArgs since they're read-only so we set ARG_DID_MARK_COMPLETE instead.
        val didMarkComplete = arguments?.getBoolean(ARG_DID_MARK_COMPLETE) ?: false
        val markComplete = navArgs.markComplete && !didMarkComplete
        if (markComplete) {
            arguments?.putBoolean(ARG_DID_MARK_COMPLETE, true) ?: run {
                arguments = Bundle().also {
                    it.putBoolean(ARG_DID_MARK_COMPLETE, true)
                }
            }
        }

        val orderIdentifier = navArgs.orderId
        presenter.loadOrderDetail(orderIdentifier, markComplete)

        val remoteNoteId = navArgs.remoteNoteId
        activity?.let { ctx ->
            if (remoteNoteId > 0) {
                presenter.markOrderNotificationRead(ctx, remoteNoteId)
            }
        }

        orderRefreshLayout?.apply {
            // Set the scrolling view in the custom SwipeRefreshLayout
            scrollUpChild = scrollView
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.ORDER_DETAIL_PULLED_TO_REFRESH)
                if (!isRefreshPending) {
                    // if undo snackbar is displayed, dismiss it and initiate request
                    // to change order status or delete shipment tracking
                    // once that is processed, initiate order detail refresh
                    when {
                        deleteOrderShipmentTrackingSnackbar?.isShownOrQueued == true -> {
                            deleteOrderShipmentTrackingSnackbar?.dismiss()
                            deleteOrderShipmentTrackingSnackbar = null
                        }
                        changeOrderStatusSnackbar?.isShownOrQueued == true -> {
                            changeOrderStatusSnackbar?.dismiss()
                            changeOrderStatusSnackbar = null
                        }
                        else -> refreshOrderDetail(true)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
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
        deleteOrderShipmentTrackingSnackbar?.dismiss()
        deleteOrderShipmentTrackingSnackbar = null
        deleteOrderShipmentTrackingResponseSnackbar?.dismiss()
        deleteOrderShipmentTrackingResponseSnackbar = null
        super.onStop()
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun getFragmentTitle() =
        getString(R.string.orderdetail_orderstatus_ordernum, presenter.orderModel?.number.orEmpty())

    override fun showRefunds(order: WCOrderModel, refunds: List<Refund>) {
        // show the refund products count if at least one refunded
        if (refunds.any { refund -> refund.items.sumBy { it.quantity } > 0 }) {
            orderDetail_refundsInfo.initView(refunds) { openRefundedProductList(order) }
            orderDetail_refundsInfo.show()
        } else {
            orderDetail_refundsInfo.hide()
        }

        if (refunds.isNotEmpty()) {
            orderDetail_paymentInfo.showRefunds(refunds.sortedBy { it.id })
        } else {
            orderDetail_paymentInfo.showRefundTotal()
        }
    }

    override fun showShippingLabels(order: WCOrderModel, shippingLabels: List<ShippingLabel>) {
        // Shipment tracking links are not available by default from the shipping label API
        // Until this is available on the API side, we need to fetch the tracking link from the
        // shipment tracking API (if available) and link the tracking link to the corresponding
        // tracking number of a shipping label
        val shipmentTrackingList = presenter.getOrderShipmentTrackingsFromDb(order)
        shippingLabels.map { it.fetchTrackingLinks(shipmentTrackingList) }

        orderDetail_shippingLabelList.initView(
            order.toAppModel(),
            shippingLabels,
            productImageMap,
            currencyFormatter.buildBigDecimalFormatter(order.currency),
            this
        )
    }

    override fun showProductList(order: WCOrderModel, refunds: List<Refund>, shippingLabels: List<ShippingLabel>) {
        // populate the Order Product List Card if not all products are associated with any of the shipping labels
        // available or if there is at least 1 product that is not refunded
        val orderModel = order.toAppModel()
        val hasUnpackagedProducts = orderModel.hasUnpackagedProducts(shippingLabels)
        if (hasUnpackagedProducts && orderModel.hasNonRefundedItems(refunds)) {
            val unpackagedAndNonRefundedProducts =
                orderModel.getUnpackagedAndNonRefundedProducts(refunds, shippingLabels)

            val listTitle = if (hasVirtualProductsOnly(unpackagedAndNonRefundedProducts)) {
                getString(R.string.orderdetail_shipping_label_virtual_products_header)
            } else if (shippingLabels.isNotEmpty() && hasUnpackagedProducts) {
                getString(R.string.orderdetail_shipping_label_unpackaged_products_header)
            } else null

            orderDetail_productList.initView(
                orderModel = order,
                orderItems = unpackagedAndNonRefundedProducts,
                productImageMap = productImageMap,
                expanded = false,
                formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
                orderListener = this,
                productListener = this,
                listTitle = listTitle
            )
            orderDetail_productList.show()
        } else {
            orderDetail_productList.hide()
        }
    }

    override fun showOrderDetail(order: WCOrderModel?, isFreshData: Boolean) {
        order?.let {
            // set the title to the order number
            updateActivityTitle()

            // Populate the Order Status Card
            val orderStatus = presenter.getOrderStatusForStatusKey(order.status)
            orderDetail_orderStatus
                    .initView(order, orderStatus, object : OrderDetailOrderStatusView.OrderStatusListener {
                        override fun openOrderStatusSelector() {
                            showOrderStatusSelector()
                        }
                    })

            // check if product is a virtual product. If it is, hide only the shipping details card
            val isVirtualProduct = presenter.isVirtualProduct(order)
            orderDetail_customerInfo.initView(
                    order = order,
                    shippingOnly = false,
                    billingOnly = isVirtualProduct)

            showOrderShippingNotice(isVirtualProduct, order)

            // Populate the Payment Information Card
            orderDetail_paymentInfo.initView(
                    order.toAppModel(),
                    currencyFormatter.buildBigDecimalFormatter(order.currency),
                    this
            )

            if (isFreshData) {
                isRefreshPending = false
            }
        }
    }

    override fun refreshCustomerInfoCard(order: WCOrderModel) {
        // hide the shipping details if products in an order is virtual
        val hideShipping = presenter.isVirtualProduct(order)
        orderDetail_customerInfo.initShippingSection(order, hideShipping)
        showOrderShippingNotice(hideShipping, order)
    }

    override fun showOrderNotes(notes: List<WCOrderNoteModel>) {
        // Populate order notes card
        orderDetail_noteList.initView(notes.map { it.toAppModel() }, this)
    }

    override fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>) {
        orderDetail_shipmentList.initView(
            trackings = trackings,
            uiMessageResolver = uiMessageResolver,
            isOrderDetail = true,
            shipmentTrackingActionListener = this
        )
        if (orderDetail_shipmentList.visibility != View.VISIBLE) {
            WooAnimUtils.scaleIn(orderDetail_shipmentList, WooAnimUtils.Duration.MEDIUM)
        }
    }

    override fun showOrderNotesSkeleton(show: Boolean) {
        orderDetail_noteList.showSkeleton(show)
    }

    override fun updateOrderNotes(notes: List<WCOrderNoteModel>) {
        // Update the notes in the notes card
        orderDetail_noteList.updateView(notes.map { it.toAppModel() })
    }

    override fun openOrderFulfillment(order: WCOrderModel) {
        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToOrderFulfillmentFragment(
                order.getIdentifier(),
                order.number,
                presenter.isShipmentTrackingsFetched
        )
        findNavController().navigateSafely(action)
    }

    override fun issueOrderRefund(order: Order) {
        AnalyticsTracker.track(ORDER_DETAIL_ISSUE_REFUND_BUTTON_TAPPED)

        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToIssueRefund(order.remoteId)
        findNavController().navigateSafely(action)
    }

    override fun showRefundDetail(orderId: Long, refundId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_VIEW_REFUND_DETAILS_BUTTON_TAPPED, mapOf(
                AnalyticsTracker.KEY_ORDER_ID to orderId,
                AnalyticsTracker.KEY_ID to refundId
        ))

        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToRefundDetailFragment(orderId, refundId)
        findNavController().navigateSafely(action)
    }

    override fun openOrderProductList(order: WCOrderModel) {
        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToOrderProductListFragment(
                order.getIdentifier(),
                order.number
        )
        findNavController().navigateSafely(action)
    }

    override fun openRefundedProductList(order: WCOrderModel) {
        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToRefundDetailFragment(
                order.remoteOrderId
        )
        findNavController().navigateSafely(action)
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun setOrderStatus(newStatus: String) {
        if (isAdded) {
            val orderStatus = presenter.getOrderStatusForStatusKey(newStatus)
            orderDetail_orderStatus.updateStatus(orderStatus)
            presenter.orderModel?.let {
                orderDetail_productList.updateView(it, this)
                orderDetail_paymentInfo.initView(
                        it.toAppModel(),
                        currencyFormatter.buildBigDecimalFormatter(it.currency),
                        this
                )
            }
        }
    }

    override fun refreshOrderStatus() {
        presenter.orderModel?.let {
            setOrderStatus(it.status)
        }
    }

    override fun refreshOrderDetail(displaySkeleton: Boolean) {
        if (isAdded) {
            orderRefreshLayout.isRefreshing = false
            if (!isRefreshPending) {
                if (!networkStatus.isConnected()) {
                    uiMessageResolver.showOfflineSnack()
                    return
                }
                isRefreshPending = true
                presenter.refreshOrderDetail(displaySkeleton)
            }
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

                // if the fulfilled status was undone, tell the main activity to update the unfilled order badge
                if (newStatus == CoreOrderStatus.COMPLETED.value ||
                        newStatus == CoreOrderStatus.PROCESSING.value ||
                        previousOrderStatus == CoreOrderStatus.COMPLETED.value ||
                        previousOrderStatus == CoreOrderStatus.PROCESSING.value) {
                    (activity as? MainActivity)?.updateOrderBadge(true)
                }

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

    override fun showSkeleton(show: Boolean) {
        when (show) {
            true -> skeletonView.show(orderDetail_container, R.layout.skeleton_order_detail, delayed = true)
            false -> skeletonView.hide()
        }
    }

    override fun showLoadOrderError() {
        showSkeleton(false)
        uiMessageResolver.showSnack(R.string.order_error_fetch_generic)

        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
        }
    }

    override fun showAddOrderNoteScreen(order: WCOrderModel) {
        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToAddOrderNoteFragment(
                order.getIdentifier(),
                order.number
        )
        findNavController().navigateSafely(action)
    }

    override fun showAddOrderNoteErrorSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_error).show()
    }

    /**
     * User tapped the button to add a note, so show the add order note screen
     */
    override fun onRequestAddNote() {
        if (!networkStatus.isConnected()) {
            uiMessageResolver.showOfflineSnack()
            return
        }

        presenter.orderModel?.let {
            showAddOrderNoteScreen(it)
        }
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

    /**
     * This method error could be because of network error
     * or a failure to delete the shipment tracking from server api.
     * In both cases, add the deleted item back to the shipment tracking list
     */
    override fun undoDeletedTrackingOnError(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel?) {
        wcOrderShipmentTrackingModel?.let {
            orderDetail_shipmentList.undoDeleteTrackingRecord(it)
            orderDetail_shipmentList.visibility = View.VISIBLE
        }
    }

    override fun showDeleteTrackingErrorSnack() {
        deleteOrderShipmentTrackingResponseSnackbar =
                uiMessageResolver.getSnack(R.string.order_shipment_tracking_delete_error)
        if ((deleteOrderShipmentTrackingSnackbar?.isShownOrQueued) == false) {
            deleteOrderShipmentTrackingResponseSnackbar?.show()
        }
    }

    override fun markTrackingDeletedOnSuccess() {
        deleteOrderShipmentTrackingResponseSnackbar =
                uiMessageResolver.getSnack(R.string.order_shipment_tracking_delete_success)
        if ((deleteOrderShipmentTrackingSnackbar?.isShownOrQueued) == false) {
            deleteOrderShipmentTrackingResponseSnackbar?.show()
        }
    }

    override fun showAddShipmentTrackingSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_added).show()
    }

    override fun showAddAddShipmentTrackingErrorSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_error).show()
    }

    override fun onOrderStatusSelected(orderStatus: String?) {
        orderStatus?.let {
            showChangeOrderStatusSnackbar(it)
        }
    }

    override fun openAddOrderShipmentTrackingScreen() {
        AnalyticsTracker.track(ORDER_DETAIL_TRACKING_ADD_TRACKING_BUTTON_TAPPED)
        presenter.orderModel?.let { order ->
            val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToAddOrderShipmentTrackingFragment(
                    orderId = order.getIdentifier(),
                    orderTrackingProvider = AppPrefs.getSelectedShipmentTrackingProviderName(),
                    isCustomProvider = AppPrefs.getIsSelectedShipmentTrackingProviderCustom()
            )
            findNavController().navigateSafely(action)
        }
    }

    override fun deleteOrderShipmentTracking(item: WCOrderShipmentTrackingModel) {
        AnalyticsTracker.track(ORDER_DETAIL_TRACKING_DELETE_BUTTON_TAPPED)
        /*
         * Check if network is available. If not display offline snack
         * remove the shipment tracking model from the tracking list.
         * If there are no more items on the list, hide the shipment tracking card
         * display snackbar message with undo option
         * if undo option is selected, add the tracking model to the list
         * if snackbar is dismissed or times out, initiate request to delete the tracking
        */
        if (!networkStatus.isConnected()) {
            uiMessageResolver.showOfflineSnack()
            return
        }

        // if undo snackbar is displayed for a deleted item and user clicks on another item to delete,
        // the first snackbar should be dismissed before displaying the second snackbar
        if (deleteOrderShipmentTrackingSnackbar?.isShownOrQueued == true) {
            deleteOrderShipmentTrackingSnackbar?.dismiss()
            deleteOrderShipmentTrackingSnackbar = null
        }

        deleteOrderShipmentTrackingSet.add(item)
        orderDetail_shipmentList.deleteTrackingProvider(item)
        orderDetail_shipmentList.getShipmentTrackingCount()?.let {
            if (it == 0) {
                orderDetail_shipmentList.visibility = View.GONE
            }
        }

        // Listener for the UNDO button in the snackbar
        val actionListener = View.OnClickListener {
            // User canceled the action to delete the shipment tracking
            deleteOrderShipmentTrackingSet.remove(item)
            orderDetail_shipmentList.undoDeleteTrackingRecord(item)
            orderDetail_shipmentList.visibility = View.VISIBLE
        }

        val callback = object : Snackbar.Callback() {
            // The onDismiss in snack bar is called multiple times.
            // In order to avoid requesting delete multiple times, we are
            // storing the deleted items in a set and removing them from the set
            // if they are dismissed or if the request to delete the item is already initiated
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (deleteOrderShipmentTrackingSet.contains(item)) {
                    presenter.deleteOrderShipmentTracking(item)
                    deleteOrderShipmentTrackingSet.remove(item)
                }
            }
        }

        // Display snack bar with undo option here
        deleteOrderShipmentTrackingSnackbar = uiMessageResolver
                .getUndoSnack(R.string.order_shipment_tracking_delete_snackbar_msg, actionListener = actionListener)
                .also {
                    it.addCallback(callback)
                    it.show()
                }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.ORDER_REFUND -> {
                presenter.refreshOrderAfterDelay(REFUNDS_REFRESH_DELAY)
            }
        }
    }

    override fun openShippingLabelRefund(orderId: Long, shippingLabelId: Long) {
        val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToOrderShippingLabelRefundFragment(
            orderId, shippingLabelId
        )
        findNavController().navigateSafely(action)
    }

    private fun showOrderStatusSelector() {
        // If the device is offline, alert the user with a snack and exit (do not show order status selector).
        if (!networkStatus.isConnected()) {
            uiMessageResolver.showOfflineSnack()
            return
        }

        presenter.orderModel?.let { order ->
            val orderStatusOptions = presenter.getOrderStatusOptions()
            val orderStatus = order.status
            orderStatusSelector = OrderStatusSelectorDialog
                    .newInstance(
                            orderStatusOptions,
                            orderStatus,
                            false,
                            listener = this)
                    .also { it.show(requireFragmentManager(), OrderStatusSelectorDialog.TAG) }
        }
    }

    /**
     * Hide the shipping method warning if order contains only virtual products
     * or if the order contains only one shipping method
     * */
    private fun showOrderShippingNotice(isVirtualProduct: Boolean, order: WCOrderModel) {
        val hideShippingMethodNotice = isVirtualProduct || !order.isMultiShippingLinesAvailable()
        orderDetail_shippingMethodNotice.visibility = if (hideShippingMethodNotice) View.GONE else View.VISIBLE
    }

    private fun hasVirtualProductsOnly(
        orderItems: List<Item>
    ): Boolean {
        val remoteProductIds: List<Long> = orderItems.map { it.productId }
        if (remoteProductIds.isNullOrEmpty()) {
            return false
        }

        // verify that the LineItem product is in the local cache and
        // that the product count in the local cache matches the lineItem count.
        val productModels = presenter.getProductsByIds(remoteProductIds)
        if (productModels.isNullOrEmpty() || productModels.count() != remoteProductIds.count()) {
            return false
        }

        return productModels.none { !it.virtual }
    }
}
