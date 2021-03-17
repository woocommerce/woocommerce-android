package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.variations.attributes.EditVariationAttributesViewModel.VariationAttributeSelectionGroup
import com.woocommerce.android.ui.products.variations.attributes.EditVariationAttributesViewModel.ViewState
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class EditVariationAttributesFragment :
    BaseFragment(R.layout.fragment_edit_variation_attributes) {
    companion object {
        const val TAG: String = "EditVariationAttributesFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditVariationAttributesViewModel by viewModels { viewModelFactory }

    private fun setupObservers() = viewModel.apply {
        viewStateLiveData.observe(viewLifecycleOwner, ::handleViewStateChanges)
        editableVariationAttributeList.observe(viewLifecycleOwner, Observer(::handleVariationAttributeListChanges))
    }

    private fun handleViewStateChanges(old: ViewState?, new: ViewState?) {
        new?.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) {
            // change skeleton visibility
        }

        new?.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
            // update refresh layout
        }

        new?.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) {
            // update empty view visibility
        }
    }

    private fun handleVariationAttributeListChanges(selectableOptions: List<VariationAttributeSelectionGroup>) {
        showAttributeSelectableOptions(selectableOptions)
        requireActivity().invalidateOptionsMenu()
    }

    private fun showAttributeSelectableOptions(selectableOptions: List<VariationAttributeSelectionGroup>) {}
}
