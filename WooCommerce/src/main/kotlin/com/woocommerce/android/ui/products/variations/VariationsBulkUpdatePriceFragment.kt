package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdatePriceBinding
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Common
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Mixed
import com.woocommerce.android.ui.products.variations.ValuesGroupType.None
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType.Regular
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType.Sale
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class VariationsBulkUpdatePriceFragment :
    VariationsBulkUpdateBaseFragment(R.layout.fragment_variations_bulk_update_price) {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override val viewModel: VariationsBulkUpdatePriceViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdatePriceBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationsBulkUpdatePriceBinding.bind(view)
        binding.price.value.filterNotNull().observe(viewLifecycleOwner) { viewModel.onPriceEntered(it.toString()) }
        binding.price.editText.showKeyboardWithDelay()
        observeViewStateChanges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewStateChanges() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.priceType.takeIfNotEqualTo(old?.priceType) {
                requireActivity().title = when (new.priceType) {
                    Regular -> getString(R.string.variations_bulk_update_regular_price)
                    Sale -> getString(R.string.variations_bulk_update_sale_price)
                }
                binding.price.hint = when (new.priceType) {
                    Regular -> getString(R.string.product_regular_price)
                    Sale -> getString(R.string.product_sale_price)
                }
            }
            new.variationsToUpdateCount?.takeIfNotEqualTo(old?.variationsToUpdateCount) {
                binding.priceUpdateInfo.text =
                    getString(R.string.variations_bulk_update_price_info).format(new.variationsToUpdateCount)
            }
            new.pricesGroupType?.takeIfNotEqualTo(old?.pricesGroupType) {
                updateCurrentPricesLabel(new.pricesGroupType, new)
            }
        }
        viewModel.isProgressDialogShown.observe(viewLifecycleOwner) { isShown ->
            val priceType = viewModel.viewStateData.liveData.value?.priceType ?: return@observe
            val title = when (priceType) {
                Sale -> R.string.variations_bulk_update_sale_prices_dialog_title
                Regular -> R.string.variations_bulk_update_regular_prices_dialog_title
            }
            updateProgressbarDialogVisibility(isShown, title)
        }
    }

    private fun updateCurrentPricesLabel(
        pricesGroupType: ValuesGroupType,
        viewState: VariationsBulkUpdatePriceViewModel.ViewState
    ) {
        binding.currentPriceInfo.text = when (pricesGroupType) {
            Mixed -> getString(R.string.variations_bulk_update_current_prices_mixed)
            None -> ""
            is Common -> {
                with(viewState) {
                    val price = pricesGroupType.data as? BigDecimal?
                    if (currency != null && price != null) {
                        getString(R.string.variations_bulk_update_current_price)
                            .format(currencyFormatter.formatCurrency(amount = price, currencyCode = currency))
                    } else {
                        ""
                    }
                }
            }
        }
    }
}
