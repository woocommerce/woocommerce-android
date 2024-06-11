package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductQuantityRulesBinding
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductQuantityRules
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductQuantityRulesFragment : BaseProductFragment(R.layout.fragment_product_quantity_rules) {
    companion object {
        val TAG: String = ProductQuantityRulesFragment::class.java.simpleName
    }

    private val navArgs: ProductQuantityRulesFragmentArgs by navArgs()

    private var _binding: FragmentProductQuantityRulesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductQuantityRulesBinding.bind(view)
        viewModel.event.observe(viewLifecycleOwner, Observer(::onEventReceived))
        initializeViews(navArgs.quantityRules)

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_quantity_rules_title),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductQuantityRules)
                }
            }
        )
    }

    private fun onEventReceived(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitProductQuantityRules -> findNavController().navigateUp()
            else -> event.isHandled = false
        }
    }

    private fun initializeViews(quantityRules: QuantityRules) {
        binding.minQuantityValue.text = quantityRules.min?.toString() ?: getString(R.string.empty_min_quantity)
        binding.maxQuantityValue.text = quantityRules.max?.toString() ?: getString(R.string.empty_max_quantity)
        binding.groupOfValue.text = quantityRules.groupOf?.toString() ?: getString(R.string.empty_group_of)
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductQuantityRules)
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
