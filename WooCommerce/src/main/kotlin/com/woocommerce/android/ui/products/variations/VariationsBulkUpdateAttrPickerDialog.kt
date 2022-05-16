package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdateAttrPickerBinding
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdateAttrPickerViewModel.RegularPriceState
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdateAttrPickerViewModel.ViewState
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

private const val DEFAULT_BG_DIM = 0.32F
private const val KEY_EXTRA_SHEET_STATE = "key_sheet_state"

class VariationsBulkUpdateAttrPickerDialog : WCBottomSheetDialogFragment() {
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

        bottomSheetBehavior.apply {
            isFitToContents = false
            addBottomSheetCallback(sheetCallback)
            halfExpandedRatio = 0.5f
            state = savedInstanceState?.getInt(KEY_EXTRA_SHEET_STATE, STATE_HALF_EXPANDED) ?: STATE_HALF_EXPANDED
        }
        dialog?.window?.setDimAmount(DEFAULT_BG_DIM)
        renderInternalSheetState(bottomSheetBehavior.state)
        lifecycleScope.launchWhenStarted {
            viewModel.viewState.collect { newState ->
                renderViewState(newState)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_EXTRA_SHEET_STATE, getSheetBehavior().state)
        super.onSaveInstanceState(outState)
    }

    private fun renderViewState(newState: ViewState) {
        binding.priceSubtitle.text = when (newState.regularPriceState) {
            RegularPriceState.None -> getString(R.string.variations_bulk_update_dialog_price_none)
            RegularPriceState.Mixed -> getString(R.string.variations_bulk_update_dialog_price_mixed)
            is RegularPriceState.Value -> newState.regularPriceState.price
        }
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
