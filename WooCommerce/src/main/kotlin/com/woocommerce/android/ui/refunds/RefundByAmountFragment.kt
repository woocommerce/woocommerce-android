package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.Event.HideValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.Event.ShowValidationError
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_by_amount.*
import javax.inject.Inject

class RefundByAmountFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: IssueRefundViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_refund_by_amount, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.refundByAmountViewState.observe(this, Observer {
            issueRefund_txtAvailableForRefund.text = it.availableForRefund
            issueRefund_refundAmount.initView(it.currency, it.decimals, currencyFormatter)
            issueRefund_refundAmount.setValue(viewModel.enteredAmount)
        })

        viewModel.eventTrigger.observe(this, Observer { event ->
            event.isHandled = true
            when (event) {
                is ShowValidationError -> issueRefund_refundAmountInputLayout.error = event.message
                is HideValidationError -> issueRefund_refundAmountInputLayout.error = null
                else -> event.isHandled = false
            }
        })

        issueRefund_refundAmount.value.observe(this, Observer {
            viewModel.onManualRefundAmountChanged(it)
        })
    }
}
