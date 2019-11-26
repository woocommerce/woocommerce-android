package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.ProductListAdapter.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductListAdapter.OnProductClickListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_list.*
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class ProductListFragment : TopLevelFragment(), OnProductClickListener,
        OnLoadMoreListener,
        OnQueryTextListener,
        OnActionExpandListener {
    companion object {
        val TAG: String = ProductListFragment::class.java.simpleName
        fun newInstance() = ProductListFragment()
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var productAdapter: ProductListAdapter

    private val viewModel: ProductListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productAdapter = ProductListAdapter(activity, this, this)
        productsRecycler.layoutManager = LinearLayoutManager(activity)
        productsRecycler.adapter = productAdapter
        productsRecycler.addItemDecoration(
                AlignedDividerDecoration(
                        activity,
                        DividerItemDecoration.VERTICAL, R.id.productName, clipToMargin = false
                )
        )

        productsRefreshLayout?.apply {
            setColorSchemeColors(
                    ContextCompat.getColor(activity, R.color.colorPrimary),
                    ContextCompat.getColor(activity, R.color.colorAccent),
                    ContextCompat.getColor(activity, R.color.colorPrimaryDark)
            )
            scrollUpChild = scroll_view
            setOnRefreshListener {
                viewModel.onRefreshRequested()
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        disableSearchListeners()
        searchView = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (hidden) {
            disableSearchListeners()
        } else {
            enableSearchListeners()
        }
    }

    override fun onReturnedFromChildFragment() {
        showOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_product_list_fragment, menu)

        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.product_search_hint)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        refreshOptionsMenu()
        super.onPrepareOptionsMenu(menu)
    }

    private fun showOptionsMenu(show: Boolean) {
        setHasOptionsMenu(show)
        if (show) {
            refreshOptionsMenu()
        }
    }

    /**
     * Use this rather than invalidateOptionsMenu() since that collapses the search menu item
     */
    private fun refreshOptionsMenu() {
        val showSearch = shouldShowSearchMenuItem()
        searchMenuItem?.let { menuItem ->
            if (menuItem.isVisible != showSearch) menuItem.isVisible = showSearch

            val isSearchActive = viewModel.viewStateLiveData.liveData.value?.isSearchActive == true
            if (menuItem.isActionViewExpanded != isSearchActive) {
                disableSearchListeners()
                if (isSearchActive) {
                    menuItem.expandActionView()
                    searchView?.setQuery(viewModel.viewStateLiveData.liveData.value?.query, false)
                } else {
                    menuItem.collapseActionView()
                }
                enableSearchListeners()
            }
        }
    }

    /**
     * Prevent search from appearing when a child fragment is active
     */
    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        return !isChildShowing
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(Stat.PRODUCT_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun closeSearchView() {
        disableSearchListeners()
        updateActivityTitle()
        searchMenuItem?.collapseActionView()
    }

    private fun disableSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
    }

    private fun enableSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel.onSearchRequested()
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.onSearchQueryChanged(newText)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        viewModel.onSearchOpened()
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        viewModel.onSearchClosed()
        closeSearchView()
        return true
    }

    private fun setupObservers(viewModel: ProductListViewModel) {
        viewModel.viewStateLiveData.observe(this) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.productList?.takeIfNotEqualTo(old?.productList) { showProductList(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productsRefreshLayout.isRefreshing = it }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    val message: Int
                    val showImage: Boolean
                    if (new.isSearchActive == true) {
                        message = R.string.product_list_empty_search
                        showImage = false
                    } else {
                        message = R.string.product_list_empty
                        showImage = !DisplayUtils.isLandscape(activity)
                    }

                    empty_view.show(message, showImage)
                } else {
                    empty_view.hide()
                }
            }
        }

        viewModel.event.observe(this, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        })
    }

    override fun getFragmentTitle() = getString(R.string.products)

    override fun refreshFragmentState() {
        if (isActive) {
            viewModel.refreshProducts()
        }
    }

    override fun scrollToTop() {
        productsRecycler.smoothScrollToPosition(0)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            showProductWIPNoticeCard(false)
            skeletonView.show(productsRecycler, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showLoadMoreProgress(show: Boolean) {
        loadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showProductList(products: List<Product>) {
        productAdapter.setProductList(products)
        showProductWIPNoticeCard(true)
    }

    private fun showProductWIPNoticeCard(show: Boolean) {
        if (show) {
            products_wip_card.visibility = View.VISIBLE
            products_wip_card.initView()
        } else {
            products_wip_card.visibility = View.GONE
        }
    }

    override fun onProductClick(remoteProductId: Long) {
        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested()
    }
}
