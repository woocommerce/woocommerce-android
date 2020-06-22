package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_FULFILLMENT_MARK_ORDER_COMPLETE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_FULFILLMENT_TRACKING_ADD_TRACKING_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_FULFILLMENT_TRACKING_DELETE_BUTTON_TAPPED
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.AppRatingDialog
import kotlinx.android.synthetic.main.fragment_order_fulfillment.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import javax.inject.Inject

class OrderFulfillmentFragment : BaseFragment(), OrderFulfillmentContract.View, View.OnClickListener {
    @Inject lateinit var presenter: OrderFulfillmentContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var networkStatus: NetworkStatus

    /**
     * Keep track of the deleted [WCOrderShipmentTrackingModel] in case
     * the request to server fails, we need to display an error message
     * and add the deleted [WCOrderShipmentTrackingModel] back to the list
     */
    private var deleteOrderShipmentTrackingSnackbar: Snackbar? = null
    private var deleteOrderShipmentTrackingResponseSnackbar: Snackbar? = null
    private var deleteOrderShipmentTrackingSet = mutableSetOf<WCOrderShipmentTrackingModel>()

    private val navArgs: OrderFulfillmentFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_fulfillment, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_order_fulfillment, navArgs.orderNumber)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        // isUsingCachedShipmentTrackings is used to check if the shipment trackings is using cache only.
        //  If shipment trackings list is already fetched from api in order detail, we can just
        // fetch the same from local cache
        presenter.loadOrderDetail(
                navArgs.orderId,
                navArgs.isUsingCachedShipmentTrackings
        )
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onStop() {
        deleteOrderShipmentTrackingSnackbar?.dismiss()
        deleteOrderShipmentTrackingSnackbar = null
        deleteOrderShipmentTrackingResponseSnackbar?.dismiss()
        deleteOrderShipmentTrackingResponseSnackbar = null
        super.onStop()
    }

    override fun showOrderDetail(order: WCOrderModel, refunds: List<Refund>) {
        // Populate the Order Product List Card
        orderFulfill_products.initView(
                orderModel = order,
                productImageMap = productImageMap,
                expanded = true,
                formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
                orderListener = null,
                productListener = this,
                refunds = refunds
        )

        // check if product is a virtual product
        val isVirtualProduct = presenter.isVirtualProduct(order)

        // Populate the Customer Information Card
        // hide shipping card if product is virtual or if no shipping address is available
        val hideShipping = isVirtualProduct || !orderFulfill_customerInfo.isShippingAvailable(order)
        if (hideShipping) {
            orderFulfill_customerInfo.visibility = View.GONE
        } else {
            // Populate the Customer Information Card
            orderFulfill_customerInfo.visibility = View.VISIBLE
            orderFulfill_customerInfo.initView(order, true)
        }

        // load shipment tracking card only if product is NOT virtual
        if (!isVirtualProduct) {
            presenter.loadOrderShipmentTrackings()
        }

        orderFulfill_btnComplete.setOnClickListener(this)
    }

    /**
     * The Optional shipment tracking card should be displayed
     * even if there are no trackings available
     */
    override fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>) {
            orderFulfill_addShipmentTracking.initView(
                    trackings = trackings,
                    uiMessageResolver = uiMessageResolver,
                    isOrderDetail = false,
                    shipmentTrackingActionListener = this
            )
            if (orderFulfill_addShipmentTracking.visibility != View.VISIBLE) {
                WooAnimUtils.scaleIn(orderFulfill_addShipmentTracking, WooAnimUtils.Duration.MEDIUM)
            }
    }

    override fun showAddShipmentTrackingSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_added).show()
    }

    override fun showAddAddShipmentTrackingErrorSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_error).show()
    }

    /**
     * This method error could be because of network error
     * or a failure to delete the shipment tracking from server api.
     * In both cases, add the deleted item back to the shipment tracking list
     */
    override fun undoDeletedTrackingOnError(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel?) {
        wcOrderShipmentTrackingModel?.let {
            orderFulfill_addShipmentTracking.undoDeleteTrackingRecord(it)
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

    override fun onClick(v: View?) {
        // User has clicked the button to mark this order complete.
        context?.let {
            AnalyticsTracker.track(ORDER_FULFILLMENT_MARK_ORDER_COMPLETE_BUTTON_TAPPED)

            presenter.orderModel?.let {
                presenter.markOrderComplete()
                AppRatingDialog.incrementInteractions()
            }
        }
    }

    override fun toggleCompleteButton(isEnabled: Boolean) {
        orderFulfill_btnComplete?.isEnabled = isEnabled
    }

    override fun fulfillOrder() {
        presenter.orderModel?.let { order ->
            (activity as? MainNavigationRouter)?.showOrderDetail(
                    localSiteId = order.localSiteId,
                    remoteOrderId = order.remoteOrderId,
                    orderStatus = order.status,
                    markComplete = true
            )
        }
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun openAddOrderShipmentTrackingScreen() {
        AnalyticsTracker.track(ORDER_FULFILLMENT_TRACKING_ADD_TRACKING_BUTTON_TAPPED)
        presenter.orderModel?.let { order ->
            val action =
                    OrderFulfillmentFragmentDirections.actionOrderFulfillmentFragmentToAddOrderShipmentTrackingFragment(
                            orderId = order.getIdentifier(),
                            orderTrackingProvider = AppPrefs.getSelectedShipmentTrackingProviderName(),
                            isCustomProvider = AppPrefs.getIsSelectedShipmentTrackingProviderCustom()
                    )
            findNavController().navigateSafely(action)
        }
    }

    override fun deleteOrderShipmentTracking(item: WCOrderShipmentTrackingModel) {
        AnalyticsTracker.track(ORDER_FULFILLMENT_TRACKING_DELETE_BUTTON_TAPPED)
        /*
         * Check if network is available. If not display offline snack
         * remove the shipment tracking model from the tracking list
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
        orderFulfill_addShipmentTracking.deleteTrackingProvider(item)

        // Listener for the UNDO button in the snackbar
        val actionListener = View.OnClickListener {
            // User canceled the action to delete the shipment tracking
            deleteOrderShipmentTrackingSet.remove(item)
            orderFulfill_addShipmentTracking.undoDeleteTrackingRecord(item)
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
}
