package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUIModel
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.fragment_product_filter_list.*
import javax.inject.Inject

class ProductFilterListFragment : BaseFragment() {
    companion object {
        const val TAG = "ProductFilterListFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductFilterListViewModel by navGraphViewModels(
            R.id.nav_graph_product_filters
    ) { viewModelFactory }

    private lateinit var productFilterListAdapter: ProductFilterListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_filter_list, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_list_filters)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireContext()

        productFilterListAdapter = ProductFilterListAdapter(requireContext())
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
    }

    private fun setupObservers(viewModel: ProductFilterListViewModel) {
        viewModel.productFilterListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.filterList?.takeIfNotEqualTo(old?.filterList) {
                showProductFilterList(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun showProductFilterList(productFilterList: List<FilterListItemUIModel>) {
        productFilterListAdapter.setProductFilterList(productFilterList)
    }
}
