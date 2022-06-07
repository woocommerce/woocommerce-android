package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdateAttrPickerBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdateAttrPickerViewModel.OpenVariationsBulkUpdatePrice
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdateAttrPickerViewModel.ViewState
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceUpdateData
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

private const val DEFAULT_BG_DIM = 0.32F
private const val HALF_EXPANDED_RATIO = 0.5F
private const val KEY_EXTRA_SHEET_STATE = "key_sheet_state"

@AndroidEntryPoint
class VariationsBulkUpdateAttrPickerDialog : WCBottomSheetDialogFragment() {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var _binding: FragmentVariationsBulkUpdateAttrPickerBinding? = null
    private val binding get() = _binding!!

    private val sheetCallback by lazy {
        object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) = renderInternalSheetState(newState)
            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        }
    }
    private val viewModel: VariationsBulkUpdateAttrPickerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVariationsBulkUpdateAttrPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetBehavior = getSheetBehavior()
        binding.collapsedStateHeader.setOnClickListener { bottomSheetBehavior.state = STATE_EXPANDED }
        binding.fullscreenStateToolbar.setNavigationOnClickListener { dismiss() }
        binding.regularPrice.setOnClickListener { viewModel.onRegularPriceUpdateClicked() }
        binding.salePrice.setOnClickListener { viewModel.onSalePriceUpdateClicked() }

        bottomSheetBehavior.apply {
            isFitToContents = false
            addBottomSheetCallback(sheetCallback)
            halfExpandedRatio = HALF_EXPANDED_RATIO
            state = savedInstanceState?.getInt(KEY_EXTRA_SHEET_STATE, STATE_HALF_EXPANDED) ?: STATE_HALF_EXPANDED
        }
        dialog?.window?.setDimAmount(DEFAULT_BG_DIM)
        renderInternalSheetState(bottomSheetBehavior.state)
        listenForViewStateChange()
        listenForEvents()
    }

    private fun listenForViewStateChange() {
        viewModel.viewState.observe(viewLifecycleOwner, ::renderViewState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_EXTRA_SHEET_STATE, getSheetBehavior().state)
        super.onSaveInstanceState(outState)
    }

    private fun renderViewState(newState: ViewState) {
        if (newState.currency != null) {
            binding.priceSubtitle.text = formatPriceSubtitle(newState.currency, newState.regularPriceGroupType)
            binding.salePriceSubtitle.text = formatPriceSubtitle(newState.currency, newState.salePriceGroupType)
        }
    }

    private fun formatPriceSubtitle(currency: String, priceGroupType: ValuesGroupType) = when (priceGroupType) {
        ValuesGroupType.None -> getString(R.string.variations_bulk_update_dialog_price_none)
        ValuesGroupType.Mixed -> getString(R.string.variations_bulk_update_dialog_price_mixed)
        is ValuesGroupType.Common -> {
            val price = priceGroupType.data as? BigDecimal?
            if (price != null) currencyFormatter.formatCurrency(amount = price, currencyCode = currency) else ""
        }
    }

    private fun listenForEvents() {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is OpenVariationsBulkUpdatePrice -> openRegularPriceUpdate(it.data)
            }
        }
    }

    private fun openRegularPriceUpdate(data: PriceUpdateData) {
        VariationsBulkUpdateAttrPickerDialogDirections
            .actionVariationsBulkUpdateAttrPickerFragmentToVariationsBulkUpdatePriceFragment(data)
            .run { findNavController().navigateSafely(this) }
        dismiss()
    }

    private fun renderInternalSheetState(bottomSheetState: Int) {
        when (bottomSheetState) {
            STATE_EXPANDED -> {
                getSheetBehavior().isFitToContents = false
                dialog?.window?.setDimAmount(0F)
                binding.collapsedStateHeader.visibility = GONE
                binding.fullscreenStateToolbar.visibility = VISIBLE
            }
            STATE_DRAGGING -> {
                dialog?.window?.setDimAmount(DEFAULT_BG_DIM)
                binding.collapsedStateHeader.visibility = VISIBLE
                binding.fullscreenStateToolbar.visibility = GONE
            }
            STATE_COLLAPSED, STATE_HALF_EXPANDED -> {
                dialog?.window?.setDimAmount(DEFAULT_BG_DIM)
                binding.collapsedStateHeader.visibility = VISIBLE
                binding.fullscreenStateToolbar.visibility = GONE
            }
        }
    }

    private fun getSheetBehavior() = (dialog as BottomSheetDialog).behavior
}
