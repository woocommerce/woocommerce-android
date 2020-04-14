package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_product_visibility.*

/**
 * Settings screen which enables editing a product's slug
 */
class ProductSlugFragment : BaseFragment() {
    companion object {
        const val ARG_SLUG = "slug"
    }

    private val navArgs: ProductVisibilityFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_slug, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun navigateBackWithResult() {
        val bundle = Bundle().also {
            it.putBoolean(ARG_SLUG, btnFeatured.isChecked)
        }
        requireActivity().navigateBackWithResult(
                RequestCodes.PRODUCT_SETTINGS_SLUG,
                bundle,
                R.id.nav_host_fragment_main,
                R.id.productSettingsFragment
        )
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_slug)
}
