package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.filters.adapter.OrderFilterCategoryAdapter
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowFilterOptionsForCategory
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderFilterCategoriesFragment :
    BaseFragment(R.layout.fragment_order_filter_list),
    BackPressListener {
    companion object {
        const val KEY_UPDATED_FILTER_OPTIONS = "key_updated_filter_options"
    }

    private val viewModel: OrderFilterCategoriesViewModel by viewModels()

    lateinit var orderFilterCategoryAdapter: OrderFilterCategoryAdapter

    private var clearAllMenuItem: MenuItem? = null

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp,
            hasShadow = false,
            hasDivider = true
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentOrderFilterListBinding.bind(view)
        setHasOptionsMenu(true)
        setUpObservers(viewModel)

        setUpFiltersRecyclerView(binding)
        binding.showOrdersButton.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
        handleResult<OrderFilterCategoryUiModel>(KEY_UPDATED_FILTER_OPTIONS) {
            viewModel.onFilterOptionsUpdated(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_clear, menu)
        clearAllMenuItem = menu.findItem(R.id.menu_clear)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_clear -> {
                viewModel.onClearFilters()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpFiltersRecyclerView(binding: FragmentOrderFilterListBinding) {
        orderFilterCategoryAdapter = OrderFilterCategoryAdapter(
            onFilterCategoryClicked = { selectedFilterCategory ->
                viewModel.onFilterCategorySelected(selectedFilterCategory)
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

    private fun navigateToFilterOptions(category: OrderFilterCategoryUiModel) {
        val action = OrderFilterCategoriesFragmentDirections
            .actionOrderFilterListFragmentToOrderFilterOptionListFragment(category)
        findNavController().navigateSafely(action)
    }

    private fun setUpObservers(viewModel: OrderFilterCategoriesViewModel) {
        viewModel.categories.observe(viewLifecycleOwner) { _, newValue ->
            showOrderFilters(newValue.list)
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowFilterOptionsForCategory -> navigateToFilterOptions(event.category)
                is ShowDialog -> event.showDialog()
                is OnShowOrders -> navigateBackWithNotice(
                    OrderListFragment.FILTER_CHANGE_NOTICE_KEY
                )
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

    private fun showOrderFilters(orderFilters: List<OrderFilterCategoryUiModel>) {
        orderFilterCategoryAdapter.submitList(orderFilters)
    }

    override fun onRequestAllowBackPress() = viewModel.onBackPressed()
}
