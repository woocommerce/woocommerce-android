package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments) {
    private val viewModel: SimplePaymentsFragmentViewModel by viewModels()

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSimplePaymentsBinding.bind(view)) {
            showOrder(viewModel.order, this)
        }
    }

    private fun showOrder(order: Order, binding: FragmentSimplePaymentsBinding) {
        val subTotal = currencyFormatter.formatCurrency(order.total, viewModel.currencyCode)
        binding.textCustomAmount.text = subTotal
        binding.textSubtotal.text = subTotal

        // TODO nbradbury - email
        // TODO nbradbury - taxes
        // TODO nbradbury - customer note
        // TODO nbradbury - take payment
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_title)
}
