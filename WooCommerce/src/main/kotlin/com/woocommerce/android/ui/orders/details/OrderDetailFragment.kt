package com.woocommerce.android.ui.orders.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.notes.AddOrderNoteFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class OrderDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: OrderDetailViewModel by viewModels { viewModelFactory }

    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap

    private val skeletonView = SkeletonView()

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
                orderDetail_shipmentList.isVisible = it
                orderDetail_shipmentList.showAddTrackingButton(it)
            }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                orderRefreshLayout.isRefreshing = it
            }
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
                    displayOrderStatusChangeSnackbar(event.message, event.undoAction, event.dismissAction)
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
    }

    private fun showOrderDetail(order: Order) {
        orderDetail_orderStatus.updateOrder(order)
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
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency)
                )
                showOrderFulfillOption(order.status == CoreOrderStatus.PROCESSING)
            }
        }.otherwise { orderDetail_productList.hide() }
    }

    private fun showShipmentTrackings(shipmentTrackings: List<OrderShipmentTracking>) {
        shipmentTrackings.whenNotNullNorEmpty {
            orderDetail_shipmentList.updateShipmentTrackingList(shipmentTrackings)
        }
    }

    private fun showShippingLabels(shippingLabels: List<ShippingLabel>) {
        shippingLabels.whenNotNullNorEmpty {
            val order = requireNotNull(viewModel.order)
            with(orderDetail_shippingLabelList) {
                show()
                updateShippingLabels(
                    shippingLabels = shippingLabels,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency)
                )
            }
        }.otherwise { orderDetail_shippingLabelList.hide() }
    }

    private fun displayOrderStatusChangeSnackbar(
        message: String,
        actionListener: View.OnClickListener,
        dismissCallback: Snackbar.Callback
    ) {
        uiMessageResolver.getUndoSnack(
            message = message,
            actionListener = actionListener
        ).also {
            it.addCallback(dismissCallback)
            it.show()
        }
    }
}
