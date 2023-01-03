package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdateInventoryBinding
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Common
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Mixed
import com.woocommerce.android.ui.products.variations.ValuesGroupType.None
import com.woocommerce.android.util.StringUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VariationsBulkUpdateInventoryFragment :
    VariationsBulkUpdateBaseFragment(R.layout.fragment_variations_bulk_update_inventory) {

    override val viewModel: VariationsBulkUpdateInventoryViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdateInventoryBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationsBulkUpdateInventoryBinding.bind(view)
        binding.stockQuantityEditText.run {
            editText?.showKeyboardWithDelay()
            setOnTextChangedListener { rawQuantity ->
                viewModel.onStockQuantityChanged(rawQuantity.toString())
            }
        }

        observeViewStateChanges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewStateChanges() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.stockQuantity?.takeIfNotEqualTo(old?.stockQuantity) {
                val quantity = StringUtils.formatCountDecimal(it, forInput = true)
                binding.stockQuantityEditText.setTextIfDifferent(quantity)
            }
            new.variationsToUpdateCount?.takeIfNotEqualTo(old?.variationsToUpdateCount) {
                binding.currentStockQuantity.text =
                    getString(R.string.variations_bulk_update_stock_quantity_info).format(new.variationsToUpdateCount)
            }
            new.stockQuantityGroupType?.takeIfNotEqualTo(old?.stockQuantityGroupType) {
                updateCurrentStockQuantityLabel(new.stockQuantityGroupType, new)
            }
            new.isDoneEnabled.takeIfNotEqualTo(old?.isDoneEnabled) { isEnabled ->
                enableDoneButton(isEnabled)
            }
        }
        viewModel.isProgressDialogShown.observe(viewLifecycleOwner) { isVisible ->
            val title = R.string.variations_bulk_update_stock_quantity_dialog_title
            updateProgressbarDialogVisibility(isVisible, title)
        }
    }

    private fun updateCurrentStockQuantityLabel(
        groupType: ValuesGroupType,
        viewState: VariationsBulkUpdateInventoryViewModel.ViewState
    ) {
        binding.currentStockQuantity.text = when (groupType) {
            Mixed -> getString(R.string.variations_bulk_update_current_stock_quantity_mixed)
            None -> ""
            is Common -> getString(R.string.variations_bulk_update_current_stock_quantity, viewState.stockQuantity)
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_stock_quantity)
}
