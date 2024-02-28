package com.woocommerce.android.ui.products

import android.app.Activity
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductListBinding
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.IsTabletLogicNeeded
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductListToolbarHelper @Inject constructor(
    private val activity: Activity,
    private val isTabletLogicNeeded: IsTabletLogicNeeded,
) : DefaultLifecycleObserver,
    MenuItem.OnActionExpandListener,
    SearchView.OnQueryTextListener,
    Toolbar.OnMenuItemClickListener,
    WCProductSearchTabView.ProductSearchTypeChangedListener {
    private var fragment: ProductListFragment? = null
    private var viewModel: ProductListViewModel? = null
    private var binding: FragmentProductListBinding? = null

    private var searchMenuItem: MenuItem? = null
    private var scanBarcodeMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreate(owner: LifecycleOwner) {
        (activity as FragmentActivity).onBackPressedDispatcher.addCallback(
            owner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isTabletLogicNeeded()) {
                        if (binding?.detailNavContainer?.findNavController()?.popBackStack() != true) {
                            fragment?.findNavController()?.popBackStack()
                        }
                    } else if (searchMenuItem?.isActionViewExpanded == true) {
                        searchMenuItem?.collapseActionView()
                    } else {
                        fragment?.findNavController()?.navigateUp()
                    }
                }
            }
        )
    }

    fun onViewCreated(
        fragment: ProductListFragment,
        productListViewModel: ProductListViewModel,
        binding: FragmentProductListBinding
    ) {
        this.fragment = fragment
        this.viewModel = productListViewModel
        this.binding = binding

        fragment.lifecycle.addObserver(this)

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
        viewModel = null
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
        viewModel?.onSearchOpened()
        binding?.productsSearchTabView?.show(this)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        viewModel?.onSearchClosed()
        binding?.productsSearchTabView?.hide()
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel?.onSearchRequested()
        ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel?.onSearchQueryChanged(newText)
        return true
    }

    override fun onProductSearchTypeChanged(isSkuSearch: Boolean) {
        viewModel?.onSearchTypeChanged(isSkuSearch)
    }

    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.title = activity.getString(R.string.products)
        toolbar.setOnMenuItemClickListener(this)
        toolbar.inflateMenu(R.menu.menu_product_list_fragment)
        toolbar.navigationIcon = null

        searchMenuItem = toolbar.menu.findItem(R.id.menu_search)
        searchMenuItem?.setOnActionExpandListener(this)

        searchView = searchMenuItem?.actionView as SearchView
        searchView?.queryHint = activity.getString(R.string.product_search_hint)
        searchView?.queryHint = getSearchQueryHint()

        scanBarcodeMenuItem = toolbar.menu.findItem(R.id.menu_scan_barcode)

        refreshOptionsMenu()
    }

    private fun refreshOptionsMenu() {
        val showSearch = shouldShowSearchMenuItem()
        searchMenuItem?.let { menuItem ->
            if (menuItem.isVisible != showSearch) menuItem.isVisible = showSearch

            val isSearchActive = viewModel?.viewStateLiveData?.liveData?.value?.isSearchActive == true
            if (menuItem.isActionViewExpanded != isSearchActive) {
                if (isSearchActive) {
                    disableSearchListeners()
                    menuItem.expandActionView()
                    val queryHint = getSearchQueryHint()
                    searchView?.queryHint = queryHint
                    searchView?.setQuery(viewModel?.viewStateLiveData?.liveData?.value?.query, false)
                    enableSearchListeners()
                }
            }
        }
        scanBarcodeMenuItem?.isVisible = !(viewModel?.isSquarePluginActive() ?: false)
    }

    private fun getSearchQueryHint(): String {
        return if (viewModel?.viewStateLiveData?.liveData?.value?.isFilteringActive == true) {
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
