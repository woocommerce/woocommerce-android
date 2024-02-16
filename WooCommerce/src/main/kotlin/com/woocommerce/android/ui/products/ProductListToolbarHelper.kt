package com.woocommerce.android.ui.products

import android.app.Activity
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductListBinding
import com.woocommerce.android.ui.main.MainNavigationRouter
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductListToolbarHelper @Inject constructor(
    private val activity: Activity,
) : DefaultLifecycleObserver,
    MenuItem.OnActionExpandListener,
    SearchView.OnQueryTextListener,
    Toolbar.OnMenuItemClickListener,
    WCProductSearchTabView.ProductSearchTypeChangedListener {
    private var fragment: ProductListFragment? = null
    private var productListViewModel: ProductListViewModel? = null
    private var binding: FragmentProductListBinding? = null

    private var searchMenuItem: MenuItem? = null
    private var scanBarcodeMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    fun onViewCreated(
        fragment: ProductListFragment,
        productListViewModel: ProductListViewModel,
        binding: FragmentProductListBinding
    ) {
        this.fragment = fragment
        this.productListViewModel = productListViewModel
        this.binding = binding

        if (productListViewModel.isSearching()) {
            binding.productsSearchTabView.isVisible = true
            binding.productsSearchTabView.show(this, productListViewModel.isSkuSearch())
        }

        setupToolbar(binding.toolbar)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        fragment = null
        searchMenuItem = null
        scanBarcodeMenuItem = null
        searchView = null
        productListViewModel = null
        binding = null
        disableSearchListeners()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }

            R.id.menu_scan_barcode -> {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_PRODUCT_BARCODE_SCANNING_TAPPED)
                ProductListFragmentDirections.actionProductListFragmentToScanToUpdateInventory().let {
                    fragment?.findNavController()?.navigate(it)
                }
                searchMenuItem?.collapseActionView()
                true
            }

            else -> false
        }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        productListViewModel?.onSearchOpened()
        fragment?.onSearchViewActiveChanged(isActive = true)
        binding?.productsSearchTabView?.show(this)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        productListViewModel?.onSearchClosed()
        fragment?.onSearchViewActiveChanged(isActive = false)
        binding?.productsSearchTabView?.hide()
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        productListViewModel?.onSearchRequested()
        ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        productListViewModel?.onSearchQueryChanged(newText)
        return true
    }

    override fun onProductSearchTypeChanged(isSkuSearch: Boolean) {
        productListViewModel?.onSearchTypeChanged(isSkuSearch)
    }

    private fun setupToolbar(
        toolbar: Toolbar
    ) {
        toolbar.title = activity.getString(R.string.products)
        toolbar.setOnMenuItemClickListener(this)
        toolbar.inflateMenu(R.menu.menu_product_list_fragment)
        toolbar.navigationIcon = null
        val searchMenuItem = toolbar.menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView
        searchView?.queryHint = activity.getString(R.string.product_search_hint)
        scanBarcodeMenuItem = toolbar.menu.findItem(R.id.menu_scan_barcode)
        searchView?.queryHint = getSearchQueryHint()
        searchMenuItem.setOnActionExpandListener(this)
        refreshOptionsMenu()
    }

    private fun refreshOptionsMenu() {
        val showSearch = shouldShowSearchMenuItem()
        searchMenuItem?.let { menuItem ->
            if (menuItem.isVisible != showSearch) menuItem.isVisible = showSearch

            val isSearchActive = productListViewModel?.viewStateLiveData?.liveData?.value?.isSearchActive == true
            if (menuItem.isActionViewExpanded != isSearchActive) {
                if (isSearchActive) {
                    disableSearchListeners()
                    menuItem.expandActionView()
                    val queryHint = getSearchQueryHint()
                    searchView?.queryHint = queryHint
                    searchView?.setQuery(productListViewModel?.viewStateLiveData?.liveData?.value?.query, false)
                    enableSearchListeners()
                }
            }
        }
        scanBarcodeMenuItem?.isVisible = !(productListViewModel?.isSquarePluginActive() ?: false)
    }

    private fun getSearchQueryHint(): String {
        return if (productListViewModel?.viewStateLiveData?.liveData?.value?.isFilteringActive == true) {
            activity.getString(R.string.product_search_hint_active_filters)
        } else {
            activity.getString(R.string.product_search_hint)
        }
    }

    /**
     * Prevent search from appearing when a child fragment is active
     */
    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        return !isChildShowing
    }

    fun disableSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
    }

    private fun enableSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)
    }
}
