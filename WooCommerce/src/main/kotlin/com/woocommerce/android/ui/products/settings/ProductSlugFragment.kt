package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductSlugBinding

/**
 * Settings screen which enables editing a product's slug
 */
class ProductSlugFragment : BaseProductSettingsFragment(R.layout.fragment_product_slug) {
    companion object {
        const val ARG_SLUG = "slug"
    }

    private val navArgs: ProductSlugFragmentArgs by navArgs()

    private var _binding: FragmentProductSlugBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductSlugBinding.bind(view)

        binding.editSlug.setText(navArgs.slug)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun hasChanges() = getSlug() != navArgs.slug

    override fun getChangesResult(): Pair<String, Any> = ARG_SLUG to getSlug()

    override fun validateChanges() = true

    /**
     * As with the web, we trim the string and replace any spaces with hyphens
     */
    private fun getSlug() = binding.editSlug.getText().trim().replace(" ", "-")

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_slug)
}
