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
import com.woocommerce.android.ui.products.ProductListViewModel.ShowSnackbar
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
        private const val KEY_SEARCH_ACTIVE = "search_active"
        private const val KEY_SEARCH_QUERY = "search_query"
        fun newInstance() = ProductListFragment()
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var productAdapter: ProductListAdapter

    private val viewModel: ProductListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var isSearchActive: Boolean = false
    private var searchQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { bundle ->
            isSearchActive = bundle.getBoolean(KEY_SEARCH_ACTIVE)
            searchQuery = bundle.getString(KEY_SEARCH_QUERY)
        }
    }

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
            scrollUpChild = productsRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_LIST_PULLED_TO_REFRESH)
                viewModel.refreshProducts(searchQuery)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SEARCH_ACTIVE, isSearchActive)
        outState.putString(KEY_SEARCH_QUERY, searchQuery)
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

        initializeViewModel()
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

            if (menuItem.isActionViewExpanded != isSearchActive) {
                disableSearchListeners()
                if (isSearchActive) {
                    menuItem.expandActionView()
                    searchView?.setQuery(searchQuery, false)
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

    private fun clearSearchResults() {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = null
            disableSearchListeners()
            updateActivityTitle()
            searchMenuItem?.collapseActionView()
        }
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
        submitSearch(query)
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.length > 2) {
            submitSearch(newText)
        } else {
            productAdapter.clearAdapterData()
        }
        showEmptyView(false)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        productAdapter.clearAdapterData()
        isSearchActive = true
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        clearSearchResults()
        viewModel.loadProducts()
        return true
    }

    private fun submitSearch(query: String) {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_SEARCHED,
                mapOf(AnalyticsTracker.KEY_SEARCH to query)
        )
        viewModel.loadProducts(searchQuery = query)
        searchQuery = query
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(searchQuery)
    }

    private fun setupObservers(viewModel: ProductListViewModel) {
        viewModel.viewStateLiveData.observe(this) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.productList?.takeIfNotEqualTo(old?.productList) { showProductList(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productsRefreshLayout.isRefreshing = it }
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
            viewModel.refreshProducts(searchQuery)
        }
    }

    override fun scrollToTop() {
        productsRecycler.smoothScrollToPosition(0)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productsRecycler, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showLoadMoreProgress(show: Boolean) {
        loadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(show: Boolean) {
        if (show) {
            if (isSearchActive) {
                empty_view.show(R.string.product_list_empty_search, false)
            } else {
                val showImage = !DisplayUtils.isLandscape(activity)
                empty_view.show(R.string.product_list_empty, showImage)
            }
        } else {
            empty_view.hide()
        }
    }

    private fun showProductList(products: List<Product>) {
        productAdapter.setProductList(products)
        showEmptyView(products.isEmpty())
    }

    override fun onProductClick(remoteProductId: Long) {
        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun onRequestLoadMore() {
        viewModel.loadProducts(loadMore = true, searchQuery = searchQuery)
    }
}
