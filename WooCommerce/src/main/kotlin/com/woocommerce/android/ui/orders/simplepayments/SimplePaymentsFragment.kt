package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.creation.views.OrderCreationSectionView
import com.woocommerce.android.ui.orders.taxes.OrderTaxesAdapter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments), BackPressListener {
    private val viewModel: SimplePaymentsFragmentViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<SimplePaymentsSharedViewModel>(R.id.nav_graph_main)

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var _orderTaxesAdapter: OrderTaxesAdapter? = null
    private val orderTaxesAdapter: OrderTaxesAdapter
        get() = _orderTaxesAdapter!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSimplePaymentsBinding.bind(view)
        binding.buttonDone.setOnClickListener {
            viewModel.onDoneButtonClicked()
        }

        setupObservers(binding)
        setupResultHandlers()

        binding.switchChargeTaxes.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onChargeTaxesChanged(isChecked)
        }

        binding.editEmail.addTextChangedListener {
            val email = binding.editEmail.text.toString()
            viewModel.onBillingEmailChanged(email)
        }

        binding.notesSection.setAddButtons(
            listOf(
                OrderCreationSectionView.AddButton(
                    text = getString(R.string.order_creation_add_customer_note),
                    onClickListener = {
                        viewModel.onCustomerNoteClicked()
                    }
                )
            )
        )
        binding.notesSection.setOnEditButtonClicked {
            viewModel.onCustomerNoteClicked()
        }

        _orderTaxesAdapter = OrderTaxesAdapter(currencyFormatter, sharedViewModel.currencyCode)
        binding.listTaxes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderTaxesAdapter
            isNestedScrollingEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return true
    }

    private fun setupObservers(binding: FragmentSimplePaymentsBinding) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
                is SimplePaymentsFragmentViewModel.ShowCustomerNoteEditor -> {
                    showCustomerNoteEditor()
                }
                is SimplePaymentsFragmentViewModel.ShowTakePaymentScreen -> {
                    showTakePaymentScreen()
                }
                is SimplePaymentsFragmentViewModel.CancelSimplePayment -> {
                    viewModel.deleteDraftOrder(viewModel.orderDraft)
                }
            }
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.orderSubtotal.takeIfNotEqualTo(old?.orderSubtotal) { subtotal ->
                val subTotalStr = currencyFormatter.formatCurrency(subtotal, sharedViewModel.currencyCode)
                binding.textCustomAmount.text = subTotalStr
                binding.textSubtotal.text = subTotalStr
            }
            new.orderTaxes.takeIfNotEqualTo(old?.orderTaxes) { taxes ->
                orderTaxesAdapter.submitList(taxes)
            }
            new.orderTotal.takeIfNotEqualTo(old?.orderTotal) { total ->
                val totalStr = currencyFormatter.formatCurrency(total, sharedViewModel.currencyCode)
                binding.textTotal.text = totalStr
                binding.buttonDone.text = getString(R.string.simple_payments_take_payment_button, totalStr)
            }
            new.chargeTaxes.takeIfNotEqualTo(old?.chargeTaxes) { chargeTaxes ->
                binding.switchChargeTaxes.isChecked = chargeTaxes
                if (chargeTaxes) {
                    binding.listTaxes.isVisible = true
                    binding.textTaxMessage.isVisible = true
                } else {
                    binding.listTaxes.isVisible = false
                    binding.textTaxMessage.isVisible = false
                }
            }
            new.customerNote.takeIfNotEqualTo(old?.customerNote) { customerNote ->
                bindNotesSection(binding.notesSection, customerNote)
            }
            new.isBillingEmailValid.takeIfNotEqualTo(old?.isBillingEmailValid) { isValidEmail ->
                if (isValidEmail) {
                    binding.editEmail.error = null
                } else {
                    binding.editEmail.error = getString(R.string.email_invalid)
                }
            }
        }
    }

    private fun setupResultHandlers() {
        handleResult<String>(SimplePaymentsCustomerNoteFragment.SIMPLE_PAYMENTS_CUSTOMER_NOTE_RESULT) {
            viewModel.onCustomerNoteChanged(it)
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

    private fun showCustomerNoteEditor() {
        SimplePaymentsFragmentDirections
            .actionSimplePaymentsFragmentToSimplePaymentsCustomerNoteFragment(viewModel.viewState.customerNote)
            .let { findNavController().navigateSafely(it) }
    }

    private fun showTakePaymentScreen() {
        SimplePaymentsFragmentDirections
            .actionSimplePaymentsFragmentToTakePaymentFragment(viewModel.orderDraft)
            .let { findNavController().navigateSafely(it) }
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_title)

    override fun onDestroy() {
        super.onDestroy()
        _orderTaxesAdapter = null
    }
}
