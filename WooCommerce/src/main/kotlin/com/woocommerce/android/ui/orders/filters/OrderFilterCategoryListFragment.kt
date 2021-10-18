package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListCategoryUiModel
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.OrderFilterListEvent.ShowOrderStatusFilterOptions
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderFilterCategoryListFragment :
    BaseFragment(R.layout.fragment_order_filter_list),
    BackPressListener {

    private val viewModel: OrderFilterListViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_filters)

    lateinit var orderFilterCategoryAdapter: OrderFilterCategoryAdapter

    private var clearAllMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentOrderFilterListBinding.bind(view)
        setHasOptionsMenu(true)
        setUpObservers(viewModel)

        setUpFiltersRecyclerView(binding)
        binding.filterListBtnShowOrders.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_clear, menu)
        clearAllMenuItem = menu.findItem(R.id.menu_clear)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_clear -> {
                viewModel.onClearFilterSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpFiltersRecyclerView(binding: FragmentOrderFilterListBinding) {
        orderFilterCategoryAdapter = OrderFilterCategoryAdapter(
            onFilterCategoryClicked = { selectedFilterCategory ->
                viewModel.onFilterCategoryClicked(selectedFilterCategory)
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

    private fun navigateToFilterOptions() {
        val action = OrderFilterCategoryListFragmentDirections
            .actionOrderFilterListFragmentToOrderFilterOptionListFragment()
        findNavController().navigateSafely(action)
    }

    private fun setUpObservers(viewModel: OrderFilterListViewModel) {
        viewModel.orderFilterCategories.observe(viewLifecycleOwner) {
            showOrderFilters(it)
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowOrderStatusFilterOptions -> navigateToFilterOptions()
                is ShowDialog -> event.showDialog()
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
        viewModel.orderFilterCategoryViewState.observe(viewLifecycleOwner) { viewState ->
            requireActivity().title = viewState.screenTitle
            showClearAllAction(viewState.displayClearButton)
        }
    }

    private fun showClearAllAction(show: Boolean) {
        view?.post { clearAllMenuItem?.isVisible = show }
    }

    private fun showOrderFilters(orderFilters: List<FilterListCategoryUiModel>) {
        orderFilterCategoryAdapter.submitList(orderFilters)
    }

    override fun onRequestAllowBackPress() = viewModel.onBackPressed()
}
