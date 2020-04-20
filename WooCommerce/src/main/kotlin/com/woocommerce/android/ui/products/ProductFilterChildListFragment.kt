package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductFilterChildListAdapter.OnProductFilterChildClickListener
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListChildItemUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.fragment_product_filter_child_list.*
import javax.inject.Inject

class ProductFilterChildListFragment : BaseFragment(), OnProductFilterChildClickListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductFilterListViewModel by navGraphViewModels(
            R.id.nav_graph_product_filters
    ) { viewModelFactory }

    private val arguments: ProductFilterChildListFragmentArgs by navArgs()

    private lateinit var productFilterChildListAdapter: ProductFilterChildListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_filter_child_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        productFilterChildListAdapter = ProductFilterChildListAdapter(this)
        with(filterChildList) {
            addItemDecoration(
                    AlignedDividerDecoration(
                            requireActivity(),
                            DividerItemDecoration.VERTICAL,
                            alignStartToStartOf = R.id.filterItemName
                    )
            )
            layoutManager = LinearLayoutManager(activity)
            adapter = productFilterChildListAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }
    }

    private fun setupObservers(viewModel: ProductFilterListViewModel) {
        viewModel.productFilterChildListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle.takeIfNotEqualTo(old?.screenTitle) { requireActivity().title = it }
        }

        viewModel.filterChildListItems.observe(viewLifecycleOwner, Observer {
            showProductFilterList(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Exit -> requireActivity().onBackPressed()
                else -> event.isHandled = false
            }
        })

        viewModel.loadChildFilters(arguments.selectedFilterItemPosition)
    }

    private fun showProductFilterList(productFilterChildList: List<FilterListChildItemUiModel>) {
        productFilterChildListAdapter.filterList = productFilterChildList
    }

    override fun onChildFilterItemClick(selectedFilter: FilterListChildItemUiModel) {
        viewModel.onChildFilterItemSelected(arguments.selectedFilterItemPosition, selectedFilter)
    }
}
