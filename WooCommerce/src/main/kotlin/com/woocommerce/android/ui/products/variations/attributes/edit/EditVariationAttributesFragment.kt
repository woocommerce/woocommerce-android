package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditVariationAttributesBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesViewModel.VariationAttributeSelectionGroup
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesViewModel.ViewState
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import javax.inject.Inject

class EditVariationAttributesFragment :
    BaseFragment(R.layout.fragment_edit_variation_attributes) {
    companion object {
        const val TAG: String = "EditVariationAttributesFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditVariationAttributesViewModel by viewModels { viewModelFactory }

    private val navArgs: EditVariationAttributesFragmentArgs by navArgs()

    private lateinit var binding: FragmentEditVariationAttributesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditVariationAttributesBinding.bind(view)
        setupObservers()
        setupViews()
        viewModel.start(navArgs.remoteProductId, navArgs.remoteVariationId)
    }

    private fun setupObservers() = viewModel.apply {
        viewStateLiveData.observe(viewLifecycleOwner, ::handleViewStateChanges)
        editableVariationAttributeList.observe(viewLifecycleOwner, Observer(::handleVariationAttributeListChanges))
    }

    private fun setupViews() = binding.apply {
        variationList.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            itemAnimator = null
            addItemDecoration(
                AlignedDividerDecoration(
                    requireContext(), DividerItemDecoration.VERTICAL, R.id.variationOptionName, clipToMargin = false
                )
            )
        }
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

    private fun showAttributeSelectableOptions(
        selectableOptions: List<VariationAttributeSelectionGroup>
    ) = binding.apply {
        variationList
            .run { adapter as? VariationAttributesAdapter }
            ?.refreshSourceData(selectableOptions)
            ?: variationList.apply {
                adapter = VariationAttributesAdapter(selectableOptions)
            }
    }
}
