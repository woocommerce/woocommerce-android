package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSimplePaymentsBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimplePaymentsFragment : BaseFragment(R.layout.fragment_simple_payments) {
    private val viewModel: SimplePaymentsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentSimplePaymentsBinding.bind(view)) {
            setupObserversWith(this)
            setupHandleResults()
        }
    }

    private fun setupObserversWith(binding: FragmentSimplePaymentsBinding) {
        // TODO nbradbury
    }

    private fun setupHandleResults() {
        // TODO nbradbury - customer note screen
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_title)
}
