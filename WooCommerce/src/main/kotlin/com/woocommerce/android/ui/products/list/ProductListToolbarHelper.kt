package com.woocommerce.android.ui.products.list

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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductListBinding
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.WCProductSearchTabView
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductListToolbarHelper @Inject constructor(
    private val activity: Activity,
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact,
) : DefaultLifecycleObserver,
    MenuItem.OnActionExpandListener,
    SearchView.OnQueryTextListener,
    Toolbar.OnMenuItemClickListener,
    WCProductSearchTabView.ProductSearchTypeChangedListener {
    private var listFragment: ProductListFragment? = null
    private var viewModel: ProductListViewModel? = null
    private var binding: FragmentProductListBinding? = null

    private var searchMenuItem: MenuItem? = null
    private var scanBarcodeMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    private val refreshOptionsMenuCallback = Runnable {
        refreshOptionsMenu()
    }

    override fun onCreate(owner: LifecycleOwner) {
        (activity as FragmentActivity).onBackPressedDispatcher.addCallback(
            owner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isWindowClassLargeThanCompact()) {
                        val navHostFragment = binding?.detailNavContainer?.getFragment<NavHostFragment?>()
                        val detailsFragment = navHostFragment?.childFragmentManager?.fragments?.getOrNull(0)
                        if (detailsFragment is MainActivity.Companion.BackPressListener) {
                            if (detailsFragment.onRequestAllowBackPress()) {
                                if (!navHostFragment.findNavController().popBackStack()) {
                                    listFragment?.findNavController()?.popBackStack()
                                }
                            }
                        } else {
                            if (navHostFragment?.findNavController()?.popBackStack() == false) {
                                listFragment?.findNavController()?.popBackStack()
                            }
                        }
                    } else if (searchMenuItem?.isActionViewExpanded == true) {
                        searchMenuItem?.collapseActionView()
                    } else {
                        val detailsPaneIsNotNavigationRoot =
                            binding?.detailNavContainer?.findNavController()?.navigateUp() ?: false
                        if (!detailsPaneIsNotNavigationRoot && binding?.productsRefreshLayout?.isVisible == false) {
                            // There are no more fragments in the back stack, UI used to be a two pane layout (tablet)
                            // and now it's a single pane layout (phone), e.g. due to a configuration change.
                            // In this case we need to switch panes – show the list pane instead of details pane.
                            listFragment?.displayListPaneOnly()
                        } else {
                            listFragment?.findNavController()?.navigateUp()
                        }
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
        this.listFragment = fragment
        this.viewModel = productListViewModel
        this.binding = binding

        fragment.viewLifecycleOwner.lifecycle.addObserver(this)

        if (productListViewModel.isSearching()) {
            binding.productsSearchTabView.isVisible = true
            binding.productsSearchTabView.show(this, productListViewModel.isSkuSearch())
        }

        setupToolbar(binding.toolbar)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        disableSearchListeners()
        binding?.toolbar?.removeCallbacks(refreshOptionsMenuCallback)
        listFragment = null
        searchMenuItem = null
        scanBarcodeMenuItem = null
        searchView = null
        viewModel = null
        binding = null
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
                    listFragment?.findNavController()?.navigate(it)
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

        searchView = searchMenuItem?.actionView as SearchView
        searchView?.queryHint = activity.getString(R.string.product_search_hint)
        searchView?.queryHint = getSearchQueryHint()

        scanBarcodeMenuItem = toolbar.menu.findItem(R.id.menu_scan_barcode)

        // We want to refresh the options menu after the toolbar has been inflated
        // Otherwise, logic in it will be executed before the toolbar is in restored state after configuration change
        toolbar.post(refreshOptionsMenuCallback)
    }

    private fun refreshOptionsMenu() {
        val showSearch = shouldShowSearchMenuItem()
        searchMenuItem?.let { menuItem ->
            if (menuItem.isVisible != showSearch) menuItem.isVisible = showSearch

            val isSearchActive = viewModel?.viewStateLiveData?.liveData?.value?.isSearchActive == true
            if (isSearchActive) {
                if (menuItem.isActionViewExpanded) {
                    enableSearchListeners()
                } else {
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
        return try {
            (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        } catch (e: IllegalStateException) {
            // as we don't know the reason why this happens and the worst case scenario is that the search
            // won't be shown, we workaround this crash
            return false
        }
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
