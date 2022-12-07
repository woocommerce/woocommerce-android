package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogProductListBulkPriceUpdateBinding
import com.woocommerce.android.databinding.FragmentProductListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.pinFabAboveBottomNavigationBar
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ScrollToTop
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.SelectProducts
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowAddProductBottomSheet
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowBulkProductPriceUpdateDialog
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowProductFilterScreen
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowProductSortingBottomSheet
import com.woocommerce.android.ui.products.ProductSortAndFiltersCard.ProductSortAndFilterListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductListFragment :
    TopLevelFragment(R.layout.fragment_product_list),
    ProductSortAndFilterListener,
    OnLoadMoreListener,
    OnQueryTextListener,
    OnActionExpandListener,
    WCProductSearchTabView.ProductSearchTypeChangedListener,
    ActionMode.Callback,
    MenuProvider {
    companion object {
        val TAG: String = ProductListFragment::class.java.simpleName
        const val PRODUCT_FILTER_RESULT_KEY = "product_filter_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var _productAdapter: ProductListAdapter? = null
    private val productAdapter: ProductListAdapter
        get() = _productAdapter!!

    private var tracker: SelectionTracker<Long>? = null
    private var actionMode: ActionMode? = null
    private val selectionPredicate = MutableMultipleSelectionPredicate<Long>()

    private val viewModel: ProductListViewModel by viewModels()

    private val skeletonView = SkeletonView()

    private var searchMenuItem: MenuItem? = null
    private var multiSelectMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    private var trashProductUndoSnack: Snackbar? = null
    private var pendingTrashProductId: Long? = null

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val feedbackState: FeatureFeedbackSettings.FeedbackState
        get() =
            FeedbackPrefs.getFeatureFeedbackSettings(FeatureFeedbackSettings.Feature.PRODUCT_VARIATIONS)?.feedbackState
                ?: FeatureFeedbackSettings.FeedbackState.UNANSWERED

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        _binding = FragmentProductListBinding.bind(view)

        view.doOnPreDraw { startPostponedEnterTransition() }

        setupObservers(viewModel)
        setupResultHandlers()
        ViewGroupCompat.setTransitionGroup(binding.productsRefreshLayout, true)
        _productAdapter = ProductListAdapter(
            ::onProductClick,
            loadMoreListener = this,
            currencyFormatter = currencyFormatter
        )
        binding.productsRecycler.layoutManager = LinearLayoutManager(requireActivity())
        binding.productsRecycler.adapter = productAdapter

        // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
        // and only processes the first click event. More details on this issue can be found here:
        // https://github.com/woocommerce/woocommerce-android/issues/2074
        binding.productsRecycler.isMotionEventSplittingEnabled = false

        binding.productsRefreshLayout.apply {
            scrollUpChild = binding.productsRecycler
            setOnRefreshListener {
                viewModel.onRefreshRequested()
            }
        }

        initAddProductFab(binding.addProductButton)

        addSelectionTracker()

        when {
            viewModel.isSearching() -> {
                binding.productsSearchTabView.isVisible = true
                binding.productsSearchTabView.show(this, viewModel.isSkuSearch())
            }
            // Do nothing on selection mode
            viewModel.isSelecting() -> {}
            else -> {
                viewModel.reloadProductsFromDb(excludeProductId = pendingTrashProductId)
            }
        }
    }

    private fun addSelectionTracker() {
        tracker = SelectionTracker.Builder(
            "productSelection", // a string to identity our selection in the context of this fragment
            binding.productsRecycler, // the RecyclerView where we will apply the tracker
            ProductSelectionItemKeyProvider(binding.productsRecycler), // the source of selection keys
            DefaultProductListItemLookup(binding.productsRecycler), // the source of information about recycler items
            StorageStrategy.createLongStorage() // strategy for type-safe storage of the selection state
        ).withSelectionPredicate(selectionPredicate)
            .build() // allows multiple items to be selected without any restriction

        productAdapter.tracker = tracker

        tracker?.addObserver(
            object : SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    val selectionCount = tracker?.selection?.size() ?: 0
                    if (selectionCount > 0 && actionMode == null) {
                        viewModel.enterSelectionMode()
                        actionMode = (requireActivity() as AppCompatActivity)
                            .startSupportActionMode(this@ProductListFragment)
                    }
                    when (selectionCount) {
                        0 -> {
                            viewModel.exitSelectionMode()
                            actionMode?.finish()
                        }
                        else -> {
                            actionMode?.title = StringUtils.getQuantityString(
                                context = requireContext(),
                                quantity = selectionCount,
                                default = R.string.product_selection_count,
                                one = R.string.product_selection_count_single
                            )
                        }
                    }
                    super.onSelectionChanged()
                }
            })
    }

    private fun enableProductsRefresh(enable: Boolean) {
        binding.productsRefreshLayout.isEnabled = enable
    }

    private fun initAddProductFab(fabButton: FloatingActionButton) {
        fabButton.setOnClickListener {
            viewModel.onAddProductButtonClicked()
        }

        pinFabAboveBottomNavigationBar(fabButton)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        disableSearchListeners()
        searchView = null
        _productAdapter = null
        actionMode = null
        tracker = null
        searchMenuItem = null
        multiSelectMenuItem = null
        binding.productsSearchTabView.hide()
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        trashProductUndoSnack?.dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        tracker?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        tracker?.run {
            onRestoreInstanceState(savedInstanceState)
            if (hasSelection()) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(this@ProductListFragment)
                actionMode?.title = StringUtils.getQuantityString(
                    context = requireContext(),
                    quantity = selection.size(),
                    default = R.string.product_selection_count,
                    one = R.string.product_selection_count_single
                )
            }
        }

        super.onViewStateRestored(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val fadeThroughTransition = MaterialFadeThrough().apply { duration = transitionDuration }
        enterTransition = fadeThroughTransition
        exitTransition = fadeThroughTransition
        reenterTransition = fadeThroughTransition
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_product_list_fragment, menu)

        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.product_search_hint)

        multiSelectMenuItem = menu.findItem(R.id.menu_multiselect)
        multiSelectMenuItem?.isVisible = FeatureFlag.PRODUCTS_BULK_EDITING.isEnabled()
    }

    override fun onPrepareMenu(menu: Menu) {
        refreshOptionsMenu()
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
                    val queryHint = getSearchQueryHint()
                    searchView?.queryHint = queryHint
                } else {
                    menuItem.collapseActionView()
                }
                enableSearchListeners()
            }
        }
    }

    private fun getSearchQueryHint(): String {
        return if (viewModel.viewStateLiveData.liveData.value?.isFilteringActive == true) {
            getString(R.string.product_search_hint_active_filters)
        } else {
            getString(R.string.product_search_hint)
        }
    }

    /**
     * Prevent search from appearing when a child fragment is active
     */
    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false
        return !isChildShowing
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }
            R.id.menu_multiselect -> {
                viewModel.onSelectProductsClicked()
                true
            }
            else -> false
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

    override fun onProductSearchTypeChanged(isSkuSearch: Boolean) {
        viewModel.onSearchTypeChanged(isSkuSearch)
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        viewModel.onSearchOpened()
        onSearchViewActiveChanged(isActive = true)
        binding.productsSearchTabView.show(this)
        multiSelectMenuItem?.isVisible = false
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        viewModel.onSearchClosed()
        closeSearchView()
        onSearchViewActiveChanged(isActive = false)
        binding.productsSearchTabView.hide()
        multiSelectMenuItem?.isVisible = FeatureFlag.PRODUCTS_BULK_EDITING.isEnabled()
        return true
    }

    private fun setIsRefreshing(isRefreshing: Boolean) {
        binding.productsRefreshLayout.isRefreshing = isRefreshing
    }

    @Suppress("LongMethod")
    private fun setupObservers(viewModel: ProductListViewModel) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                setIsRefreshing(it)
            }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    when {
                        new.isSearchActive == true -> {
                            binding.emptyView.show(
                                EmptyViewType.SEARCH_RESULTS,
                                searchQueryOrFilter = viewModel.getSearchQuery()
                            )
                        }
                        new.filterCount?.compareTo(0) == 1 -> binding.emptyView.show(EmptyViewType.FILTER_RESULTS)
                        else -> {
                            binding.emptyView.show(EmptyViewType.PRODUCT_LIST) {
                                showAddProductBottomSheet()
                            }
                        }
                    }
                } else {
                    binding.emptyView.hide()
                }
            }
            new.displaySortAndFilterCard?.takeIfNotEqualTo(old?.displaySortAndFilterCard) {
                showProductSortAndFiltersCard(it)
            }
            new.filterCount?.takeIfNotEqualTo(old?.filterCount) { updateFilterSelection(it) }

            new.sortingTitleResource?.takeIfNotEqualTo(old?.sortingTitleResource) {
                binding.productsSortFilterCard.setSortingTitle(getString(it))
            }
            new.isAddProductButtonVisible?.takeIfNotEqualTo(old?.isAddProductButtonVisible) { isVisible ->
                showAddProductButton(show = isVisible)
            }
            new.isBottomNavBarVisible.takeIfNotEqualTo(old?.isBottomNavBarVisible) { isBottomNavBarVisible ->
                showBottomNavBar(isVisible = isBottomNavBarVisible)
            }
            new.isSearchActive.takeIfNotEqualTo(old?.isSearchActive) {
                refreshOptionsMenu()
            }
            new.productListState.takeIfNotEqualTo(old?.productListState) {
                handleListState(it)
            }
        }

        viewModel.productList.observe(viewLifecycleOwner) {
            showProductList(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ScrollToTop -> scrollToTop()
                is ShowAddProductBottomSheet -> showAddProductBottomSheet()
                is ShowProductFilterScreen -> showProductFilterScreen(
                    event.stockStatusFilter,
                    event.productTypeFilter,
                    event.productStatusFilter,
                    event.productCategoryFilter,
                    event.selectedCategoryName
                )
                is ShowProductSortingBottomSheet -> showProductSortingBottomSheet()
                is SelectProducts -> tracker?.setItemsSelected(event.productsIds, true)
                is ShowBulkProductPriceUpdateDialog -> showBulkUpdatePriceDialog(event.productIds)
                else -> event.isHandled = false
            }
        }
    }

    private fun showBulkUpdatePriceDialog(productRemoteIdsToUpdate: List<Long>) {
        val dialogBinding = DialogProductListBulkPriceUpdateBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(R.string.product_bulk_update_regular_price))
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onUpdatePriceClicked(productRemoteIdsToUpdate, dialogBinding.priceInputLayout.getText())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()

        dialogBinding.priceInputLayout.post {
            dialogBinding.priceInputLayout.editText.apply {
                requestFocus()
                showKeyboardWithDelay()
            }
        }
    }

    private fun handleListState(productListState: ProductListViewModel.ProductListState) {
        when (productListState) {
            ProductListViewModel.ProductListState.Selecting -> {
                delayMultiSelection()
                onListSelectionActiveChanged(true)
                enableProductsRefresh(false)
                enableProductSortAndFiltersCard(false)
            }
            ProductListViewModel.ProductListState.Browsing -> {
                tracker?.clearSelection()
                onListSelectionActiveChanged(false)
                enableProductsRefresh(true)
                enableProductSortAndFiltersCard(true)
            }
        }
    }

    private fun delayMultiSelection() {
        selectionPredicate.selectMultiple = false
        binding.productsRecycler.post {
            selectionPredicate.selectMultiple = true
        }
    }

    private fun setupResultHandlers() {
        handleResult<Bundle>(ProductDetailFragment.KEY_PRODUCT_DETAIL_RESULT) { bundle ->
            if (bundle.getBoolean(ProductDetailFragment.KEY_PRODUCT_DETAIL_DID_TRASH)) {
                // User chose to trash from product detail, but we do the actual trashing here
                // so we can show a snackbar enabling the user to undo the trashing.
                val remoteProductId = bundle.getLong(ProductDetailFragment.KEY_REMOTE_PRODUCT_ID)
                trashProduct(remoteProductId)
            }
        }
        handleResult<ProductFilterResult>(PRODUCT_FILTER_RESULT_KEY) { result ->
            viewModel.onFiltersChanged(
                stockStatus = result.stockStatus,
                productStatus = result.productStatus,
                productType = result.productType,
                productCategory = result.productCategory,
                productCategoryName = result.productCategoryName
            )
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
            actionListener = actionListener
        )
            .also {
                it.addCallback(callback)
                it.show()
            }
    }

    override fun getFragmentTitle() = getString(R.string.products)

    override fun scrollToTop() {
        binding.productsRecycler.smoothScrollToPosition(0)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            showProductWIPNoticeCard(false)
            skeletonView.show(binding.productsRecycler, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showLoadMoreProgress(show: Boolean) {
        binding.loadMoreProgress.isVisible = show
    }

    private fun showProductList(products: List<Product>) {
        productAdapter.submitList(products)

        // set to false to remove the new feature banner temporarily
        showProductWIPNoticeCard(false)
    }

    private fun showProductWIPNoticeCard(show: Boolean) {
        if (show && feedbackState != FeatureFeedbackSettings.FeedbackState.DISMISSED) {
            val wipCardTitleId = R.string.product_wip_title_m5
            val wipCardMessageId = R.string.product_wip_message_variations

            binding.productsWipCard.visibility = View.VISIBLE
            binding.productsWipCard.initView(
                title = getString(wipCardTitleId),
                message = getString(wipCardMessageId),
                onGiveFeedbackClick = ::onGiveFeedbackClicked,
                onDismissClick = ::onDismissProductWIPNoticeCardClicked
            )
        } else {
            binding.productsWipCard.visibility = View.GONE
        }
    }

    private fun showProductSortAndFiltersCard(show: Boolean) {
        if (show) {
            binding.productsSortFilterCard.visibility = View.VISIBLE
            binding.productsSortFilterCard.initView(this)
        } else {
            binding.productsSortFilterCard.visibility = View.GONE
        }
    }

    private fun enableProductSortAndFiltersCard(enable: Boolean) {
        binding.productsSortFilterCard.isEnabled(enable)
    }

    private fun showBottomNavBar(isVisible: Boolean) {
        if (!isVisible) {
            (activity as? MainActivity)?.hideBottomNav()
        } else {
            (activity as? MainActivity)?.showBottomNav()
        }
    }

    private fun updateFilterSelection(filterCount: Int) {
        binding.productsSortFilterCard.updateFilterSelection(filterCount)
    }

    private fun showAddProductButton(show: Boolean) {
        when (show) {
            true -> {
                uiMessageResolver.anchorViewId = binding.addProductButton.id
                binding.addProductButton.show()
            }
            else -> {
                uiMessageResolver.anchorViewId = null
                binding.addProductButton.hide()
            }
        }
    }

    private fun onProductClick(remoteProductId: Long, sharedView: View?) {
        (activity as? MainNavigationRouter)?.let { router ->
            if (sharedView == null) {
                router.showProductDetail(remoteProductId, enableTrash = true)
            } else {
                router.showProductDetailWithSharedTransition(remoteProductId, sharedView, enableTrash = true)
            }
        }
    }

    private fun showAddProductBottomSheet() {
        val action = ProductListFragmentDirections.actionProductListFragmentToProductTypesBottomSheet(
            isAddProduct = true
        )
        findNavController().navigateSafely(action)
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested()
    }

    private fun showProductFilterScreen(
        stockStatus: String?,
        productType: String?,
        productStatus: String?,
        productCategory: String?,
        productCategoryName: String?
    ) {
        (activity as? MainNavigationRouter)?.showProductFilters(
            stockStatus,
            productType,
            productStatus,
            productCategory,
            productCategoryName
        )
    }

    override fun onFilterOptionSelected() {
        viewModel.onFiltersButtonTapped()
    }

    private fun showProductSortingBottomSheet() {
        val bottomSheet = ProductSortingFragment()
        bottomSheet.show(childFragmentManager, bottomSheet.tag)
    }

    override fun onSortOptionSelected() {
        viewModel.onSortButtonTapped()
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_PRODUCTS_VARIATIONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.PRODUCT)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissProductWIPNoticeCardClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_PRODUCTS_VARIATIONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.DISMISSED)
        showProductWIPNoticeCard(false)
    }

    private fun registerFeedbackSetting(state: FeatureFeedbackSettings.FeedbackState) {
        FeatureFeedbackSettings(
            FeatureFeedbackSettings.Feature.PRODUCT_VARIATIONS,
            state
        ).registerItself()
    }

    override fun shouldExpandToolbar(): Boolean {
        val isNotSearching = !viewModel.isSearching()
        val isNotSelecting = !viewModel.isSelecting()
        return binding.productsRecycler.computeVerticalScrollOffset() == 0 && isNotSearching && isNotSelecting
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_action_mode_products_list, menu)
        menu.iterator().forEach { item -> item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER) }
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_update_status -> true
            R.id.menu_update_price -> {
                viewModel.onBulkUpdatePriceClicked(tracker?.selection?.toList().orEmpty())
                true
            }
            R.id.menu_select_all -> {
                viewModel.onSelectAllProductsClicked()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        tracker?.clearSelection()
        actionMode = null
    }
}
