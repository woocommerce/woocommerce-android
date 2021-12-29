package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentTakePaymentBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TakePaymentFragment : BaseFragment(R.layout.fragment_take_payment) {
    private val viewModel: TakePaymentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpObservers()
        val binding = FragmentTakePaymentBinding.bind(view)

        binding.textCash.setOnClickListener {
            viewModel.onCashPaymentClicked()
        }
        binding.textCard.setOnClickListener {
            // TODO nbradbury
        }
    }

    private fun setUpObservers() {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is MultiLiveEvent.Event.ShowDialog -> {
                        event.showDialog()
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
    }

    // TODO nbradbury show payment amount in title via simple_payments_take_payment_button
    override fun getFragmentTitle() = getString(R.string.simple_payments_dialog_title)
}
