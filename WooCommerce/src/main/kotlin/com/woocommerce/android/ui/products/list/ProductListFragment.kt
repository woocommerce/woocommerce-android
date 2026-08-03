package com.woocommerce.android.ui.products.list

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogProductListBulkPriceUpdateBinding
import com.woocommerce.android.databinding.FragmentProductListBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.setDesignSystemContent
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.AddProductNavigator
import com.woocommerce.android.ui.products.ProductSortingFragment
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductsCommunicationViewModel
import com.woocommerce.android.ui.products.UpdateProductStockStatusFragment
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusExitState
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.details.ProductDetailFragmentArgs
import com.woocommerce.android.ui.products.filter.ProductFilterResult
import com.woocommerce.android.ui.products.list.ProductListEvent.OpenEmptyProduct
import com.woocommerce.android.ui.products.list.ProductListEvent.OpenProduct
import com.woocommerce.android.ui.products.list.ProductListEvent.ScrollToTop
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowAddProductBottomSheet
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowBarcodeScanner
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowDiscardProductChangesConfirmationDialog
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowProductFilterScreen
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowProductSortingBottomSheet
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowProductUpdateStockStatusScreen
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowUpdateDialog
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.TabletLayoutSetupHelper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LargeClass")
@AndroidEntryPoint
class ProductListFragment :
    TopLevelFragment(R.layout.fragment_product_list),
    ActionMode.Callback,
    TabletLayoutSetupHelper.Screen {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var addProductNavigator: AddProductNavigator

    @Inject
    lateinit var tabletLayoutSetupHelper: TabletLayoutSetupHelper

    @Inject
    lateinit var mediaFileUploadHandler: MediaFileUploadHandler

    @Inject
    lateinit var isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact

    private val productsCommunicationViewModel: ProductsCommunicationViewModel by activityViewModels()
    private val productListViewModel: ProductListViewModel by viewModels()

    private var actionMode: ActionMode? = null
    private var trashProductUndoSnack: Snackbar? = null
    private var pendingTrashProductId: Long? = null
    private var isDestroyingView = false
    private var isListAtTop = true
    private var isAddProductFabAvailable = false
    private val isPullToRefreshEnabled = MutableStateFlow(true)
    private val scrollToTopRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = requireNotNull(_binding)

    override val twoPaneLayoutGuideline
        get() = binding.twoPaneLayoutGuideline
    override val listPaneContainer: View
        get() = binding.productsComposeContainer
    override val detailPaneContainer: View
        get() = binding.detailNavContainer
    override var twoPanesWereShownBeforeConfigChange: Boolean = false
    override val automaticallyAdjustLayoutAfterConfigChange: Boolean = true
    override val listFragment: Fragment
        get() = this
    override val navigation
        get() = TabletLayoutSetupHelper.Screen.Navigation(
            detailsNavGraphId = R.navigation.nav_graph_products,
            detailsInitialBundle = ProductDetailFragmentArgs(
                mode = ProductDetailFragment.Mode.Loading,
            ).toBundle()
        )

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        twoPanesWereShownBeforeConfigChange = savedInstanceState?.getBoolean(
            TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY,
            false
        ) ?: false
        tabletLayoutSetupHelper.onRootFragmentCreated(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        isDestroyingView = false
        _binding = FragmentProductListBinding.bind(view)
        uiMessageResolver.anchorViewId = null
        ViewGroupCompat.setTransitionGroup(binding.productsComposeContainer, true)
        binding.productsComposeContainer.setDesignSystemContent {
            ProductListScreen(
                viewModel = productListViewModel,
                currencyFormatter = currencyFormatter,
                activeUploadProductIds = mediaFileUploadHandler.activeUploadProductIds,
                isPullToRefreshEnabled = isPullToRefreshEnabled,
                scrollToTopRequests = scrollToTopRequests,
                isTwoPaneLayout = isWindowClassLargeThanCompact(),
                onEmptyAddProductClicked = ::showAddProductBottomSheet,
                onListAtTopChanged = { isListAtTop = it },
            )
        }
        view.doOnPreDraw { startPostponedEnterTransition() }

        setupObservers()
        setupResultHandlers()
        setupBackHandling()

        if (!productListViewModel.isSearching()) {
            productListViewModel.reloadProductsFromDb(excludeProductId = pendingTrashProductId)
        }
    }

    override fun onDestroyView() {
        isDestroyingView = true
        actionMode?.finish()
        actionMode = null
        uiMessageResolver.anchorViewId = null
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
        outState.putBoolean(
            TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY,
            _binding?.detailNavContainer?.isVisible == true &&
                _binding?.productsComposeContainer?.isVisible == true
        )
        super.onSaveInstanceState(outState)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun setupObservers() {
        productListViewModel.viewStateLiveData.observe(viewLifecycleOwner) { _, new ->
            isAddProductFabAvailable = new.isAddProductButtonVisible == true
            updateSnackbarAnchor(isAddProductFabAvailable && !productListViewModel.isSelecting())
            updateBottomNavVisibility(
                isSearchActive = new.isSearchActive == true,
                isSelecting = productListViewModel.isSelecting(),
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                productListViewModel.selectedProductIds.collect { selectedIds ->
                    updateSnackbarAnchor(isAddProductFabAvailable && selectedIds.isEmpty())
                    updateSelectionPresentation(selectedIds.size)
                    updateBottomNavVisibility(
                        isSearchActive = productListViewModel.isSearching(),
                        isSelecting = selectedIds.isNotEmpty(),
                    )
                }
            }
        }
        productListViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ScrollToTop -> scrollToTop()
                is ShowAddProductBottomSheet -> showAddProductBottomSheet()
                is ShowProductFilterScreen -> showProductFilterScreen(event)
                is ShowProductSortingBottomSheet -> showProductSortingBottomSheet()
                is ShowBarcodeScanner -> findNavController().navigateSafely(
                    ProductListFragmentDirections.actionProductListFragmentToScanToUpdateInventory()
                )
                is ShowUpdateDialog -> handleUpdateDialogs(event)
                is OpenProduct -> openProduct(event)
                is OpenEmptyProduct -> openEmptyProduct()
                is ShowProductUpdateStockStatusScreen -> showProductUpdateStockStatusScreen(event.productIds)
                is ShowDiscardProductChangesConfirmationDialog -> showDiscardProductChangesConfirmationDialog(
                    event.productName,
                    event.productId,
                )
                else -> event.isHandled = false
            }
        }
        productsCommunicationViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ProductsCommunicationViewModel.CommunicationEvent.ProductTrashed -> trashProduct(event.productId)
                is ProductsCommunicationViewModel.CommunicationEvent.ProductUpdated -> {
                    productListViewModel.reloadProductsFromDb()
                }
                is ProductsCommunicationViewModel.CommunicationEvent.ProductSelected -> {
                    productListViewModel.onOpenProduct(event.productId)
                }
                is ProductsCommunicationViewModel.CommunicationEvent.ProductChanges -> {
                    productListViewModel.productHasChanges = event.hasChanges
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        productListViewModel.isSelecting() -> productListViewModel.exitSelectionMode()
                        productListViewModel.isSearching() -> productListViewModel.onSearchClosed()
                        isWindowClassLargeThanCompact() -> handleTabletBackPress()
                        else -> handlePhoneBackPress()
                    }
                }
            }
        )
    }

    private fun handleTabletBackPress() {
        val navHostFragment = binding.detailNavContainer.getFragment<NavHostFragment?>()
        val detailsFragment = navHostFragment?.childFragmentManager?.fragments?.getOrNull(0)
        if (detailsFragment is MainActivity.Companion.BackPressListener) {
            if (detailsFragment.onRequestAllowBackPress() &&
                navHostFragment.findNavController().popBackStack().not()
            ) {
                findNavController().popBackStack()
            }
        } else if (navHostFragment?.findNavController()?.popBackStack() == false) {
            findNavController().popBackStack()
        }
    }

    private fun handlePhoneBackPress() {
        val detailsPaneIsNotNavigationRoot = binding.detailNavContainer.findNavController().navigateUp()
        if (!detailsPaneIsNotNavigationRoot && binding.productsComposeContainer.isVisible.not()) {
            displayListPaneOnly()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun updateSelectionPresentation(selectionCount: Int) {
        if (selectionCount > 0) {
            if (actionMode == null) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(this)
            }
            actionMode?.title = StringUtils.getQuantityString(
                context = requireContext(),
                quantity = selectionCount,
                default = R.string.product_selection_count,
                one = R.string.product_selection_count_single,
            )
        } else {
            actionMode?.finish()
            actionMode = null
        }
    }

    private fun updateBottomNavVisibility(isSearchActive: Boolean, isSelecting: Boolean) {
        if (isSearchActive || isSelecting) {
            (activity as? MainActivity)?.hideBottomNav()
        } else {
            (activity as? MainActivity)?.showBottomNav()
        }
    }

    private fun updateSnackbarAnchor(isAddProductFabVisible: Boolean) {
        uiMessageResolver.anchorViewId = binding.addProductSnackbarAnchor.id.takeIf { isAddProductFabVisible }
    }

    private fun openProduct(event: OpenProduct) {
        tabletLayoutSetupHelper.openItemDetails(
            tabletNavigateTo = {
                R.id.nav_graph_products to ProductDetailFragmentArgs(
                    mode = ProductDetailFragment.Mode.ShowProduct(event.productId),
                ).toBundle()
            },
            navigateWithPhoneNavigation = { onProductClick(event.productId) },
        )
    }

    private fun openEmptyProduct() {
        tabletLayoutSetupHelper.openItemDetails(
            tabletNavigateTo = {
                R.id.nav_graph_products to ProductDetailFragmentArgs(
                    mode = ProductDetailFragment.Mode.Empty,
                ).toBundle()
            },
            navigateWithPhoneNavigation = { error("Should not be invoked on a phone") },
        )
    }

    fun displayListPaneOnly() {
        tabletLayoutSetupHelper.displayListPaneOnly(this)
    }

    private fun setupResultHandlers() {
        handleResult<ProductFilterResult>(PRODUCT_FILTER_RESULT_KEY) { result ->
            productListViewModel.onFiltersChanged(
                stockStatus = result.stockStatus,
                productStatus = result.productStatus,
                productType = result.productType,
                productCategory = result.productCategory,
                productCategoryName = result.productCategoryName,
            )
        }
        handleDialogResult<UpdateStockStatusExitState>(
            UpdateProductStockStatusFragment.UPDATE_STOCK_STATUS_EXIT_STATE_KEY,
            R.id.products,
        ) { result ->
            when (result) {
                UpdateStockStatusExitState.Success -> productListViewModel.onRefreshRequested()
                UpdateStockStatusExitState.Error, UpdateStockStatusExitState.NoChange -> Unit
            }
            productListViewModel.exitSelectionMode()
        }
    }

    private fun showProductUpdateStockStatusScreen(productRemoteIdsToUpdate: List<Long>) {
        val action = ProductListFragmentDirections.actionProductListFragmentToUpdateProductStockStatusFragment(
            productRemoteIdsToUpdate.toLongArray()
        )
        findNavController().navigateSafely(action)
    }

    private fun showDiscardProductChangesConfirmationDialog(productName: String, productId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.product_list_unsaved_product_unselected_title, productName))
            .setMessage(R.string.product_list_unsaved_product_unselected_message)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                productListViewModel.productHasChanges = false
                productListViewModel.onOpenProduct(productId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleUpdateDialogs(event: ShowUpdateDialog) {
        when (event) {
            is ShowUpdateDialog.Price -> showBulkUpdatePriceDialog(event.productIds)
            is ShowUpdateDialog.Status -> showBulkUpdateStatusDialog(event.productIds)
        }
    }

    private fun showBulkUpdatePriceDialog(productRemoteIdsToUpdate: List<Long>) {
        val dialogBinding = DialogProductListBulkPriceUpdateBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(R.string.product_bulk_update_regular_price))
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                productListViewModel.onUpdatePriceConfirmed(
                    productRemoteIdsToUpdate,
                    dialogBinding.priceInputLayout.getText(),
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        dialogBinding.priceInputLayout.post {
            dialogBinding.priceInputLayout.editText.apply {
                requestFocus()
                showKeyboardWithDelay()
            }
        }
    }

    private fun showBulkUpdateStatusDialog(productRemoteIdsToUpdate: List<Long>) {
        val statuses = ProductStatus.values()
        val statusItems = statuses.map { it.toLocalizedString(requireActivity(), long = true) }.toTypedArray()
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(R.string.product_bulk_update_status))
            .setSingleChoiceItems(statusItems, -1, null)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val checkedItemPosition = (dialog as AlertDialog).listView.checkedItemPosition
                if (checkedItemPosition in statuses.indices) {
                    productListViewModel.onUpdateStatusConfirmed(
                        productRemoteIdsToUpdate,
                        statuses[checkedItemPosition],
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun trashProduct(remoteProductId: Long) {
        var trashProductCancelled = false
        pendingTrashProductId = remoteProductId
        productListViewModel.reloadProductsFromDb(excludeProductId = remoteProductId)
        isPullToRefreshEnabled.value = false

        val callback = object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                isPullToRefreshEnabled.value = true
                pendingTrashProductId = null
                if (trashProductCancelled) {
                    productListViewModel.reloadProductsFromDb()
                } else {
                    productListViewModel.trashProduct(remoteProductId)
                }
            }
        }
        trashProductUndoSnack = uiMessageResolver.getUndoSnack(
            stringResId = R.string.product_trash_undo_snackbar_message,
            actionListener = { trashProductCancelled = true },
        ).apply {
            addCallback(callback)
            show()
        }
    }

    override fun scrollToTop() {
        scrollToTopRequests.tryEmit(Unit)
    }

    private fun onProductClick(remoteProductId: Long) {
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId, sharedView = null)
    }

    private fun showAddProductBottomSheet() {
        with(addProductNavigator) {
            findNavController().navigateToAddProducts(
                aiBottomSheetAction = ProductListFragmentDirections.actionProductsToAddProductWithAIBottomSheet(),
                typesBottomSheetAction = ProductListFragmentDirections
                    .actionProductListFragmentToProductTypesBottomSheet(isAddProduct = true),
            )
        }
    }

    private fun showProductFilterScreen(event: ShowProductFilterScreen) {
        (activity as? MainNavigationRouter)?.showProductFilters(
            event.stockStatusFilter,
            event.productTypeFilter,
            event.productStatusFilter,
            event.productCategoryFilter,
            event.selectedCategoryName,
        )
    }

    private fun showProductSortingBottomSheet() {
        ProductSortingFragment().let { it.show(childFragmentManager, it.tag) }
    }

    override fun shouldExpandToolbar(): Boolean =
        isListAtTop && !productListViewModel.isSearching() && !productListViewModel.isSelecting()

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_action_mode_products_list, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedProductIds = productListViewModel.selectedProductIds.value.toList()
        return when (item.itemId) {
            R.id.menu_update_status -> {
                productListViewModel.onBulkUpdateStatusClicked(selectedProductIds)
                true
            }
            R.id.menu_update_price -> {
                productListViewModel.onBulkUpdatePriceClicked(selectedProductIds)
                true
            }
            R.id.menu_select_all -> {
                productListViewModel.onSelectAllProductsClicked()
                true
            }
            R.id.menu_update_stock_status -> {
                productListViewModel.onBulkUpdateStockStatusClicked(selectedProductIds)
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        if (!isDestroyingView) productListViewModel.exitSelectionMode()
    }

    companion object {
        val TAG: String = ProductListFragment::class.java.simpleName
        const val PRODUCT_FILTER_RESULT_KEY = "product_filter_result"
        private const val TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY = "non_root_navigation_in_detail_pane"
    }
}
