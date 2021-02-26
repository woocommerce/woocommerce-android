package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductMenuOrderBinding
import org.wordpress.android.util.StringUtils

/**
 * Settings screen which enables editing a product's menu order
 */
class ProductMenuOrderFragment : BaseProductSettingsFragment(R.layout.fragment_product_menu_order) {
    companion object {
        const val ARG_MENU_ORDER = "menu_order"
    }

    private val navArgs: ProductMenuOrderFragmentArgs by navArgs()

    private var _binding: FragmentProductMenuOrderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductMenuOrderBinding.bind(view)

        binding.productMenuOrder.setText(navArgs.menuOrder.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun hasChanges() = getMenuOrder() != navArgs.menuOrder

    override fun validateChanges() = true

    override fun getChangesResult(): Pair<String, Any> = ARG_MENU_ORDER to getMenuOrder()

    private fun getMenuOrder() = StringUtils.stringToInt(binding.productMenuOrder.getText())

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_menu_order)
}
