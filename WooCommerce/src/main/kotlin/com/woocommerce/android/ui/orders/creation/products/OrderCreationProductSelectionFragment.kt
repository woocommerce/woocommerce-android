package com.woocommerce.android.ui.orders.creation.products

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationProductSelectionBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigator
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.products.OrderCreationProductSelectionViewModel.AddProduct
import com.woocommerce.android.ui.orders.creation.products.OrderCreationProductSelectionViewModel.ViewState
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductListAdapter
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationProductSelectionFragment :
    BaseFragment(R.layout.fragment_order_creation_product_selection),
    OnLoadMoreListener,
    OnQueryTextListener,
    OnActionExpandListener {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val productListViewModel by viewModels<OrderCreationProductSelectionViewModel>()

    private val skeletonView = SkeletonView()
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentOrderCreationProductSelectionBinding.bind(view)) {
            productsList.layoutManager = LinearLayoutManager(requireActivity())
            setupObserversWith(this)
        }
    }

    private fun setupObserversWith(binding: FragmentOrderCreationProductSelectionBinding) {
        productListViewModel.productListData.observe(viewLifecycleOwner) {
            binding.loadProductsAdapterWith(it)
        }
        productListViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            onViewStateChanged(binding, old, new)
        }
        productListViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is AddProduct -> {
                    sharedViewModel.onProductSelected(event.productId)
                    findNavController().navigateUp()
                }
                is ShowProductVariations -> OrderCreationNavigator.navigate(this, event)
            }
        }
    }

    private fun FragmentOrderCreationProductSelectionBinding.loadProductsAdapterWith(
        products: List<Product>
    ) {
        val adapter = productsList.adapter
            .let { it as? ProductListAdapter }
            ?: ProductListAdapter(
                clickListener = { id, _ -> productListViewModel.onProductSelected(id) },
                loadMoreListener = this@OrderCreationProductSelectionFragment
            ).also { productsList.adapter = it }
        adapter.setProductList(products)
    }

    override fun onRequestLoadMore() {
        productListViewModel.loadProductList(loadMore = true)
    }

    private fun onViewStateChanged(
        binding: FragmentOrderCreationProductSelectionBinding,
        old: ViewState?,
        new: ViewState
    ) {
        new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) {
            showSkeleton(binding, it)
        }
    }

    private fun showSkeleton(
        binding: FragmentOrderCreationProductSelectionBinding,
        show: Boolean
    ) {
        if (show) {
            skeletonView.show(binding.productsList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_products)

    // region Search configuration and events
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_product_selection_fragment, menu)

        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.product_search_hint)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                searchMenuItem?.setOnActionExpandListener(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
        productListViewModel.onSearchOpened()
        return true
    }

    override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
        productListViewModel.onSearchClosed()
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        TODO("Not yet implemented")
    }

    // endregion
}
