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
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitLinkedProducts
import com.woocommerce.android.util.StringUtils
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
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitLinkedProducts -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
        updateProductView(viewModel.getProduct())
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val numUpsells = viewModel.getProduct().productDraft?.upsellProductIds?.size ?: 0
        if (numUpsells > 0) {
            val upsellDesc = StringUtils.getQuantityString(
                resourceProvider = viewModel.getResources(),
                quantity = numUpsells,
                default = R.string.products_count,
                one = R.string.products_single
            )
            upsells_count.text = upsellDesc
            upsells_count.show()
            add_upsell_products.hide()
        } else {
            upsells_count.hide()
            add_upsell_products.show()
        }

        val numCrossSells = viewModel.getProduct().productDraft?.crossSellProductIds?.size ?: 0
        if (numCrossSells > 0) {
            val crossSellDesc = StringUtils.getQuantityString(
                resourceProvider = viewModel.getResources(),
                quantity = numCrossSells,
                default = R.string.products_count,
                one = R.string.products_single
            )
            cross_sells_count.text = crossSellDesc
            upsells_count.show()
            add_cross_sell_products.hide()
        } else {
            cross_sells_count.hide()
            add_cross_sell_products.show()
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked(ExitLinkedProducts())
}
