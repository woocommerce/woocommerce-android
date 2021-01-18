package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentProductFilterListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductFilterListAdapter.OnProductFilterClickListener
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import javax.inject.Inject

class ProductFilterListFragment : BaseFragment(R.layout.fragment_product_filter_list),
    OnProductFilterClickListener,
    BackPressListener {
    companion object {
        const val TAG = "ProductFilterListFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductFilterListViewModel by navGraphViewModels(
        R.id.nav_graph_product_filters
    ) { viewModelFactory }

    private lateinit var productFilterListAdapter: ProductFilterListAdapter

    private var clearAllMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentProductFilterListBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)

        productFilterListAdapter = ProductFilterListAdapter(this)
        with(binding.filterList) {
            addItemDecoration(
                AlignedDividerDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL,
                    alignStartToStartOf = R.id.filterItemName
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
                Stat.PRODUCT_FILTER_LIST_SHOW_PRODUCTS_BUTTON_TAPPED,
                mapOf(AnalyticsTracker.KEY_FILTERS to viewModel.getFilterString())
            )
            viewModel.onShowProductsClicked()
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
                AnalyticsTracker.track(Stat.PRODUCT_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED)
                viewModel.onClearFilterSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers(viewModel: ProductFilterListViewModel) {
        viewModel.productFilterListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle.takeIfNotEqualTo(old?.screenTitle) { requireActivity().title = it }
            new.displayClearButton?.takeIfNotEqualTo(old?.displayClearButton) { showClearAllAction(it) }
        }
        viewModel.filterListItems.observe(viewLifecycleOwner, Observer {
            showProductFilterList(it)
        })
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                is ShowDialog -> event.showDialog()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(ProductListFragment.PRODUCT_FILTER_RESULT_KEY, event.data)
                }
                else -> event.isHandled = false
            }
        })

        viewModel.loadFilters()
    }

    private fun showProductFilterList(productFilterList: List<FilterListItemUiModel>) {
        productFilterListAdapter.filterList = productFilterList
    }

    private fun showClearAllAction(show: Boolean) {
        view?.post { clearAllMenuItem?.isVisible = show }
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
}
