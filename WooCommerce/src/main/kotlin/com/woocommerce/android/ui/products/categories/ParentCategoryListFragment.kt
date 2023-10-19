package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductCategoriesListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ParentCategoryListFragment :
    BaseFragment(R.layout.fragment_product_categories_list),
    OnLoadMoreListener,
    OnProductCategoryClickListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: AddProductCategoryViewModel by fixedHiltNavGraphViewModels(
        navGraphId = R.id.nav_graph_add_product_category
    )

    private lateinit var parentCategoryListAdapter: ParentCategoryListAdapter

    private val skeletonView = SkeletonView()

    private var _binding: FragmentProductCategoriesListBinding? = null
    private val binding get() = _binding!!

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductCategoriesListBinding.bind(view)

        setupObservers(viewModel)
        viewModel.fetchParentCategories()
    }

    override fun getFragmentTitle() = getString(R.string.product_add_category)

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        parentCategoryListAdapter = ParentCategoryListAdapter(
            viewModel.getSelectedParentId(), this, this
        )
        with(binding.productCategoriesRecycler) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
            adapter = parentCategoryListAdapter
        }

        binding.productCategoriesLayout.apply {
            scrollUpChild = binding.productCategoriesRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.PARENT_CATEGORIES_PULLED_TO_REFRESH)
                viewModel.refreshParentCategories()
            }
        }
    }

    private fun setupObservers(viewModel: AddProductCategoryViewModel) {
        viewModel.parentCategoryListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { binding.productCategoriesLayout.isRefreshing = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    WooAnimUtils.fadeIn(binding.emptyView)
                    binding.emptyView.show(EmptyViewType.PRODUCT_CATEGORY_LIST)
                } else {
                    WooAnimUtils.fadeOut(binding.emptyView)
                    binding.emptyView.hide()
                }
            }
        }

        viewModel.parentCategories.observe(viewLifecycleOwner) {
            showParentCategories(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun showParentCategories(productCategories: List<ProductCategoryItemUiModel>) {
        parentCategoryListAdapter.submitList(productCategories)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(
                binding.productCategoriesRecycler,
                R.layout.skeleton_product_categories_list,
                delayed = true
            )
        } else {
            skeletonView.hide()
        }
    }

    private fun showLoadMoreProgress(show: Boolean) {
        binding.loadMoreCategoriesProgress.isVisible = show
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreParentCategoriesRequested()
    }

    override fun onProductCategoryClick(productCategoryItemUiModel: ProductCategoryItemUiModel) {
        viewModel.onParentCategorySelected(productCategoryItemUiModel.category.remoteCategoryId)
    }
}
