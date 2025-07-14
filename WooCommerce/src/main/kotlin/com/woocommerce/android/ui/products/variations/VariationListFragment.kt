package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentVariationListBinding
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetFragment.Companion.KEY_ADD_NEW_VARIATION
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetFragment.Companion.KEY_GENERATE_ALL_VARIATIONS
import com.woocommerce.android.ui.products.variations.VariationDetailFragment.Companion.KEY_VARIATION_DETAILS_RESULT
import com.woocommerce.android.ui.products.variations.VariationDetailViewModel.DeletedVariationData
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ProgressDialogState.Hidden
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ProgressDialogState.Shown
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ProgressDialogState.Shown.VariationsCardinality.MULTIPLE
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ProgressDialogState.Shown.VariationsCardinality.SINGLE
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowAddAttributeView
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowBulkUpdateAttrPicker
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowBulkUpdateLimitExceededWarning
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationConfirmation
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.LimitExceeded
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.NetworkError
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowGenerateVariationsError.NoCandidates
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowVariationDetail
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowVariationDialog
import com.woocommerce.android.ui.products.variations.domain.GenerateVariationCandidates
import com.woocommerce.android.ui.products.variations.domain.VariationCandidate
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
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

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: VariationListViewModel by viewModels()

    private val skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null

    private val navArgs: VariationListFragmentArgs by navArgs()

    private var _binding: FragmentVariationListBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationListBinding.bind(view)

        initializeViews(savedInstanceState)
        initializeViewModel()
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
        _binding = null
        progressDialog = null
        layoutManager = null
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

        savedInstanceState?.parcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        binding.variationList.layoutManager = layoutManager
        binding.variationList.itemAnimator = null
        binding.variationList.addItemDecoration(
            AlignedDividerDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL,
                R.id.variationOptionName,
                clipToMargin = false
            )
        )

        binding.variationListRefreshLayout.apply {
            scrollUpChild = binding.variationList
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_VARIANTS_PULLED_TO_REFRESH)
                viewModel.refreshVariations(navArgs.remoteProductId)
            }
        }
        binding.addVariationButton.text = getString(R.string.variation_list_add)
        binding.firstVariationView.setOnClickListener {
            viewModel.onAddVariationsClicked()
        }
        binding.addVariationButton.setOnClickListener {
            viewModel.onAddVariationsClicked()
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_variations),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onExit()
                }
                onCreateMenu(toolbar)
            }
        )
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
        viewModel.start()
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
            new.progressDialogState?.takeIfNotEqualTo(old?.progressDialogState) { progressDialogState ->
                handleProgressDialogState(progressDialogState)
            }
            new.isVariationsOptionsMenuEnabled.takeIfNotEqualTo(old?.isVariationsOptionsMenuEnabled) {
                onPrepareMenu(binding.toolbar.menu)
            }
            new.isBulkUpdateProgressDialogShown.takeIfNotEqualTo(old?.isBulkUpdateProgressDialogShown) { dialogShown ->
                if (dialogShown) {
                    showProgressDialog(R.string.variation_loading_dialog_title)
                } else {
                    hideProgressDialog()
                }
            }
            new.isAddVariationButtonVisible.takeIfNotEqualTo(old?.isAddVariationButtonVisible) { isVisible ->
                binding.addVariationButton.isVisible = isVisible
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
                is ShowGenerateVariationConfirmation -> showGenerateVariationConfirmation(event.variationCandidates)
                is ShowGenerateVariationsError -> handleGenerateVariationError(event)
                is ShowVariationDialog -> showGenerateVariationBottomSheet()
                is ExitWithResult<*> -> navigateBackWithResult(KEY_VARIATION_LIST_RESULT, event.data)
                is Exit -> activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun handleProgressDialogState(progressDialogState: VariationListViewModel.ProgressDialogState) {
        when (progressDialogState) {
            Hidden -> {
                hideProgressDialog()
            }
            is Shown -> {
                val dialogLabel = when (progressDialogState.cardinality) {
                    SINGLE -> R.string.variation_create_dialog_title
                    MULTIPLE -> R.string.variations_bulk_creation_progress_title
                }
                showProgressDialog(dialogLabel)
            }
        }
    }

    private fun handleGenerateVariationError(event: ShowGenerateVariationsError) {
        when (event) {
            is LimitExceeded -> showGenerateVariationsLimitExceeded(event.variationCandidatesSize)
            NetworkError -> showGenerateVariationsNetworkError()
            NoCandidates -> showNoVariationCandidatesError()
        }
    }

    private fun showGenerateVariationConfirmation(variationCandidatesSize: List<VariationCandidate>) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.variations_bulk_creation_confirmation_title)
            .setMessage(getString(R.string.variations_bulk_creation_confirmation_message, variationCandidatesSize.size))
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                viewModel.onGenerateVariationsConfirmed(variationCandidatesSize)
                dialogInterface.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun showNoVariationCandidatesError() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.variations_bulk_creation_no_candidates_title)
            .setMessage(R.string.variations_bulk_creation_no_candidates_message)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun showGenerateVariationsNetworkError() {
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.error_generic_network)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun showGenerateVariationsLimitExceeded(variationCandidatesSize: Int) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.variations_bulk_creation_warning_title)
            .setMessage(
                getString(
                    R.string.variations_bulk_creation_warning_message,
                    GenerateVariationCandidates.VARIATION_CREATION_LIMIT,
                    variationCandidatesSize
                )
            )
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun showGenerateVariationBottomSheet() {
        VariationListFragmentDirections
            .actionVariationListFragmentToGenerateVariationBottomSheetFragment()
            .run { findNavController().navigateSafely(this) }
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
        handleDialogNotice(KEY_ADD_NEW_VARIATION, R.id.variationListFragment) {
            viewModel.onNewVariationClicked()
        }
        handleDialogNotice(KEY_GENERATE_ALL_VARIATIONS, R.id.variationListFragment) {
            viewModel.onAddAllVariationsClicked()
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
                Glide.with(this),
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
        onPrepareMenu(binding.toolbar.menu)
    }

    private fun showProgressDialog(@StringRes title: Int) {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(title),
            getString(R.string.product_update_dialog_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onExit()
        return false
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.menu_variation_list_fragment)
        onPrepareMenu(toolbar.menu)
    }

    private fun onPrepareMenu(menu: Menu) {
        val isBatchUpdateEnabled = viewModel.viewStateLiveData.liveData.value?.isVariationsOptionsMenuEnabled ?: false
        menu.findItem(R.id.menu_bulk_update)?.isVisible = isBatchUpdateEnabled
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_bulk_update -> {
                viewModel.onBulkUpdateClicked()
                true
            }
            else -> false
        }
    }
}
