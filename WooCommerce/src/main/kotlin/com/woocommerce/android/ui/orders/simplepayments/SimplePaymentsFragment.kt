package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments) {
    private val viewModel: SimplePaymentsFragmentViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<SimplePaymentsSharedViewModel>(R.id.nav_graph_main)

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSimplePaymentsBinding.bind(view)) {
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
        val subTotal = currencyFormatter.formatCurrency(order.total, sharedViewModel.currencyCode)
        binding.textCustomAmount.text = subTotal
        binding.textSubtotal.text = subTotal

        val tax = currencyFormatter.formatCurrency(order.totalTax, sharedViewModel.currencyCode)
        binding.textTax.text = tax

        val total = currencyFormatter.formatCurrency(order.total + order.totalTax, sharedViewModel.currencyCode)
        binding.textTotal.text = total

        // TODO nbradbury - email
        // TODO nbradbury - taxes
        // TODO nbradbury - customer note
        // TODO nbradbury - take payment
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_title)
}
