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
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.OnProductCategoryClickListener
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.ProductCategoryViewHolderModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.android.synthetic.main.fragment_product_categories_list.*
import org.wordpress.android.util.ActivityUtils

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
        val sortedList = viewModel.sortAndStyleProductCategories(product, productCategories)
        productCategoriesAdapter.setProductCategories(sortedList)
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
            it.id == productCategoryViewHolderModel.category.remoteId }

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
