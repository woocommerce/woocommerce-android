package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckedTextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductCatalogVisibilityBinding
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.CATALOG
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.HIDDEN
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.SEARCH
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.VISIBLE

/**
 * Settings screen which enables choosing a product's catalog visibility
 */
class ProductCatalogVisibilityFragment : BaseProductSettingsFragment(R.layout.fragment_product_catalog_visibility),
    OnClickListener {
    companion object {
        const val ARG_CATALOG_VISIBILITY = "catalog_visibility"
        const val ARG_IS_FEATURED = "is_featured"
    }

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_CATALOG_VISIBLITY

    private val navArgs: ProductCatalogVisibilityFragmentArgs by navArgs()
    private var selectedCatalogVisibility: String? = null

    private var _binding: FragmentProductCatalogVisibilityBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductCatalogVisibilityBinding.bind(view)
        setHasOptionsMenu(true)

        selectedCatalogVisibility = savedInstanceState?.getString(ARG_CATALOG_VISIBILITY) ?: navArgs.catalogVisibility
        binding.btnFeatured.isChecked = savedInstanceState?.getBoolean(ARG_IS_FEATURED) ?: navArgs.featured

        selectedCatalogVisibility?.let {
            getButtonForVisibility(it)?.isChecked = true
        }

        binding.btnVisibilityVisible.setOnClickListener(this)
        binding.btnVisibilityCatalog.setOnClickListener(this)
        binding.btnVisibilitySearch.setOnClickListener(this)
        binding.btnVisibilityHidden.setOnClickListener(this)

        binding.btnFeatured.setOnCheckedChangeListener { _, isChecked ->
            changesMade()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_CATALOG_VISIBILITY, selectedCatalogVisibility)
        outState.putBoolean(ARG_IS_FEATURED, binding.btnFeatured.isChecked)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            binding.btnVisibilityVisible.isChecked = it == binding.btnVisibilityVisible
            binding.btnVisibilityCatalog.isChecked = it == binding.btnVisibilityCatalog
            binding.btnVisibilitySearch.isChecked = it == binding.btnVisibilitySearch
            binding.btnVisibilityHidden.isChecked = it == binding.btnVisibilityHidden
            selectedCatalogVisibility = getVisibilityForButtonId(it.id)

            changesMade()
        }
    }

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putString(ARG_CATALOG_VISIBILITY, selectedCatalogVisibility)
            it.putBoolean(ARG_IS_FEATURED, binding.btnFeatured.isChecked)
        }
    }

    override fun hasChanges(): Boolean {
        return navArgs.featured != binding.btnFeatured.isChecked ||
                navArgs.catalogVisibility != selectedCatalogVisibility
    }

    override fun validateChanges() = true

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_catalog_visibility)

    private fun getButtonForVisibility(visibility: String): CheckedTextView? {
        return when (ProductCatalogVisibility.fromString(visibility)) {
            VISIBLE -> binding.btnVisibilityVisible
            CATALOG -> binding.btnVisibilityCatalog
            SEARCH -> binding.btnVisibilitySearch
            HIDDEN -> binding.btnVisibilityHidden
            else -> null
        }
    }

    private fun getVisibilityForButtonId(@IdRes buttonId: Int): String? {
        return when (buttonId) {
            R.id.btnVisibilityVisible -> VISIBLE.toString()
            R.id.btnVisibilityCatalog -> CATALOG.toString()
            R.id.btnVisibilitySearch -> SEARCH.toString()
            R.id.btnVisibilityHidden -> HIDDEN.toString()
            else -> null
        }
    }
}
