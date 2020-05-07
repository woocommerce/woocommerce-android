package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.CATALOG
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.HIDDEN
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.SEARCH
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.VISIBLE
import kotlinx.android.synthetic.main.fragment_product_catalog_visibility.*

/**
 * Settings screen which enables choosing a product's catalog visibility
 */
class ProductCatalogVisibilityFragment : BaseProductSettingsFragment(), OnClickListener {
    companion object {
        const val ARG_CATALOG_VISIBILITY = "catalog_visibility"
        const val ARG_IS_FEATURED = "is_featured"
    }

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_CATALOG_VISIBLITY

    private val navArgs: ProductCatalogVisibilityFragmentArgs by navArgs()
    private var selectedCatalogVisibility: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_catalog_visibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedCatalogVisibility = savedInstanceState?.getString(ARG_CATALOG_VISIBILITY) ?: navArgs.catalogVisibility
        btnFeatured.isChecked = savedInstanceState?.getBoolean(ARG_IS_FEATURED) ?: navArgs.featured

        selectedCatalogVisibility?.let {
            getButtonForVisibility(it)?.isChecked = true
        }

        btnVisibilityVisible.setOnClickListener(this)
        btnVisibilityCatalog.setOnClickListener(this)
        btnVisibilitySearch.setOnClickListener(this)
        btnVisibilityHidden.setOnClickListener(this)

        btnFeatured.setOnCheckedChangeListener { _, isChecked ->
            changesMade()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_CATALOG_VISIBILITY, selectedCatalogVisibility)
        outState.putBoolean(ARG_IS_FEATURED, btnFeatured.isChecked)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            btnVisibilityVisible.isChecked = it == btnVisibilityVisible
            btnVisibilityCatalog.isChecked = it == btnVisibilityCatalog
            btnVisibilitySearch.isChecked = it == btnVisibilitySearch
            btnVisibilityHidden.isChecked = it == btnVisibilityHidden
            selectedCatalogVisibility = getVisibilityForButtonId(it.id)

            changesMade()
        }
    }

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putString(ARG_CATALOG_VISIBILITY, selectedCatalogVisibility)
            it.putBoolean(ARG_IS_FEATURED, btnFeatured.isChecked)
        }
    }

    override fun hasChanges(): Boolean {
        return navArgs.featured != btnFeatured.isChecked ||
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
            VISIBLE -> btnVisibilityVisible
            CATALOG -> btnVisibilityCatalog
            SEARCH -> btnVisibilitySearch
            HIDDEN -> btnVisibilityHidden
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
