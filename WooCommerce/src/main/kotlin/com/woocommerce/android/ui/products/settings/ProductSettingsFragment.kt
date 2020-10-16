package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.RequestCodes.PRODUCT_SETTINGS_MENU_ORDER
import com.woocommerce.android.RequestCodes.PRODUCT_SETTINGS_PURCHASE_NOTE
import com.woocommerce.android.RequestCodes.PRODUCT_SETTINGS_VISIBLITY
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPurchaseNoteEditor
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityFragment.Companion.ARG_CATALOG_VISIBILITY
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityFragment.Companion.ARG_IS_FEATURED
import com.woocommerce.android.ui.products.settings.ProductSlugFragment.Companion.ARG_SLUG
import com.woocommerce.android.ui.products.settings.ProductStatusFragment.Companion.ARG_SELECTED_STATUS
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment.Companion.ARG_PASSWORD
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment.Companion.ARG_VISIBILITY
import com.woocommerce.android.util.FeatureFlag
import kotlinx.android.synthetic.main.fragment_product_settings.*

class ProductSettingsFragment : BaseProductFragment(), NavigationResult {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()

        productStatus.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_STATUS_TAPPED)
            viewModel.onSettingsStatusButtonClicked()
        }
        productCatalogVisibility.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_CATALOG_VISIBILITY_TAPPED)
            viewModel.onSettingsCatalogVisibilityButtonClicked()
        }
        productVisibility.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_VISIBILITY_TAPPED)
            viewModel.onSettingsVisibilityButtonClicked()
        }
        productSlug.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_SLUG_TAPPED)
            viewModel.onSettingsSlugButtonClicked()
        }
        productMenuOrder.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_MENU_ORDER_TAPPED)
            viewModel.onSettingsMenuOrderButtonClicked()
        }

        val isSimple = viewModel.getProduct().productDraft?.productType == ProductType.SIMPLE
        if (isSimple) {
            productIsVirtual.visibility = View.VISIBLE
            productIsVirtual.setOnCheckedChangeListener { _, isChecked ->
                AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_VIRTUAL_TOGGLED)
                viewModel.updateProductDraft(isVirtual = isChecked)
                activity?.invalidateOptionsMenu()
            }
        } else {
            productIsVirtual.visibility = View.GONE
        }

        productReviewsAllowed.visibility = View.VISIBLE
        productReviewsAllowedDivider.visibility = View.VISIBLE
        productReviewsAllowed.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_REVIEWS_TOGGLED)
            viewModel.updateProductDraft(reviewsAllowed = isChecked)
            activity?.invalidateOptionsMenu()
        }

        if (FeatureFlag.PRODUCT_RELEASE_M5.isEnabled() && viewModel.getProduct().storedProduct?.type == SIMPLE) {
            productIsDownloadable.visibility = View.VISIBLE
            productIsDownloadableDivider.visibility = View.VISIBLE
            productIsDownloadable.setOnCheckedChangeListener { checkbox, isChecked ->
                updateIsDownloadableFlag(checkbox, isChecked)
            }
        } else {
            productIsDownloadable.visibility = View.GONE
            productIsDownloadableDivider.visibility = View.GONE
        }

        productPurchaseNote.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_PURCHASE_NOTE_TAPPED)
            val purchaseNote = viewModel.getProduct().productDraft?.purchaseNote ?: ""
            viewModel.onEditProductCardClicked(
                    ViewProductPurchaseNoteEditor(
                            purchaseNote,
                            getString(R.string.product_purchase_note),
                            getString(R.string.product_purchase_note_caption)
                    )
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_done)?.isVisible = viewModel.hasSettingsChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                AnalyticsTracker.track(Stat.PRODUCT_SETTINGS_DONE_BUTTON_TAPPED)
                viewModel.onDoneButtonClicked(ExitSettings(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        if (requestCode == RequestCodes.PRODUCT_SETTINGS_STATUS) {
            (result.getString(ARG_SELECTED_STATUS))?.let {
                val status = ProductStatus.fromString(it)
                viewModel.updateProductDraft(productStatus = status)
            }
        } else if (requestCode == RequestCodes.PRODUCT_SETTINGS_CATALOG_VISIBLITY) {
            (result.getString(ARG_CATALOG_VISIBILITY))?.let {
                val catalogVisibility = ProductCatalogVisibility.fromString(it)
                viewModel.updateProductDraft(
                        catalogVisibility = catalogVisibility,
                        isFeatured = result.getBoolean(ARG_IS_FEATURED)
                )
            }
        } else if (requestCode == RequestCodes.PRODUCT_SETTINGS_SLUG) {
            viewModel.updateProductDraft(slug = result.getString(ARG_SLUG))
        } else if (requestCode == PRODUCT_SETTINGS_PURCHASE_NOTE) {
            if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                viewModel.updateProductDraft(
                        purchaseNote = result.getString(AztecEditorFragment.ARG_AZTEC_EDITOR_TEXT)
                )
            }
        } else if (requestCode == PRODUCT_SETTINGS_MENU_ORDER) {
            viewModel.updateProductDraft(
                        menuOrder = result.getInt(ProductMenuOrderFragment.ARG_MENU_ORDER, 0)
            )
        } else if (requestCode == PRODUCT_SETTINGS_VISIBLITY) {
            ProductVisibility.fromString(result.getString(ARG_VISIBILITY) ?: "")?.let { visibility ->
                val password = result.getString(ARG_PASSWORD) ?: ""
                viewModel.updateProductVisibility(visibility, password)
            }
        }

        updateProductView()
        activity?.invalidateOptionsMenu()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitSettings())
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
        productStatus.optionValue = product.status?.toLocalizedString(requireActivity(), true)
        productCatalogVisibility.optionValue = product.catalogVisibility?.toLocalizedString(requireActivity())
        productSlug.optionValue = valueOrNotSet(product.slug)
        productReviewsAllowed.isChecked = product.reviewsAllowed
        productIsVirtual.isChecked = product.isVirtual
        productPurchaseNote.optionValue = valueOrNotSet(product.purchaseNote.fastStripHtml())
        productVisibility.optionValue = viewModel.getProductVisibility().toLocalizedString(requireActivity())
        productMenuOrder.optionValue = valueOrNotSet(product.menuOrder)
        productIsDownloadable.isChecked = product.isDownloadable
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitSettings -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

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
