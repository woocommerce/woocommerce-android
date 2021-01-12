package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentProductFilterOptionListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_STOCK_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_TYPE_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListOptionItemUiModel
import com.woocommerce.android.ui.products.ProductFilterOptionListAdapter.OnProductFilterOptionClickListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.Lazy
import javax.inject.Inject

class ProductFilterOptionListFragment : BaseFragment(R.layout.fragment_product_filter_option_list),
    OnProductFilterOptionClickListener {
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    private val viewModel: ProductFilterListViewModel by navGraphViewModels(
            R.id.nav_graph_product_filters
    ) { viewModelFactory.get() }

    private val arguments: ProductFilterOptionListFragmentArgs by navArgs()

    private lateinit var mProductFilterOptionListAdapter: ProductFilterOptionListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentProductFilterOptionListBinding.bind(view)
        setupObservers(viewModel)

        mProductFilterOptionListAdapter = ProductFilterOptionListAdapter(this)
        with(binding.filterOptionList) {
            addItemDecoration(
                    AlignedDividerDecoration(
                            requireActivity(),
                            DividerItemDecoration.VERTICAL,
                            alignStartToStartOf = R.id.filterOptionItem_name
                    )
            )
            layoutManager = LinearLayoutManager(activity)
            adapter = mProductFilterOptionListAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }

        binding.filterOptionListBtnShowProducts.setOnClickListener {
            AnalyticsTracker.track(
                    Stat.PRODUCT_FILTER_LIST_SHOW_PRODUCTS_BUTTON_TAPPED,
                    mapOf(AnalyticsTracker.KEY_FILTERS to viewModel.getFilterString())
            )
            val bundle = Bundle()
            bundle.putString(ARG_PRODUCT_FILTER_STOCK_STATUS, viewModel.getFilterByStockStatus())
            bundle.putString(ARG_PRODUCT_FILTER_STATUS, viewModel.getFilterByProductStatus())
            bundle.putString(ARG_PRODUCT_FILTER_TYPE_STATUS, viewModel.getFilterByProductType())
            requireActivity().navigateBackWithResult(
                    RequestCodes.PRODUCT_LIST_FILTERS,
                    bundle,
                    R.id.nav_host_fragment_main,
                    R.id.products
            )
        }
    }

    private fun setupObservers(viewModel: ProductFilterListViewModel) {
        viewModel.productFilterOptionListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle.takeIfNotEqualTo(old?.screenTitle) { requireActivity().title = it }
        }

        viewModel.filterOptionListItems.observe(viewLifecycleOwner, Observer {
            showProductFilterList(it)
        })

        viewModel.loadFilterOptions(arguments.selectedFilterItemPosition)
    }

    private fun showProductFilterList(productFilterOptionList: List<FilterListOptionItemUiModel>) {
        mProductFilterOptionListAdapter.filterList = productFilterOptionList
    }

    override fun onFilterOptionClick(selectedFilter: FilterListOptionItemUiModel) {
        viewModel.onFilterOptionItemSelected(arguments.selectedFilterItemPosition, selectedFilter)
    }
}
