package com.woocommerce.android.ui.orders.fulfill

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentOrderFulfillBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderFulfillFragment :
    BaseFragment(R.layout.fragment_order_fulfill),
    OrderProductActionListener,
    BackPressListener {
    companion object {
        val TAG: String = OrderFulfillFragment::class.java.simpleName
    }

    private val viewModel: OrderFulfillViewModel by viewModels()

    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var dateUtils: DateUtils

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentOrderFulfillBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(binding)
        setupResultHandlers(viewModel)
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun openOrderProductVariationDetail(remoteProductId: Long, remoteVariationId: Long) {
        (activity as? MainNavigationRouter)?.showProductVariationDetail(remoteProductId, remoteVariationId)
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }

    private fun setupObservers(binding: FragmentOrderFulfillBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.order?.takeIfNotEqualTo(old?.order) {
                showOrderDetail(it, binding)
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) { screenTitle = it }
            new.isShipmentTrackingAvailable?.takeIfNotEqualTo(old?.isShipmentTrackingAvailable) {
                showAddShipmentTracking(it, binding)
            }
        }
        viewModel.productList.observe(
            viewLifecycleOwner,
            Observer {
                showOrderProducts(it, viewModel.order.currency, binding)
            }
        )
        viewModel.shipmentTrackings.observe(
            viewLifecycleOwner,
            Observer {
                showShipmentTrackings(it, binding)
            }
        )

        viewModel.event.observe(
            viewLifecycleOwner,
            Observer { event ->
                when (event) {
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    is Exit -> findNavController().navigateUp()
                    is ExitWithResult<*> -> navigateBackWithResult(event.key!!, event.data)
                    is OrderNavigationTarget -> navigator.navigate(this, event)
                    is ShowUndoSnackbar -> displayUndoSnackbar(event.message, event.undoAction, event.dismissAction)
                    else -> event.isHandled = false
                }
            }
        )
    }

    private fun setupResultHandlers(viewModel: OrderFulfillViewModel) {
        handleResult<OrderShipmentTracking>(AddOrderShipmentTrackingFragment.KEY_ADD_SHIPMENT_TRACKING_RESULT) {
            viewModel.onNewShipmentTrackingAdded(it)
        }
    }

    private fun showOrderDetail(order: Order, binding: FragmentOrderFulfillBinding) {
        binding.orderDetailCustomerInfo.updateCustomerInfo(
            order = order,
            isVirtualOrder = viewModel.hasVirtualProductsOnly(),
            isReadOnly = true
        )
        binding.buttonMarkOrderCompete.setOnClickListener {
            viewModel.onMarkOrderCompleteButtonClicked()
        }
    }

    private fun showOrderProducts(
        products: List<Order.Item>,
        currency: String,
        binding: FragmentOrderFulfillBinding
    ) {
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

    private fun showAddShipmentTracking(
        show: Boolean,
        binding: FragmentOrderFulfillBinding
    ) {
        with(binding.orderDetailShipmentList) {
            isVisible = show
            showAddTrackingButton(show) { viewModel.onAddShipmentTrackingClicked() }
        }
    }

    private fun showShipmentTrackings(
        shipmentTrackings: List<OrderShipmentTracking>,
        binding: FragmentOrderFulfillBinding
    ) {
        binding.orderDetailShipmentList.updateShipmentTrackingList(
            shipmentTrackings = shipmentTrackings,
            dateUtils = dateUtils,
            onDeleteShipmentTrackingClicked = {
                viewModel.onDeleteShipmentTrackingClicked(it)
            }
        )
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
