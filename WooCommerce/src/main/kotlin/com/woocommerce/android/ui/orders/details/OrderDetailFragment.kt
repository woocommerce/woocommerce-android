package com.woocommerce.android.ui.orders.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import javax.inject.Inject

class OrderDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: OrderDetailViewModel by viewModels { viewModelFactory }

    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

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
        }

        viewModel.orderNotes.observe(viewLifecycleOwner, Observer {
            showOrderNotes(it)
        })
        viewModel.loadOrderDetail()
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
            formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency)
        )
    }

    private fun showOrderStatus(orderStatus: OrderStatus) {
        orderDetail_orderStatus.updateStatus(orderStatus)
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
        orderDetail_noteList.updateOrderNotesView(orderNotes)
    }
}
