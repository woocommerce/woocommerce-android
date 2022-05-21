package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdatePriceBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Common
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Mixed
import com.woocommerce.android.ui.products.variations.ValuesGroupType.None
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType.Regular
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType.Sale
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

@AndroidEntryPoint
class VariationsBulkUpdatePriceFragment : BaseFragment(R.layout.fragment_variations_bulk_update_price) {
    private val viewModel: VariationsBulkUpdatePriceViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdatePriceBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        _binding = FragmentVariationsBulkUpdatePriceBinding.bind(view)
        binding.price.setOnTextChangedListener {
            val price = it.toString().toBigDecimalOrNull()
            viewModel.onPriceEntered(price)
        }
        binding.price.showKeyboard()
        observeViewStateChanges()
    }

    private fun observeViewStateChanges() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.priceType?.takeIfNotEqualTo(old?.priceType) {
                requireActivity().title = when (new.priceType) {
                    Regular -> getString(R.string.variations_bulk_update_regular_price)
                    Sale -> getString(R.string.variations_bulk_update_sale_price)
                }
            }
            new.currency?.takeIfNotEqualTo(old?.currency) {
                setupPriceInputField(new.currency, new.isCurrencyPrefix)
            }
            new.variationsToUpdateCount?.takeIfNotEqualTo(old?.variationsToUpdateCount) {
                binding.priceUpdateInfo.text =
                    getString(R.string.variations_bulk_update_price_info).format(new.variationsToUpdateCount)
            }
            new.pricesGroupType?.takeIfNotEqualTo(old?.pricesGroupType) {
                updateCurrentPricesLabel(new.pricesGroupType, new)
            }
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
                            .format(formatPrice(price, currency, isCurrencyPrefix))
                    } else {
                        ""
                    }
                }
            }
        }
    }

    private fun setupPriceInputField(currency: String, isCurrencyPrefix: Boolean) {
        with(binding.price) {
            if (isCurrencyPrefix) {
                prefixText = currency
            } else {
                suffixText = currency
            }
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
}
