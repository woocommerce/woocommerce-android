package com.woocommerce.android.ui.orders.creation.variations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationProductSelectionBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.VariationListAdapter

class OrderCreationVariationSelectionFragment : Fragment(R.layout.fragment_order_creation_product_selection) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val viewModel by viewModels<OrderCreationVariationSelectionViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreationProductSelectionBinding.bind(view)
        with(binding) {
            productsList.layoutManager = LinearLayoutManager(requireContext())
        }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentOrderCreationProductSelectionBinding) {
        viewModel.viewState.observe(viewLifecycleOwner) {
            if (binding.productsList.adapter == null) {
                binding.productsList.adapter = VariationListAdapter(
                    requireContext(),
                    GlideApp.with(requireContext()),
                    parentProduct = it.parentProduct,
                    loadMoreListener = object : OnLoadMoreListener {
                        override fun onRequestLoadMore() {
                            viewModel.onLoadMore()
                        }
                    },
                    onItemClick = {
                        // TODO
                    }
                )
            }
            (binding.productsList.adapter as VariationListAdapter).setVariationList(it.variationsList)
        }
    }
}
