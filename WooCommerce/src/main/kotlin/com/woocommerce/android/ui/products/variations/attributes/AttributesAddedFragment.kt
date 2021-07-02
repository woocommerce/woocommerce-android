package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAttributesAddedBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.widgets.CustomProgressDialog

class AttributesAddedFragment : BaseProductFragment(R.layout.fragment_attribute_list) {
    companion object {
        const val TAG: String = "AttributesAddedFragment"
    }

    private var progressDialog: CustomProgressDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentAttributesAddedBinding.bind(view).apply {
            generateVariationButton.setOnClickListener(::onGenerateVariationClicked)
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.attributeListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isCreatingVariationDialogShown?.takeIfNotEqualTo(old?.isCreatingVariationDialogShown) {
                showProgressDialog(it)
            }
        }
    }

    private fun onGenerateVariationClicked(view: View) {
        viewModel.onGenerateVariationClicked()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return false
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.variation_create_dialog_title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
