package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAttributesAddedBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitAttributesAdded
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttributesAddedFragment : BaseProductFragment(R.layout.fragment_attributes_added) {
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

    override fun getFragmentTitle() = getString(R.string.product_variations)

    private fun setupObservers() {
        viewModel.attributeListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isCreatingVariationDialogShown?.takeIfNotEqualTo(old?.isCreatingVariationDialogShown) {
                showProgressDialog(it)
            }
        }
        viewModel.event.observe(viewLifecycleOwner, Observer(::onEventReceived))
    }

    private fun onEventReceived(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitAttributesAdded ->
                AttributesAddedFragmentDirections
                    .actionAttributesAddedFragmentToProductDetailFragment()
                    .apply { findNavController().navigateSafely(this) }
            is ShowSnackbar -> uiMessageResolver.getSnack(event.message)
            else -> event.isHandled = false
        }
    }

    private fun onGenerateVariationClicked(view: View) {
        viewModel.onGenerateVariationClicked()
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitAttributesAdded)
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
