package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListCategoryUiModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderFilterListFragment : BaseFragment(R.layout.fragment_order_filter_list) {

    private val viewModel: OrderFilterListViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_filters)

    lateinit var orderFilterCategoryAdapter: OrderFilterCategoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        val binding = FragmentOrderFilterListBinding.bind(view)
        setUpObservers(viewModel)
        setUpFiltersRecyclerView(binding)
        binding.filterListBtnShowOrders.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
    }

    private fun setUpFiltersRecyclerView(binding: FragmentOrderFilterListBinding) {
        orderFilterCategoryAdapter = OrderFilterCategoryAdapter(
            OrderFilterItemDiffCallBack(),
            onFilterCategoryClicked = { position ->
                navigateToFilterOptions(position)
            }
        )
        binding.filterList.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = orderFilterCategoryAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun navigateToFilterOptions(selectedFilterPosition: Int) {
        val action = OrderFilterListFragmentDirections
            .actionOrderFilterListFragmentToOrderFilterOptionListFragment(selectedFilterPosition)
        findNavController().navigateSafely(action)
    }

    private fun setUpObservers(viewModel: OrderFilterListViewModel) {
        viewModel.orderFilterCategories.observe(viewLifecycleOwner) {
            showOrderFilters(it)
        }
    }

    private fun showOrderFilters(orderFilters: List<FilterListCategoryUiModel>) {
        orderFilterCategoryAdapter.submitList(orderFilters)
    }
}
