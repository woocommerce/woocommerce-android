package com.woocommerce.android.ui.products.grouped

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentGroupedProductListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupedProductListFragment :
    BaseFragment(R.layout.fragment_grouped_product_list),
    MainActivity.Companion.BackPressListener {
    @Inject
    lateinit var navigator: ProductNavigator

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    val viewModel: GroupedProductListViewModel by viewModels()

    private val skeletonView = SkeletonView()
    private val productListAdapter: GroupedProductListAdapter by lazy {
        GroupedProductListAdapter(viewModel::onProductDeleted, currencyFormatter)
    }

    private var _binding: FragmentGroupedProductListBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentGroupedProductListBinding.bind(view)

        setupObservers()
        setupResultHandlers()

        binding.productsRecycler.layoutManager = LinearLayoutManager(requireActivity())
        binding.productsRecycler.adapter = productListAdapter
        binding.productsRecycler.isMotionEventSplittingEnabled = false

        setupTabletSecondPaneToolbar(
            title = getString(viewModel.groupedProductListType.titleId),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    if (viewModel.onBackButtonClicked()) {
                        findNavController().navigateUp()
                    }
                }
            }
        )
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
            new.isEmptyViewShown?.takeIfNotEqualTo(old?.isEmptyViewShown) {
                showEmptyView(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ExitWithResult<*> -> {
                    navigateBackWithResult(viewModel.getKeyForGroupedProductListType(), event.data as List<*>)
                }
                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        }

        viewModel.productList.observe(viewLifecycleOwner) {
            productListAdapter.submitList(it)
        }
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

    private fun showEmptyView(show: Boolean) {
        if (show) {
            binding.emptyView.show(WCEmptyView.EmptyViewType.GROUPED_PRODUCT_LIST)
        } else {
            binding.emptyView.hide()
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked()
}
