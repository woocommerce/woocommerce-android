package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductItemSelectorDialog.ProductItemSelectorDialogListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.android.synthetic.main.fragment_product_inventory.*

class ProductInventoryFragment : BaseProductEditorFragment(R.layout.fragment_product_inventory),
    BackPressListener, ProductItemSelectorDialogListener {
    private val viewModel: ProductInventoryViewModel by viewModels { viewModelFactory.get() }

    override val isDoneButtonVisible: Boolean
        get() = viewModel.viewStateData.liveData.value?.isDoneButtonVisible ?: false
    override val isDoneButtonEnabled: Boolean
        get() = viewModel.viewStateData.liveData.value?.isDoneButtonEnabled ?: false
    override val lastEvent: Event?
        get() = viewModel.event.value

    private var productBackOrderSelectorDialog: ProductItemSelectorDialog? = null
    private var productStockStatusSelectorDialog: ProductItemSelectorDialog? = null

    override fun onPause() {
        super.onPause()
        productBackOrderSelectorDialog?.dismiss()
        productBackOrderSelectorDialog = null

        productStockStatusSelectorDialog?.dismiss()
        productStockStatusSelectorDialog = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        setupViews()
    }

    override fun getFragmentTitle() = getString(R.string.product_inventory)

    private fun setupObservers(viewModel: ProductInventoryViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.skuErrorMessage?.takeIfNotEqualTo(old?.skuErrorMessage) {
                displaySkuError(it)
            }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isVisible ->
                doneButton?.isVisible = isVisible
            }
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                doneButton?.isEnabled = isEnabled
            }
            new.isStockManagementVisible?.takeIfNotEqualTo(old?.isStockManagementVisible) { isVisible ->
                stockManagementPanel.isVisible = isVisible
                soldIndividually_switch.isVisible = isVisible && new.isIndividualSaleSwitchVisible == true
            }
            new.isStockStatusVisible?.takeIfNotEqualTo(old?.isStockStatusVisible) { isVisible ->
                edit_product_stock_status.isVisible = isVisible
            }
            new.inventoryData.backorderStatus?.takeIfNotEqualTo(old?.inventoryData?.backorderStatus) {
                edit_product_backorders.setText(ProductBackorderStatus.backordersToDisplayString(requireContext(), it))
            }
            new.inventoryData.stockStatus?.takeIfNotEqualTo(old?.inventoryData?.stockStatus) {
                edit_product_stock_status.setText(ProductStockStatus.stockStatusToDisplayString(requireContext(), it))
            }
            new.inventoryData.isStockManaged?.takeIfNotEqualTo(old?.inventoryData?.isStockManaged) { isStockManaged ->
                new.isStockManagementVisible?.let { isVisible ->
                    if (isVisible) {
                        enableManageStockStatus(
                            isStockManaged,
                            new.isStockStatusVisible ?: edit_product_stock_status.isVisible
                        )
                    } else {
                        manageStock_switch.isVisible = false
                        edit_product_stock_status.isVisible = false
                    }
                }
            }
            new.inventoryData.sku?.takeIfNotEqualTo(old?.inventoryData?.sku) {
                if (product_sku.getText() != it) {
                    product_sku.setText(it)
                }
            }
            new.inventoryData.stockQuantity?.takeIfNotEqualTo(old?.inventoryData?.stockQuantity) {
                val quantity = it.toString()
                if (product_stock_quantity.getText() != quantity) {
                    product_stock_quantity.setText(quantity)
                }
            }
            new.inventoryData.isSoldIndividually?.takeIfNotEqualTo(old?.inventoryData?.isSoldIndividually) {
                soldIndividually_switch.isChecked = it
            }

            viewModel.event.observe(viewLifecycleOwner, Observer { event ->
                when (event) {
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    is ExitWithResult<*> -> navigateBackWithResult(KEY_INVENTORY_DIALOG_RESULT, event.data)
                    is Exit -> findNavController().navigateUp()
                    is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                        requireActivity(),
                        event.positiveBtnAction,
                        event.negativeBtnAction,
                        messageId = event.messageId
                    )
                    else -> event.isHandled = false
                }
            })
        }
    }

    private fun displaySkuError(messageId: Int) {
        if (messageId != 0) {
            product_sku.error = getString(messageId)
        } else {
            product_sku.helperText = getString(R.string.product_sku_summary)
        }
    }

    private fun setupViews() {
        if (!isAdded) return

        with(product_sku) {
            setOnTextChangedListener {
                viewModel.onSkuChanged(it.toString())
            }
        }

        with(manageStock_switch) {
            setOnCheckedChangeListener { _, isChecked ->
                enableManageStockStatus(isChecked, edit_product_stock_status.isVisible)
                viewModel.onDataChanged(isStockManaged = isChecked)
            }
        }

        with(product_stock_quantity) {
            setOnTextChangedListener {
                it.toString().toIntOrNull()?.let { quantity ->
                    viewModel.onDataChanged(stockQuantity = quantity)
                }
            }
        }

        with(edit_product_backorders) {
            setClickListener {
                productBackOrderSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductInventoryFragment,
                    RequestCodes.PRODUCT_INVENTORY_BACKORDERS,
                    getString(R.string.product_backorders),
                    ProductBackorderStatus.toMap(requireContext()),
                    edit_product_backorders.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }

        with(edit_product_stock_status) {
            setClickListener {
                productStockStatusSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductInventoryFragment,
                    RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS,
                    getString(R.string.product_stock_status),
                    ProductStockStatus.toMap(requireContext()),
                    edit_product_stock_status.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }

        with(soldIndividually_switch) {
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.onDataChanged(isSoldIndividually = isChecked)
            }
        }
    }

    private fun enableManageStockStatus(isStockManaged: Boolean, isStockStatusVisible: Boolean) {
        manageStock_switch.isChecked = isStockManaged
        if (isStockManaged) {
            edit_product_stock_status.collapse()
            manageStock_morePanel.expand()
        } else {
            manageStock_morePanel.collapse()
            if (isStockStatusVisible) {
                edit_product_stock_status.expand()
            }
        }
    }

    override fun onProductItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_INVENTORY_BACKORDERS -> {
                selectedItem?.let {
                    edit_product_backorders.setText(getString(ProductBackorderStatus.toStringResource(it)))
                    viewModel.onDataChanged(backorderStatus = ProductBackorderStatus.fromString(it))
                }
            }
            RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS -> {
                selectedItem?.let {
                    edit_product_stock_status.setText(getString(ProductStockStatus.toStringResource(it)))
                    viewModel.onDataChanged(stockStatus = ProductStockStatus.fromString(it))
                }
            }
        }
    }

    override fun onDoneButtonClicked() {
        viewModel.onDoneButtonClicked()
    }

    override fun onExit() {
        viewModel.onExit()
    }
}
