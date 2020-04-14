package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductVisibility
import com.woocommerce.android.ui.products.ProductVisibility.CATALOG
import com.woocommerce.android.ui.products.ProductVisibility.HIDDEN
import com.woocommerce.android.ui.products.ProductVisibility.SEARCH
import com.woocommerce.android.ui.products.ProductVisibility.VISIBLE
import kotlinx.android.synthetic.main.fragment_product_status.radioGroup
import kotlinx.android.synthetic.main.fragment_product_visibility.*

/**
 * Settings screen which enables choosing a product's catalog visibility
 */
class ProductVisibilityFragment : BaseFragment() {
    companion object {
        const val ARG_VISIBILITY = "visibility"
        const val ARG_IS_FEATURED = "is_featured"
    }

    private val navArgs: ProductVisibilityFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_visibility, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // TODO: handle back button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                navigateBackWithResult()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getButtonForVisibility(navArgs.visibility)?.isChecked = true
        btnFeatured.isChecked = navArgs.featured
    }

    private fun navigateBackWithResult() {
        val bundle = Bundle().also {
            it.putSerializable(ARG_VISIBILITY, getVisibilityForButtonId(radioGroup.checkedRadioButtonId))
            it.putBoolean(ARG_IS_FEATURED, btnFeatured.isChecked)
        }
        requireActivity().navigateBackWithResult(
                RequestCodes.PRODUCT_SETTINGS_VISIBLITY,
                bundle,
                R.id.nav_host_fragment_main,
                R.id.productSettingsFragment
        )
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_catalog_visibility)

    private fun getButtonForVisibility(visibility: String): RadioButton? {
        return when (ProductVisibility.fromString(visibility)) {
            VISIBLE -> btnVisibilityVisible
            CATALOG -> btnVisibilityCatalog
            SEARCH -> btnVisibilitySearch
            HIDDEN -> btnVisibilityHidden
            else -> null
        }
    }

    private fun getVisibilityForButtonId(@IdRes buttonId: Int): ProductVisibility? {
        return when (buttonId) {
            R.id.btnVisibilityVisible -> VISIBLE
            R.id.btnVisibilityCatalog -> CATALOG
            R.id.btnVisibilitySearch -> SEARCH
            R.id.btnVisibilityHidden -> HIDDEN
            else -> null
        }
    }
}
