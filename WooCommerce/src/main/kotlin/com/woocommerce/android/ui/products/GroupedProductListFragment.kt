package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.fragment_grouped_product_list.*
import javax.inject.Inject

class GroupedProductListFragment : BaseFragment(), BackPressListener {
    companion object {
        const val KEY_GROUPED_PRODUCT_IDS_RESULT = "key_grouped_product_ids_result"
    }

    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: GroupedProductListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()
    private val productListAdapter: GroupedProductListAdapter by lazy {
        GroupedProductListAdapter(viewModel::onGroupedProductDeleted)
    }

    private var doneMenuItem: MenuItem? = null

    override fun getFragmentTitle() = getString(R.string.grouped_products)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_grouped_product_list, container, false)
    }

    override fun onDestroyView() {
        // hide the skeleton view if fragment is destroyed
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem?.isVisible = viewModel.hasChanges
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupResultHandlers()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productsRecycler.layoutManager = LinearLayoutManager(activity)
        productsRecycler.adapter = productListAdapter
        productsRecycler.isMotionEventSplittingEnabled = false
    }

    private fun setupObservers() {
        viewModel.productListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { loadMoreProgress.isVisible = it }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) {
                doneMenuItem?.isVisible = it
            }
            new.isAddProductButtonVisible.takeIfNotEqualTo(old?.isAddProductButtonVisible) {
                showAddProductButton(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                    requireActivity(),
                    event.positiveBtnAction,
                    event.negativeBtnAction,
                    event.messageId,
                    negativeButtonId = event.negativeButtonId
                )
                is Exit -> findNavController().navigateUp()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(KEY_GROUPED_PRODUCT_IDS_RESULT, event.data as List<*>)
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
        handleResult<List<Long>>(ProductSelectionListFragment.KEY_SELECTED_PRODUCT_IDS_RESULT) {
            viewModel.onGroupedProductsAdded(it)
        }
    }

    private fun showAddProductButton(show: Boolean) {
        with(addGroupedProductView) {
            isVisible = show
            initView { viewModel.onAddProductButtonClicked() }
        }
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> {
                skeletonView.show(productsRecycler, R.layout.skeleton_product_list, delayed = true)
            }
            false -> skeletonView.hide()
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked()
}
