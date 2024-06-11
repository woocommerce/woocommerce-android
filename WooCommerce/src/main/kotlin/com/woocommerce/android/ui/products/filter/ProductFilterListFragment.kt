package com.woocommerce.android.ui.products.filter

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductFilterListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.products.list.ProductListFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductFilterListFragment :
    BaseFragment(R.layout.fragment_product_filter_list),
    ProductFilterListAdapter.OnProductFilterClickListener,
    MainActivity.Companion.BackPressListener,
    MenuProvider {
    companion object {
        const val TAG = "ProductFilterListFragment"
    }

    private val viewModel: ProductFilterListViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_product_filters)

    private lateinit var productFilterListAdapter: ProductFilterListAdapter

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp,
            hasShadow = false,
            hasDivider = true
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentProductFilterListBinding.bind(view)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        setupObservers(viewModel)

        productFilterListAdapter = ProductFilterListAdapter(this)
        with(binding.filterList) {
            addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
            layoutManager = LinearLayoutManager(activity)
            adapter = productFilterListAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }

        binding.filterListBtnShowProducts.setOnClickListener {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_FILTER_LIST_SHOW_PRODUCTS_BUTTON_TAPPED,
                mapOf(AnalyticsTracker.KEY_FILTERS to viewModel.getFilterString())
            )
            viewModel.onShowProductsClicked()
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
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED)
                viewModel.onClearFilterSelected()
                updateClearButtonVisibility(item)
                true
            }
            else -> false
        }
    }

    private fun setupObservers(viewModel: ProductFilterListViewModel) {
        viewModel.productFilterListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle.takeIfNotEqualTo(old?.screenTitle) { requireActivity().title = it }
        }
        viewModel.filterListItems.observe(viewLifecycleOwner) {
            showProductFilterList(it)
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ShowDialog -> event.showDialog()
                is MultiLiveEvent.Event.ExitWithResult<*> -> {
                    navigateBackWithResult(ProductListFragment.PRODUCT_FILTER_RESULT_KEY, event.data)
                }
                else -> event.isHandled = false
            }
        }

        viewModel.loadFilters()
    }

    private fun showProductFilterList(productFilterList: List<ProductFilterListViewModel.FilterListItemUiModel>) {
        productFilterListAdapter.filterList = productFilterList
    }

    override fun onProductFilterClick(selectedFilterPosition: Int) {
        val action = ProductFilterListFragmentDirections
            .actionProductFilterListFragmentToProductFilterOptionListFragment(selectedFilterPosition)
        findNavController().navigateSafely(action)
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked()

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun updateClearButtonVisibility(clearMenuItem: MenuItem) {
        clearMenuItem.isVisible =
            viewModel.productFilterListViewStateData.liveData.value?.displayClearButton ?: false
    }
}
