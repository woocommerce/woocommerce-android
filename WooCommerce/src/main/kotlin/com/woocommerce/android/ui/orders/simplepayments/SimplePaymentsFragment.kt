package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments) {
    private val sharedViewModel by hiltNavGraphViewModels<SimplePaymentsViewModel>(R.id.nav_graph_main)

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSimplePaymentsBinding.bind(view)) {
            setupObserversWith(this)
            setupHandleResults()
            showOrder(sharedViewModel.viewState.createdOrder!!, this)
        }
    }

    private fun setupObserversWith(binding: FragmentSimplePaymentsBinding) {
        // TODO nbradbury
    }

    private fun setupHandleResults() {
        // TODO nbradbury - customer note screen
    }

    private fun showOrder(order: Order, binding: FragmentSimplePaymentsBinding) {
        val amount = currencyFormatter.formatCurrency(sharedViewModel.currentPrice, sharedViewModel.currencyCode)
        binding.textCustomAmount.text = amount
        binding.textSubtotal.text = amount
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_title)
}
