package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_list.*
import javax.inject.Inject

class ProductSelectionListFragment : BaseFragment(), OnLoadMoreListener, OnActionModeEventListener {
    companion object {
        const val KEY_SELECTED_PRODUCT_IDS_RESULT = "key_selected_product_ids_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductSelectionListViewModel by viewModels { viewModelFactory }

    private var tracker: SelectionTracker<Long>? = null
    private val productSelectionListAdapter: ProductListAdapter by lazy {
        ProductListAdapter(loadMoreListener = this)
    }

    private val skeletonView = SkeletonView()

    private var actionMode: ActionMode? = null
    private val actionModeCallback: ProductSelectionActionModeCallback by lazy {
        ProductSelectionActionModeCallback(this)
    }

    override fun onAttach(context: Context) {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        with(productsRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productSelectionListAdapter
            isMotionEventSplittingEnabled = false
        }

        tracker = SelectionTracker.Builder(
            "mySelection",
            productsRecycler,
            ProductSelectionItemKeyProvider(productsRecycler),
            ProductSelectionListItemLookup(productsRecycler),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        if (savedInstanceState != null) {
            tracker?.onRestoreInstanceState(savedInstanceState)
        }

        productSelectionListAdapter.tracker = tracker

        productsRefreshLayout?.apply {
            scrollUpChild = productsRecycler
            setOnRefreshListener {
                viewModel.onRefreshRequested()
            }
        }

        tracker?.addObserver(
            object : SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    val selectionCount = tracker?.selection?.size() ?: 0
                    if (selectionCount > 0 && actionMode == null) {
                        actionMode = requireActivity().startActionMode(actionModeCallback)
                    }

                    when (selectionCount) {
                        0 -> {
                            actionMode?.finish()
                            activity.title = getString(R.string.grouped_product_add)
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
            })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_product_list_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                // TODO: enable search
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    private fun setupObservers(viewModel: ProductSelectionListViewModel) {
        viewModel.productSelectionListViewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { loadMoreProgress.isVisible = it }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productsRefreshLayout.isRefreshing = it }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isVisible ->
                if (isVisible) {
                    empty_view.show(EmptyViewType.PRODUCT_LIST)
                } else {
                    empty_view.hide()
                }
            }
        }

        viewModel.productList.observe(viewLifecycleOwner, Observer {
            showProductList(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> {
                    navigateBackWithResult(KEY_SELECTED_PRODUCT_IDS_RESULT, event.data as? List<*>)
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productsRecycler, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProductList(productSelectionList: List<Product>) {
        productSelectionListAdapter.setProductList(productSelectionList)
    }

    private fun enableProductsRefresh(enable: Boolean) {
        productsRefreshLayout?.isEnabled = enable
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
            mode.menuInflater.inflate(R.menu.menu_done, menu)
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
