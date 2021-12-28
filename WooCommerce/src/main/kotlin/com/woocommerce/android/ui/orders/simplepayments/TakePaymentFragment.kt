package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentTakePaymentBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TakePaymentFragment : BaseFragment(R.layout.fragment_take_payment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTakePaymentBinding.bind(view)

        binding.textCash.setOnClickListener {
            // TODO nbradbury
        }
        binding.textCard.setOnClickListener {
            // TODO nbradbury
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.simple_payments_take_payment_button)
}
