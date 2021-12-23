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
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
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

            this.switchChargeTaxes.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onChargeTaxesChanged(isChecked)
            }
        }
    }

    private fun setupObservers(binding: FragmentSimplePaymentsBinding) {
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
                } else {
                    binding.containerTaxes.isVisible = false
                    binding.textTaxMessage.isVisible = false
                }
            }
            new.orderTaxPercent.takeIfNotEqualTo(old?.orderTaxPercent) { taxPercent ->
                val df = DecimalFormat("#.##")
                binding.textTaxLabel.text = getString(R.string.simple_payments_tax_with_percent, df.format(taxPercent))
            }
        }

        // TODO nbradbury - customer note
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
