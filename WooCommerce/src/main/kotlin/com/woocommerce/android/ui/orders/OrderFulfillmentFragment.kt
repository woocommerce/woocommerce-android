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
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_FULFILLMENT_MARK_ORDER_COMPLETE_BUTTON_TAPPED
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingActivity.Companion.FIELD_IS_CUSTOM_PROVIDER
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingActivity.Companion.FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingActivity.Companion.FIELD_ORDER_TRACKING_DATE_SHIPPED
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingActivity.Companion.FIELD_ORDER_TRACKING_NUMBER
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingActivity.Companion.FIELD_ORDER_TRACKING_PROVIDER
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.AppRatingDialog
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_fulfillment.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import javax.inject.Inject

class OrderFulfillmentFragment : Fragment(), OrderFulfillmentContract.View, View.OnClickListener {
    companion object {
        const val TAG = "OrderFulfillmentFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_SHIPMENT_TRACKINGS_FETCHED = "is-shipment-trackings-fetched"
        const val REQUEST_CODE_ADD_TRACKING = 101

        fun newInstance(order: WCOrderModel, isUsingCachedShipmentTrackings: Boolean = false): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)

            // Used to check if the shipment trackings is using cache only. If shipment
            // trackings list is already fetched from api in order detail, we can just
            // fetch the same from local cache
            args.putBoolean(FIELD_SHIPMENT_TRACKINGS_FETCHED, isUsingCachedShipmentTrackings)

            val fragment = OrderFulfillmentFragment()
            fragment.arguments = args
            return fragment
        }
    }

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
    private var deletedOrderShipmentTrackingModel: WCOrderShipmentTrackingModel? = null
    private var deleteOrderShipmentTrackingCancelled: Boolean = false
    private var deleteOrderShipmentTrackingSnackbar: Snackbar? = null

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
        arguments?.getString(FIELD_ORDER_IDENTIFIER, null)?.let {
            presenter.loadOrderDetail(
                    orderIdentifier = it,
                    isShipmentTrackingsFetched = arguments?.getBoolean(FIELD_SHIPMENT_TRACKINGS_FETCHED) ?: false
            )
        }

        scrollView.setOnScrollChangeListener {
            v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            if (scrollY > oldScrollY) onScrollDown() else if (scrollY < oldScrollY) onScrollUp()
        }
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
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ADD_TRACKING) {
            if (data != null) {
                val selectedShipmentTrackingProviderName = data.getStringExtra(FIELD_ORDER_TRACKING_PROVIDER)
                AppPrefs.setSelectedShipmentTrackingProviderName(selectedShipmentTrackingProviderName)

                if (resultCode == RESULT_OK) {
                    val isCustomProvider = data.getBooleanExtra(FIELD_IS_CUSTOM_PROVIDER, false)
                    AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(isCustomProvider)

                    val dateShipped = data.getStringExtra(FIELD_ORDER_TRACKING_DATE_SHIPPED)
                    val customProviderUrlText = data.getStringExtra(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL)
                    val trackingNumText = data.getStringExtra(FIELD_ORDER_TRACKING_NUMBER)

                    val orderShipmentTrackingModel = WCOrderShipmentTrackingModel()
                    orderShipmentTrackingModel.trackingNumber = trackingNumText
                    orderShipmentTrackingModel.dateShipped = dateShipped
                    orderShipmentTrackingModel.trackingProvider = selectedShipmentTrackingProviderName
                    if (isCustomProvider) {
                        customProviderUrlText?.let { orderShipmentTrackingModel.trackingLink = it }
                    }

                    orderFulfill_addShipmentTracking.addTransientTrackingProvider(orderShipmentTrackingModel)
                    presenter.pushShipmentTrackingProvider(orderShipmentTrackingModel, isCustomProvider)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun showOrderDetail(order: WCOrderModel) {
        // Populate the Order Product List Card
        orderFulfill_products.initView(
                order = order,
                productImageMap = productImageMap,
                expanded = true,
                formatCurrencyForDisplay = currencyFormatter.buildFormatter(order.currency),
                orderListener = null,
                productListener = this
        )

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

    /**
     * The Optional shipment tracking card should be displayed
     * even if there are no trackings available
     */
    override fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>) {
            orderFulfill_addShipmentTracking.initView(
                    trackings = trackings,
                    uiMessageResolver = uiMessageResolver,
                    allowAddTrackingOption = true,
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
    override fun undoDeletedTrackingOnError() {
        deletedOrderShipmentTrackingModel?.let {
            orderFulfill_addShipmentTracking.undoDeleteTrackingRecord(it)
        }
        deletedOrderShipmentTrackingModel = null
    }

    override fun showDeleteTrackingErrorSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_delete_error).show()
    }

    override fun markTrackingDeletedOnSuccess() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_delete_success).show()
        deletedOrderShipmentTrackingModel = null
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
        parentFragment?.let { router ->
            if (router is OrdersViewRouter) {
                presenter.orderModel?.let {
                    router.openOrderDetail(it, true)
                }
            }
        }
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        activity?.let { router ->
            if (router is TopLevelFragmentRouter) {
                router.showProductDetail(remoteProductId)
            }
        }
    }

    override fun openAddOrderShipmentTrackingScreen() {
        presenter.orderModel?.let {
            val intent = Intent(activity, AddOrderShipmentTrackingActivity::class.java)
            intent.putExtra(AddOrderShipmentTrackingActivity.FIELD_ORDER_IDENTIFIER, it.getIdentifier())
            intent.putExtra(FIELD_ORDER_TRACKING_PROVIDER, AppPrefs.getSelectedShipmentTrackingProviderName())
            intent.putExtra(FIELD_IS_CUSTOM_PROVIDER, AppPrefs.getIsSelectedShipmentTrackingProviderCustom())
            startActivityForResult(intent, REQUEST_CODE_ADD_TRACKING)
        }
    }

    override fun deleteOrderShipmentTracking(item: WCOrderShipmentTrackingModel) {
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

        deleteOrderShipmentTrackingCancelled = false
        orderFulfill_addShipmentTracking.deleteTrackingProvider(item)

        // Listener for the UNDO button in the snackbar
        val actionListener = View.OnClickListener {
            // User canceled the action to delete the shipment tracking
            deleteOrderShipmentTrackingCancelled = true
            orderFulfill_addShipmentTracking.undoDeleteTrackingRecord(item)
        }

        val callback = object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (!deleteOrderShipmentTrackingCancelled) {
                    deletedOrderShipmentTrackingModel = item
                    presenter.deleteOrderShipmentTracking(item)
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
