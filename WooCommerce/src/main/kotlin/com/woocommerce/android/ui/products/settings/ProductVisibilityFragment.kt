package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
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
        const val ARG_SELECTED_VISIBILITY = "selected_visibility"
    }

    private val navArgs: ProductVisibilityFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_visibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getButtonForVisibility(navArgs.visibility)?.isChecked = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            getVisibilityForButtonId(checkedId)?.let { visibility ->
                val bundle = Bundle().also {
                    it.putSerializable(ARG_SELECTED_VISIBILITY, visibility)
                }
                requireActivity().navigateBackWithResult(
                        RequestCodes.PRODUCT_SETTINGS_VISIBLITY,
                        bundle,
                        R.id.nav_host_fragment_main,
                        R.id.productSettingsFragment
                )
            }
        }
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
