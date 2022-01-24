package com.woocommerce.android.ui.orders.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationFormBinding
import com.woocommerce.android.databinding.LayoutOrderCreationCustomerInfoBinding
import com.woocommerce.android.databinding.OrderCreationPaymentSectionBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigator
import com.woocommerce.android.ui.orders.creation.views.OrderCreationSectionView
import com.woocommerce.android.ui.orders.creation.views.OrderCreationSectionView.AddButton
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.details.OrderStatusSelectorDialog.Companion.KEY_ORDER_STATUS_RESULT
import com.woocommerce.android.ui.orders.details.views.OrderDetailOrderStatusView
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationFormFragment : BaseFragment(R.layout.fragment_order_creation_form) {
    private val viewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)

    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var createOrderMenuItem: MenuItem? = null
    private var progressDialog: CustomProgressDialog? = null

    private val bigDecimalFormatter by lazy {
        currencyFormatter.buildBigDecimalFormatter(
            currencyCode = viewModel.currentDraft.currency
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        with(FragmentOrderCreationFormBinding.bind(view)) {
            setupObserversWith(this)
            setupHandleResults()
            initView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_order_creation, menu)
        createOrderMenuItem = menu.findItem(R.id.menu_create).apply {
            isVisible = viewModel.viewStateData.liveData.value?.canCreateOrder ?: false
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

    override fun onStop() {
        super.onStop()
        progressDialog?.dismiss()
    }

    private fun FragmentOrderCreationFormBinding.initView() {
        orderStatusView.initView(
            mode = OrderDetailOrderStatusView.Mode.OrderCreation,
            editOrderStatusClickListener = {
                viewModel.orderStatusData.value?.let {
                    viewModel.onEditOrderStatusClicked(it)
                }
            }
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

    private fun LayoutOrderCreationCustomerInfoBinding.changeState() {
        if (root.currentState == R.id.start) {
            root.transitionToEnd()
        } else {
            root.transitionToStart()
        }
    }

    private fun setupObserversWith(binding: FragmentOrderCreationFormBinding) {
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
                createOrderMenuItem?.isVisible = it
            }
        }

        viewModel.event.observe(viewLifecycleOwner, ::handleViewModelEvents)
    }

    private fun bindPaymentSection(paymentSection: OrderCreationPaymentSectionBinding, newOrderData: Order) {
        paymentSection.root.isVisible = newOrderData.items.isNotEmpty()
        bigDecimalFormatter(newOrderData.total).let { total ->
            paymentSection.productsTotalValue.text = total
            paymentSection.orderTotalValue.text = total
        }
    }

    private fun bindNotesSection(notesSection: OrderCreationSectionView, customerNote: String) {
        customerNote.takeIf { it.isNotBlank() }
            ?.let {
                val textView = MaterialTextView(requireContext())
                TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_Woo_Subtitle1)
                textView.text = it
                textView
            }
            .let {
                notesSection.content = it
            }
    }

    private fun bindProductsSection(productsSection: OrderCreationSectionView, products: List<ProductUIModel>?) {
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
                    adapter = OrderCreationProductsAdapter(
                        onProductClicked = viewModel::onProductClicked,
                        currencyFormatter = bigDecimalFormatter,
                        onIncreaseQuantity = viewModel::onIncreaseProductsQuantity,
                        onDecreaseQuantity = viewModel::onDecreaseProductsQuantity
                    )
                    itemAnimator = animator
                }
            }
            ((productsSection.content as RecyclerView).adapter as OrderCreationProductsAdapter).products = products
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindCustomerAddressSection(customerAddressSection: OrderCreationSectionView, order: Order) {
        customerAddressSection.setContentHorizontalPadding(R.dimen.minor_00)
        order.takeIf { it.shippingAddress != Address.EMPTY }
            ?.let {
                val view = LayoutOrderCreationCustomerInfoBinding.inflate(layoutInflater)
                view.name.text = "${order.billingAddress.firstName} ${order.billingAddress.lastName}"
                view.email.text = order.billingAddress.email
                view.shippingAddressDetails.text = order.formatShippingInformationForDisplay()
                view.billingAddressDetails.text = order.formatBillingInformationForDisplay()
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
            is OrderCreationNavigationTarget -> OrderCreationNavigator.navigate(this, event)
            is ViewOrderStatusSelector ->
                OrderCreationFormFragmentDirections
                    .actionOrderCreationFragmentToOrderStatusSelectorDialog(
                        currentStatus = event.currentStatus,
                        orderStatusList = event.orderStatusList
                    ).let { findNavController().navigateSafely(it) }
            is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
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

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_fragment_title)
}
