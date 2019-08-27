package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.os.Handler
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.ProductListAdapter.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductListAdapter.OnProductClickListener
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
        private const val SEARCH_TYPING_DELAY_MS = 500L

        fun newInstance() = ProductListFragment()
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: ProductListViewModel
    private lateinit var productAdapter: ProductListAdapter

    private val skeletonView = SkeletonView()

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private val searchHandler = Handler()
    private var isSearching: Boolean = false

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
                viewModel.refreshProducts()
            }
        }
    }

    override fun onAttach(context: Context?) {
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_product_list_fragment, menu)

        searchMenuItem = menu?.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
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
        searchMenuItem?.let {
            if (it.isActionViewExpanded) it.collapseActionView()
            if (it.isVisible != showSearch) it.isVisible = showSearch
        }
    }

    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        return !isChildShowing
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(Stat.PRODUCT_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun clearSearchResults() {
        if (isSearching) {
            isSearching = false
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
            submitSearchDelayed(newText)
        } else {
            productAdapter.clearAdapterData()
        }
        showEmptyView(false)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        productAdapter.clearAdapterData()
        isSearching = true
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        clearSearchResults()
        return true
    }

    fun submitSearchDelayed(query: String) {
        searchHandler.postDelayed({
            searchView?.let {
                // submit the search if the searchView's query still matches the passed query
                if (query == it.query.toString()) submitSearch(query)
            }
        }, SEARCH_TYPING_DELAY_MS)
    }

    fun submitSearch(query: String) {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_SEARCHED,
                mapOf(AnalyticsTracker.KEY_SEARCH to query)
        )
        viewModel.searchProducts(query)
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(ProductListViewModel::class.java).also {
            setupObservers(it)
        }
        viewModel.start()
    }

    private fun setupObservers(viewModel: ProductListViewModel) {
        viewModel.productList.observe(this, Observer {
            showProductList(it)
        })

        viewModel.isSkeletonShown.observe(this, Observer {
            showSkeleton(it)
        })

        viewModel.isLoadingMore.observe(this, Observer {
            showLoadMoreProgress(it)
        })

        viewModel.isRefreshing.observe(this, Observer {
            productsRefreshLayout.isRefreshing = it
        })

        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
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
            val showImage = !DisplayUtils.isLandscape(activity)
            empty_view.show(R.string.product_list_empty, showImage)
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
        viewModel.loadProducts(loadMore = true)
    }
}
