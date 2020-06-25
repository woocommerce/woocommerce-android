package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.android.synthetic.main.fragment_product_categories_list.*
import javax.inject.Inject

class ParentCategoryListFragment : BaseFragment(), OnLoadMoreListener, OnProductCategoryClickListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: AddProductCategoryViewModel
        by navGraphViewModels(R.id.nav_graph_add_product_category) { viewModelFactory }

    private lateinit var parentCategoryListAdapter: ParentCategoryListAdapter

    private val skeletonView = SkeletonView()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_categories_list, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        viewModel.fetchParentCategories()
    }

    override fun getFragmentTitle() = getString(R.string.product_add_category)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        parentCategoryListAdapter = ParentCategoryListAdapter(
            activity.baseContext, viewModel.getSelectedParentId(), this, this
        )
        with(productCategoriesRecycler) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
            adapter = parentCategoryListAdapter
        }

        productCategoriesLayout?.apply {
            scrollUpChild = productCategoriesRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PARENT_CATEGORIES_PULLED_TO_REFRESH)
                viewModel.refreshParentCategories()
            }
        }
    }

    private fun setupObservers(viewModel: AddProductCategoryViewModel) {
        viewModel.parentCategoryListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productCategoriesLayout.isRefreshing = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    WooAnimUtils.fadeIn(empty_view)
                    empty_view.show(EmptyViewType.PRODUCT_CATEGORY_LIST)
                } else {
                    WooAnimUtils.fadeOut(empty_view)
                    empty_view.hide()
                }
            }
        }

        viewModel.parentCategories.observe(viewLifecycleOwner, Observer {
            showParentCategories(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    private fun showParentCategories(productCategories: List<ProductCategoryItemUiModel>) {
        parentCategoryListAdapter.parentCategoryList = productCategories
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productCategoriesRecycler, R.layout.skeleton_product_categories_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showLoadMoreProgress(show: Boolean) {
        loadMoreCategoriesProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreParentCategoriesRequested()
    }

    override fun onProductCategoryClick(productCategoryItemUiModel: ProductCategoryItemUiModel) {
        viewModel.onParentCategorySelected(productCategoryItemUiModel.category.remoteCategoryId)
    }
}
