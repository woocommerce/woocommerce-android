package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.CurrencyFormatter
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_product_list.*
import kotlinx.android.synthetic.main.order_detail_product_list.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderProductListFragment : androidx.fragment.app.Fragment(), OrderProductListContract.View {
    @Inject lateinit var presenter: OrderProductListContract.Presenter
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var productImageMap: ProductImageMap

    private val navArgs: OrderProductListFragmentArgs by navArgs()

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_order_product_list, container, false)

        activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, navArgs.orderNumber)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        presenter.loadOrderDetail(navArgs.orderId)

        productList_products.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) onScrollDown() else if (dy < 0) onScrollUp()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderProducts(order: WCOrderModel) {
        orderProducts_list.initView(
                order = order,
                productImageMap = productImageMap,
                expanded = true,
                formatCurrencyForDisplay = currencyFormatter.buildFormatter(order.currency),
                orderListener = null,
                productListener = this
        )
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }
}
