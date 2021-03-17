package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_LINKED_PRODUCTS_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.LinkedProductsAction
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentLinkedProductsBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.products.GroupedProductListType.CROSS_SELLS
import com.woocommerce.android.ui.products.GroupedProductListType.UPSELLS
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitLinkedProducts

class LinkedProductsFragment : BaseProductFragment(R.layout.fragment_linked_products) {
    private var _binding: FragmentLinkedProductsBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = getString(R.string.product_detail_linked_products)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLinkedProductsBinding.bind(view)

        setupObservers()
        updateProductView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(Stat.LINKED_PRODUCTS,
            mapOf(KEY_LINKED_PRODUCTS_ACTION to LinkedProductsAction.SHOWN.value))
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitLinkedProducts -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        handleResult<List<Long>>(UPSELLS.resultKey) {
            viewModel.updateProductDraft(upsellProductIds = it)
            updateProductView()
        }

        handleResult<List<Long>>(CROSS_SELLS.resultKey) {
            viewModel.updateProductDraft(crossSellProductIds = it)
            updateProductView()
        }
    }

    private fun updateProductView() {
        if (!isAdded) return

        val numUpsells = viewModel.getProduct().productDraft?.upsellProductIds?.size ?: 0
        if (numUpsells > 0) {
            binding.upsellsCount.text = resources.getQuantityString(R.plurals.product_count, numUpsells, numUpsells)
            binding.upsellsCount.show()
            binding.addUpsellProducts.text = getString(R.string.edit_products_button)
        } else {
            binding.upsellsCount.hide()
            binding.addUpsellProducts.text = getString(R.string.add_products_button)
        }

        val numCrossSells = viewModel.getProduct().productDraft?.crossSellProductIds?.size ?: 0
        if (numCrossSells > 0) {
            binding.crossSellsCount.text = resources.getQuantityString(
                R.plurals.product_count,
                numCrossSells,
                numCrossSells
            )
            binding.crossSellsCount.show()
            binding.addCrossSellProducts.text = getString(R.string.edit_products_button)
        } else {
            binding.crossSellsCount.hide()
            binding.addCrossSellProducts.text = getString(R.string.add_products_button)
        }

        binding.addUpsellProducts.setOnClickListener {
            showGroupedProductFragment(UPSELLS)
        }

        binding.addCrossSellProducts.setOnClickListener {
            showGroupedProductFragment(CROSS_SELLS)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitLinkedProducts())
        return false
    }

    private fun showGroupedProductFragment(groupedProductType: GroupedProductListType) {
        val productIds = when (groupedProductType) {
            UPSELLS -> viewModel.getProduct().productDraft?.upsellProductIds
            else -> viewModel.getProduct().productDraft?.crossSellProductIds
        }

        // go straight to the "add products" screen if the list is empty, otherwise show the grouped
        // products screen
        val action = if (productIds.isNullOrEmpty()) {
            ProductDetailFragmentDirections
                .actionGlobalProductSelectionListFragment(
                    remoteProductId = viewModel.getRemoteProductId(),
                    groupedProductListType = groupedProductType,
                    excludedProductIds = longArrayOf()
                )
        } else {
            GroupedProductListFragmentDirections.actionGlobalGroupedProductListFragment(
                viewModel.getRemoteProductId(),
                productIds.toLongArray(),
                groupedProductType
            )
        }
        findNavController().navigateSafely(action)
    }
}
