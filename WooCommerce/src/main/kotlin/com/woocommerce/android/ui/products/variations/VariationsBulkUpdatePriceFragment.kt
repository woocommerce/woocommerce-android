package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdatePriceBinding
import com.woocommerce.android.extensions.drop
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Common
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Mixed
import com.woocommerce.android.ui.products.variations.ValuesGroupType.None
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType.Regular
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType.Sale
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class VariationsBulkUpdatePriceFragment : BaseFragment(R.layout.fragment_variations_bulk_update_price) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: VariationsBulkUpdatePriceViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdatePriceBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        _binding = FragmentVariationsBulkUpdatePriceBinding.bind(view)
        binding.price.value.filterNotNull().observe(viewLifecycleOwner) { viewModel.onPriceEntered(it) }
        binding.price.editText.showKeyboardWithDelay()
        observeViewStateChanges()
        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> requireActivity().onBackPressed()
            }
        }
    }

    private fun observeViewStateChanges() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.priceType.takeIfNotEqualTo(old?.priceType) {
                requireActivity().title = when (new.priceType) {
                    Regular -> getString(R.string.variations_bulk_update_regular_price)
                    Sale -> getString(R.string.variations_bulk_update_sale_price)
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
