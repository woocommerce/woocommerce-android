package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.products.GroupedProductListType.CROSS_SELLS
import com.woocommerce.android.ui.products.GroupedProductListType.UPSELLS
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitLinkedProducts
import kotlinx.android.synthetic.main.fragment_linked_products.*
import org.wordpress.android.util.ActivityUtils

class LinkedProductsFragment : BaseProductFragment() {
    override fun getFragmentTitle() = getString(R.string.product_detail_linked_products)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_linked_products, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(ExitLinkedProducts(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        updateProductView()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitLinkedProducts -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        handleResult<List<Long>>(GroupedProductListFragment.KEY_UPSELL_PRODUCT_IDS_RESULT) {
            viewModel.updateProductDraft(upsellProductIds = it)
            changesMade()
            updateProductView()
        }

        handleResult<List<Long>>(GroupedProductListFragment.KEY_CROSS_SELL_PRODUCT_IDS_RESULT) {
            viewModel.updateProductDraft(crossSellProductIds = it)
            changesMade()
            updateProductView()
        }
    }

    private fun updateProductView() {
        if (!isAdded) return

        val numUpsells = viewModel.getProduct().productDraft?.upsellProductIds?.size ?: 0
        if (numUpsells > 0) {
            upsells_count.text = resources.getQuantityString(R.plurals.product_count, numUpsells, numUpsells)
            upsells_count.show()
            add_upsell_products.text = getString(R.string.edit_products_button)
        } else {
            upsells_count.hide()
            add_upsell_products.text = getString(R.string.add_products_button)
        }

        val numCrossSells = viewModel.getProduct().productDraft?.crossSellProductIds?.size ?: 0
        if (numCrossSells > 0) {
            cross_sells_count.text = resources.getQuantityString(R.plurals.product_count, numCrossSells, numCrossSells)
            cross_sells_count.show()
            add_cross_sell_products.text = getString(R.string.edit_products_button)
        } else {
            cross_sells_count.hide()
            add_cross_sell_products.text = getString(R.string.add_products_button)
        }

        add_upsell_products.setOnClickListener {
            showGroupedProductFragment(UPSELLS)
        }

        add_cross_sell_products.setOnClickListener {
            showGroupedProductFragment(CROSS_SELLS)
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked(ExitLinkedProducts())

    private fun showGroupedProductFragment(groupedProductType: GroupedProductListType) {
        val productIds = when (groupedProductType) {
            UPSELLS -> viewModel.getProduct().productDraft?.upsellProductIds
            else -> viewModel.getProduct().productDraft?.crossSellProductIds
        }

        val action = GroupedProductListFragmentDirections.actionGlobalGroupedProductListFragment(
            viewModel.getRemoteProductId(),
            productIds?.joinToString(",") ?: "",
            groupedProductType
        )
        findNavController().navigateSafely(action)
    }
}
