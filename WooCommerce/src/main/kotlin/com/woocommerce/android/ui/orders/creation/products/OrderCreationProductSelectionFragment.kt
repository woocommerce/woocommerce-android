package com.woocommerce.android.ui.orders.creation.products

import android.os.Bundle
import android.view.View
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
    OnLoadMoreListener {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val productListViewModel by viewModels<OrderCreationProductSelectionViewModel>()

    private val skeletonView = SkeletonView()

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
        productsList.adapter = ProductListAdapter(
            clickListener = { id, _ -> productListViewModel.onProductSelected(id) },
            loadMoreListener = this@OrderCreationProductSelectionFragment
        ).apply { setProductList(products) }
    }

    override fun onRequestLoadMore() {
        productListViewModel.fetchProductList(loadMore = true)
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
}
