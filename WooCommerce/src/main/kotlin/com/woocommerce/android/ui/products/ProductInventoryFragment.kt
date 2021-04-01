package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.databinding.FragmentProductInventoryBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.ProductItemSelectorDialog.ProductItemSelectorDialogListener
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar

class ProductInventoryFragment : BaseProductEditorFragment(R.layout.fragment_product_inventory),
    ProductItemSelectorDialogListener {
    private val viewModel: ProductInventoryViewModel by viewModels { viewModelFactory.get() }

    override val lastEvent: Event?
        get() = viewModel.event.value

    private var productBackOrderSelectorDialog: ProductItemSelectorDialog? = null
    private var productStockStatusSelectorDialog: ProductItemSelectorDialog? = null

    private var _binding: FragmentProductInventoryBinding? = null
    private val binding get() = _binding!!

    override fun onPause() {
        super.onPause()
        productBackOrderSelectorDialog?.dismiss()
        productBackOrderSelectorDialog = null

        productStockStatusSelectorDialog?.dismiss()
        productStockStatusSelectorDialog = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductInventoryBinding.bind(view)

        setupObservers(viewModel)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getFragmentTitle() = getString(R.string.product_inventory)

    private fun setupObservers(viewModel: ProductInventoryViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.skuErrorMessage?.takeIfNotEqualTo(old?.skuErrorMessage) {
                displaySkuError(it)
            }
            new.isStockManagementVisible?.takeIfNotEqualTo(old?.isStockManagementVisible) { isVisible ->
                binding.stockManagementPanel.isVisible = isVisible
                binding.soldIndividuallySwitch.isVisible = isVisible && new.isIndividualSaleSwitchVisible == true
            }
            new.isStockStatusVisible?.takeIfNotEqualTo(old?.isStockStatusVisible) { isVisible ->
                binding.editProductStockStatus.isVisible = isVisible
            }
            new.inventoryData.backorderStatus?.takeIfNotEqualTo(old?.inventoryData?.backorderStatus) {
                binding.editProductBackorders.setText(
                    ProductBackorderStatus.backordersToDisplayString(
                        requireContext(),
                        it
                    )
                )
            }
            new.inventoryData.stockStatus?.takeIfNotEqualTo(old?.inventoryData?.stockStatus) {
                binding.editProductStockStatus.setText(
                    ProductStockStatus.stockStatusToDisplayString(
                        requireContext(),
                        it
                    )
                )
            }
            new.inventoryData.isStockManaged?.takeIfNotEqualTo(old?.inventoryData?.isStockManaged) { isStockManaged ->
                new.isStockManagementVisible?.let { isVisible ->
                    if (isVisible) {
                        enableManageStockStatus(
                            isStockManaged,
                            new.isStockStatusVisible ?: binding.editProductStockStatus.isVisible
                        )
                    } else {
                        binding.manageStockSwitch.isVisible = false
                        binding.editProductStockStatus.isVisible = false
                    }
                }
            }
            new.inventoryData.sku?.takeIfNotEqualTo(old?.inventoryData?.sku) {
                if (binding.productSku.getText() != it) {
                    binding.productSku.setText(it)
                }
            }
            new.inventoryData.stockQuantity?.takeIfNotEqualTo(old?.inventoryData?.stockQuantity) {
                val quantity = StringUtils.formatCountDecimal(it, forInput = true)

                // If the quantity is not whole decimal, make this field read-only, because the API doesn't support
                // updating decimal amount yet.
                if (! it.rem(1).equals(0.0)) {
                    binding.productStockQuantity.isEnabled = false
                }

                if (binding.productStockQuantity.getText() != quantity) {
                    binding.productStockQuantity.setText(quantity)
                }
            }
            new.inventoryData.isSoldIndividually?.takeIfNotEqualTo(old?.inventoryData?.isSoldIndividually) {
                binding.soldIndividuallySwitch.isChecked = it
            }

            viewModel.event.observe(viewLifecycleOwner, Observer { event ->
                when (event) {
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    is ExitWithResult<*> -> navigateBackWithResult(KEY_INVENTORY_DIALOG_RESULT, event.data)
                    is Exit -> findNavController().navigateUp()
                    is ShowDialog -> event.showDialog()
                    else -> event.isHandled = false
                }
            })
        }
    }

    private fun displaySkuError(messageId: Int) {
        if (messageId != 0) {
            binding.productSku.error = getString(messageId)
        } else {
            binding.productSku.helperText = getString(R.string.product_sku_summary)
        }
    }

    private fun setupViews() {
        if (!isAdded) return

        with(binding.productSku) {
            setOnTextChangedListener {
                viewModel.onSkuChanged(it.toString())
            }
        }

        with(binding.manageStockSwitch) {
            setOnCheckedChangeListener { _, isChecked ->
                enableManageStockStatus(isChecked, binding.editProductStockStatus.isVisible)
                viewModel.onDataChanged(isStockManaged = isChecked)
            }
        }

        with(binding.productStockQuantity) {
            setOnTextChangedListener {
                it.toString().toDoubleOrNull()?.let { quantity ->
                    viewModel.onDataChanged(stockQuantity = quantity)
                }
            }
        }

        with(binding.editProductBackorders) {
            setClickListener {
                productBackOrderSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductInventoryFragment,
                    RequestCodes.PRODUCT_INVENTORY_BACKORDERS,
                    getString(R.string.product_backorders),
                    ProductBackorderStatus.toMap(requireContext()),
                    binding.editProductBackorders.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }

        with(binding.editProductStockStatus) {
            setClickListener {
                productStockStatusSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductInventoryFragment,
                    RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS,
                    getString(R.string.product_stock_status),
                    ProductStockStatus.toMap(requireContext()),
                    binding.editProductStockStatus.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }

        with(binding.soldIndividuallySwitch) {
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.onDataChanged(isSoldIndividually = isChecked)
            }
        }
    }

    private fun enableManageStockStatus(isStockManaged: Boolean, isStockStatusVisible: Boolean) {
        binding.manageStockSwitch.isChecked = isStockManaged
        if (isStockManaged) {
            binding.editProductStockStatus.collapse()
            binding.manageStockMorePanel.expand()
        } else {
            binding.manageStockMorePanel.collapse()
            if (isStockStatusVisible) {
                binding.editProductStockStatus.expand()
            }
        }
    }

    override fun onProductItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_INVENTORY_BACKORDERS -> {
                selectedItem?.let {
                    binding.editProductBackorders.setText(getString(ProductBackorderStatus.toStringResource(it)))
                    viewModel.onDataChanged(backorderStatus = ProductBackorderStatus.fromString(it))
                }
            }
            RequestCodes.PRODUCT_INVENTORY_STOCK_STATUS -> {
                selectedItem?.let {
                    binding.editProductStockStatus.setText(getString(ProductStockStatus.toStringResource(it)))
                    viewModel.onDataChanged(stockStatus = ProductStockStatus.fromString(it))
                }
            }
        }
    }

    override fun onExit() {
        viewModel.onExit()
    }
}
