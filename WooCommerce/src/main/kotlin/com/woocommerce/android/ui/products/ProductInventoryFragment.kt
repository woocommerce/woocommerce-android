package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductItemSelectorDialog.ProductItemSelectorDialogListener
import com.woocommerce.android.ui.products.ProductPricingViewModel.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_inventory.*
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductInventoryFragment : BaseFragment(), BackPressListener, ProductItemSelectorDialogListener {
    companion object {
        const val KEY_INVENTORY_DIALOG_RESULT = "key_inventory_dialog_result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductInventoryViewModel by viewModels { viewModelFactory }

    private var productBackOrderSelectorDialog: ProductItemSelectorDialog? = null
    private var productStockStatusSelectorDialog: ProductItemSelectorDialog? = null

    private var doneButton: MenuItem? = null

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
        setupViews()
    }

    override fun getFragmentTitle() = getString(R.string.product_inventory)


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
        doneButton = menu.findItem(R.id.menu_done)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val viewState = viewModel.viewStateData.liveData.value
        doneButton?.isVisible = viewState?.isDoneButtonVisible ?: false
        doneButton?.isEnabled = viewState?.isDoneButtonEnabled ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
            new.isStockSectionVisible?.takeIfNotEqualTo(old?.isStockSectionVisible) { isVisible ->
                enableManageStockStatus(isVisible)
            }
            new.inventoryData.backorderStatus?.takeIfNotEqualTo(old?.inventoryData?.backorderStatus) {
                edit_product_backorders.setText(ProductBackorderStatus.backordersToDisplayString(requireContext(), it))
            }
            new.inventoryData.stockStatus?.takeIfNotEqualTo(old?.inventoryData?.stockStatus) {
                edit_product_backorders.setText(ProductStockStatus.stockStatusToDisplayString(requireContext(), it))
            }
            new.inventoryData.sku?.takeIfNotEqualTo(old?.inventoryData?.sku) {
                product_sku.setText(it)
            }
            new.inventoryData.stockQuantity?.takeIfNotEqualTo(old?.inventoryData?.stockQuantity) {
                product_stock_quantity.setText(it.toString())
            }
            new.inventoryData.isSoldIndividually?.takeIfNotEqualTo(old?.inventoryData?.isSoldIndividually) {
                soldIndividually_switch.isChecked = it
            }

            viewModel.event.observe(viewLifecycleOwner, Observer { event ->
                when (event) {
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    is ExitWithResult -> navigateBackWithResult(KEY_INVENTORY_DIALOG_RESULT, event.data)
                    is Exit -> findNavController().navigateUp()
                    is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                        requireActivity(),
                        event.positiveBtnAction,
                        event.negativeBtnAction,
                        event.messageId
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
            product_sku.error = null
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
                enableManageStockStatus(isChecked)
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

    private fun enableManageStockStatus(manageStock: Boolean) {
        if (manageStock) {
            edit_product_stock_status.visibility = View.GONE
            manageStock_morePanel.expand()
        } else {
            edit_product_stock_status.visibility = View.VISIBLE
            manageStock_morePanel.collapse()
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

    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
