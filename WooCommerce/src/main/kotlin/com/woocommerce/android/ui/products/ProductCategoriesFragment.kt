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
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_CATEGORIES_ADD_CATEGORY_TAPPED
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.OnProductCategoryClickListener
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.ProductCategoryViewHolderModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitCategories
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

    companion object {
        const val DEFAULT_CATEGORY_PADDING = 32
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
        addCategoryButton.setOnClickListener {
            navgiateToAddCategoryFragment()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productCategoriesAdapter = ProductCategoriesAdapter(activity, this, this)
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

    fun navgiateToAddCategoryFragment() {
        AnalyticsTracker.track(PRODUCT_DETAIL_VIEW_CATEGORIES_ADD_CATEGORY_TAPPED)
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
                viewModel.onDoneButtonClicked(ExitCategories(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productCategoriesViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productCategoriesLayout.isRefreshing = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    WooAnimUtils.fadeIn(empty_view)
                    empty_view.show(EmptyViewType.CATEGORY_LIST)
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
                is ExitCategories -> findNavController().navigateUp()
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
            parentChildMap[category.remoteId] = category.parent
        }

        // Sort all incoming categories by their parent
        val sortedList = sortCategoriesByParent(productCategories)

        // Update the indent of the category if it's a child
        for (categoryViewHolderModel in sortedList) {
            categoryViewHolderModel.padding = computePadding(parentChildMap, categoryViewHolderModel.category)
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
        stack.addAll(productCategories.filter { it.parent == 0L })

        // Go through the nodes until we've finished DFS
        while (stack.isNotEmpty()) {
            val category = stack.pop()
            // Do not revisit a category
            if (!visited.contains(category.remoteId)) {
                visited.add(category.remoteId)
                sortedList.add(ProductCategoryViewHolderModel(category))
            }

            // Find all children of the node from the main category list
            val children = productCategories.filter { it.parent == category.remoteId }
            if (!children.isNullOrEmpty()) {
                stack.addAll(children)
            }
        }
        return sortedList
    }

    /**
     * Computes the padding for the category name according to its parent
     *
     * @param hierarchy the map of parent to child relationship
     * @param category the category for which the padding is being calculated
     *
     * @return Int the computed padding
     */
    private fun computePadding(hierarchy: Map<Long, Long>, category: ProductCategory): Int {
        var indent = 0
        var parent = category.parent ?: 0L
        while (parent != 0L) {
            indent += DEFAULT_CATEGORY_PADDING
            parent = hierarchy[parent] ?: 0L
        }
        return indent
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
        return viewModel.onBackButtonClicked(ExitCategories())
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreCategoriesRequested()
    }

    override fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel) {
        val product = requireNotNull(viewModel.getProduct().productDraft)
        val selectedCategories = product.categories.toMutableList()

        val found = selectedCategories.find {
            it.id == productCategoryViewHolderModel.category.remoteId &&
                    it.name == productCategoryViewHolderModel.category.name }
        if (!productCategoryViewHolderModel.isSelected && found != null) {
            selectedCategories.remove(found)
            viewModel.updateProductDraft(categories = selectedCategories)
        } else if (productCategoryViewHolderModel.isSelected && found == null) {
            selectedCategories.add(productCategoryViewHolderModel.category.toCategory())
            viewModel.updateProductDraft(categories = selectedCategories)
        }
    }
}
