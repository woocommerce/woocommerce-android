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
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductVisibility
import com.woocommerce.android.ui.products.settings.ProductSlugFragment.Companion.ARG_SLUG
import com.woocommerce.android.ui.products.settings.ProductStatusFragment.Companion.ARG_SELECTED_STATUS
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment.Companion.ARG_IS_FEATURED
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment.Companion.ARG_VISIBILITY
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
        productReviewsAllowed.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateProductDraft(reviewsAllowed = isChecked)
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
            (result.getSerializable(ARG_SELECTED_STATUS) as? ProductStatus)?.let {
                viewModel.updateProductDraft(productStatus = it)
                updateProductView()
            }
        } else if (requestCode == RequestCodes.PRODUCT_SETTINGS_VISIBLITY) {
            (result.getSerializable(ARG_VISIBILITY) as? ProductVisibility)?.let {
                viewModel.updateProductDraft(visibility = it, isFeatured = result.getBoolean(ARG_IS_FEATURED))
                updateProductView()
            }
        } else if (requestCode == RequestCodes.PRODUCT_SETTINGS_SLUG) {
            viewModel.updateProductDraft(slug = result.getString(ARG_SLUG))
            updateProductView()
        }
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
