package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdateInventoryBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Common
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Mixed
import com.woocommerce.android.ui.products.variations.ValuesGroupType.None
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ActivityUtils.hideKeyboard
import javax.inject.Inject

@AndroidEntryPoint
class VariationsBulkUpdateInventoryFragment : BaseFragment(R.layout.fragment_variations_bulk_update_inventory) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: VariationsBulkUpdateInventoryViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdateInventoryBinding? = null
    private val binding get() = _binding!!

    private var progressDialog: CustomProgressDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        _binding = FragmentVariationsBulkUpdateInventoryBinding.bind(view)
        binding.stockQuantityEditText.text.observe(viewLifecycleOwner) {
            viewModel.onStockQuantityEntered(it.toString().toI)
        }
        ActivityUtils.showKeyboard(binding.stockQuantityEditText)

        observeViewStateChanges()
        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> {
                    ActivityUtils.hideKeyboardForced(binding.stockQuantityEditText)
                    uiMessageResolver.showSnack(event.message)
                }
                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun observeViewStateChanges() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.variationsToUpdateCount?.takeIfNotEqualTo(old?.variationsToUpdateCount) {
                binding.currentStockQuantity.text =
                    getString(R.string.variations_bulk_update_stock_quantity_info).format(new.variationsToUpdateCount)
            }
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) { isVisible ->
                updateProgressbarDialogVisibility(isVisible)
            }
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun updateCurrentStockQuantityLabel(
        groupType: ValuesGroupType,
        viewState: VariationsBulkUpdateInventoryViewModel.ViewState
    ) {
        binding.currentStockQuantity.text = when (groupType) {
            Mixed -> getString(R.string.variations_bulk_update_current_stock_quantity_mixed)
            None -> ""
            is Common -> getString(R.string.variations_bulk_update_current_stock_quantity, viewState.)
        }
    }

    private fun updateProgressbarDialogVisibility(visible: Boolean) {
        if (visible) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.variations_bulk_update_stock_quantity_dialog_title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_variations_bulk_update, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                viewModel.onDoneClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        hideProgressDialog()
        hideKeyboard(requireActivity())
    }
}
