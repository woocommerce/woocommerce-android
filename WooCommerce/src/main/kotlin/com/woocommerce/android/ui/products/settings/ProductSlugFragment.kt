package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import kotlinx.android.synthetic.main.fragment_product_slug.*

/**
 * Settings screen which enables editing a product's slug
 */
class ProductSlugFragment : BaseProductSettingsFragment() {
    companion object {
        const val ARG_SLUG = "slug"
    }

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_SLUG
    private val navArgs: ProductSlugFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_slug, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editSlug.setText(navArgs.slug)
        editSlug.setOnTextChangedListener {
            changesMade()
        }
    }

    override fun hasChanges() = getSlug() != navArgs.slug

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putString(ARG_SLUG, getSlug())
        }
    }

    override fun validateChanges() = true

    /**
     * As with the web, we trim the string and replace any spaces with hyphens
     */
    private fun getSlug() = editSlug.getText().trim().replace(" ", "-")

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_slug)
}
