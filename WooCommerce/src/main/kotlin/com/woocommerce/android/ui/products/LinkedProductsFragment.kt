package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_LINKED_PRODUCTS_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.LinkedProductsAction
import com.woocommerce.android.databinding.FragmentLinkedProductsBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitLinkedProducts
import com.woocommerce.android.ui.products.grouped.GroupedProductListFragmentDirections
import com.woocommerce.android.ui.products.grouped.GroupedProductListType
import com.woocommerce.android.ui.products.grouped.GroupedProductListType.CROSS_SELLS
import com.woocommerce.android.ui.products.grouped.GroupedProductListType.UPSELLS
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LinkedProductsFragment : BaseProductFragment(R.layout.fragment_linked_products) {
    private var _binding: FragmentLinkedProductsBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

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
        AnalyticsTracker.track(
            AnalyticsEvent.LINKED_PRODUCTS,
            mapOf(KEY_LINKED_PRODUCTS_ACTION to LinkedProductsAction.SHOWN.value)
        )
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitLinkedProducts -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }

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
            binding.upsellsCount.text = StringUtils.getQuantityString(
                context = requireContext(),
                quantity = numUpsells,
                default = R.string.product_count_many,
                one = R.string.product_count_one,
            )
            binding.upsellsCount.show()
            binding.addUpsellProducts.text = getString(R.string.edit_products_button)
        } else {
            binding.upsellsCount.hide()
            binding.addUpsellProducts.text = getString(R.string.add_products_button)
        }

        val numCrossSells = viewModel.getProduct().productDraft?.crossSellProductIds?.size ?: 0
        if (numCrossSells > 0) {
            binding.crossSellsCount.text = StringUtils.getQuantityString(
                context = requireContext(),
                quantity = numCrossSells,
                default = R.string.product_count_many,
                one = R.string.product_count_one,
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

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_detail_linked_products),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitLinkedProducts)
                }
            }
        )
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitLinkedProducts)
        return false
    }

    private fun showGroupedProductFragment(groupedProductType: GroupedProductListType) {
        val productIds = when (groupedProductType) {
            UPSELLS -> viewModel.getProduct().productDraft?.upsellProductIds ?: emptyList()
            else -> viewModel.getProduct().productDraft?.crossSellProductIds ?: emptyList()
        }

        findNavController().navigateSafely(
            GroupedProductListFragmentDirections.actionGlobalGroupedProductListFragment(
                remoteProductId = viewModel.getRemoteProductId(),
                productIds = productIds.toLongArray(),
                groupedProductListType = groupedProductType
            )
        )
    }
}
