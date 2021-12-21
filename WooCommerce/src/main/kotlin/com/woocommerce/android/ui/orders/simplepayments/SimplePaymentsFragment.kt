package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments) {
    private val viewModel: SimplePaymentsFragmentViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<SimplePaymentsSharedViewModel>(R.id.nav_graph_main)

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSimplePaymentsBinding.bind(view)) {
            this.buttonDone.setOnClickListener {
                validateEmail(this.editEmail)
                // TODO nbradbury - take payment if email is valid
            }
            setupObservers(this)
        }
    }

    private fun setupObservers(binding: FragmentSimplePaymentsBinding) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.order.takeIfNotEqualTo(old?.order) { order ->
                showOrder(order!!, binding)
            }
        }
    }

    private fun showOrder(order: Order, binding: FragmentSimplePaymentsBinding) {
        val feeLine = order.feesLines[0]
        val subTotal = currencyFormatter.formatCurrency(feeLine.total, sharedViewModel.currencyCode)
        binding.textCustomAmount.text = subTotal
        binding.textSubtotal.text = subTotal

        val total = currencyFormatter.formatCurrency(order.total, sharedViewModel.currencyCode)
        binding.textTotal.text = total
        binding.buttonDone.text = getString(R.string.simple_payments_take_payment_button, total)

        val tax = currencyFormatter.formatCurrency(order.totalTax, sharedViewModel.currencyCode)
        binding.textTax.text = tax

        val hasTaxes = order.totalTax > BigDecimal.ZERO
        showTaxes(hasTaxes, binding)
        binding.switchChargeTaxes.isChecked = hasTaxes
        binding.switchChargeTaxes.setOnCheckedChangeListener { button, checked ->
            showTaxes(checked, binding)
        }

        // TODO nbradbury - customer note
    }

    private fun showTaxes(hasTaxes: Boolean, binding: FragmentSimplePaymentsBinding) {
        if (hasTaxes) {
            binding.containerTaxes.isVisible = true
            binding.textTaxMessage.isVisible = true
        } else {
            binding.containerTaxes.isVisible = false
            binding.textTaxMessage.isVisible = false
        }
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
