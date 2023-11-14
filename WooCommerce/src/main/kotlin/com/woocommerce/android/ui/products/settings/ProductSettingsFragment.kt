package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductSettingsBinding
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPurchaseNoteEditor
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityFragment.Companion.ARG_CATALOG_VISIBILITY
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductSettingsFragment : BaseProductFragment(R.layout.fragment_product_settings) {
    private var _binding: FragmentProductSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductSettingsBinding.bind(view)

        setupObservers()

        binding.productStatus.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_STATUS_TAPPED)
            viewModel.onSettingsStatusButtonClicked()
        }
        binding.productCatalogVisibility.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_CATALOG_VISIBILITY_TAPPED)
            viewModel.onSettingsCatalogVisibilityButtonClicked()
        }
        binding.productVisibility.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_VISIBILITY_TAPPED)
            viewModel.onSettingsVisibilityButtonClicked()
        }
        binding.productSlug.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_SLUG_TAPPED)
            viewModel.onSettingsSlugButtonClicked()
        }
        binding.productMenuOrder.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_MENU_ORDER_TAPPED)
            viewModel.onSettingsMenuOrderButtonClicked()
        }

        binding.productReviewsAllowed.visibility = View.VISIBLE
        binding.productReviewsAllowedDivider.visibility = View.VISIBLE
        binding.productReviewsAllowed.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_REVIEWS_TOGGLED)
            viewModel.updateProductDraft(reviewsAllowed = isChecked)
            activity?.invalidateOptionsMenu()
        }

        if (viewModel.getProduct().productDraft?.productType == SIMPLE) {
            binding.productIsDownloadable.visibility = View.VISIBLE
            binding.productIsDownloadableDivider.visibility = View.VISIBLE
            binding.productIsDownloadable.setOnCheckedChangeListener { checkbox, isChecked ->
                updateIsDownloadableFlag(checkbox, isChecked)
            }
        } else {
            binding.productIsDownloadable.visibility = View.GONE
            binding.productIsDownloadableDivider.visibility = View.GONE
        }

        binding.productPurchaseNote.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_SETTINGS_PURCHASE_NOTE_TAPPED)
            val purchaseNote = viewModel.getProduct().productDraft?.purchaseNote ?: ""
            viewModel.onEditProductCardClicked(
                ViewProductPurchaseNoteEditor(
                    purchaseNote,
                    getString(R.string.product_purchase_note),
                    getString(R.string.product_purchase_note_caption)
                )
            )
        }

        setupResultHandlers()
    }

    private fun setupResultHandlers() {
        handleResult<Bundle>(AztecEditorFragment.AZTEC_EDITOR_RESULT) { result ->
            if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                viewModel.updateProductDraft(
                    purchaseNote = result.getString(AztecEditorFragment.ARG_AZTEC_EDITOR_TEXT)
                )
                updateProductView()
            }
        }
        handleResult<ProductVisibilityResult>(ProductVisibilityFragment.PRODUCT_VISIBILITY_RESULT) { result ->
            ProductVisibility.fromString(result.selectedVisiblity)?.let { visibility ->
                viewModel.updateProductVisibility(visibility, result.password)
                updateProductView()
            }
        }
        handleResult<ProductStatus>(ProductStatusFragment.ARG_SELECTED_STATUS) { status ->
            viewModel.updateProductDraft(productStatus = status)
            updateProductView()
        }
        handleResult<ProductCatalogVisibilityResult>(ARG_CATALOG_VISIBILITY) { result ->
            viewModel.updateProductDraft(
                catalogVisibility = result.catalogVisibility,
                isFeatured = result.isFeatured
            )
            updateProductView()
        }
        handleResult<String>(ProductSlugFragment.ARG_SLUG) { slug ->
            viewModel.updateProductDraft(slug = slug)
            updateProductView()
        }
        handleResult<Int>(ProductMenuOrderFragment.ARG_MENU_ORDER) { menuOrder ->
            viewModel.updateProductDraft(menuOrder = menuOrder)
            updateProductView()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitSettings())
        return false
    }

    override fun getFragmentTitle() = getString(R.string.product_settings)

    private fun updateProductView() {
        if (!isAdded) return

        fun valueOrNotSet(value: String?): String {
            return if (value.isNullOrBlank()) {
                resources.getString(R.string.value_not_set)
            } else {
                value
            }
        }

        fun valueOrNotSet(value: Int): String {
            return if (value == 0) {
                resources.getString(R.string.value_not_set)
            } else {
                value.toString()
            }
        }

        val product = requireNotNull(viewModel.getProduct().productDraft)
        binding.productStatus.optionValue = product.status?.toLocalizedString(requireActivity(), true)
        binding.productCatalogVisibility.optionValue = product.catalogVisibility?.toLocalizedString(requireActivity())
        binding.productSlug.optionValue = valueOrNotSet(product.slug)
        binding.productReviewsAllowed.isChecked = product.reviewsAllowed
        binding.productPurchaseNote.optionValue = valueOrNotSet(product.purchaseNote.fastStripHtml())
        binding.productVisibility.optionValue = viewModel.getProductVisibility().toLocalizedString(requireActivity())
        binding.productMenuOrder.optionValue = valueOrNotSet(product.menuOrder)
        binding.productIsDownloadable.isChecked = product.isDownloadable
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitSettings -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }

        updateProductView()
    }

    private fun updateIsDownloadableFlag(checkBox: CompoundButton, isChecked: Boolean) {
        fun updateProductDraft(value: Boolean) {
            viewModel.updateProductDraft(isDownloadable = value)
            activity?.invalidateOptionsMenu()
        }

        if (!isChecked) {
            MaterialAlertDialogBuilder(requireActivity())
                .setView(R.layout.dialog_uncheck_is_downloadable_warning)
                .setPositiveButton(R.string.product_uncheck_is_downloadable_warning_yes_button) { _, _ ->
                    updateProductDraft(false)
                }
                .setNegativeButton(R.string.product_uncheck_is_downloadable_warning_no_button) { _, _ ->
                    checkBox.isChecked = true
                }
                .setCancelable(false)
                .show()
        } else {
            updateProductDraft(true)
        }
    }
}
