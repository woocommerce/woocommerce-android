package com.woocommerce.android.ui.products.variations.attributes.edit

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog displays a list of Attribute Options and
 * allows for selecting of single item
 *
 * This fragment should be instantiated using the [AttributeOptionSelectorDialog.newInstance] method.
 */
class AttributeOptionSelectorDialog : DialogFragment() {
    companion object {
        fun newInstance(
            attributeGroup: VariationAttributeSelectionGroup,
            onAttributeOptionSelected: (VariationAttributeSelectionGroup) -> Unit
        ) = AttributeOptionSelectorDialog().apply {
            retainInstance = true
            this.attributeGroup = attributeGroup
            this.listener = onAttributeOptionSelected
        }
    }

    private var listener: (VariationAttributeSelectionGroup) -> Unit = {}
    private var attributeGroup: VariationAttributeSelectionGroup? = null

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(attributeGroup?.attributeName)
            .setSingleChoiceItems(
                attributeGroup?.options?.toTypedArray(),
                attributeGroup?.selectedOptionIndex ?: 0,
                ::onOptionSelected
            ).create()

    private fun onOptionSelected(dialog: DialogInterface, index: Int) {
        attributeGroup?.apply {
            selectedOptionIndex = index
            listener(this)
            dialog.dismiss()
        }
    }
}
