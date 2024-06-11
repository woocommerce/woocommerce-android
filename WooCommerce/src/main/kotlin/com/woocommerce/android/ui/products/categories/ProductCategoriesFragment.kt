package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductCategoriesListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.AddProductCategoryFragment.Companion.ARG_CATEGORY_UPDATE_RESULT
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.CategoryUpdateResult
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.UpdateAction.Add
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.UpdateAction.Delete
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.UpdateAction.Update
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductCategoriesFragment :
    BaseProductFragment(R.layout.fragment_product_categories_list),
    OnLoadMoreListener,
    OnProductCategoryClickListener {
    private lateinit var productCategoriesAdapter: ProductCategoriesAdapter

    private val skeletonView = SkeletonView()

    private var _binding: FragmentProductCategoriesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView

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

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_categories),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductCategories)
                }

                toolbar.inflateMenu(R.menu.menu_search)
                searchMenuItem = toolbar.menu.findItem(R.id.menu_search)
                searchView = searchMenuItem.actionView as SearchView
                searchView.queryHint = getString(R.string.product_category_selector_search_hint)

                initSearchView()
            }
        )
    }

    private fun initSearchView() {
        viewModel.productCategoriesViewStateData.liveData.value?.let {
            if (it.isSearchOpen) {
                searchMenuItem.expandActionView()
                searchView.setQuery(it.searchQuery, false)
            } else {
                searchMenuItem.collapseActionView()
            }
        }

        val textQueryListener = object : OnQueryTextListener, SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (isAdded) {
                    viewModel.onProductCategorySearchQueryChanged(query.orEmpty())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (isAdded) {
                    viewModel.onProductCategorySearchQueryChanged(newText.orEmpty())
                }
                return true
            }
        }

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (isAdded) {
                    viewModel.onProductCategorySearchStateChanged(open = true)
                    searchView.setOnQueryTextListener(textQueryListener)
                }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (isAdded) {
                    searchView.setOnQueryTextListener(null)
                    viewModel.onProductCategorySearchStateChanged(open = false)
                }
                return true
            }
        })
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productCategoriesAdapter = ProductCategoriesAdapter(this, this)
        with(binding.productCategoriesRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productCategoriesAdapter
            addItemDecoration(
                AlignedDividerDecoration(
                    activity,
                    DividerItemDecoration.VERTICAL,
                    R.id.categoryName,
                    clipToMargin = false
                )
            )
        }

        binding.productCategoriesLayout.apply {
            scrollUpChild = binding.productCategoriesRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CATEGORIES_PULLED_TO_REFRESH)
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

        viewModel.productCategories.observe(viewLifecycleOwner) {
            showProductCategories(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitProductCategories -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun setupResultHandlers() {
        handleResult<CategoryUpdateResult>(ARG_CATEGORY_UPDATE_RESULT) { categoryUpdateResult ->
            when (categoryUpdateResult.action) {
                Add -> viewModel.onProductCategoryAdded(categoryUpdateResult.updatedCategory)
                Update -> viewModel.productCategoryEdited(categoryUpdateResult.updatedCategory)
                Delete -> viewModel.productCategoryDeleted(categoryUpdateResult.updatedCategory)
            }
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
        viewModel.onBackButtonClicked(ExitProductCategories)
        return false
    }

    override fun onProductCategoryChecked(productCategoryItemUiModel: ProductCategoryItemUiModel) {
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
        }
    }

    override fun onProductCategorySelected(productCategoryItemUiModel: ProductCategoryItemUiModel) {
        viewModel.onEditCategory(productCategoryItemUiModel.category)
    }
}
