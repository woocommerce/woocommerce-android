package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductInventorySelectorDialog.ProductInventorySelectorDialogListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_inventory.*
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductInventoryFragment : BaseProductFragment(), ProductInventorySelectorDialogListener,
        BackPressListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products) { viewModelFactory }

    private var productBackOrderSelectorDialog: ProductInventorySelectorDialog? = null
    private var productStockStatusSelectorDialog: ProductInventorySelectorDialog? = null

    private var publishMenuItem: MenuItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_inventory, container, false)
    }

    override fun onPause() {
        super.onPause()
        productBackOrderSelectorDialog?.dismiss()
        productBackOrderSelectorDialog = null

        productStockStatusSelectorDialog?.dismiss()
        productStockStatusSelectorDialog = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun getFragmentTitle() = getString(R.string.product_inventory)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        publishMenuItem = menu.findItem(R.id.menu_done)
    }

    override fun setupObservers(viewModel: ProductDetailViewModel) {
        super.setupObservers(viewModel)
        viewModel.productInventoryViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.skuErrorMessage?.takeIfNotEqualTo(old?.skuErrorMessage) { displaySkuError(it) }
        }

        updateProductView(viewModel.getProduct())
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.product)
        with(product_sku) {
            if (product.sku.isNotEmpty()) {
                setText(product.sku)
            }
            setOnTextChangedListener {
                viewModel.updateProductDraft(sku = it.toString())
                // TODO: migrate sku  logic to ProductDetailViewModel
//                viewModel.onSkuChanged(it.toString())
            }
        }

        val manageStock = product.manageStock
        enableManageStockStatus(manageStock)
        with(manageStock_switch) {
            isChecked = manageStock
            setOnCheckedChangeListener { _, b ->
                enableManageStockStatus(b)
                viewModel.updateProductDraft(manageStock = b)
            }
        }

        with(product_stock_quantity) {
            setText(product.stockQuantity.toString())
            setOnTextChangedListener {
                if (it.toString().isNotEmpty()) {
                    viewModel.updateProductDraft(stockQuantity = it.toString())
                }
            }
        }

        with(edit_product_backorders) {
            setText(ProductBackorderStatus.backordersToDisplayString(requireContext(), product.backorderStatus))
            setClickListener {
                productBackOrderSelectorDialog = ProductInventorySelectorDialog.newInstance(
                        this@ProductInventoryFragment, RequestCodes.PRODUCT_INVENTORY_BACKORDERS,
                        getString(R.string.product_backorders), ProductBackorderStatus.toMap(requireContext()),
                        edit_product_backorders.getText()
                ).also { it.show(parentFragmentManager, ProductInventorySelectorDialog.TAG) }
            }
        }

        with(edit_product_stock_status) {
            setText(ProductStockStatus.stockStatusToDisplayString(requireContext(), product.stockStatus))
            setClickListener {
                productStockStatusSelectorDialog = ProductInventorySelectorDialog.newInstance(
                        this@ProductInventoryFragment, RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS,
                        getString(R.string.product_stock_status), ProductStockStatus.toMap(requireContext()),
                        edit_product_stock_status.getText()
                ).also { it.show(parentFragmentManager, ProductInventorySelectorDialog.TAG) }
            }
        }

        with(soldIndividually_switch) {
            isChecked = product.soldIndividually
            setOnCheckedChangeListener { _, b ->
                viewModel.updateProductDraft(soldIndividually = b)
            }
        }
    }

    private fun enableManageStockStatus(manageStock: Boolean) {
        if (manageStock) {
            edit_product_stock_status.visibility = View.GONE
            manageStock_morePanel.expand()
        } else {
            edit_product_stock_status.visibility = View.VISIBLE
            manageStock_morePanel.collapse()
        }
    }

    override fun showUpdateProductAction(show: Boolean) {
        view?.post { publishMenuItem?.isVisible = show }
    }

    private fun displaySkuError(messageId: Int) {
        if (messageId != 0) {
            product_sku.setError(getString(messageId))
            publishMenuItem?.isEnabled = false
        } else {
            product_sku.clearError()
            publishMenuItem?.isEnabled = true
        }
    }

    override fun onProductInventoryItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_INVENTORY_BACKORDERS -> {
                selectedItem?.let {
                    edit_product_backorders.setText(getString(ProductBackorderStatus.toStringResource(it)))
                    viewModel.updateProductDraft(backorderStatus = ProductBackorderStatus.fromString(it))
                }
            }
            RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS -> {
                selectedItem?.let {
                    edit_product_stock_status.setText(getString(ProductStockStatus.toStringResource(it)))
                    viewModel.updateProductDraft(stockStatus = ProductStockStatus.fromString(it))
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ProductFieldType.PRODUCT_INVENTORY)
    }
}
