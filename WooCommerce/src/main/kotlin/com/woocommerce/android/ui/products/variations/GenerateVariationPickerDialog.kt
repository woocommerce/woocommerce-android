package com.woocommerce.android.ui.products.variations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogGenerateVariationsBinding
import com.woocommerce.android.ui.products.variations.domain.VariationCandidate

class GenerateVariationPickerDialog(context: Context) : BottomSheetDialog(context) {
    private var binding: DialogGenerateVariationsBinding =
        DialogGenerateVariationsBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        binding.allVariation.setOnClickListener {
            listener?.onGenerateAllVariations(variationCandidates)
            dismiss()
        }
        binding.newVariation.setOnClickListener {
            listener?.onGenerateNewVariation()
            dismiss()
        }
    }

    var variationCandidates: List<VariationCandidate> = emptyList()
        set(value) {
            field = value
            binding.allVariation.visibility = if (variationCandidates.isNotEmpty()) View.VISIBLE else View.GONE
            if (variationCandidates.isNotEmpty()) {
                binding.allVariationTitle.text = context.getString(R.string.variation_add_all, variationCandidates.size)
            }
        }

    var listener: GenerateVariationPickerDialogListener? = null

    interface GenerateVariationPickerDialogListener {
        fun onGenerateAllVariations(variationCandidates: List<VariationCandidate>)
        fun onGenerateNewVariation()
    }
}
