package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitInventory
import com.woocommerce.android.ui.products.ProductInventorySelectorDialog.ProductInventorySelectorDialogListener
import kotlinx.android.synthetic.main.fragment_product_inventory.*
import org.wordpress.android.util.ActivityUtils

class ProductInventoryFragment : BaseProductFragment(), ProductInventorySelectorDialogListener {
    private var productBackOrderSelectorDialog: ProductInventorySelectorDialog? = null
    private var productStockStatusSelectorDialog: ProductInventorySelectorDialog? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun getFragmentTitle() = getString(R.string.product_inventory)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(ExitInventory(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productInventoryViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.skuErrorMessage?.takeIfNotEqualTo(old?.skuErrorMessage) { displaySkuError(it) }
            new.stockQuantityErrorMessage?.takeIfNotEqualTo(old?.stockQuantityErrorMessage) {
                displayStockQuantityError(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitInventory -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
        updateProductView(viewModel.getProduct())
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.productDraft)
        with(product_sku) {
            setText(product.sku)
            setOnTextChangedListener {
                viewModel.updateProductDraft(sku = it.toString())
                viewModel.onSkuChanged(it.toString())
                changesMade()
            }
        }

        // Only the SKU field should be displayed for external and grouped products
        if (ProductType.isGroupedOrExternalProduct(product.type)) {
            manageStock_switch.visibility = View.GONE
            product_stock_quantity.visibility = View.GONE
            edit_product_backorders.visibility = View.GONE
            edit_product_stock_status.visibility = View.GONE
            soldIndividually_switch.visibility = View.GONE
            return
        }

        val manageStock = product.manageStock
        enableManageStockStatus(manageStock)
        with(manageStock_switch) {
            isChecked = manageStock
            setOnCheckedChangeListener { _, isChecked ->
                enableManageStockStatus(isChecked)
                viewModel.updateProductDraft(manageStock = isChecked)
                changesMade()
            }
        }

        with(product_stock_quantity) {
            setText(product.stockQuantity.toString())
            setOnTextChangedListener {
                viewModel.onStockQuantityChanged(it.toString())
                changesMade()
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
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateProductDraft(soldIndividually = isChecked)
                changesMade()
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

    private fun displaySkuError(messageId: Int) {
        if (messageId != 0) {
            product_sku.setError(getString(messageId))
            enableUpdateMenuItem(false)
        } else {
            product_sku.clearError()
            enableUpdateMenuItem(true)
        }
    }

    private fun displayStockQuantityError(messageId: Int) {
        if (messageId != 0) {
            product_stock_quantity.setError(getString(messageId))
            enableUpdateMenuItem(false)
        } else {
            product_stock_quantity.clearError()
            enableUpdateMenuItem(true)
        }
    }

    override fun onProductInventoryItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_INVENTORY_BACKORDERS -> {
                selectedItem?.let {
                    edit_product_backorders.setText(getString(ProductBackorderStatus.toStringResource(it)))
                    viewModel.updateProductDraft(backorderStatus = ProductBackorderStatus.fromString(it))
                    changesMade()
                }
            }
            RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS -> {
                selectedItem?.let {
                    edit_product_stock_status.setText(getString(ProductStockStatus.toStringResource(it)))
                    viewModel.updateProductDraft(stockStatus = ProductStockStatus.fromString(it))
                    changesMade()
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitInventory())
    }
}
