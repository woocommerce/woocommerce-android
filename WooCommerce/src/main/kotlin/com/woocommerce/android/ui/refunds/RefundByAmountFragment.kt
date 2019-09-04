package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_by_amount.*
import javax.inject.Inject

class RefundByAmountFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private var refundAmountChangeListener: TextWatcher? = null

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
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(IssueRefundViewModel::class.java).also {
            setupObservers(it)
            initializeViews(it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.availableForRefund.observe(this, Observer {
            issueRefund_txtAvailableForRefund.text = it
        })

        viewModel.currencySymbol.observe(this, Observer {
            issueRefund_refundAmount.setCurrency(it)
            issueRefund_refundAmount.setText(viewModel.enteredAmount.toString())
        })

        viewModel.showValidationError.observe(this, Observer {
            issueRefund_refundAmountInputLayout.error = it
        })
    }

    private fun initializeViews(viewModel: IssueRefundViewModel) {
        issueRefund_refundAmount.setDelimiter(false)
        issueRefund_refundAmount.setDecimals(true)

        refundAmountChangeListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    viewModel.onManualRefundAmountChanged(issueRefund_refundAmount.cleanDoubleValue.toBigDecimal())
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        issueRefund_refundAmount.addTextChangedListener(refundAmountChangeListener)
    }

    override fun onDestroy() {
        refundAmountChangeListener?.let {
            issueRefund_refundAmount.removeTextChangedListener(it)
        }
        super.onDestroy()
    }
}
