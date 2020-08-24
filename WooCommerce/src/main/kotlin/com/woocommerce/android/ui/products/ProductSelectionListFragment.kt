package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_list.*
import javax.inject.Inject

class ProductSelectionListFragment : BaseFragment(), OnLoadMoreListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductSelectionListViewModel by viewModels { viewModelFactory }

    private var tracker: SelectionTracker<Long>? = null
    private val productSelectionListAdapter: ProductListAdapter by lazy {
        ProductListAdapter(loadMoreListener = this)
    }

    private val skeletonView = SkeletonView()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        with(productsRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productSelectionListAdapter
            isMotionEventSplittingEnabled = false
        }

        tracker = SelectionTracker.Builder(
            "mySelection",
            productsRecycler,
            ProductSelectionItemKeyProvider(productsRecycler),
            ProductSelectionListItemLookup(productsRecycler),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        if (savedInstanceState != null) {
            tracker?.onRestoreInstanceState(savedInstanceState)
        }

        productSelectionListAdapter.tracker = tracker
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    private fun setupObservers(viewModel: ProductSelectionListViewModel) {
        viewModel.productSelectionListViewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { loadMoreProgress.isVisible = it }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productsRefreshLayout.isRefreshing = it }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isVisible ->
                if (isVisible) {
                    empty_view.show(EmptyViewType.PRODUCT_LIST)
                } else {
                    empty_view.hide()
                }
            }
        }

        viewModel.productList.observe(viewLifecycleOwner, Observer {
            showProductList(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        })
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productsRecycler, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProductList(productSelectionList: List<Product>) {
        productSelectionListAdapter.setProductList(productSelectionList)
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested()
    }
}
