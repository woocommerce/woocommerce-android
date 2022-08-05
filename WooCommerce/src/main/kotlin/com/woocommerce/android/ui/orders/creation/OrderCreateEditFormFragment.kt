package com.woocommerce.android.ui.orders.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditFormBinding
import com.woocommerce.android.databinding.LayoutOrderCreationCustomerInfoBinding
import com.woocommerce.android.databinding.OrderCreationPaymentSectionBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.MultipleLinesContext.None
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.MultipleLinesContext.Warning
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigator
import com.woocommerce.android.ui.orders.creation.views.OrderCreateEditSectionView
import com.woocommerce.android.ui.orders.creation.views.OrderCreateEditSectionView.AddButton
import com.woocommerce.android.ui.orders.details.OrderStatusSelectorDialog.Companion.KEY_ORDER_STATUS_RESULT
import com.woocommerce.android.ui.orders.details.views.OrderDetailOrderStatusView
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.WCReadMoreTextView
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreateEditFormFragment : BaseFragment(R.layout.fragment_order_create_edit_form), BackPressListener {
    private val viewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)

    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var createOrderMenuItem: MenuItem? = null
    private var progressDialog: CustomProgressDialog? = null
    private var orderUpdateFailureSnackBar: Snackbar? = null

    private val bigDecimalFormatter by lazy {
        currencyFormatter.buildBigDecimalFormatter(
            currencyCode = viewModel.currentDraft.currency
        )
    }

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
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        with(FragmentOrderCreateEditFormBinding.bind(view)) {
            setupObserversWith(this)
            setupHandleResults()
            initView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_create -> {
                viewModel.onCreateOrderClicked(viewModel.currentDraft)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                        viewModel.onCustomerClicked()
                    }
                )
            )
        )
        customerSection.setOnEditButtonClicked {
            viewModel.onCustomerClicked()
        }
        customerSection.setEditButtonContentDescription(
            contentDescription = getString(R.string.order_creation_customer_edit_content_description)
        )
    }

    private fun FragmentOrderCreateEditFormBinding.initProductsSection() {
        productsSection.setAddButtons(
            listOf(
                AddButton(
                    text = getString(R.string.order_creation_add_products),
                    onClickListener = {
                        viewModel.onAddProductClicked()
                    }
                )
            )
        )
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

        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) { show ->
                if (show) showProgressDialog() else hideProgressDialog()
            }
            new.canCreateOrder.takeIfNotEqualTo(old?.canCreateOrder) {
                createOrderMenuItem?.isEnabled = it
            }
            new.isIdle.takeIfNotEqualTo(old?.isIdle) { enabled ->
                when (viewModel.mode) {
                    OrderCreateEditViewModel.Mode.Creation -> {
                        binding.paymentSection.loadingProgress.isVisible = !enabled
                    }
                    is OrderCreateEditViewModel.Mode.Edit -> {
                        binding.loadingProgress.isVisible = !enabled
                    }
                }
                if (new.isEditable) {
                    binding.paymentSection.shippingButton.isEnabled = enabled
                    binding.paymentSection.feeButton.isEnabled = enabled
                    binding.productsSection.isEachAddButtonEnabled = enabled
                }
            }
            new.isUpdatingOrderDraft.takeIfNotEqualTo(old?.isUpdatingOrderDraft) { show ->
                if (new.isEditable) {
                    binding.productsSection.content.productsAdapter?.areProductsEditable = show.not()
                }
            }
            new.showOrderUpdateSnackbar.takeIfNotEqualTo(old?.showOrderUpdateSnackbar) { show ->
                showOrHideErrorSnackBar(show)
            }
            new.isEditable.takeIfNotEqualTo(old?.isEditable) { isEditable ->
                if (isEditable) showEditableControls(binding) else hideEditableControls(binding)
            }
            new.multipleLinesContext.takeIfNotEqualTo(old?.multipleLinesContext) { multipleLinesContext ->
                when (multipleLinesContext) {
                    None -> binding.multipleLinesWarningSection.root.visibility = View.GONE
                    is Warning -> {
                        binding.multipleLinesWarningSection.header.text = multipleLinesContext.header
                        binding.multipleLinesWarningSection.explanation.text = multipleLinesContext.explanation
                        binding.multipleLinesWarningSection.root.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner, ::handleViewModelEvents)
    }

    private fun bindPaymentSection(paymentSection: OrderCreationPaymentSectionBinding, newOrderData: Order) {
        paymentSection.bindFeesSubSection(newOrderData)

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
        paymentSection.orderTotalValue.text = bigDecimalFormatter(newOrderData.total)
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
        } else {
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
    }

    @SuppressLint("SetTextI18n")
    private fun bindCustomerAddressSection(customerAddressSection: OrderCreateEditSectionView, order: Order) {
        customerAddressSection.setContentHorizontalPadding(R.dimen.minor_00)
        order.takeIf { it.billingAddress != Address.EMPTY }
            ?.let {
                val view = LayoutOrderCreationCustomerInfoBinding.inflate(layoutInflater)
                view.name.text = "${order.billingAddress.firstName} ${order.billingAddress.lastName}"
                view.email.text = order.billingAddress.email

                val shippingAddressDetails =
                    if (order.shippingAddress != Address.EMPTY) {
                        order.formatShippingInformationForDisplay()
                    } else {
                        order.formatBillingInformationForDisplay()
                    }
                view.shippingAddressDetails.text = shippingAddressDetails
                view.shippingAddressDetails.contentDescription =
                    shippingAddressDetails.replace("\n", ". ")

                val billingAddressDetails = order.formatBillingInformationForDisplay()
                view.billingAddressDetails.text = billingAddressDetails
                view.billingAddressDetails.contentDescription =
                    billingAddressDetails.replace("\n", ". ")

                view.customerInfoViewMoreButtonTitle.setOnClickListener {
                    view.changeState()
                }
                view.root
            }
            .let {
                customerAddressSection.content = it
            }
    }

    private fun setupHandleResults() {
        handleDialogResult<OrderStatusUpdateSource>(
            key = KEY_ORDER_STATUS_RESULT,
            entryId = R.id.orderCreationFragment
        ) { viewModel.onOrderStatusChanged(Order.Status.fromValue(it.newStatus)) }
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

    private fun showEditableControls(binding: FragmentOrderCreateEditFormBinding) {
        binding.messageNoEditableFields.visibility = View.GONE
        binding.productsSection.apply {
            isLocked = false
            isEachAddButtonEnabled = true
            content.productsAdapter?.areProductsEditable = true
        }
        binding.paymentSection.apply {
            feeButton.isEnabled = true
            shippingButton.isEnabled = true
            lockIcon.isVisible = false
        }
    }

    private fun hideEditableControls(binding: FragmentOrderCreateEditFormBinding) {
        binding.messageNoEditableFields.visibility = View.VISIBLE
        binding.productsSection.apply {
            isLocked = true
            isEachAddButtonEnabled = false
            content.productsAdapter?.areProductsEditable = false
        }
        binding.paymentSection.apply {
            feeButton.isEnabled = false
            shippingButton.isEnabled = false
            lockIcon.isVisible = true
        }
    }
}
