package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentProductCategoriesListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.ui.products.categories.AddProductCategoryFragment.Companion.ARG_ADDED_CATEGORY
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType

class ProductCategoriesFragment : BaseProductFragment(R.layout.fragment_product_categories_list),
    OnLoadMoreListener, OnProductCategoryClickListener {
    private lateinit var productCategoriesAdapter: ProductCategoriesAdapter

    private val skeletonView = SkeletonView()

    private var _binding: FragmentProductCategoriesListBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = getString(R.string.product_categories)

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
        setupResultHandlers()
        viewModel.fetchProductCategories()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productCategoriesAdapter = ProductCategoriesAdapter(activity.baseContext, this, this)
        with(binding.productCategoriesRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productCategoriesAdapter
            addItemDecoration(
                AlignedDividerDecoration(
                activity, DividerItemDecoration.VERTICAL, R.id.categoryName, clipToMargin = false
            )
            )
        }

        binding.productCategoriesLayout.apply {
            scrollUpChild = binding.productCategoriesRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_CATEGORIES_PULLED_TO_REFRESH)
                viewModel.refreshProductCategories()
            }
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productCategoriesViewStateData.observe(viewLifecycleOwner) { old, new ->
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
            new.isAddCategoryButtonVisible.takeIfNotEqualTo(old?.isAddCategoryButtonVisible) {
                showAddCategoryButton(it)
            }
        }

        viewModel.productCategories.observe(viewLifecycleOwner, Observer {
            showProductCategories(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductCategories -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    private fun setupResultHandlers() {
        handleResult<ProductCategory>(ARG_ADDED_CATEGORY) { category ->
            viewModel.onProductCategoryAdded(category)
            changesMade()
        }
    }

    private fun showProductCategories(productCategories: List<ProductCategory>) {
        val product = requireNotNull(viewModel.getProduct().productDraft)
        val sortedList = viewModel.sortAndStyleProductCategories(product, productCategories)
        productCategoriesAdapter.setProductCategories(sortedList)
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

    private fun showAddCategoryButton(show: Boolean) {
        with(binding.addProductCategoryView) {
            isVisible = show
            initView { viewModel.onAddCategoryButtonClicked() }
        }
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreCategoriesRequested()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductCategories())
    }

    override fun onProductCategoryClick(productCategoryItemUiModel: ProductCategoryItemUiModel) {
        val product = requireNotNull(viewModel.getProduct().productDraft)
        val selectedCategories = product.categories.toMutableList()

        val found = selectedCategories.find {
            it.remoteCategoryId == productCategoryItemUiModel.category.remoteCategoryId
        }

        var changeRequired = false
        if (!productCategoryItemUiModel.isSelected && found != null) {
            selectedCategories.remove(found)
            changeRequired = true
        } else if (productCategoryItemUiModel.isSelected && found == null) {
            selectedCategories.add(productCategoryItemUiModel.category.toProductCategory())
            changeRequired = true
        }

        if (changeRequired) {
            viewModel.updateProductDraft(categories = selectedCategories)
            changesMade()
        }
    }
}
