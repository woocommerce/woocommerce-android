package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.list.ProductListAdapter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductSelectionListFragment :
    BaseFragment(R.layout.fragment_product_list),
    OnLoadMoreListener,
    OnActionModeEventListener,
    OnQueryTextListener,
    OnActionExpandListener {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    val viewModel: ProductSelectionListViewModel by viewModels()

    private var tracker: SelectionTracker<Long>? = null
    private val productSelectionListAdapter: ProductListAdapter by lazy {
        ProductListAdapter(
            loadMoreListener = this,
            currencyFormatter = currencyFormatter,
            isProductHighlighted = { false }
        )
    }

    private val skeletonView = SkeletonView()

    private var actionMode: ActionMode? = null
    private val actionModeCallback: ProductSelectionActionModeCallback by lazy {
        ProductSelectionActionModeCallback(this)
    }

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        skeletonView.hide()
        disableSearchListeners()
        searchView = null
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductListBinding.bind(view)

        setupObservers(viewModel)

        with(binding.productsRecycler) {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = productSelectionListAdapter
            isMotionEventSplittingEnabled = false
        }

        tracker = SelectionTracker.Builder(
            "mySelection", // a string to identity our selection in the context of this fragment
            binding.productsRecycler, // the RecyclerView where we will apply the tracker
            ProductSelectionItemKeyProvider(binding.productsRecycler), // the source of selection keys
            SelectableProductListItemLookup(binding.productsRecycler), // the source of information about recycler items
            StorageStrategy.createLongStorage() // strategy for type-safe storage of the selection state
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything() // allows multiple items to be selected without any restriction
        ).build()

        if (savedInstanceState != null) {
            tracker?.onRestoreInstanceState(savedInstanceState)
        }

        productSelectionListAdapter.tracker = tracker

        binding.productsRefreshLayout.apply {
            scrollUpChild = binding.productsRecycler
            setOnRefreshListener {
                viewModel.onRefreshRequested()
            }
        }

        tracker?.addObserver(
            object : SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    val selectionCount = tracker?.selection?.size() ?: 0
                    if (selectionCount > 0 && actionMode == null) {
                        actionMode = requireActivity().startActionMode(actionModeCallback)
                    }

                    when (selectionCount) {
                        0 -> {
                            actionMode?.finish()
                            actionMode?.title = getString(R.string.grouped_product_add)
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
                }
            }
        )

        setupTabletSecondPaneToolbar(
            title = getString(R.string.grouped_product_add),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
                onCreateMenu(toolbar)
            }
        )
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.menu_product_selection_fragment)

        searchMenuItem = toolbar.menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.product_search_hint)
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                enableSearchListeners()
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    private fun setupObservers(viewModel: ProductSelectionListViewModel) {
        viewModel.productSelectionListViewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { binding.loadMoreProgress.isVisible = it }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { binding.productsRefreshLayout.isRefreshing = it }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isVisible ->
                if (isVisible) {
                    when {
                        new.isSearchActive == true -> {
                            binding.emptyView.show(
                                EmptyViewType.SEARCH_RESULTS,
                                searchQueryOrFilter = viewModel.searchQuery
                            )
                        }
                        else -> binding.emptyView.show(EmptyViewType.PRODUCT_LIST)
                    }
                } else {
                    binding.emptyView.hide()
                }
            }
        }

        viewModel.productList.observe(viewLifecycleOwner) {
            showProductList(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> {
                    val key = viewModel.groupedProductListType.resultKey
                    val productIds = (event.data as? List<*>) ?: emptyList<Long>()
                    navigateBackWithResult(key, productIds)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.productsRecycler, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProductList(productSelectionList: List<Product>) {
        productSelectionListAdapter.submitList(productSelectionList)
    }

    private fun enableProductsRefresh(enable: Boolean) {
        binding.productsRefreshLayout.isEnabled = enable
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
        viewModel.onRefreshRequested()
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.onSearchQueryChanged(newText)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        viewModel.onSearchOpened()
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        viewModel.onSearchClosed()
        closeSearchView()
        return true
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested()
    }

    override fun onActionModeCreated() {
        enableProductsRefresh(false)
    }

    override fun onActionModeClicked() {
        viewModel.onDoneButtonClicked(tracker?.selection?.toList())
        actionMode?.finish()
    }

    override fun onActionModeDestroyed() {
        enableProductsRefresh(true)
        tracker?.clearSelection()
        actionMode = null
    }

    class ProductSelectionActionModeCallback(
        private val onActionModeEventListener: OnActionModeEventListener
    ) : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_done -> {
                    onActionModeEventListener.onActionModeClicked()
                    true
                }
                else -> false
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // display done menu button & disable PTR action
            mode.menuInflater.inflate(R.menu.menu_action_mode_check, menu)
            onActionModeEventListener.onActionModeCreated()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onDestroyActionMode(mode: ActionMode) {
            // The long press selection is cancelled
            // clear selection & enable PTR action again
            onActionModeEventListener.onActionModeDestroyed()
        }
    }
}
