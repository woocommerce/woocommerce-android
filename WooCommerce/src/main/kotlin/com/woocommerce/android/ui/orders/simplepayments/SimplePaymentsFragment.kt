package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.views.OrderCreationSectionView
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments) {
    private val viewModel: SimplePaymentsFragmentViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<SimplePaymentsSharedViewModel>(R.id.nav_graph_main)

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSimplePaymentsBinding.bind(view)
        binding.buttonDone.setOnClickListener {
            if (validateEmail(binding.editEmail)) {
                viewModel.onDoneButtonClicked()
            }
        }

        setupObservers(binding)
        setupResultHandlers()

        binding.switchChargeTaxes.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onChargeTaxesChanged(isChecked)
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
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers(binding: FragmentSimplePaymentsBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is SimplePaymentsFragmentViewModel.ShowCustomerNoteEditor -> {
                        showCustomerNoteEditor()
                    }
                    is SimplePaymentsFragmentViewModel.ShowTakePaymentScreen -> {
                        showTakePaymentScreen()
                    }
                }
            }
        )

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.orderSubtotal.takeIfNotEqualTo(old?.orderSubtotal) { subtotal ->
                val subTotalStr = currencyFormatter.formatCurrency(subtotal, sharedViewModel.currencyCode)
                binding.textCustomAmount.text = subTotalStr
                binding.textSubtotal.text = subTotalStr
            }
            new.orderTotalTax.takeIfNotEqualTo(old?.orderTotalTax) { totalTax ->
                val taxStr = currencyFormatter.formatCurrency(totalTax, sharedViewModel.currencyCode)
                binding.textTax.text = taxStr
            }
            new.orderTotal.takeIfNotEqualTo(old?.orderTotal) { total ->
                val totalStr = currencyFormatter.formatCurrency(total, sharedViewModel.currencyCode)
                binding.textTotal.text = totalStr
                binding.buttonDone.text = getString(R.string.simple_payments_take_payment_button, totalStr)
            }
            new.chargeTaxes.takeIfNotEqualTo(old?.chargeTaxes) { chargeTaxes ->
                binding.switchChargeTaxes.isChecked = chargeTaxes
                if (chargeTaxes) {
                    binding.containerTaxes.isVisible = true
                    binding.textTaxMessage.isVisible = true
                    binding.textTaxLabel.text =
                        getString(R.string.simple_payments_tax_with_percent, viewModel.taxRatePercent)
                } else {
                    binding.containerTaxes.isVisible = false
                    binding.textTaxMessage.isVisible = false
                }
            }
            new.customerNote.takeIfNotEqualTo(old?.customerNote) { customerNote ->
                bindNotesSection(binding.notesSection, customerNote)
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

    private fun validateEmail(emailEditText: EditText): Boolean {
        val email = emailEditText.text.toString()
        return if (email.isEmpty() || StringUtils.isValidEmail(email)) {
            true
        } else {
            emailEditText.error = getString(R.string.email_invalid)
            false
        }
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_title)
}
