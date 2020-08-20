package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductFilterListAdapter.OnProductFilterClickListener
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_STOCK_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_TYPE_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.fragment_product_filter_list.*
import javax.inject.Inject

class ProductFilterListFragment : BaseFragment(), OnProductFilterClickListener, BackPressListener {
    companion object {
        const val TAG = "ProductFilterListFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductFilterListViewModel by navGraphViewModels(
            R.id.nav_graph_product_filters
    ) { viewModelFactory }

    private lateinit var productFilterListAdapter: ProductFilterListAdapter

    private var clearAllMenuItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_filter_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        productFilterListAdapter = ProductFilterListAdapter(this)
        with(filterList) {
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

        filterList_btnShowProducts.setOnClickListener {
            AnalyticsTracker.track(Stat.PRODUCT_FILTER_LIST_SHOW_PRODUCTS_BUTTON_TAPPED,
                    mapOf(AnalyticsTracker.KEY_FILTERS to viewModel.getFilterString()))
            val bundle = Bundle()
            bundle.putString(ARG_PRODUCT_FILTER_STOCK_STATUS, viewModel.getFilterByStockStatus())
            bundle.putString(ARG_PRODUCT_FILTER_STATUS, viewModel.getFilterByProductStatus())
            bundle.putString(ARG_PRODUCT_FILTER_TYPE_STATUS, viewModel.getFilterByProductType())
            (requireActivity() as? MainActivity)?.navigateBackWithResult(
                    RequestCodes.PRODUCT_LIST_FILTERS,
                    bundle,
                    R.id.nav_host_fragment_main,
                    R.id.rootFragment
            )
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
