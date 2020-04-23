package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.RequestCodes.PRODUCT_SETTINGS_MENU_ORDER
import com.woocommerce.android.RequestCodes.PRODUCT_SETTINGS_PURCHASE_NOTE
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPurchaseNoteEditor
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductVisibility
import com.woocommerce.android.ui.products.settings.ProductSlugFragment.Companion.ARG_SLUG
import com.woocommerce.android.ui.products.settings.ProductStatusFragment.Companion.ARG_SELECTED_STATUS
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment.Companion.ARG_IS_FEATURED
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
            viewModel.onSettingsStatusButtonClicked()
        }
        productVisibility.setOnClickListener {
            viewModel.onSettingsVisibilityButtonClicked()
        }
        productSlug.setOnClickListener {
            viewModel.onSettingsSlugButtonClicked()
        }
        productMenuOrder.setOnClickListener {
            viewModel.onSettingsMenuOrderButtonClicked()
        }
        if (FeatureFlag.PRODUCT_RELEASE_M3.isEnabled()) {
            productReviewsAllowed.visibility = View.VISIBLE
            productReviewsAllowedDivider.visibility = View.VISIBLE
            productReviewsAllowed.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateProductDraft(reviewsAllowed = isChecked)
            }
        } else {
            productReviewsAllowed.visibility = View.GONE
            productReviewsAllowedDivider.visibility = View.GONE
        }
        productPurchaseNote.setOnClickListener {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
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
        } else if (requestCode == RequestCodes.PRODUCT_SETTINGS_VISIBLITY) {
            (result.getString(ARG_VISIBILITY))?.let {
                val visibility = ProductVisibility.fromString(it)
                viewModel.updateProductDraft(visibility = visibility, isFeatured = result.getBoolean(ARG_IS_FEATURED))
            }
        } else if (requestCode == RequestCodes.PRODUCT_SETTINGS_SLUG) {
            viewModel.updateProductDraft(slug = result.getString(ARG_SLUG))
        } else if (requestCode == PRODUCT_SETTINGS_PURCHASE_NOTE) {
            if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                viewModel.updateProductDraft(purchaseNote = result.getString(AztecEditorFragment.ARG_AZTEC_EDITOR_TEXT))
                updateProductView()
            }
        } else if (requestCode == PRODUCT_SETTINGS_MENU_ORDER) {
            viewModel.updateProductDraft(menuOrder = result.getInt(ProductMenuOrderFragment.ARG_MENU_ORDER, 0))
        }

        updateProductView()
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

        val product = requireNotNull(viewModel.getProduct().productDraft)
        productStatus.optionValue = product.status?.toLocalizedString(requireActivity())
        productVisibility.optionValue = product.visibility?.toLocalizedString(requireActivity())
        productSlug.optionValue = valueOrNotSet(product.slug)
        productReviewsAllowed.isChecked = product.reviewsAllowed
        productPurchaseNote.optionValue = valueOrNotSet(product.purchaseNote.fastStripHtml())
        productMenuOrder.optionValue = product.menuOrder.toString()
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
}
