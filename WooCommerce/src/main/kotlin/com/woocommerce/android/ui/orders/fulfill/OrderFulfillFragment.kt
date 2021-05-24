package com.woocommerce.android.ui.orders.fulfill

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentOrderFulfillBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderFulfillFragment : BaseFragment(R.layout.fragment_order_fulfill), OrderProductActionListener {
    companion object {
        val TAG: String = OrderFulfillFragment::class.java.simpleName
    }

    private val viewModel: OrderFulfillViewModel by viewModels()

    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var dateUtils: DateUtils

    private var _binding: FragmentOrderFulfillBinding? = null
    private val binding get() = _binding!!

    private var undoSnackbar: Snackbar? = null
    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        undoSnackbar?.dismiss()
        super.onStop()
    }

    override fun getFragmentTitle() = screenTitle

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderFulfillBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun openOrderProductVariationDetail(remoteProductId: Long, remoteVariationId: Long) {
        (activity as? MainNavigationRouter)?.showProductVariationDetail(remoteProductId, remoteVariationId)
    }

    private fun setupObservers(viewModel: OrderFulfillViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.order?.takeIfNotEqualTo(old?.order) {
                showOrderDetail(it)
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) { screenTitle = it }
            new.isShipmentTrackingAvailable?.takeIfNotEqualTo(old?.isShipmentTrackingAvailable) {
                showAddShipmentTracking(it)
            }
        }
        viewModel.productList.observe(viewLifecycleOwner, Observer {
            showOrderProducts(it, viewModel.order.currency)
        })
        viewModel.shipmentTrackings.observe(viewLifecycleOwner, Observer {
            showShipmentTrackings(it)
        })
    }

    private fun showOrderDetail(order: Order) {
        binding.orderDetailCustomerInfo.updateCustomerInfo(
            order = order,
            isVirtualOrder = viewModel.hasVirtualProductsOnly()
        )
    }

    private fun showOrderProducts(products: List<Order.Item>, currency: String) {
        products.whenNotNullNorEmpty {
            with(binding.orderDetailProductList) {
                showProductListMenuButton(false)
                showMarkOrderCompleteButton(false) { }
                updateProductList(
                    orderItems = products,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency),
                    productClickListener = this@OrderFulfillFragment,
                    onProductMenuItemClicked = { /* will be added in a separate commit */ }
                )
            }
        }.otherwise { binding.orderDetailProductList.hide() }
    }

    private fun showAddShipmentTracking(show: Boolean) {
        with(binding.orderDetailShipmentList) {
            isVisible = show
            showAddTrackingButton(show) { /* will be added in a separate commit */ }
        }
    }

    private fun showShipmentTrackings(
        shipmentTrackings: List<OrderShipmentTracking>
    ) {
        binding.orderDetailShipmentList.updateShipmentTrackingList(
            shipmentTrackings = shipmentTrackings,
            dateUtils = dateUtils,
            onDeleteShipmentTrackingClicked = {
                /* will be added in a separate commit */
            })
    }
}
