package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_STOCK_STATUS
import com.woocommerce.android.ui.products.ProductFilterListViewModel.Companion.ARG_PRODUCT_FILTER_TYPE_STATUS
import com.woocommerce.android.ui.products.ProductListAdapter.OnProductClickListener
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ScrollToTop
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowAddProductBottomSheet
import com.woocommerce.android.ui.products.ProductSortAndFiltersCard.ProductSortAndFilterListener
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_list.*
import javax.inject.Inject

class ProductListFragment : TopLevelFragment(), OnProductClickListener, ProductSortAndFilterListener,
    OnLoadMoreListener,
    OnQueryTextListener,
    OnActionExpandListener,
    NavigationResult {
    companion object {
        val TAG: String = ProductListFragment::class.java.simpleName
        const val KEY_LIST_STATE = "list-state"
        fun newInstance() = ProductListFragment()
    }

    // TODO this is to help test the click!
    var count = 0

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var productAdapter: ProductListAdapter
    private var listState: Parcelable? = null

    private val viewModel: ProductListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    private var trashProductUndoSnack: Snackbar? = null
    private var pendingTrashProductId: Long? = null

    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)?.state ?: UNANSWERED

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

        listState = savedInstanceState?.getParcelable(KEY_LIST_STATE)

        productAdapter = ProductListAdapter(this, this)
        productsRecycler.layoutManager = LinearLayoutManager(activity)
        productsRecycler.adapter = productAdapter

        // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
        // and only processes the first click event. More details on this issue can be found here:
        // https://github.com/woocommerce/woocommerce-android/issues/2074
        productsRecycler.isMotionEventSplittingEnabled = false

        productsRefreshLayout?.apply {
            scrollUpChild = productsRecycler
            setOnRefreshListener {
                viewModel.onRefreshRequested()
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_LIST_STATE, productsRecycler.layoutManager?.onSaveInstanceState())
        super.onSaveInstanceState(outState)
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

    override fun onStop() {
        super.onStop()
        trashProductUndoSnack?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
        setupResultHandlers()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (hidden) {
            disableSearchListeners()
            trashProductUndoSnack?.dismiss()
        } else {
            enableSearchListeners()
        }
    }

    override fun onReturnedFromChildFragment() {
        showOptionsMenu(true)

        if (!viewModel.isSearching()) {
            viewModel.reloadProductsFromDb(excludeProductId = pendingTrashProductId)
        }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.PRODUCT_LIST_FILTERS -> {
                viewModel.onFiltersChanged(
                    stockStatus = result.getString(ARG_PRODUCT_FILTER_STOCK_STATUS),
                    productStatus = result.getString(ARG_PRODUCT_FILTER_STATUS),
                    productType = result.getString(ARG_PRODUCT_FILTER_TYPE_STATUS)
                )
            }
        }
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
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productsRefreshLayout.isRefreshing = it }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    when {
                        new.isSearchActive == true -> {
                            empty_view.show(
                                EmptyViewType.SEARCH_RESULTS,
                                searchQueryOrFilter = viewModel.getSearchQuery()
                            )
                        }
                        new.filterCount?.compareTo(0) == 1 -> empty_view.show(EmptyViewType.FILTER_RESULTS)
                        else -> empty_view.show(EmptyViewType.PRODUCT_LIST)
                    }
                } else {
                    empty_view.hide()
                }
            }
            new.displaySortAndFilterCard?.takeIfNotEqualTo(old?.displaySortAndFilterCard) {
                showProductSortAndFiltersCard(it)
            }
            new.filterCount?.takeIfNotEqualTo(old?.filterCount) { updateFilterSelection(it) }

            new.sortingTitleResource?.takeIfNotEqualTo(old?.sortingTitleResource) {
                products_sort_filter_card.setSortingTitle(getString(it))
            }
            new.isAddProductButtonVisible?.takeIfNotEqualTo(old?.isAddProductButtonVisible) { isVisible ->
                showAddProductButton(show = isVisible)
            }
        }

        viewModel.productList.observe(viewLifecycleOwner, Observer {
            showProductList(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ScrollToTop -> scrollToTop()
                is ShowAddProductBottomSheet -> showAddProductBottomSheet()
                else -> event.isHandled = false
            }
        })
    }

    private fun setupResultHandlers() {
        handleResult<Bundle>(ProductDetailFragment.KEY_PRODUCT_DETAIL_RESULT, R.id.rootFragment) { bundle ->
            if (bundle.getBoolean(ProductDetailFragment.KEY_PRODUCT_DETAIL_DID_TRASH)) {
                // User chose to trash from product detail, but we do the actual trashing here
                // so we can show a snackbar enabling the user to undo the trashing.
                val remoteProductId = bundle.getLong(ProductDetailFragment.KEY_REMOTE_PRODUCT_ID)
                trashProduct(remoteProductId)
            }
        }
    }

    private fun trashProduct(remoteProductId: Long) {
        var trashProductCancelled = false
        pendingTrashProductId = remoteProductId

        // reload the product list without this product
        viewModel.reloadProductsFromDb(excludeProductId = remoteProductId)

        val actionListener = View.OnClickListener {
            trashProductCancelled = true
        }

        val callback = object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                pendingTrashProductId = null
                if (trashProductCancelled) {
                    viewModel.reloadProductsFromDb()
                } else {
                    viewModel.trashProduct(remoteProductId)
                }
            }
        }

        trashProductUndoSnack = uiMessageResolver.getUndoSnack(
            R.string.product_trash_undo_snackbar_message,
            actionListener = actionListener)
            .also {
                it.addCallback(callback)
                it.show()
            }
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
        loadMoreProgress.isVisible = show
    }

    private fun showProductList(products: List<Product>) {
        productAdapter.setProductList(products)
        listState?.let {
            productsRecycler.layoutManager?.onRestoreInstanceState(it)
            listState = null
        }

        showProductWIPNoticeCard(true)
    }

    private fun showProductWIPNoticeCard(show: Boolean) {
        if (show && feedbackState != DISMISSED) {
            val wipCardMessageId = R.string.product_wip_message_m3
            products_wip_card.visibility = View.VISIBLE
            products_wip_card.initView(
                getString(R.string.product_wip_title),
                getString(wipCardMessageId),
                onGiveFeedbackClick = ::onGiveFeedbackClicked,
                onDismissClick = ::onDismissProductWIPNoticeCardClicked
            )
        } else {
            products_wip_card.visibility = View.GONE
        }
    }

    private fun showProductSortAndFiltersCard(show: Boolean) {
        if (show) {
            products_sort_filter_card.visibility = View.VISIBLE
            products_sort_filter_card.initView(this)
        } else {
            products_sort_filter_card.visibility = View.GONE
        }
    }

    private fun updateFilterSelection(filterCount: Int) {
        products_sort_filter_card.updateFilterSelection(filterCount)
    }

    private fun showAddProductButton(show: Boolean) {
        fun showButton() = run { addProductButton.isVisible = true }
        fun hideButton() = run { addProductButton.isVisible = false }
        when (show) {
            true -> {
                if (FeatureFlag.PRODUCT_RELEASE_M4.isEnabled()) {
                    showButton()
                    addProductButton.setOnClickListener {
                        viewModel.onAddProductButtonClicked()
                    }
                } else {
                    hideButton()
                }
            }
            else -> hideButton()
        }
    }

    override fun onProductClick(remoteProductId: Long) = showProductDetails(remoteProductId)

    private fun showProductDetails(remoteProductId: Long) {
        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId, enableTrash = true)
    }

    private fun showAddProductBottomSheet() = (activity as? MainNavigationRouter)?.showProductAddBottomSheet()

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested()
    }

    override fun onFilterOptionSelected() {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_VIEW_FILTER_OPTIONS_TAPPED)
        disableSearchListeners()
        showOptionsMenu(false)
        (activity as? MainNavigationRouter)?.showProductFilters(
            viewModel.getFilterByStockStatus(),
            viewModel.getFilterByProductType(),
            viewModel.getFilterByProductStatus()
        )
    }

    override fun onSortOptionSelected() {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_VIEW_SORTING_OPTIONS_TAPPED)
        val bottomSheet = ProductSortingFragment()
        bottomSheet.show(childFragmentManager, bottomSheet.tag)
    }

    private fun onGiveFeedbackClicked(view: View) {
        registerFeedbackSetting(GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.PRODUCT)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissProductWIPNoticeCardClicked(view: View) {
        registerFeedbackSetting(DISMISSED)
        showProductWIPNoticeCard(false)
    }

    private fun registerFeedbackSetting(state: FeedbackState) {
        FeatureFeedbackSettings(products_wip_card.wipFeatureType.name, state)
            .run { FeedbackPrefs.setFeatureFeedbackSettings(TAG, this) }
    }
}
