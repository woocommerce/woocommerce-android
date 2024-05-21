package com.woocommerce.android.ui.orders.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditFormBinding
import com.woocommerce.android.databinding.LayoutOrderCreationCustomerInfoBinding
import com.woocommerce.android.databinding.OrderCreationAdditionalInfoCollectionSectionBinding
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.selector.CouponSelectorFragment.Companion.KEY_COUPON_SELECTOR_RESULT
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.CustomAmountTypeBottomSheetDialog
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode.Creation
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode.Edit
import com.woocommerce.android.ui.orders.creation.configuration.EditProductConfigurationResult
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfigurationFragment
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListFragment
import com.woocommerce.android.ui.orders.creation.giftcards.OrderCreateEditGiftCardFragment.Companion.GIFT_CARD_RESULT
import com.woocommerce.android.ui.orders.creation.giftcards.OrderCreateEditGiftCardViewModel.GiftCardResult
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigator
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountFragment.Companion.KEY_PRODUCT_DISCOUNT_RESULT
import com.woocommerce.android.ui.orders.creation.shipping.OrderShippingFragment.Companion.REMOVE_SHIPPING_RESULT
import com.woocommerce.android.ui.orders.creation.shipping.OrderShippingFragment.Companion.UPDATE_SHIPPING_RESULT
import com.woocommerce.android.ui.orders.creation.shipping.ShippingLineFormSection
import com.woocommerce.android.ui.orders.creation.shipping.ShippingUpdateResult
import com.woocommerce.android.ui.orders.creation.simplepaymentsmigration.OrderCreateEditSimplePaymentsMigrationBottomSheetFragment
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRate
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorFragment.Companion.KEY_SELECTED_TAX_RATE
import com.woocommerce.android.ui.orders.creation.totals.OrderCreateEditTotalsView
import com.woocommerce.android.ui.orders.creation.views.ExpandableGroupedProductCard
import com.woocommerce.android.ui.orders.creation.views.ExpandableGroupedProductCardLoading
import com.woocommerce.android.ui.orders.creation.views.ExpandableProductCard
import com.woocommerce.android.ui.orders.creation.views.OrderCreateEditSectionView
import com.woocommerce.android.ui.orders.creation.views.OrderCreateEditSectionView.AddButton
import com.woocommerce.android.ui.orders.details.OrderStatusSelectorDialog.Companion.KEY_ORDER_STATUS_RESULT
import com.woocommerce.android.ui.orders.details.views.OrderDetailOrderStatusView
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorFragmentArgs
import com.woocommerce.android.ui.products.selector.ProductSelectorSharedViewModel
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.WCReadMoreTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@Suppress("LargeClass")
@AndroidEntryPoint
class OrderCreateEditFormFragment :
    BaseFragment(R.layout.fragment_order_create_edit_form),
    BackPressListener {
    private companion object {
        private const val TABLET_PANES_WIDTH_RATIO = 0.5F
        private const val XL_TABLET_PANES_WIDTH_RATIO = 0.6F
    }
    private val viewModel by fixedHiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    private val sharedViewModel: ProductSelectorSharedViewModel by activityViewModels()

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var uiHelper: OrderCreateEditFormAddInfoButtonsStatusHelper

    private var createOrderMenuItem: MenuItem? = null
    private var progressDialog: CustomProgressDialog? = null
    private var orderUpdateFailureSnackBar: Snackbar? = null

    private val args: OrderCreateEditFormFragmentArgs by navArgs()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val View?.customAmountAdapter
        get() = (this as? RecyclerView)
            ?.run { adapter as? OrderCreateEditCustomAmountAdapter }

    override fun onStart() {
        super.onStart()
        val navController =
            childFragmentManager.findFragmentById(R.id.product_selector_nav_container)?.findNavController()
        val args = ProductSelectorFragmentArgs(
            selectionHandling = ProductSelectorViewModel.SelectionHandling.NORMAL,
            selectedItems = viewModel.selectedItems.value.toTypedArray(),
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            selectionMode = ProductSelectorViewModel.SelectionMode.LIVE,
        )
        navController?.setGraph(R.navigation.nav_graph_product_selector, args.toBundle())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(FragmentOrderCreateEditFormBinding.bind(view)) {
            setupObserversWith(this)
            setupHandleResults(this)
            initView()
        }
        handleCouponEditResult()
        handleProductDetailsEditResult()
        handleResult<String>(KEY_COUPON_SELECTOR_RESULT) {
            viewModel.onCouponAdded(it)
        }
        handleTaxRateSelectionResult()
        viewModel.onDeviceConfigurationChanged(requireContext().windowSizeClass)
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) syncSelectedItems()
    }

    private fun syncSelectedItems() {
        lifecycleScope.launch {
            viewModel.pendingSelectedItems.collect {
                sharedViewModel.updateSelectedItems(it)
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            sharedViewModel.updateSelectedItems(viewModel.pendingSelectedItems.value)
            sharedViewModel.selectedItems.drop(1).collect {
                viewModel.onItemsSelectionChanged(it)
            }
        }
    }

    private fun handleTaxRateSelectionResult() {
        handleResult<TaxRate>(KEY_SELECTED_TAX_RATE) {
            viewModel.onTaxRateSelected(it)
        }
    }

    private fun handleProductDetailsEditResult() {
        handleResult<OrderCreationProduct>(KEY_PRODUCT_DISCOUNT_RESULT) {
            viewModel.onProductDiscountEditResult(it)
        }
    }

    private fun handleCouponEditResult() {
        args.couponEditResult?.let {
            viewModel.onCouponEditResult(it)
        }
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
        orderUpdateFailureSnackBar?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.updateSelectedItems(emptyList())
    }

    private fun FragmentOrderCreateEditFormBinding.initView() {
        initOrderStatusView()
        initCustomerAndNotesEmptySection()
        initProductsSection()
        initAdditionalInfoCollectionSection()
        initTaxRateSelectorSection()
        initTotalsSection()
        adjustUIForScreenSize()
    }

    private fun FragmentOrderCreateEditFormBinding.adjustUIForScreenSize() {
        productSelectorNavContainer.isVisible = requireContext().windowSizeClass != WindowSizeClass.Compact
        when (requireContext().windowSizeClass) {
            WindowSizeClass.Compact -> twoPaneLayoutGuideline.setGuidelinePercent(0.0f)
            WindowSizeClass.Medium -> twoPaneLayoutGuideline.setGuidelinePercent(TABLET_PANES_WIDTH_RATIO)
            WindowSizeClass.ExpandedAndBigger -> twoPaneLayoutGuideline.setGuidelinePercent(XL_TABLET_PANES_WIDTH_RATIO)
        }
        setupToolbars(requireContext().windowSizeClass != WindowSizeClass.Compact)
    }

    private fun FragmentOrderCreateEditFormBinding.setupToolbars(isTablet: Boolean) {
        twoPaneModeToolbar.isVisible = isTablet
        if (isTablet) {
            mainToolbar.navigationIcon = null
            mainToolbar.title = getString(R.string.order_creation_tablet_mode_fragment_title)
            twoPaneModeToolbar.title = getTitle()
            twoPaneModeToolbar.setNavigationIcon(R.drawable.ic_back_24dp)
        } else {
            val navigationIcon = when (viewModel.mode) {
                is Creation -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_back_24dp)
                is Edit -> null
            }
            mainToolbar.navigationIcon = navigationIcon
            mainToolbar.title = getTitle()
        }
        mainToolbar.setNavigationOnClickListener { viewModel.onBackButtonClicked() }
        twoPaneModeToolbar.setNavigationOnClickListener { viewModel.onBackButtonClicked() }
        mainToolbar.inflateMenu(R.menu.menu_order_creation)
        createOrderMenuItem = mainToolbar.menu.findItem(R.id.menu_create).apply {
            when (viewModel.mode) {
                is Creation -> title = resources.getString(R.string.create)
                is Edit -> isVisible = false
            }
            isEnabled = viewModel.viewStateData.liveData.value?.isCreateOrderButtonEnabled ?: false
        }
        mainToolbar.setOnMenuItemClickListener { item ->
            return@setOnMenuItemClickListener when (item.itemId) {
                R.id.menu_create -> {
                    viewModel.onCreateOrderClicked(viewModel.currentDraft)
                    true
                }
                else -> false
            }
        }
    }

    private fun getTitle(): CharSequence = when (viewModel.mode) {
        is Creation -> getString(R.string.order_creation_fragment_title)
        is Edit -> {
            val orderId = (viewModel.mode as Edit).orderId.toString()
            getString(R.string.orderdetail_orderstatus_ordernum, orderId)
        }
    }

    private fun FragmentOrderCreateEditFormBinding.initTotalsSection() {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            viewModel.onScreenScrolledVertically(scrollY, oldScrollY)
        }
        totalsSection.setContent { OrderCreateEditTotalsView(viewModel) }
    }

    private fun FragmentOrderCreateEditFormBinding.initTaxRateSelectorSection() {
        taxRateSelectorSection.isVisible = true
        setTaxRateButton.setOnClickListener {
            viewModel.onSetTaxRateClicked()
        }
    }

    private fun FragmentOrderCreateEditFormBinding.initOrderStatusView() {
        when (viewModel.mode) {
            is Creation -> {
                orderStatusView.visibility = View.GONE
            }

            is Edit -> {
                orderStatusView.initView(
                    mode = OrderDetailOrderStatusView.Mode.OrderEdit,
                    editOrderStatusClickListener = {
                        viewModel.orderStatusData.value?.let {
                            viewModel.onEditOrderStatusClicked(it)
                        }
                    }
                )
            }
        }
    }

    private fun FragmentOrderCreateEditFormBinding.initNotesSection() {
        notesSection.setEditButtonContentDescription(
            contentDescription = getString(R.string.order_creation_customer_note_edit_content_description)
        )
        notesSection.setAddButtons(
            listOf(
                AddButton(
                    text = getString(R.string.order_creation_add_customer_note),
                    onClickListener = {
                        viewModel.onCustomerNoteClicked()
                    }
                )
            )
        )
        notesSection.setOnEditButtonClicked {
            viewModel.onCustomerNoteClicked()
        }
        notesSection.setEditButtonContentDescription(
            contentDescription = getString(R.string.order_creation_customer_note_edit_content_description)
        )
    }

    private fun FragmentOrderCreateEditFormBinding.initCustomerSection() {
        customerSection.setAddButtons(
            listOf(
                AddButton(
                    text = getString(R.string.order_creation_add_customer),
                    onClickListener = {
                        viewModel.onAddCustomerClicked()
                    }
                )
            )
        )
        customerSection.setOnEditButtonClicked {
            viewModel.onEditCustomerClicked()
        }
        customerSection.setEditButtonContentDescription(
            contentDescription = getString(R.string.order_creation_customer_edit_content_description)
        )
    }

    private fun FragmentOrderCreateEditFormBinding.initCustomerAndNotesEmptySection() {
        customerSection.setAddButtons(
            listOf(
                AddButton(
                    text = getString(R.string.order_creation_add_customer),
                    onClickListener = {
                        viewModel.onAddCustomerClicked()
                    }
                ),
                AddButton(
                    text = getString(R.string.order_creation_add_customer_note),
                    onClickListener = {
                        viewModel.onCustomerNoteClicked()
                    }
                )
            )
        )
    }

    private fun FragmentOrderCreateEditFormBinding.initProductsSection() {
        productsSection.hideHeader()
        renderDefaultProductsSectionButtons()
    }

    private fun FragmentOrderCreateEditFormBinding.initAdditionalInfoCollectionSection() {
        additionalInfoCollectionSection.addShippingButton.setOnClickListener {
            viewModel.onAddOrEditShipping()
        }
    }

    private fun LayoutOrderCreationCustomerInfoBinding.changeState() {
        if (root.currentState == R.id.start) {
            root.transitionToEnd()
        } else {
            root.transitionToStart()
        }
    }

    private fun setupObserversWith(binding: FragmentOrderCreateEditFormBinding) {
        viewModel.orderDraft.observe(viewLifecycleOwner) { newOrderData ->
            binding.orderStatusView.updateOrder(newOrderData)
            bindCustomerAddressAndNotesSection(binding, newOrderData)
            bindAdditionalInfoCollectionSection(binding.additionalInfoCollectionSection, newOrderData)
        }

        viewModel.orderStatusData.observe(viewLifecycleOwner) {
            binding.orderStatusView.updateStatus(it)
        }

        viewModel.products.observe(viewLifecycleOwner) {
            bindProductsSection(binding.productsSection, viewModel.products)
        }

        viewModel.customAmounts.observe(viewLifecycleOwner) {
            bindCustomAmountsSection(binding.customAmountsSection, it)
        }

        bindShippingLinesSection(binding)

        observeViewStateChanges(binding)

        viewModel.event.observe(viewLifecycleOwner) { handleViewModelEvents(it, binding) }
    }
    private fun bindShippingLinesSection(binding: FragmentOrderCreateEditFormBinding) {
        binding.shippingLines.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                viewModel.shippingLineList.observeAsState().value?.let { shippingLines ->
                    WooThemeWithBackground {
                        ShippingLineFormSection(
                            shippingLineDetails = shippingLines,
                            formatCurrency = { amount -> currencyFormatter.formatCurrency(amount) },
                            modifier = Modifier.padding(bottom = 1.dp),
                            onAdd = { viewModel.onAddOrEditShipping() },
                            onEdit = { id -> viewModel.onAddOrEditShipping(id) }
                        )
                    }
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun observeViewStateChanges(binding: FragmentOrderCreateEditFormBinding) {
        viewModel.combinedProductAndCustomAmountsLiveData.observe(viewLifecycleOwner) {
            updateProductsAndCustomAmountsSectionUI(it, binding)
        }
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            updateProductsAndCustomAmountsSectionUI(new, binding)
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) { show ->
                if (show) showProgressDialog() else hideProgressDialog()
            }
            new.isCreateOrderButtonEnabled.takeIfNotEqualTo(old?.isCreateOrderButtonEnabled) {
                createOrderMenuItem?.isEnabled = it
            }
            new.isIdle.takeIfNotEqualTo(old?.isIdle) { idle ->
                updateProgressBarsVisibility(binding, !idle)
                if (new.isEditable) {
                    uiHelper.changeAddInfoButtonsEnabledState(
                        binding,
                        AddInfoButtonsStateTransition(
                            isAddShippingButtonState = AddInfoButtonsStateTransition.State.Change(
                                enabled = new.isAddShippingButtonEnabled && idle
                            ),
                            isAddCouponButtonState = AddInfoButtonsStateTransition.State.Change(
                                enabled = new.isCouponButtonEnabled && idle
                            ),
                            isAddGiftCardButtonState = AddInfoButtonsStateTransition.State.Change(
                                enabled = new.isAddGiftCardButtonEnabled && idle
                            )
                        )
                    )

                    binding.productsSection.isEachAddButtonEnabled = idle
                }
                sharedViewModel.onProductSelectionStateChanged(idle && new.isEditable)
            }
            new.showOrderUpdateSnackbar.takeIfNotEqualTo(old?.showOrderUpdateSnackbar) { show ->
                showOrHideErrorSnackBar(show)
            }
            new.isEditable.takeIfNotEqualTo(old?.isEditable) { isEditable ->
                if (isEditable) {
                    binding.showEditableControls(new)
                } else {
                    binding.hideEditableControls()
                }
            }
            new.isCouponButtonEnabled.takeIfNotEqualTo(old?.isCouponButtonEnabled) {
                uiHelper.changeAddInfoButtonsEnabledState(
                    binding,
                    AddInfoButtonsStateTransition(
                        isAddCouponButtonState = AddInfoButtonsStateTransition.State.Change(
                            enabled = it
                        )
                    )
                )
            }
            new.isAddShippingButtonEnabled.takeIfNotEqualTo(old?.isAddShippingButtonEnabled) {
                uiHelper.changeAddInfoButtonsEnabledState(
                    binding,
                    AddInfoButtonsStateTransition(
                        isAddShippingButtonState = AddInfoButtonsStateTransition.State.Change(
                            enabled = it
                        )
                    )
                )
            }
            new.isAddGiftCardButtonEnabled.takeIfNotEqualTo(old?.isAddGiftCardButtonEnabled) {
                uiHelper.changeAddInfoButtonsEnabledState(
                    binding,
                    AddInfoButtonsStateTransition(
                        isAddGiftCardButtonState = AddInfoButtonsStateTransition.State.Change(
                            enabled = it
                        )
                    )
                )
            }
            new.shouldDisplayAddGiftCardButton.takeIfNotEqualTo(old?.shouldDisplayAddGiftCardButton) {
                binding.additionalInfoCollectionSection.addGiftCardButtonGroup.isVisible = it
            }
            new.taxRateSelectorButtonState.takeIfNotEqualTo(old?.taxRateSelectorButtonState) {
                binding.taxRateSelectorSection.isVisible = it.isShown
                binding.setTaxRateButton.text = it.label
            }
        }
    }

    private fun updateProductsAndCustomAmountsSectionUI(
        viewState: OrderCreateEditViewModel.ViewState?,
        binding: FragmentOrderCreateEditFormBinding
    ) {
        when {
            // Both products and custom amounts are empty
            (viewState == null || viewState.productsSectionState.isEmpty) &&
                (viewState == null || viewState.customAmountSectionState.isEmpty) -> {
                bothProductsAndCustomAmountsAreUnset(binding)
            }

            // Product has been added, but the custom amount remains unset.
            !viewState.productsSectionState.isEmpty && viewState.customAmountSectionState.isEmpty -> {
                productAddedCustomAmountUnset(binding)
            }

            // Custom amount has been set, but no product has been added.
            viewState.productsSectionState.isEmpty && !viewState.customAmountSectionState.isEmpty -> {
                customAmountAddedProductUnset(binding)
            }

            // Both the product and custom amount have been added.
            !viewState.productsSectionState.isEmpty && !viewState.customAmountSectionState.isEmpty -> {
                productAndCustomAmountAdded(binding)
            }
        }
    }

    private fun customAmountAddedProductUnset(binding: FragmentOrderCreateEditFormBinding) {
        binding.customAmountsSection.removeCustomSectionButtons()
        binding.customAmountsSection.show()
        binding.customAmountsSection.showHeader()
        if (viewModel.viewStateData.liveData.value?.isEditable == true) {
            binding.customAmountsSection.showAddAction()
        } else {
            binding.customAmountsSection.hideAddAction()
        }

        binding.productsSection.removeCustomSectionButtons()
        binding.productsSection.hideAddProductsHeaderActions()
        binding.productsSection.hideHeader()
        binding.productsSection.content = null
        if (requireContext().windowSizeClass == WindowSizeClass.Compact) {
            binding.productsSection.setProductSectionButtons(
                addProductsButton = AddButton(
                    text = getString(R.string.order_creation_add_products),
                    onClickListener = {
                        viewModel.onAddProductClicked()
                    }
                ),
                addProductsViaScanIconButton = AddButton(
                    text = getString(R.string.order_creation_add_product_via_barcode_scanning),
                    onClickListener = { viewModel.onScanClicked() }
                ),
            )
        } else {
            binding.productsSection.setProductSectionButtons(
                addProductsViaScanButton = AddButton(
                    text = getString(R.string.order_creation_scan_products),
                    onClickListener = { viewModel.onScanClicked() },
                )
            )
        }
    }

    private fun productAddedCustomAmountUnset(binding: FragmentOrderCreateEditFormBinding) {
        if (viewModel.viewStateData.liveData.value?.isEditable == true) {
            if (requireContext().windowSizeClass == WindowSizeClass.Compact) {
                binding.productsSection.showAddProductsHeaderActions()
            } else {
                binding.productsSection.hideAddProductsHeaderActions()
                binding.productsSection.showScanProductsHeaderAction()
            }
        } else {
            binding.productsSection.hideAddProductsHeaderActions()
        }
        binding.productsSection.showHeader()
        binding.productsSection.removeProductsButtons()
        binding.customAmountsSection.show()
        binding.customAmountsSection.hideAddAction()
        binding.customAmountsSection.content = null

        binding.customAmountsSection.setCustomAmountsSectionButtons(
            addCustomAmountsButton = AddButton(
                text = getString(R.string.order_creation_add_custom_amounts),
                onClickListener = { navigateToCustomAmountsDialog() }
            )
        )
    }

    private fun productAndCustomAmountAdded(binding: FragmentOrderCreateEditFormBinding) {
        binding.productsSection.showHeader()
        binding.productsSection.removeProductsButtons()
        binding.customAmountsSection.show()
        binding.customAmountsSection.removeCustomSectionButtons()
        binding.customAmountsSection.showHeader()
        if (viewModel.viewStateData.liveData.value?.isEditable == true) {
            if (requireContext().windowSizeClass == WindowSizeClass.Compact) {
                binding.productsSection.showAddProductsHeaderActions()
            } else {
                binding.productsSection.hideAddProductsHeaderActions()
                binding.productsSection.showScanProductsHeaderAction()
            }
            binding.customAmountsSection.showAddAction()
        } else {
            binding.customAmountsSection.hideAddAction()
            binding.productsSection.hideAddProductsHeaderActions()
        }
    }

    private fun bothProductsAndCustomAmountsAreUnset(binding: FragmentOrderCreateEditFormBinding) {
        binding.productsSection.hideAddProductsHeaderActions()
        binding.productsSection.hideHeader()
        binding.productsSection.content = null
        binding.renderDefaultProductsSectionButtons()
        binding.customAmountsSection.hide()
    }

    private fun FragmentOrderCreateEditFormBinding.renderDefaultProductsSectionButtons() {
        if (requireContext().windowSizeClass == WindowSizeClass.Compact) {
            productsSection.setProductSectionButtons(
                addProductsButton = AddButton(
                    text = getString(R.string.order_creation_add_products),
                    onClickListener = {
                        viewModel.onAddProductClicked()
                    }
                ),
                addProductsViaScanIconButton = AddButton(
                    text = getString(R.string.order_creation_add_product_via_barcode_scanning),
                    onClickListener = { viewModel.onScanClicked() }
                ),
                addCustomAmountsButton = AddButton(
                    text = getString(R.string.order_creation_add_custom_amounts),
                    onClickListener = {
                        navigateToCustomAmountsDialog()
                    }
                )
            )
        } else {
            productsSection.setProductSectionButtons(
                addProductsViaScanButton = AddButton(
                    text = getString(R.string.order_creation_scan_products),
                    onClickListener = { viewModel.onScanClicked() },
                ),
                addCustomAmountsButton = AddButton(
                    text = getString(R.string.order_creation_add_custom_amounts),
                    onClickListener = {
                        navigateToCustomAmountsDialog()
                    }
                )
            )
        }
    }

    private fun navigateToCustomAmountsDialog(
        customAmountUIModel: CustomAmountUIModel = CustomAmountUIModel.EMPTY,
        orderTotal: String = viewModel.orderDraft.value?.total.toString(),
    ) {
        if (viewModel.orderContainsProductsOrCustomAmounts()) {
            displayCustomAmountTypeBottomSheet()
        } else {
            OrderCreateEditNavigator.navigate(
                this,
                OrderCreateEditNavigationTarget.CustomAmountDialog(
                    customAmountUIModel.copy(type = FIXED_CUSTOM_AMOUNT),
                    orderTotal
                )
            )
        }
    }

    private fun displayCustomAmountTypeBottomSheet() {
        val bottomSheet = CustomAmountTypeBottomSheetDialog()
        bottomSheet.show(requireActivity().supportFragmentManager, bottomSheet.tag)
    }

    private fun updateProgressBarsVisibility(
        binding: FragmentOrderCreateEditFormBinding,
        shouldShowProgressBars: Boolean
    ) {
        binding.loadingProgress.isVisible = shouldShowProgressBars
    }

    private fun bindAdditionalInfoCollectionSection(
        additionalInfoCollectionSection: OrderCreationAdditionalInfoCollectionSectionBinding,
        newOrderData: Order
    ) {
        additionalInfoCollectionSection.addCouponButton.setOnClickListener { viewModel.onAddCouponButtonClicked() }
        additionalInfoCollectionSection.bindGiftCardSubSection(newOrderData)

        val firstShipping = newOrderData.shippingLines.firstOrNull { it.methodId != null }
        if (firstShipping != null) {
            additionalInfoCollectionSection.addShippingButtonGroup.hide()
        } else {
            additionalInfoCollectionSection.addShippingButtonGroup.show()
        }
    }

    private fun OrderCreationAdditionalInfoCollectionSectionBinding.bindGiftCardSubSection(newOrderData: Order) {
        when (viewModel.mode) {
            is Creation -> bindGiftCardForOrderCreation(newOrderData)
            is Edit -> addGiftCardButtonGroup.isVisible = false
        }
    }

    private fun OrderCreationAdditionalInfoCollectionSectionBinding.bindGiftCardForOrderCreation(
        newOrderData: Order
    ) {
        if (newOrderData.selectedGiftCard.isNullOrEmpty()) {
            addGiftCardButtonGroup.isVisible = viewModel.isGiftCardExtensionEnabled
            addGiftCardButton.setOnClickListener { viewModel.onAddGiftCardButtonClicked() }
        } else {
            addGiftCardButtonGroup.isVisible = false
        }
    }

    private fun bindNotesSection(notesSection: OrderCreateEditSectionView, customerNote: String) {
        notesSection.show()
        notesSection.showHeader()
        customerNote.takeIf { it.isNotBlank() }
            ?.let { noteText ->
                WCReadMoreTextView(requireContext()).also {
                    it.show(
                        content = noteText,
                        dialogCaptionId = R.string.order_creation_customer_note
                    )
                    notesSection.content = it
                }
            }
    }

    private fun bindProductsSection(
        productsSection: OrderCreateEditSectionView,
        products: LiveData<List<OrderCreationProduct>>
    ) {
        productsSection.setContentHorizontalPadding(R.dimen.minor_00)
        if (products.value.isNullOrEmpty()) {
            productsSection.content = null
        }
        if (productsSection.content == null) {
            productsSection.content = ComposeView(requireContext()).apply {
                setViewCompositionStrategy(DisposeOnLifecycleDestroyed(viewLifecycleOwner))
                bindExpandableProductsSection(products)
            }
        }
        productsSection.barcodeIcon.setOnClickListener {
            viewModel.onScanClicked()
        }
        productsSection.addIcon.setOnClickListener {
            viewModel.onAddProductClicked()
        }
    }

    private fun bindCustomAmountsSection(
        customAmountsSection: OrderCreateEditSectionView,
        customAmounts: List<CustomAmountUIModel>?
    ) {
        customAmountsSection.setContentHorizontalPadding(R.dimen.minor_00)
        if (customAmounts.isNullOrEmpty()) {
            customAmountsSection.hide()
        } else {
            if (customAmountsSection.content == null) {
                val animator = DefaultItemAnimator().apply {
                    // Disable change animations to avoid duplicating viewholders
                    supportsChangeAnimations = false
                }
                customAmountsSection.content = RecyclerView(requireContext()).apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = OrderCreateEditCustomAmountAdapter(
                        currencyFormatter,
                        onCustomAmountClick = {
                            viewModel.selectCustomAmount(it)
                            navigateToCustomAmountsDialog(
                                customAmountUIModel = it,
                            )
                        }
                    )
                    itemAnimator = animator
                    isNestedScrollingEnabled = false
                }
            }
            customAmountsSection.content.customAmountAdapter?.apply {
                submitList(customAmounts)
            }
            customAmountsSection.addIcon.setOnClickListener {
                navigateToCustomAmountsDialog()
            }
        }
    }

    @Suppress("LongMethod")
    private fun ComposeView.bindExpandableProductsSection(items: LiveData<List<OrderCreationProduct>>) {
        setContent {
            val state = items.observeAsState(emptyList())
            WooTheme {
                Column {
                    state.value.forEach { item ->
                        var isExpanded by rememberSaveable { mutableStateOf(false) }
                        when {
                            item is OrderCreationProduct.ProductItemWithRules &&
                                item.getConfiguration().childrenConfiguration?.keys?.size?.compareTo(0) == 1 -> {
                                val modifier = if (isExpanded) {
                                    Modifier.border(
                                        1.dp,
                                        colorResource(id = R.color.color_on_surface),
                                        shape = RoundedCornerShape(
                                            topStart = dimensionResource(id = R.dimen.corner_radius_large),
                                            topEnd = dimensionResource(id = R.dimen.corner_radius_large)
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                                ExpandableGroupedProductCardLoading(
                                    state = viewModel.viewStateData.liveData.observeAsState(),
                                    product = item,
                                    childrenSize = item.getConfiguration().childrenConfiguration?.keys?.size ?: 0,
                                    onRemoveProductClicked = { viewModel.onRemoveProduct(item) },
                                    onDiscountButtonClicked = { viewModel.onDiscountButtonClicked(item) },
                                    onItemAmountChanged = { viewModel.onItemAmountChanged(item, it) },
                                    onEditConfigurationClicked = { viewModel.onEditConfiguration(item) },
                                    onProductExpanded = { expanded, product ->
                                        isExpanded = expanded
                                        viewModel.onProductExpanded(isExpanded, product)
                                    },
                                    modifier = modifier,
                                    isExpanded = isExpanded
                                )
                            }

                            item is OrderCreationProduct.GroupedProductItemWithRules -> {
                                val modifier = if (isExpanded) {
                                    Modifier.border(
                                        1.dp,
                                        colorResource(id = R.color.color_on_surface),
                                        shape = RoundedCornerShape(
                                            topStart = dimensionResource(id = R.dimen.corner_radius_large),
                                            topEnd = dimensionResource(id = R.dimen.corner_radius_large)
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                                ExpandableGroupedProductCard(
                                    state = viewModel.viewStateData.liveData.observeAsState(),
                                    product = item,
                                    children = item.children,
                                    onRemoveProductClicked = { viewModel.onRemoveProduct(item) },
                                    onDiscountButtonClicked = { viewModel.onDiscountButtonClicked(item) },
                                    onEditConfigurationClicked = { viewModel.onEditConfiguration(item) },
                                    onProductExpanded = { expanded, product ->
                                        isExpanded = expanded
                                        viewModel.onProductExpanded(isExpanded, product)
                                    },
                                    onItemAmountChanged = { viewModel.onItemAmountChanged(item, it) },
                                    onChildProductExpanded = viewModel::onProductExpanded,
                                    modifier = modifier,
                                    isExpanded = isExpanded
                                )
                            }

                            else -> {
                                ExpandableProductCard(
                                    viewModel.viewStateData.liveData.observeAsState(),
                                    item,
                                    onRemoveProductClicked = { viewModel.onRemoveProduct(item) },
                                    onDiscountButtonClicked = { viewModel.onDiscountButtonClicked(item) },
                                    onItemAmountChanged = { viewModel.onItemAmountChanged(item, it) },
                                    onEditConfigurationClicked = { viewModel.onEditConfiguration(item) },
                                    onProductExpanded = { expanded, product ->
                                        isExpanded = expanded
                                        viewModel.onProductExpanded(isExpanded, product)
                                    },
                                    modifier = Modifier
                                        .padding(
                                            horizontal = dimensionResource(id = R.dimen.major_100),
                                            vertical = dimensionResource(id = R.dimen.minor_50)
                                        )
                                        .border(
                                            1.dp,
                                            colorResource(
                                                id = if (isExpanded) {
                                                    R.color.color_on_surface
                                                } else {
                                                    R.color.divider_color
                                                }
                                            ),
                                            shape = RoundedCornerShape(
                                                dimensionResource(id = R.dimen.corner_radius_large)
                                            )
                                        ),
                                    isExpanded = isExpanded
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindCustomerAddressSection(binding: FragmentOrderCreateEditFormBinding, order: Order) {
        val customerAddressSection: OrderCreateEditSectionView = binding.customerSection
        customerAddressSection.setContentHorizontalPadding(R.dimen.minor_00)

        val customer = order.customer
        if (customer == null || customer == Order.Customer.EMPTY) {
            customerAddressSection.content = null
            return
        }

        val view = LayoutOrderCreationCustomerInfoBinding.inflate(layoutInflater)
        val customerEmailOrNamePresent =
            customer.email.isNotNullOrEmpty() ||
                customer.firstName.isNotNullOrEmpty() ||
                customer.lastName.isNotNullOrEmpty()
        if (customerEmailOrNamePresent) {
            view.nameEmail.isVisible = true
            view.name.text = "${customer.firstName} ${customer.lastName}"
            view.email.text = customer.email
            if (customer.shippingAddress == Address.EMPTY && customer.billingAddress == Address.EMPTY) {
                view.nameDivider.isVisible = false
            }
        } else {
            view.nameEmail.isVisible = false
        }

        if (customer.shippingAddress != Address.EMPTY) {
            view.shippingGroup.isVisible = true
            val shippingAddressDetails = order.formatShippingInformationForDisplay()
            view.shippingAddressDetails.text = shippingAddressDetails
            view.shippingAddressDetails.contentDescription =
                shippingAddressDetails.replace("\n", ". ")
        } else {
            view.shippingGroup.isVisible = false
        }

        if (customer.billingAddress != Address.EMPTY) {
            view.billingGroup.isVisible = true
            val billingAddressDetails = order.formatBillingInformationForDisplay()
            view.billingAddressDetails.text = billingAddressDetails
            view.billingAddressDetails.contentDescription =
                billingAddressDetails.replace("\n", ". ")
            view.customerInfoViewMoreButtonTitle.setOnClickListener {
                view.changeState()
            }
        } else {
            view.billingGroup.isVisible = false
        }

        customerAddressSection.content = view.root
    }

    private fun bindCustomerAddressAndNotesSection(binding: FragmentOrderCreateEditFormBinding, newOrderData: Order) {
        with(binding) {
            when {
                shouldHideCustomerAddressAndNotesSections(newOrderData) -> {
                    hideCustomerAddressAndNotesSections()
                }

                shouldShowCustomerSectionOnly(newOrderData) -> {
                    showCustomerSectionOnly(newOrderData)
                }

                shouldShowNotesSectionOnly(newOrderData) -> {
                    showNotesSectionOnly(newOrderData)
                }
                // Both customer address and customer notes are added
                else -> {
                    displayCustomerAddress(newOrderData)
                    displayCustomerNotes(newOrderData)
                }
            }
        }
    }

    private fun shouldHideCustomerAddressAndNotesSections(newOrderData: Order) =
        (newOrderData.customer == null || newOrderData.customer == Order.Customer.EMPTY) &&
            newOrderData.customerNote.isEmpty()

    private fun shouldShowCustomerSectionOnly(newOrderData: Order) =
        (newOrderData.customer != null && newOrderData.customer != Order.Customer.EMPTY) &&
            newOrderData.customerNote.isEmpty()

    private fun shouldShowNotesSectionOnly(newOrderData: Order) =
        (newOrderData.customer == null || newOrderData.customer == Order.Customer.EMPTY) &&
            newOrderData.customerNote.isNotNullOrEmpty()

    private fun FragmentOrderCreateEditFormBinding.hideCustomerAddressAndNotesSections() {
        customerSection.apply {
            hideHeader()
        }
        notesSection.apply {
            content = null
            hideHeader()
            removeCustomSectionButtons()
            hide()
        }
    }

    private fun FragmentOrderCreateEditFormBinding.showCustomerSectionOnly(newOrderData: Order) {
        with(this) {
            notesSection.apply {
                show()
                hideHeader()
            }
            initNotesSection()
            displayCustomerAddress(newOrderData)
        }
    }

    private fun FragmentOrderCreateEditFormBinding.showNotesSectionOnly(newOrderData: Order) {
        with(this) {
            customerSection.apply {
                show()
                hideHeader()
            }
            displayCustomerNotes(newOrderData)
            initCustomerSection()
        }
    }

    private fun FragmentOrderCreateEditFormBinding.displayCustomerNotes(newOrderData: Order) {
        with(this) {
            initNotesSection()
            notesSection.apply {
                show()
                showHeader()
                bindNotesSection(this, newOrderData.customerNote)
            }
        }
    }

    private fun FragmentOrderCreateEditFormBinding.displayCustomerAddress(newOrderData: Order) {
        with(this) {
            initCustomerSection()
            customerSection.apply {
                show()
                showHeader()
                header = getString(R.string.order_creation_customer)
            }
            bindCustomerAddressSection(this, newOrderData)
        }
    }

    private fun setupHandleResults(fragmentOrderCreateEditFormBinding: FragmentOrderCreateEditFormBinding) {
        handleDialogResult<OrderStatusUpdateSource>(
            key = KEY_ORDER_STATUS_RESULT,
            entryId = R.id.orderCreationFragment
        ) { viewModel.onOrderStatusChanged(Order.Status.fromValue(it.newStatus)) }
        handleDialogNotice(
            key = OrderCreateEditSimplePaymentsMigrationBottomSheetFragment.KEY_ON_ADD_CUSTOM_AMOUNT_CLICKED_NOTICE,
            entryId = R.id.orderCreationFragment
        ) {
            navigateToCustomAmountDialogWhenViewIsCreated(fragmentOrderCreateEditFormBinding.root)
        }

        handleResult<Collection<SelectedItem>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            viewModel.onProductsSelected(it)
        }
        handleResult<CodeScannerStatus>(BarcodeScanningFragment.KEY_BARCODE_SCANNING_SCAN_STATUS) { status ->
            viewModel.handleBarcodeScannedStatus(status)
        }
        handleResult<EditProductConfigurationResult>(
            ProductConfigurationFragment.PRODUCT_CONFIGURATION_EDITED_RESULT
        ) { result ->
            viewModel.onConfigurationChanged(result.itemId, result.productConfiguration)
        }
        handleResult<GiftCardResult>(GIFT_CARD_RESULT) { result ->
            viewModel.onGiftCardSelected(result.selectedGiftCard)
        }
        handleResult<Order.Customer>(CustomerListFragment.KEY_CUSTOMER_RESULT) {
            viewModel.onCustomerEdited(it)
        }
        handleResult<ShippingUpdateResult>(UPDATE_SHIPPING_RESULT) { result ->
            viewModel.onUpdatedShipping(result)
        }
        handleResult<Long>(REMOVE_SHIPPING_RESULT) { result ->
            viewModel.onRemoveShipping(result)
        }
    }

    @Suppress("ComplexMethod")
    private fun handleViewModelEvents(event: Event, binding: FragmentOrderCreateEditFormBinding) {
        when (event) {
            is OrderCreateEditNavigationTarget -> OrderCreateEditNavigator.navigate(this, event)
            is ViewOrderStatusSelector ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderStatusSelectorDialog(
                        currentStatus = event.currentStatus,
                        orderStatusList = event.orderStatusList
                    ).let { findNavController().navigateSafely(it) }

            is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            is ShowDialog -> event.showDialog()
            is OnAddingProductViaScanningFailed -> {
                uiMessageResolver.getRetrySnack(
                    message = event.message,
                    isIndefinite = false,
                    actionListener = event.retry
                ).show()
            }

            is OpenBarcodeScanningFragment -> {
                findNavController().navigateSafely(
                    OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToBarcodeScanningFragment()
                )
            }

            is VMKilledWhenScanningInProgress -> {
                ToastUtils.showToast(
                    context,
                    event.message
                )
            }

            is OnCouponRejectedByBackend -> {
                uiMessageResolver.getSnack(
                    stringResId = event.message
                ).show()
            }

            is OnCustomAmountTypeSelected -> {
                OrderCreateEditNavigator.navigate(
                    this,
                    OrderCreateEditNavigationTarget.CustomAmountDialog(
                        customAmountUIModel = viewModel.selectedCustomAmount.value?.copy(
                            type = event.type
                        ) ?: CustomAmountUIModel.EMPTY.copy(type = event.type),
                        orderTotal = viewModel.orderDraft.value?.total.toString(),
                    )
                )
            }

            is OnTotalsSectionHeightChanged -> {
                binding.scrollView.setPadding(0, 0, 0, event.newHeight)
            }

            is OnSelectedProductsSyncRequested -> {
                if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                    sharedViewModel.selectedItems.value.let { viewModel.onProductsSelected(it) }
                }
            }

            is Exit -> findNavController().navigateUp()
        }
    }

    private fun showProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(R.string.order_creation_loading_dialog_title),
            getString(R.string.order_creation_loading_dialog_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    @Suppress("MagicNumber")
    private fun showOrHideErrorSnackBar(show: Boolean) {
        if (show) {
            val orderUpdateFailureSnackBar = orderUpdateFailureSnackBar ?: uiMessageResolver.getIndefiniteActionSnack(
                message = getString(R.string.order_sync_failed),
                actionText = getString(R.string.retry),
                actionListener = { viewModel.onRetryPaymentsClicked() }
            ).also {
                orderUpdateFailureSnackBar = it
            }

            // If the snackbar was dismissed recently, a call to show will be ignore
            val delay = if (orderUpdateFailureSnackBar.isShown) 500L else 0L
            requireView().postDelayed({
                orderUpdateFailureSnackBar.show()
            }, delay)
        } else {
            orderUpdateFailureSnackBar?.dismiss()
        }
    }

    /**
     * This is workaround, as in this point navigation component
     * still didn't finish previous navigation, we have to make sure
     * that to delay navigation to the dialog. As marker that we can navigate is the view is created
     */
    private fun navigateToCustomAmountDialogWhenViewIsCreated(root: View) {
        root.post { navigateToCustomAmountsDialog() }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }

    private fun FragmentOrderCreateEditFormBinding.showEditableControls(
        state: OrderCreateEditViewModel.ViewState
    ) {
        messageNoEditableFields.visibility = View.GONE
        productsSection.apply {
            isLocked = false
            isEachAddButtonEnabled = true
        }
        uiHelper.changeAddInfoButtonsEnabledState(
            this,
            AddInfoButtonsStateTransition(
                isAddShippingButtonState = AddInfoButtonsStateTransition.State.Change(
                    enabled = true
                ),
                isAddCouponButtonState = AddInfoButtonsStateTransition.State.Change(
                    enabled = state.isCouponButtonEnabled
                ),
                isAddGiftCardButtonState = AddInfoButtonsStateTransition.State.Change(
                    enabled = state.isAddGiftCardButtonEnabled
                )
            )
        )
        customAmountsSection.apply {
            isLocked = false
            isEachAddButtonEnabled = true
        }
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            sharedViewModel.onProductSelectionStateChanged(true)
        }
    }

    private fun FragmentOrderCreateEditFormBinding.hideEditableControls() {
        messageNoEditableFields.visibility = View.VISIBLE
        productsSection.apply {
            isLocked = true
            isEachAddButtonEnabled = false
        }
        uiHelper.changeAddInfoButtonsEnabledState(
            this,
            AddInfoButtonsStateTransition(
                isAddShippingButtonState = AddInfoButtonsStateTransition.State.Change(
                    enabled = false
                ),
                isAddCouponButtonState = AddInfoButtonsStateTransition.State.Change(
                    enabled = false
                ),
                isAddGiftCardButtonState = AddInfoButtonsStateTransition.State.Change(
                    enabled = false
                )
            )
        )
        customAmountsSection.apply {
            isLocked = true
            isEachAddButtonEnabled = false
        }
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            sharedViewModel.onProductSelectionStateChanged(false)
        }
    }
}
