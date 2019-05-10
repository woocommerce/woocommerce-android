package com.woocommerce.android.ui.orders

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.NestedScrollView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_FULFILLMENT_MARK_ORDER_COMPLETE_BUTTON_TAPPED
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import com.woocommerce.android.ui.base.UIMessageResolver
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
        const val REQUEST_CODE_ADD_TRACKING = 101

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
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var productImageMap: ProductImageMap

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ADD_TRACKING) {
            if (resultCode == RESULT_OK && data != null) {
                val trackingNumText = data.getStringExtra(FIELD_ORDER_TRACKING_NUMBER)
                val dateShippedText = data.getStringExtra(FIELD_ORDER_TRACKING_DATE_SHIPPED)
                val providerText = data.getStringExtra(FIELD_ORDER_TRACKING_PROVIDER)
                orderFulfill_addShipmentTracking.addTransientTrackingProvider(
                        providerText,
                        trackingNumText,
                        dateShippedText
                )
                presenter.pushShipmentTrackingProvider(providerText, trackingNumText, dateShippedText)
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

    override fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>) {
        if (trackings.isNotEmpty()) {
            orderFulfill_addShipmentTracking.initView(
                    trackings = trackings,
                    uiMessageResolver = uiMessageResolver,
                    allowAddTrackingOption = true,
                    shipmentTrackingActionListener = this
            )
            if (orderFulfill_addShipmentTracking.visibility != View.VISIBLE) {
                WooAnimUtils.scaleIn(orderFulfill_addShipmentTracking, WooAnimUtils.Duration.MEDIUM)
            }
        } else {
            if (orderFulfill_addShipmentTracking.visibility == View.VISIBLE) {
                WooAnimUtils.scaleOut(orderFulfill_addShipmentTracking, WooAnimUtils.Duration.MEDIUM)
            }
        }
    }

    override fun showAddShipmentTrackingSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_added).show()
    }

    override fun showAddAddShipmentTrackingErrorSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_error).show()
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
            startActivityForResult(intent, REQUEST_CODE_ADD_TRACKING)
        }
    }
}
