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
import com.woocommerce.android.ui.products.ProductVisibility
import com.woocommerce.android.ui.products.ProductVisibility.CATALOG
import com.woocommerce.android.ui.products.ProductVisibility.HIDDEN
import com.woocommerce.android.ui.products.ProductVisibility.SEARCH
import com.woocommerce.android.ui.products.ProductVisibility.VISIBLE
import kotlinx.android.synthetic.main.fragment_product_visibility.*

/**
 * Settings screen which enables choosing a product's catalog visibility
 */
class ProductVisibilityFragment : BaseProductSettingsFragment(), OnClickListener {
    companion object {
        const val ARG_VISIBILITY = "visibility"
        const val ARG_IS_FEATURED = "is_featured"
    }

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_VISIBLITY

    private val navArgs: ProductVisibilityFragmentArgs by navArgs()
    private var selectedVisibility: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_visibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedVisibility = savedInstanceState?.getString(ARG_VISIBILITY) ?: navArgs.visibility
        btnFeatured.isChecked = savedInstanceState?.getBoolean(ARG_IS_FEATURED) ?: navArgs.featured

        selectedVisibility?.let {
            getButtonForVisibility(it)?.isChecked = true
        }

        btnVisibilityVisible.setOnClickListener(this)
        btnVisibilityCatalog.setOnClickListener(this)
        btnVisibilitySearch.setOnClickListener(this)
        btnVisibilityHidden.setOnClickListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_VISIBILITY, selectedVisibility)
        outState.putBoolean(ARG_IS_FEATURED, btnFeatured.isChecked)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            btnVisibilityVisible.isChecked = it == btnVisibilityVisible
            btnVisibilityCatalog.isChecked = it == btnVisibilityCatalog
            btnVisibilitySearch.isChecked = it == btnVisibilitySearch
            btnVisibilityHidden.isChecked = it == btnVisibilityHidden
            selectedVisibility = getVisibilityForButtonId(it.id)
        }
    }

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putString(ARG_VISIBILITY, selectedVisibility)
            it.putBoolean(ARG_IS_FEATURED, btnFeatured.isChecked)
        }
    }

    override fun hasChanges(): Boolean {
        return navArgs.featured != btnFeatured.isChecked ||
                navArgs.visibility != selectedVisibility
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_catalog_visibility)

    private fun getButtonForVisibility(visibility: String): CheckedTextView? {
        return when (ProductVisibility.fromString(visibility)) {
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
