package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentVariationListBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.VariationDetailFragment.Companion.KEY_VARIATION_DETAILS_RESULT
import com.woocommerce.android.ui.products.variations.VariationDetailViewModel.DeletedVariationData
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowAddAttributeView
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowBulkUpdateAttrPicker
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowBulkUpdateLimitExceededWarning
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowVariationDetail
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VariationListFragment :
    BaseFragment(R.layout.fragment_variation_list),
    BackPressListener,
    OnLoadMoreListener {
    companion object {
        const val TAG: String = "VariationListFragment"
        const val KEY_VARIATION_LIST_RESULT = "key_variation_list_result"
        private const val LIST_STATE_KEY = "list_state"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: VariationListViewModel by viewModels()

    private val skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null

    private val navArgs: VariationListFragmentArgs by navArgs()

    private var _binding: FragmentVariationListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationListBinding.bind(view)

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        initializeViewModel()
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        binding.variationList.layoutManager = layoutManager
        binding.variationList.itemAnimator = null
        binding.variationList.addItemDecoration(
            AlignedDividerDecoration(
                requireContext(), DividerItemDecoration.VERTICAL, R.id.variationOptionName, clipToMargin = false
            )
        )

        binding.variationListRefreshLayout.apply {
            scrollUpChild = binding.variationList
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_VARIANTS_PULLED_TO_REFRESH)
                viewModel.refreshVariations(navArgs.remoteProductId)
            }
        }

        binding.firstVariationView.setOnClickListener {
            viewModel.onCreateFirstVariationRequested()
        }

        binding.addVariationButton.setOnClickListener {
            viewModel.onCreateEmptyVariationClick()
        }
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: VariationListViewModel) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.variationListRefreshLayout.isRefreshing = it
            }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) {
                binding.loadMoreProgress.isVisible = it
            }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible, ::handleEmptyViewChanges)
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                hideProgressDialog()
                showProgressDialog(it, R.string.variation_create_dialog_title)
            }
            new.isVariationsOptionsMenuEnabled.takeIfNotEqualTo(old?.isVariationsOptionsMenuEnabled) {
                requireActivity().invalidateOptionsMenu()
            }
            new.isBulkUpdateProgressDialogShown.takeIfNotEqualTo(old?.isBulkUpdateProgressDialogShown) {
                hideProgressDialog()
                showProgressDialog(it, R.string.variation_loading_dialog_title)
            }
        }

        viewModel.variationList.observe(viewLifecycleOwner) {
            showVariations(it, viewModel.viewStateLiveData.liveData.value?.parentProduct)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowVariationDetail -> openVariationDetail(event.variation)
                is ShowAddAttributeView -> openAddAttributeView()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowBulkUpdateAttrPicker -> openBulkUpdateView(event.variationsToUpdate)
                is ShowBulkUpdateLimitExceededWarning -> showBulkUpdateLimitExceededWarning()
                is ExitWithResult<*> -> navigateBackWithResult(KEY_VARIATION_LIST_RESULT, event.data)
                is Exit -> activity?.onBackPressed()
            }
        }
    }

    private fun showBulkUpdateLimitExceededWarning() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(getString(R.string.variations_bulk_update_warning_title))
            .setMessage(getString(R.string.variations_bulk_update_warning_message))
            .setNegativeButton(getString(R.string.variations_bulk_limit_exceeded_button_ok), null)
            .show()
    }

    private fun setupResultHandlers(viewModel: VariationListViewModel) {
        handleResult<DeletedVariationData>(KEY_VARIATION_DETAILS_RESULT) {
            viewModel.onVariationDeleted(it.productID, it.variationID)
        }
    }

    private fun openVariationDetail(variation: ProductVariation) {
        val action = VariationListFragmentDirections.actionVariationListFragmentToVariationDetailFragment(
            remoteProductId = variation.remoteProductId,
            remoteVariationId = variation.remoteVariationId
        )
        findNavController().navigateSafely(action)
    }

    private fun openAddAttributeView() =
        VariationListFragmentDirections
            .actionVariationListFragmentToAddAttributeFragment(true)
            .run { findNavController().navigateSafely(this) }

    private fun openBulkUpdateView(variationsToUpdate: Collection<ProductVariation>) {
        VariationListFragmentDirections
            .actionVariationListFragmentToVariationsBulkUpdateAttrPickerFragment(variationsToUpdate.toTypedArray())
            .run { findNavController().navigateSafely(this) }
    }

    override fun getFragmentTitle() = getString(R.string.product_variations)

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested(navArgs.remoteProductId)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.variationList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showVariations(variations: List<ProductVariation>, parentProduct: Product?) {
        val adapter: VariationListAdapter
        if (binding.variationList.adapter == null) {
            adapter = VariationListAdapter(
                requireContext(),
                GlideApp.with(this),
                this,
                parentProduct,
                viewModel::onItemClick
            )
            binding.variationList.adapter = adapter
        } else {
            adapter = binding.variationList.adapter as VariationListAdapter
        }

        adapter.submitList(variations)
    }

    private fun handleEmptyViewChanges(isEmptyViewVisible: Boolean) {
        binding.variationInfoContainer.visibility = if (isEmptyViewVisible) INVISIBLE else VISIBLE
        binding.firstVariationView.updateVisibility(
            shouldBeVisible = isEmptyViewVisible,
            showButton = true
        )
        requireActivity().invalidateOptionsMenu()
    }

    private fun showProgressDialog(show: Boolean, @StringRes title: Int) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onExit()
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_variation_list_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val isBatchUpdateEnabled = viewModel.viewStateLiveData.liveData.value?.isVariationsOptionsMenuEnabled ?: false
        menu.findItem(R.id.menu_bulk_update)?.isVisible = isBatchUpdateEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_bulk_update -> {
                viewModel.onBulkUpdateClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
