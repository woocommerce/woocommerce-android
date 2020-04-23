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
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductFilterListAdapter.OnProductFilterClickListener
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUiModel
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.fragment_product_filter_list.*
import javax.inject.Inject

class ProductFilterListFragment : BaseFragment(), OnProductFilterClickListener {
    companion object {
        const val TAG = "ProductFilterListFragment"
        const val ARG_PRODUCT_FILTER_REQUEST_CODE = "arg_product_filter_request_code"
        const val ARG_PRODUCT_FILTER_STOCK_STATUS = "arg_product_filter_stock_status"
        const val ARG_PRODUCT_FILTER_STATUS = "arg_product_filter_status"
        const val ARG_PRODUCT_FILTER_TYPE_STATUS = "arg_product_type"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductFilterListViewModel by navGraphViewModels(
            R.id.nav_graph_product_filters
    ) { viewModelFactory }

    private lateinit var productFilterListAdapter: ProductFilterListAdapter

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
            // TODO: add tracking event
            val bundle = Bundle()
            bundle.putInt(ARG_PRODUCT_FILTER_REQUEST_CODE, RequestCodes.PRODUCT_LIST_FILTERS)
            bundle.putString(ARG_PRODUCT_FILTER_STOCK_STATUS, viewModel.getFilterByStockStatus())
            bundle.putString(ARG_PRODUCT_FILTER_STATUS, viewModel.getFilterByProductStatus())
            bundle.putString(ARG_PRODUCT_FILTER_TYPE_STATUS, viewModel.getFilterByProductType())
            findNavController().navigate(R.id.rootFragment, bundle)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_clear, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_clear -> {
                // TODO: add tracking event
                viewModel.onClearFilterSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers(viewModel: ProductFilterListViewModel) {
        viewModel.productFilterListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle.takeIfNotEqualTo(old?.screenTitle) { requireActivity().title = it }
        }
        viewModel.filterListItems.observe(viewLifecycleOwner, Observer {
            showProductFilterList(it)
        })
        viewModel.loadFilters()
    }

    private fun showProductFilterList(productFilterList: List<FilterListItemUiModel>) {
        productFilterListAdapter.filterList = productFilterList
    }

    override fun onProductFilterClick(selectedFilterPosition: Int) {
        val action = ProductFilterListFragmentDirections
                .actionProductFilterListFragmentToProductFilterOptionListFragment(selectedFilterPosition)
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
