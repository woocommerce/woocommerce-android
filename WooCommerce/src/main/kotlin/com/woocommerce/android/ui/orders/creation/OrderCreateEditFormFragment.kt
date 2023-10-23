package com.woocommerce.android.ui.orders.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditFormBinding
import com.woocommerce.android.databinding.LayoutOrderCreationCustomerInfoBinding
import com.woocommerce.android.databinding.OrderCreationPaymentSectionBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.coupons.selector.CouponSelectorFragment.Companion.KEY_COUPON_SELECTOR_RESULT
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.MultipleLinesContext.None
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.MultipleLinesContext.Warning
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigator
import com.woocommerce.android.ui.orders.creation.product.details.OrderCreateEditProductDetailsFragment.Companion.KEY_PRODUCT_DETAILS_EDIT_RESULT
import com.woocommerce.android.ui.orders.creation.product.details.OrderCreateEditProductDetailsViewModel.ProductDetailsEditResult
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountFragment.Companion.KEY_PRODUCT_DISCOUNT_RESULT
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRate
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorFragment.Companion.KEY_SELECTED_TAX_RATE
import com.woocommerce.android.ui.orders.creation.views.OrderCreateEditSectionView
import com.woocommerce.android.ui.orders.creation.views.OrderCreateEditSectionView.AddButton
import com.woocommerce.android.ui.orders.creation.views.TaxLineUiModel
import com.woocommerce.android.ui.orders.creation.views.TaxLines
import com.woocommerce.android.ui.orders.details.OrderStatusSelectorDialog.Companion.KEY_ORDER_STATUS_RESULT
import com.woocommerce.android.ui.orders.details.views.OrderDetailOrderStatusView
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.WCReadMoreTextView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LargeClass")
@AndroidEntryPoint
class OrderCreateEditFormFragment :
    BaseFragment(R.layout.fragment_order_create_edit_form),
    BackPressListener,
    MenuProvider {
    private val viewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var customAmountsFeatureFlag: CustomAmountsFeatureFlag

    private var createOrderMenuItem: MenuItem? = null
    private var progressDialog: CustomProgressDialog? = null
    private var orderUpdateFailureSnackBar: Snackbar? = null

    private val bigDecimalFormatter by lazy {
        currencyFormatter.buildBigDecimalFormatter(
            currencyCode = viewModel.currentDraft.currency
        )
    }

    private val args: OrderCreateEditFormFragmentArgs by navArgs()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = when (viewModel.mode) {
                OrderCreateEditViewModel.Mode.Creation -> R.drawable.ic_back_24dp
                is OrderCreateEditViewModel.Mode.Edit -> null
            }
        )

    private val View?.productsAdapter
        get() = (this as? RecyclerView)
            ?.run { adapter as? OrderCreateEditProductsAdapter }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        with(FragmentOrderCreateEditFormBinding.bind(view)) {
            setupObserversWith(this)
            setupHandleResults()
            initView()
        }
        handleCouponEditResult()
        handleProductDetailsEditResult()
        handleResult<String>(KEY_COUPON_SELECTOR_RESULT) {
            viewModel.onCouponAdded(it)
        }
        handleTaxRateSelectionResult()
    }

    private fun handleTaxRateSelectionResult() {
        handleResult<TaxRate>(KEY_SELECTED_TAX_RATE) {
            viewModel.onTaxRateSelected(it)
        }
    }

    private fun handleProductDetailsEditResult() {
        handleResult<ProductDetailsEditResult>(KEY_PRODUCT_DETAILS_EDIT_RESULT) {
            viewModel.onProductDetailsEditResult(it)
        }
        handleResult<Order.Item>(KEY_PRODUCT_DISCOUNT_RESULT) {
            viewModel.onProductDiscountEditResult(it)
        }
    }

    private fun handleCouponEditResult() {
        args.couponEditResult?.let {
            viewModel.onCouponEditResult(it)
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_order_creation, menu)

        createOrderMenuItem = menu.findItem(R.id.menu_create).apply {
            title = resources.getString(
                when (viewModel.mode) {
                    OrderCreateEditViewModel.Mode.Creation -> R.string.create
                    is OrderCreateEditViewModel.Mode.Edit -> R.string.done
                }
            )
            isEnabled = viewModel.viewStateData.liveData.value?.canCreateOrder ?: false
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_create -> {
                viewModel.onCreateOrderClicked(viewModel.currentDraft)
                true
            }
            else -> false
        }
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
        orderUpdateFailureSnackBar?.dismiss()
    }

    private fun FragmentOrderCreateEditFormBinding.initView() {
        initOrderStatusView()
        initNotesSection()
        initCustomerSection()
        initProductsSection()
        initPaymentSection()
        initTaxRateSelectorSection()
    }

    private fun FragmentOrderCreateEditFormBinding.initTaxRateSelectorSection() {
        taxRateSelectorSection.isVisible = FeatureFlag.ORDER_CREATION_TAX_RATE_SELECTOR.isEnabled()
        setTaxRateButton.setOnClickListener {
            viewModel.onSetTaxRateClicked()
        }
    }

    private fun FragmentOrderCreateEditFormBinding.initOrderStatusView() {
        val mode = when (viewModel.mode) {
            OrderCreateEditViewModel.Mode.Creation -> OrderDetailOrderStatusView.Mode.OrderCreation
            is OrderCreateEditViewModel.Mode.Edit -> OrderDetailOrderStatusView.Mode.OrderEdit
        }
        orderStatusView.initView(
            mode = mode,
            editOrderStatusClickListener = {
                viewModel.orderStatusData.value?.let {
                    viewModel.onEditOrderStatusClicked(it)
                }
            }
        )
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

    private fun FragmentOrderCreateEditFormBinding.initProductsSection() {
        if (customAmountsFeatureFlag()) {
            productsSection.hideHeader()
            productsSection.setProductSectionButtons(
                addProductsButton = AddButton(
                    text = getString(R.string.order_creation_add_products),
                    onClickListener = {
                        viewModel.onAddProductClicked()
                    }
                ),
                addProductsViaScanButton = AddButton(
                    text = getString(R.string.order_creation_add_product_via_barcode_scanning),
                    onClickListener = { viewModel.onScanClicked() }
                ),
                addCustomAmountsButton = AddButton(
                    text = getString(R.string.order_creation_add_custom_amounts),
                    onClickListener = {
                        // Implement custom amounts click listener
                    }
                )
            )
        } else {
            productsSection.setProductSectionButtons(
                addProductsButton = AddButton(
                    text = getString(R.string.order_creation_add_products),
                    onClickListener = {
                        viewModel.onAddProductClicked()
                    }
                ),
                addProductsViaScanButton = AddButton(
                    text = getString(R.string.order_creation_add_product_via_barcode_scanning),
                    onClickListener = { viewModel.onScanClicked() }
                ),
            )
        }
    }

    private fun FragmentOrderCreateEditFormBinding.initPaymentSection() {
        paymentSection.shippingButton.setOnClickListener {
            viewModel.onShippingButtonClicked()
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
            bindNotesSection(binding.notesSection, newOrderData.customerNote)
            bindCustomerAddressSection(binding.customerSection, newOrderData)
            bindPaymentSection(binding.paymentSection, newOrderData)
        }

        viewModel.orderStatusData.observe(viewLifecycleOwner) {
            binding.orderStatusView.updateStatus(it)
        }

        viewModel.products.observe(viewLifecycleOwner) {
            bindProductsSection(binding.productsSection, it)
        }

        observeViewStateChanges(binding)

        viewModel.event.observe(viewLifecycleOwner, ::handleViewModelEvents)
    }

    private fun observeViewStateChanges(binding: FragmentOrderCreateEditFormBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) { show ->
                if (show) showProgressDialog() else hideProgressDialog()
            }
            new.canCreateOrder.takeIfNotEqualTo(old?.canCreateOrder) {
                createOrderMenuItem?.isEnabled = it
            }
            new.isIdle.takeIfNotEqualTo(old?.isIdle) { idle ->
                updateProgressBarsVisibility(binding, !idle)
                if (new.isEditable) {
                    binding.paymentSection.shippingButton.isEnabled = idle
                    binding.paymentSection.feeButton.isEnabled = idle
                    binding.paymentSection.couponButton.isEnabled =
                        new.isCouponButtonEnabled && idle
                    binding.paymentSection.addCouponButton.isEnabled =
                        new.isCouponButtonEnabled && idle
                    binding.productsSection.isEachAddButtonEnabled = idle
                }
            }
            new.isUpdatingOrderDraft.takeIfNotEqualTo(old?.isUpdatingOrderDraft) { show ->
                if (new.isEditable) {
                    binding.productsSection.content.productsAdapter?.areProductsEditable =
                        show.not()
                }
            }
            new.showOrderUpdateSnackbar.takeIfNotEqualTo(old?.showOrderUpdateSnackbar) { show ->
                showOrHideErrorSnackBar(show)
            }
            new.isEditable.takeIfNotEqualTo(old?.isEditable) { isEditable ->
                if (isEditable) binding.showEditableControls(new) else binding.hideEditableControls()
            }
            new.multipleLinesContext.takeIfNotEqualTo(old?.multipleLinesContext) { multipleLinesContext ->
                when (multipleLinesContext) {
                    None -> binding.multipleLinesWarningSection.root.visibility = View.GONE
                    is Warning -> {
                        binding.multipleLinesWarningSection.header.text =
                            multipleLinesContext.header
                        binding.multipleLinesWarningSection.explanation.text =
                            multipleLinesContext.explanation
                        binding.multipleLinesWarningSection.root.visibility = View.VISIBLE
                    }
                }
            }
            new.isCouponButtonEnabled.takeIfNotEqualTo(old?.isCouponButtonEnabled) {
                binding.paymentSection.couponButton.isEnabled = it
                binding.paymentSection.addCouponButton.isEnabled = it
            }
            new.taxBasedOnSettingLabel.takeIfNotEqualTo(old?.taxBasedOnSettingLabel) {
                bindTaxBasedOnSettingLabel(binding.paymentSection, it)
            }
            new.taxRateSelectorButtonState.takeIfNotEqualTo(old?.taxRateSelectorButtonState) {
                binding.taxRateSelectorSection.isVisible = it.isShown
                binding.setTaxRateButton.text = it.label
            }
        }
    }

    private fun updateProgressBarsVisibility(
        binding: FragmentOrderCreateEditFormBinding,
        shouldShowProgressBars: Boolean
    ) {
        when (viewModel.mode) {
            OrderCreateEditViewModel.Mode.Creation -> {
                binding.paymentSection.loadingProgress.isVisible = shouldShowProgressBars
            }

            is OrderCreateEditViewModel.Mode.Edit -> {
                binding.loadingProgress.isVisible = shouldShowProgressBars
            }
        }
    }

    private fun bindPaymentSection(paymentSection: OrderCreationPaymentSectionBinding, newOrderData: Order) {
        paymentSection.bindFeesSubSection(newOrderData)
        paymentSection.bindCouponsSubSection(newOrderData)

        val firstShipping = newOrderData.shippingLines.firstOrNull { it.methodId != null }
        paymentSection.shippingButton.setText(
            if (firstShipping != null) R.string.order_creation_edit_shipping
            else R.string.order_creation_add_shipping
        )
        paymentSection.shippingButton.setIconResource(
            if (firstShipping != null) 0
            else R.drawable.ic_add
        )
        paymentSection.shippingValue.isVisible = firstShipping != null
        newOrderData.shippingLines.sumByBigDecimal { it.total }.let {
            paymentSection.shippingValue.text = bigDecimalFormatter(it)
        }

        paymentSection.productsTotalValue.text = bigDecimalFormatter(newOrderData.productsTotal)
        paymentSection.taxValue.text = bigDecimalFormatter(newOrderData.totalTax)
        val hasDiscount = newOrderData.discountTotal.isNotEqualTo(BigDecimal.ZERO)
        paymentSection.discountLayout.isVisible = hasDiscount
        if (hasDiscount) {
            paymentSection.discountValue.text = getString(
                R.string.order_creation_discounts_total_value,
                bigDecimalFormatter(newOrderData.discountTotal)
            )
        }
        paymentSection.orderTotalValue.text = bigDecimalFormatter(newOrderData.total)
        bindTaxLinesSection(
            paymentSection,
            newOrderData
        )
        paymentSection.taxHelpButton.setOnClickListener { viewModel.onTaxHelpButtonClicked() }
    }

    private fun bindTaxBasedOnSettingLabel(
        paymentSection: OrderCreationPaymentSectionBinding,
        settingText: String
    ) {
        paymentSection.taxBasedOnLabel.text = settingText
    }

    private fun bindTaxLinesSection(
        paymentSection: OrderCreationPaymentSectionBinding,
        newOrderData: Order
    ) {
        val taxLines = newOrderData.taxLines.map {
            TaxLineUiModel(
                label = it.label,
                ratePercent = "${it.ratePercent}%",
                taxTotal = bigDecimalFormatter(BigDecimal(it.taxTotal))
            )
        }
        paymentSection.taxLines.setContent {
            TaxLines(taxLines)
        }
    }

    private fun OrderCreationPaymentSectionBinding.bindFeesSubSection(newOrderData: Order) {
        feeButton.setOnClickListener { viewModel.onFeeButtonClicked() }

        val currentFeeTotal = newOrderData.feesTotal

        val hasFee = currentFeeTotal.isNotEqualTo(BigDecimal.ZERO)

        if (hasFee) {
            feeButton.setText(R.string.order_creation_payment_fee)
            feeButton.setIconResource(0)
            feeValue.isVisible = true
            feeValue.text = bigDecimalFormatter(currentFeeTotal)
        } else {
            feeButton.setText(R.string.order_creation_add_fee)
            feeButton.setIconResource(R.drawable.ic_add)
            feeValue.isVisible = false
        }
    }

    private fun OrderCreationPaymentSectionBinding.bindCouponsSubSection(newOrderData: Order) {
        couponButton.setOnClickListener { viewModel.onCouponButtonClicked() }
        addCouponButton.setOnClickListener { viewModel.onAddCouponButtonClicked() }

        if (newOrderData.discountCodes.isNotNullOrEmpty()) {
            couponButton.isVisible = true
            couponValue.isVisible = true
            addCouponButton.isVisible = true
            couponButton.text = getString(R.string.order_creation_coupon_codes, newOrderData.discountCodes)
            couponValue.text = getString(
                R.string.order_creation_coupon_discount_value,
                bigDecimalFormatter(newOrderData.discountTotal)
            )
        } else {
            couponButton.isVisible = false
            couponValue.isVisible = false
            addCouponButton.isVisible = true
        }
    }

    private fun bindNotesSection(notesSection: OrderCreateEditSectionView, customerNote: String) {
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

    private fun bindProductsSection(productsSection: OrderCreateEditSectionView, products: List<ProductUIModel>?) {
        productsSection.setContentHorizontalPadding(R.dimen.minor_00)
        if (products.isNullOrEmpty()) {
            productsSection.content = null
            productsSection.hideAddProductsHeaderActions()
            productsSection.hideHeader()
            productsSection.content
            productsSection.setProductSectionButtons(
                addProductsButton = AddButton(
                    text = getString(R.string.order_creation_add_products),
                    onClickListener = {
                        viewModel.onAddProductClicked()
                    }
                ),
                addProductsViaScanButton = AddButton(
                    text = getString(R.string.order_creation_add_product_via_barcode_scanning),
                    onClickListener = { viewModel.onScanClicked() }
                ),
                addCustomAmountsButton = AddButton(
                    text = getString(R.string.order_creation_add_custom_amounts),
                    onClickListener = {
                        // Implement custom amounts click listener
                        findNavController().navigateSafely(
                            OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToCustomAmountsDialog()
                        )
                    }
                )
            )
        } else {
            productsSection.showAddProductsHeaderActions()
            productsSection.showHeader()
            productsSection.removeProductsButtons()
            // To make list changes smoother, we don't need to change the RecyclerView's instance if it was already set
            if (productsSection.content == null) {
                val animator = DefaultItemAnimator().apply {
                    // Disable change animations to avoid duplicating viewholders
                    supportsChangeAnimations = false
                }
                productsSection.content = RecyclerView(requireContext()).apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = OrderCreateEditProductsAdapter(
                        onProductClicked = viewModel::onProductClicked,
                        currencyFormatter = currencyFormatter,
                        currencyCode = viewModel.currentDraft.currency,
                        onIncreaseQuantity = viewModel::onIncreaseProductsQuantity,
                        onDecreaseQuantity = viewModel::onDecreaseProductsQuantity
                    )
                    itemAnimator = animator
                    isNestedScrollingEnabled = false
                }
            }
            productsSection.content.productsAdapter?.apply {
                submitList(products)
                areProductsEditable = viewModel.currentDraft.isEditable
            }
        }

        productsSection.barcodeIcon.setOnClickListener {
            viewModel.onScanClicked()
        }
        productsSection.addProductIcon.setOnClickListener {
            viewModel.onAddProductClicked()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindCustomerAddressSection(customerAddressSection: OrderCreateEditSectionView, order: Order) {
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

    private fun setupHandleResults() {
        handleDialogResult<OrderStatusUpdateSource>(
            key = KEY_ORDER_STATUS_RESULT,
            entryId = R.id.orderCreationFragment
        ) { viewModel.onOrderStatusChanged(Order.Status.fromValue(it.newStatus)) }
        handleResult<Collection<SelectedItem>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            viewModel.onProductsSelected(it)
        }
        handleResult<CodeScannerStatus>(BarcodeScanningFragment.KEY_BARCODE_SCANNING_SCAN_STATUS) { status ->
            viewModel.handleBarcodeScannedStatus(status)
        }
    }

    private fun handleViewModelEvents(event: Event) {
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

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun getFragmentTitle() = when (viewModel.mode) {
        OrderCreateEditViewModel.Mode.Creation -> getString(R.string.order_creation_fragment_title)
        is OrderCreateEditViewModel.Mode.Edit -> {
            val orderId = (viewModel.mode as OrderCreateEditViewModel.Mode.Edit).orderId.toString()
            getString(R.string.orderdetail_orderstatus_ordernum, orderId)
        }
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
            content.productsAdapter?.areProductsEditable = true
        }
        paymentSection.apply {
            feeButton.isEnabled = true
            shippingButton.isEnabled = true
            lockIcon.isVisible = false
            couponButton.isEnabled = state.isCouponButtonEnabled
            addCouponButton.isEnabled = state.isCouponButtonEnabled
        }
    }

    private fun FragmentOrderCreateEditFormBinding.hideEditableControls() {
        messageNoEditableFields.visibility = View.VISIBLE
        productsSection.apply {
            isLocked = true
            isEachAddButtonEnabled = false
            content.productsAdapter?.areProductsEditable = false
        }
        paymentSection.apply {
            feeButton.isEnabled = false
            shippingButton.isEnabled = false
            lockIcon.isVisible = true
            couponButton.isEnabled = false
            addCouponButton.isEnabled = false
        }
    }
}
