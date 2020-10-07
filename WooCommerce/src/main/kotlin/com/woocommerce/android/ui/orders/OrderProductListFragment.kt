package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.CurrencyFormatter
import kotlinx.android.synthetic.main.fragment_order_product_list.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderProductListFragment : BaseFragment(), OrderProductListContract.View {
    @Inject lateinit var presenter: OrderProductListContract.Presenter
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var productImageMap: ProductImageMap

    private val navArgs: OrderProductListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_order_product_list, container, false)
    }

    override fun getFragmentTitle(): String = getString(R.string.orderdetail_orderstatus_ordernum, navArgs.orderNumber)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        presenter.loadOrderDetail(navArgs.orderId)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderProducts(order: WCOrderModel, refunds: List<Refund>) {
        orderProducts_list.initView(
                orderModel = order,
                orderItems = refunds.getNonRefundedProducts(order.toAppModel().items),
                productImageMap = productImageMap,
                expanded = true,
                formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
                orderListener = null,
                productListener = this
        )
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }
}
