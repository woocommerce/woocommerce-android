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
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.Companion.DEFAULT_CATEGORY_MARGIN
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.OnProductCategoryClickListener
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.ProductCategoryViewHolderModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.android.synthetic.main.fragment_product_categories_list.*
import org.wordpress.android.util.ActivityUtils
import java.util.Stack

class ProductCategoriesFragment : BaseProductFragment(), OnLoadMoreListener, OnProductCategoryClickListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_categories_list, container, false)
    }

    private lateinit var productCategoriesAdapter: ProductCategoriesAdapter

    private val skeletonView = SkeletonView()

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
        viewModel.initialiseCategories()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productCategoriesAdapter = ProductCategoriesAdapter(activity.baseContext, this, this)
        with(productCategoriesRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productCategoriesAdapter
        }

        productCategoriesLayout?.apply {
            scrollUpChild = productCategoriesRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_CATEGORIES_PULLED_TO_REFRESH)
                viewModel.refreshProductCategories()
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_categories)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(ExitProductCategories(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_done)?.isVisible = viewModel.hasCategoryChanges()
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productCategoriesViewStateData.observe(viewLifecycleOwner) { old, new ->
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

    private fun showProductCategories(productCategories: List<ProductCategory>) {
        val product = requireNotNull(viewModel.getProduct().productDraft)

        // Get the categories of the product
        val selectedCategories = product.categories
        val parentChildMap = mutableMapOf<Long, Long>()

        // Build a parent child relationship table
        for (category in productCategories) {
            parentChildMap[category.remoteId] = category.parentId
        }

        // Sort all incoming categories by their parent
        val sortedList =
                sortCategoriesByParent(productCategories.sortedByDescending { it.name })

        // Update the margin of the category
        for (categoryViewHolderModel in sortedList) {
            categoryViewHolderModel.margin = computeCascadingMargin(parentChildMap, categoryViewHolderModel.category)
        }

        // Mark the product categories as selected in the sorted list
        for (productCategoryViewHolderModel in sortedList) {
            for (selectedCategory in selectedCategories) {
                if (productCategoryViewHolderModel.category.remoteId == selectedCategory.id &&
                        productCategoryViewHolderModel.category.name == selectedCategory.name)
                    productCategoryViewHolderModel.isSelected = true
            }
        }

        productCategoriesAdapter.setProductCategories(sortedList.toList())
    }

    /**
     * The method does a Depth First Traversal of the Product Categories retrieved from the server
     *
     * @param productCategories All the categories retrieved from the server
     * @return [Set<ProductCategoryViewHolderModel>] a sorted set of view holder models containing category data
     */
    private fun sortCategoriesByParent(
        productCategories: List<ProductCategory>
    ): Set<ProductCategoryViewHolderModel> {
        val sortedList = mutableSetOf<ProductCategoryViewHolderModel>()
        val stack = Stack<ProductCategory>()
        val visited = mutableSetOf<Long>()

        // add root nodes to the Stack
        stack.addAll(productCategories.filter { it.parentId == 0L })

        // Go through the nodes until we've finished DFS
        while (stack.isNotEmpty()) {
            val category = stack.pop()
            // Do not revisit a category
            if (!visited.contains(category.remoteId)) {
                visited.add(category.remoteId)
                sortedList.add(ProductCategoryViewHolderModel(category))
            }

            // Find all children of the node from the main category list
            val children = productCategories.filter { it.parentId == category.remoteId }
            if (!children.isNullOrEmpty()) {
                stack.addAll(children)
            }
        }
        return sortedList
    }

    /**
     * Computes the cascading margin for the category name according to its parent
     *
     * @param hierarchy the map of parent to child relationship
     * @param category the category for which the padding is being calculated
     *
     * @return Int the computed margin
     */
    private fun computeCascadingMargin(hierarchy: Map<Long, Long>, category: ProductCategory): Int {
        var margin = DEFAULT_CATEGORY_MARGIN
        var parent = category.parentId
        while (parent != 0L) {
            margin += DEFAULT_CATEGORY_MARGIN
            parent = hierarchy[parent] ?: 0L
        }
        return margin
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

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductCategories())
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreCategoriesRequested()
    }

    override fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel) {
        val product = requireNotNull(viewModel.getProduct().productDraft)
        val selectedCategories = product.categories.toMutableList()

        val found = selectedCategories.find {
            it.id == productCategoryViewHolderModel.category.remoteId  }

        var changeRequired = false
        if (!productCategoryViewHolderModel.isSelected && found != null) {
            selectedCategories.remove(found)
            changeRequired = true
        } else if (productCategoryViewHolderModel.isSelected && found == null) {
            selectedCategories.add(productCategoryViewHolderModel.category.toCategory())
            changeRequired = true
        }

        if (changeRequired) {
            viewModel.updateProductDraft(categories = selectedCategories)
            activity?.invalidateOptionsMenu()
        }
    }
}
