package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAttributesAddedBinding
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitAttributesAdded
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetFragment
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetFragment.Companion.KEY_ADD_NEW_VARIATION
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetFragment.Companion.KEY_GENERATE_ALL_VARIATIONS
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ProgressDialogState
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationConfirmation
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.LimitExceeded
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.NetworkError
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.NoCandidates
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.ui.products.variations.domain.VariationCandidate
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttributesAddedFragment :
    BaseProductFragment(R.layout.fragment_attributes_added) {
    companion object {
        const val TAG: String = "AttributesAddedFragment"
    }

    private var progressDialog: CustomProgressDialog? = null

    private var generateVariationPickerDialog: GenerateVariationBottomSheetFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentAttributesAddedBinding.bind(view).apply {
            generateVariationButton.setOnClickListener { showAddVariationSelectDialog() }
        }
        setupObservers()
        setupResultHandlers()

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_variations),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitAttributesAdded)
                }
            }
        )
    }

    private fun setupObservers() {
        viewModel.attributeListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.progressDialogState?.takeIfNotEqualTo(old?.progressDialogState) { progressDialogState ->
                handleProgressDialogState(progressDialogState)
            }
            new.isFetchingVariations.takeIfNotEqualTo(old?.isFetchingVariations) { isFetchingVariations ->
                if (isFetchingVariations) {
                    showProgressDialog(R.string.variation_loading_dialog_title)
                } else {
                    hideProgressDialog()
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner, Observer(::onEventReceived))
    }

    private fun handleProgressDialogState(progressDialogState: ProgressDialogState) {
        when (progressDialogState) {
            ProgressDialogState.Hidden -> {
                hideProgressDialog()
            }
            is ProgressDialogState.Shown -> {
                val dialogLabel = when (progressDialogState.cardinality) {
                    ProgressDialogState.Shown.VariationsCardinality.SINGLE -> R.string.variation_create_dialog_title
                    ProgressDialogState.Shown.VariationsCardinality.MULTIPLE ->
                        R.string.variations_bulk_creation_progress_title
                }
                showProgressDialog(dialogLabel)
            }
        }
    }

    private fun onEventReceived(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitAttributesAdded ->
                AttributesAddedFragmentDirections
                    .actionAttributesAddedFragmentToProductDetailFragment(
                        mode = ProductDetailFragment.Mode.AddNewProduct
                    ).apply { findNavController().navigateSafely(this) }
            is ShowSnackbar -> uiMessageResolver.getSnack(event.message)
            is ShowGenerateVariationConfirmation -> showGenerateVariationConfirmation(event.variationCandidates)
            is ShowGenerateVariationsError -> handleGenerateVariationError(event)
            else -> event.isHandled = false
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitAttributesAdded)
        return false
    }

    private fun showProgressDialog(@StringRes title: Int) {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(title), getString(R.string.product_update_dialog_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        generateVariationPickerDialog = null
        progressDialog = null
    }

    private fun setupResultHandlers() {
        handleDialogNotice(KEY_ADD_NEW_VARIATION, R.id.attributesAddedFragment) {
            viewModel.onGenerateVariationClicked()
        }
        handleDialogNotice(KEY_GENERATE_ALL_VARIATIONS, R.id.attributesAddedFragment) {
            viewModel.onAddAllVariationsClicked()
        }
    }

    private fun showAddVariationSelectDialog() {
        AttributesAddedFragmentDirections.actionAttributesAddedFragmentToGenerateVariationBottomSheetFragment()
            .run { findNavController().navigateSafely(this) }
    }

    private fun handleGenerateVariationError(event: ShowGenerateVariationsError) {
        when (event) {
            is LimitExceeded -> showGenerateVariationsLimitExceeded(event.variationCandidatesSize)
            NetworkError -> showGenerateVariationsNetworkError()
            NoCandidates -> showNoVariationCandidatesError()
        }
    }

    private fun showGenerateVariationConfirmation(variationCandidatesSize: List<VariationCandidate>) {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.variations_bulk_creation_confirmation_title)
            .setMessage(getString(R.string.variations_bulk_creation_confirmation_message, variationCandidatesSize.size))
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                viewModel.onGenerateVariationsConfirmed(variationCandidatesSize)
                dialogInterface.dismiss()
            }.setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.show()
    }

    private fun showNoVariationCandidatesError() {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.variations_bulk_creation_no_candidates_title)
            .setMessage(R.string.variations_bulk_creation_no_candidates_message)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.show()
    }

    private fun showGenerateVariationsNetworkError() {
        MaterialAlertDialogBuilder(requireActivity()).setMessage(R.string.error_generic_network)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.show()
    }

    private fun showGenerateVariationsLimitExceeded(variationCandidatesSize: Int) {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.variations_bulk_creation_warning_title)
            .setMessage(
                getString(
                    R.string.variations_bulk_creation_warning_message,
                    GenerateVariationCandidates.VARIATION_CREATION_LIMIT,
                    variationCandidatesSize
                )
            ).setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.show()
    }
}
