package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentGroupedProductListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import javax.inject.Inject

class GroupedProductListFragment : BaseFragment(R.layout.fragment_grouped_product_list), BackPressListener,
    OnActionModeEventListener {
    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: GroupedProductListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()
    private val productListAdapter: GroupedProductListAdapter by lazy {
        GroupedProductListAdapter(isEditMode = false, onItemDeleted = viewModel::onProductDeleted)
    }

    private var actionMode: ActionMode? = null
    private val actionModeCallback: ProductEditActionModeCallback by lazy {
        ProductEditActionModeCallback(this)
    }

    private var _binding: FragmentGroupedProductListBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = resources.getString(viewModel.groupedProductListType.titleId)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentGroupedProductListBinding.bind(view)

        setHasOptionsMenu(true)

        setupObservers()
        setupResultHandlers()

        binding.productsRecycler.layoutManager = LinearLayoutManager(requireActivity())
        binding.productsRecycler.adapter = productListAdapter
        binding.productsRecycler.isMotionEventSplittingEnabled = false
    }

    override fun onDestroyView() {
        // hide the skeleton view if fragment is destroyed
        skeletonView.hide()
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers() {
        viewModel.productListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { binding.loadMoreProgress.isVisible = it }
            new.isAddProductButtonVisible.takeIfNotEqualTo(old?.isAddProductButtonVisible) {
                showAddProductButton(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(viewModel.getKeyForGroupedProductListType(), event.data as List<*>)
                }
                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })

        viewModel.productList.observe(viewLifecycleOwner, Observer {
            productListAdapter.setProductList(it)
        })
    }

    private fun setupResultHandlers() {
        handleResult<List<Long>>(GroupedProductListType.UPSELLS.resultKey) {
            viewModel.onProductsAdded(it)
        }
        handleResult<List<Long>>(GroupedProductListType.CROSS_SELLS.resultKey) {
            viewModel.onProductsAdded(it)
        }
        handleResult<List<Long>>(GroupedProductListType.GROUPED.resultKey) {
            viewModel.onProductsAdded(it)
        }
    }

    private fun showAddProductButton(show: Boolean) {
        with(binding.addGroupedProductView) {
            isVisible = show
            initView { viewModel.onAddProductButtonClicked() }
        }
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> {
                skeletonView.show(binding.productsRecycler, R.layout.skeleton_product_list, delayed = true)
            }
            false -> skeletonView.hide()
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit -> {
                setEditModeUI()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setEditModeUI() {
        TransitionManager.beginDelayedTransition(binding.groupedProductsRoot)
        actionMode = requireActivity().startActionMode(actionModeCallback)
        binding.addGroupedProductView.isVisible = false
        productListAdapter.setEditMode(true)
    }

    class ProductEditActionModeCallback(
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
            // display done menu button & back button
            mode.menuInflater.inflate(R.menu.menu_action_mode_check, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onDestroyActionMode(mode: ActionMode) {
            // exit from edit mode
            onActionModeEventListener.onActionModeDestroyed()
        }
    }

    override fun onActionModeCreated() {
        // no-op
    }

    override fun onActionModeDestroyed() {
        TransitionManager.beginDelayedTransition(binding.groupedProductsRoot)
        binding.addGroupedProductView.isVisible = true
        productListAdapter.setEditMode(false)
        actionMode = null
    }

    override fun onActionModeClicked() {
        actionMode?.finish()
    }
}
