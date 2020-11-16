package com.woocommerce.android.ui.orders.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.FEATURE_FEEDBACK_BANNER
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.SHIPPING_LABELS_M1
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingFragment
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.OnShippingLabelClickListener
import com.woocommerce.android.ui.orders.notes.AddOrderNoteFragment
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class OrderDetailFragment : BaseFragment(), NavigationResult, OrderProductActionListener {
    companion object {
        val TAG: String = OrderDetailFragment::class.java.simpleName
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: OrderDetailViewModel by viewModels { viewModelFactory }

    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap

    private val skeletonView = SkeletonView()
    private var undoSnackbar: Snackbar? = null

    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)?.state ?: UNANSWERED

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_order_detail, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        undoSnackbar?.dismiss()
        super.onStop()
    }

    override fun getFragmentTitle() = viewModel.toolbarTitle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        orderRefreshLayout?.apply {
            scrollUpChild = scrollView
            setOnRefreshListener { viewModel.onRefreshRequested() }
        }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        if (requestCode == RequestCodes.ORDER_REFUND) {
            viewModel.onOrderItemRefunded()
        }
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    private fun setupObservers(viewModel: OrderDetailViewModel) {
        viewModel.orderDetailViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.order?.takeIfNotEqualTo(old?.order) { showOrderDetail(it) }
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) { showOrderStatus(it) }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) { activity?.title = it }
            new.isOrderDetailSkeletonShown?.takeIfNotEqualTo(old?.isOrderDetailSkeletonShown) { showSkeleton(it) }
            new.isOrderNotesSkeletonShown?.takeIfNotEqualTo(old?.isOrderNotesSkeletonShown) {
                showOrderNotesSkeleton(it)
            }
            new.isShipmentTrackingAvailable?.takeIfNotEqualTo(old?.isShipmentTrackingAvailable) {
                showAddShipmentTracking(it)
            }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                orderRefreshLayout.isRefreshing = it
            }
            new.refreshedProductId?.takeIfNotEqualTo(old?.refreshedProductId) { refreshProduct(it) }
        }

        viewModel.orderNotes.observe(viewLifecycleOwner, Observer {
            showOrderNotes(it)
        })
        viewModel.orderRefunds.observe(viewLifecycleOwner, Observer {
            showOrderRefunds(it)
        })
        viewModel.productList.observe(viewLifecycleOwner, Observer {
            showOrderProducts(it)
        })
        viewModel.shipmentTrackings.observe(viewLifecycleOwner, Observer {
            showShipmentTrackings(it)
        })
        viewModel.shippingLabels.observe(viewLifecycleOwner, Observer {
            showShippingLabels(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowUndoSnackbar -> {
                    displayUndoSnackbar(event.message, event.undoAction, event.dismissAction)
                }
                is OrderNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
    }

    private fun setupResultHandlers(viewModel: OrderDetailViewModel) {
        handleResult<String>(OrderStatusSelectorDialog.KEY_ORDER_STATUS_RESULT) {
            viewModel.onOrderStatusChanged(it)
        }
        handleResult<OrderNote>(AddOrderNoteFragment.KEY_ADD_NOTE_RESULT) {
            viewModel.onNewOrderNoteAdded(it)
        }
        handleResult<Boolean>(ShippingLabelRefundFragment.KEY_REFUND_SHIPPING_LABEL_RESULT) {
            viewModel.onShippingLabelRefunded()
        }
        handleResult<OrderShipmentTracking>(AddOrderShipmentTrackingFragment.KEY_ADD_SHIPMENT_TRACKING_RESULT) {
            viewModel.onNewShipmentTrackingAdded(it)
        }
    }

    private fun showOrderDetail(order: Order) {
        orderDetail_orderStatus.updateOrder(order)
            .takeIf { it.not() }
            ?.let {
                // showErrorScreen
            }
            ?.also { return }

        orderDetail_shippingMethodNotice.isVisible = order.multiShippingLinesAvailable
        orderDetail_customerInfo.updateCustomerInfo(
            order = order,
            isVirtualOrder = viewModel.hasVirtualProductsOnly()
        )
        orderDetail_paymentInfo.updatePaymentInfo(
            order = order,
            formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
            onIssueRefundClickListener = { viewModel.onIssueOrderRefundClicked() }
        )
    }

    private fun showOrderStatus(orderStatus: OrderStatus) {
        orderDetail_orderStatus.updateStatus(orderStatus) {
            viewModel.onEditOrderStatusSelected()
        }
        orderDetail_productList.showOrderFulfillOption(orderStatus.statusKey == CoreOrderStatus.PROCESSING.value) {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED)
            viewModel.onOrderStatusChanged(CoreOrderStatus.COMPLETED.value)
        }
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> skeletonView.show(orderDetail_container, R.layout.skeleton_order_detail, delayed = true)
            false -> skeletonView.hide()
        }
    }

    private fun showOrderNotesSkeleton(show: Boolean) {
        orderDetail_noteList.showSkeleton(show)
    }

    private fun refreshProduct(remoteProductId: Long) {
        orderDetail_productList.notifyProductChanged(remoteProductId)
    }

    private fun showOrderNotes(orderNotes: List<OrderNote>) {
        orderDetail_noteList.updateOrderNotesView(orderNotes) {
            viewModel.onAddOrderNoteClicked()
        }
    }

    private fun showOrderRefunds(refunds: List<Refund>) {
        // display the refunds count in the refunds section
        val refundsCount = refunds.sumBy { refund -> refund.items.sumBy { it.quantity } }
        if (refundsCount > 0) {
            orderDetail_refundsInfo.show()
            orderDetail_refundsInfo.updateRefundCount(refundsCount) {
                viewModel.onViewRefundedProductsClicked()
            }
        } else {
            orderDetail_refundsInfo.hide()
        }

        // display refunds list in the payment info section, if available
        val order = requireNotNull(viewModel.order)
        val formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)

        refunds.whenNotNullNorEmpty {
            orderDetail_paymentInfo.showRefunds(order, it, formatCurrency)
        }.otherwise {
            orderDetail_paymentInfo.showRefundTotal(
                show = order.isRefundAvailable,
                refundTotal = order.refundTotal,
                formatCurrencyForDisplay = formatCurrency
            )
        }
    }

    private fun showOrderProducts(products: List<Order.Item>) {
        products.whenNotNullNorEmpty {
            val order = requireNotNull(viewModel.order)
            with(orderDetail_productList) {
                show()
                updateProductList(
                    orderItems = products,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
                    productClickListener = this@OrderDetailFragment
                )
            }
        }.otherwise { orderDetail_productList.hide() }
    }

    private fun showAddShipmentTracking(show: Boolean) {
        with(orderDetail_shipmentList) {
            isVisible = show
            showAddTrackingButton(show) { viewModel.onAddShipmentTrackingClicked() }
        }
    }

    private fun showShipmentTrackings(
        shipmentTrackings: List<OrderShipmentTracking>
    ) {
        orderDetail_shipmentList.updateShipmentTrackingList(shipmentTrackings) {
            viewModel.onDeleteShipmentTrackingClicked(it)
        }
    }

    private fun showShippingLabels(shippingLabels: List<ShippingLabel>) {
        shippingLabels.whenNotNullNorEmpty {
            val order = requireNotNull(viewModel.order)
            with(orderDetail_shippingLabelList) {
                show()
                displayShippingLabelsWIPCard(true)
                updateShippingLabels(
                    shippingLabels = shippingLabels,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
                    productClickListener = this@OrderDetailFragment,
                    shippingLabelClickListener = object : OnShippingLabelClickListener {
                        override fun onRefundRequested(shippingLabel: ShippingLabel) {
                            viewModel.onRefundShippingLabelClick(shippingLabel.id)
                        }

                        override fun onPrintShippingLabelClicked(shippingLabel: ShippingLabel) {
                            viewModel.onPrintShippingLabelClicked(shippingLabel.id)
                        }
                    }
                )
            }
        }.otherwise {
            orderDetail_shippingLabelList.hide()
            displayShippingLabelsWIPCard(false)
        }
    }

    private fun displayShippingLabelsWIPCard(show: Boolean) {
        if (show && feedbackState != DISMISSED) {
            orderDetail_shippingLabelsWipCard.isVisible = true
            val wipCardMessageId = R.string.orderdetail_shipping_label_wip_message
            orderDetail_shippingLabelsWipCard.initView(
                getString(R.string.orderdetail_shipping_label_wip_title),
                getString(wipCardMessageId),
                onGiveFeedbackClick = ::onGiveFeedbackClicked,
                onDismissClick = ::onDismissProductWIPNoticeCardClicked
            )
        } else orderDetail_shippingLabelsWipCard.isVisible = false
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER, mapOf(
            AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SHIPPING_LABELS_M1_FEEDBACK,
            AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
        ))
        registerFeedbackSetting(GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.SHIPPING_LABELS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissProductWIPNoticeCardClicked() {
        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER, mapOf(
            AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SHIPPING_LABELS_M1_FEEDBACK,
            AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
        ))
        registerFeedbackSetting(DISMISSED)
        displayShippingLabelsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeedbackState) {
        FeatureFeedbackSettings(SHIPPING_LABELS_M1.name, state)
            .run { FeedbackPrefs.setFeatureFeedbackSettings(TAG, this) }
    }

    private fun displayUndoSnackbar(
        message: String,
        actionListener: View.OnClickListener,
        dismissCallback: Snackbar.Callback
    ) {
        undoSnackbar = uiMessageResolver.getUndoSnack(
            message = message,
            actionListener = actionListener
        ).also {
            it.addCallback(dismissCallback)
            it.show()
        }
    }
}
