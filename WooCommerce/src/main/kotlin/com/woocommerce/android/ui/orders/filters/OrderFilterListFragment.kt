package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListItemUiModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderFilterListFragment :
    BaseFragment(R.layout.fragment_order_filter_list) {

    private val viewModel: OrderFilterListViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_filters)

    @Inject lateinit var orderFilterListAdapter: OrderFilterListAdapter

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
        binding.filterList.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = orderFilterListAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun setUpObservers(viewModel: OrderFilterListViewModel) {
        viewModel.filterListItems.observe(viewLifecycleOwner) {
            showOrderFilters(it)
        }
    }

    private fun showOrderFilters(orderFilters: List<FilterListItemUiModel>) {
        orderFilterListAdapter.submitList(orderFilters)
    }
}
