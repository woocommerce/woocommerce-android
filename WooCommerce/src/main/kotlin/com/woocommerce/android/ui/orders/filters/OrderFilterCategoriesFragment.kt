package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
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
    BackPressListener,
    MenuProvider {
    companion object {
        const val KEY_UPDATED_FILTER_OPTIONS = "key_updated_filter_options"
    }

    private val viewModel: OrderFilterCategoriesViewModel by viewModels()

    private lateinit var orderFilterCategoryAdapter: OrderFilterCategoryAdapter

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp,
            hasShadow = false,
            hasDivider = true
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentOrderFilterListBinding.bind(view)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        setUpObservers(viewModel)

        setUpFiltersRecyclerView(binding)
        binding.showOrdersButton.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
        handleResult<OrderFilterCategoryUiModel>(KEY_UPDATED_FILTER_OPTIONS) {
            viewModel.onFilterOptionsUpdated(it)
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_clear, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        updateClearButtonVisibility(menu.findItem(R.id.menu_clear))
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_clear -> {
                viewModel.onClearFilters()
                updateClearButtonVisibility(item)
                true
            }
            else -> false
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
        }
    }

    private fun showOrderFilters(orderFilters: List<OrderFilterCategoryUiModel>) {
        orderFilterCategoryAdapter.submitList(orderFilters)
    }

    override fun onRequestAllowBackPress() = viewModel.onBackPressed()

    private fun updateClearButtonVisibility(clearMenuItem: MenuItem) {
        clearMenuItem.isVisible =
            viewModel.orderFilterCategoryViewState.value?.displayClearButton ?: false
    }
}
