package com.woocommerce.android.ui.orders.creation.variations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditProductSelectionBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductListAdapter
import com.woocommerce.android.ui.products.ProductSelectionItemKeyProvider
import com.woocommerce.android.ui.products.SelectableProductListItemLookup
import com.woocommerce.android.ui.products.variations.VariationListAdapter
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreateEditVariationSelectionFragment : BaseFragment(R.layout.fragment_order_create_edit_product_selection) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    private val viewModel by viewModels<OrderCreateEditVariationSelectionViewModel>()

    private val skeletonView = SkeletonView()

    private var tracker: SelectionTracker<Long>? = null

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreateEditProductSelectionBinding.bind(view)
        with(binding) {
            productsList.layoutManager = LinearLayoutManager(requireContext())
        }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentOrderCreateEditProductSelectionBinding) {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            screenTitle = state.parentProduct?.name ?: getString(R.string.order_creation_variations_screen_title)

            state.variationsList?.let { variations ->
                binding.bindVariationsList(variations, state.parentProduct)
            }

            binding.showSkeleton(state.isSkeletonShown)
        }
    }

    private fun FragmentOrderCreateEditProductSelectionBinding.bindVariationsList(
        variationsList: List<ProductVariation>,
        parentProduct: Product?
    ) {
        if (productsList.adapter == null) {
            productsList.adapter = VariationListAdapter(
                requireContext(),
                GlideApp.with(requireContext()),
                parentProduct = parentProduct,
                loadMoreListener = object : OnLoadMoreListener {
                    override fun onRequestLoadMore() {
                        viewModel.onLoadMore()
                    }
                },
                onItemClick = {
//                    sharedViewModel.onProductSelected(it.remoteProductId, it.remoteVariationId)
//                    findNavController().popBackStack(R.id.orderCreationFragment, false)

                    val variationProduct = sharedViewModel.selectedProducts.firstOrNull { products ->
                        products.first == it.remoteProductId
                    }
                    variationProduct?.let { productPair ->
                        if (productPair.second?.contains(it.remoteVariationId) == true) {
                            productPair.second?.remove(it.remoteVariationId)
                            if (productPair.second?.isNullOrEmpty() == true) {
                                sharedViewModel.selectedProducts.remove(productPair)
                            } else {

                            }
                        } else {
                            productPair.second?.add(it.remoteVariationId)
                        }
                    } ?: run {
                        sharedViewModel.selectedProducts.add(Pair(it.remoteProductId, mutableListOf(it.remoteVariationId)))
                    }
                }
            )
//            sharedViewModel.tracker = tracker
        }
        val newVariationsList = variationsList.apply {
            this.map { productVariation ->
                sharedViewModel.selectedProducts.filter {
                    it.second != null
                }.forEach {
                    it.second?.forEach { selectedVariationId ->
                        if (productVariation.remoteVariationId == selectedVariationId) {
                            productVariation.isSelected = true
                        }
                    }
                }
            }
        }
        (productsList.adapter as VariationListAdapter).submitList(newVariationsList)
    }

    private fun FragmentOrderCreateEditProductSelectionBinding.showSkeleton(
        show: Boolean
    ) {
        if (show) {
            skeletonView.show(productsList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun getFragmentTitle() = screenTitle
}
