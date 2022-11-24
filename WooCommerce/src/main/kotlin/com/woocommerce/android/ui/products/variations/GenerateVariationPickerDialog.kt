package com.woocommerce.android.ui.products.variations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogGenerateVariationsBinding

class GenerateVariationPickerDialog(context: Context) : BottomSheetDialog(context) {
    private var binding: DialogGenerateVariationsBinding =
        DialogGenerateVariationsBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        binding.allVariation.setOnClickListener {
            listener?.onGenerateAllVariations()
            dismiss()
        }
        binding.newVariation.setOnClickListener {
            listener?.onGenerateNewVariation()
            dismiss()
        }
    }

    var totalVariations: Int = 0
        set(value) {
            field = value
            binding.allVariation.visibility = if (value > 0) View.VISIBLE else View.GONE
            if (value > 0) {
                binding.allVariationTitle.text = context.getString(R.string.variation_add_all, value)
            }
        }

    var listener: GenerateVariationPickerDialogListener? = null

    interface GenerateVariationPickerDialogListener {
        fun onGenerateAllVariations()
        fun onGenerateNewVariation()
    }
}
