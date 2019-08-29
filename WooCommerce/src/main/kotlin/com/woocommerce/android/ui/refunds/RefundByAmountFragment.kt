package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.content.Context
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
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_refund_by_amount.*
import javax.inject.Inject

class RefundByAmountFragment : androidx.fragment.app.Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_refund_by_amount, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(RefundsViewModel::class.java).also {
            initializeViews(it)
            setupObservers(it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers(viewModel: RefundsViewModel) {
        viewModel.availableForRefund.observe(this, Observer {
            refunds_txtAvailableForRefund.text = it
        })

        viewModel.currencySymbol.observe(this, Observer {
            refunds_refundAmount.setCurrency(it)
            refunds_refundAmount.setText(viewModel.enteredAmount.toString())
        })
    }

    private fun initializeViews(viewModel: RefundsViewModel) {
        refunds_refundAmount.setDelimiter(false)
        refunds_refundAmount.setDecimals(true)
        refunds_refundAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onManualRefundAmountChanged(refunds_refundAmount.cleanDoubleValue.toBigDecimal())
            }
        })

        refunds_btnNext.setOnClickListener {
//            val action = RefundsFragmentDirections.a(
//                    order.getIdentifier(),
//                    order.number,
//                    presenter.isShipmentTrackingsFetched
//            )
//            findNavController().navigate(action)
        }
    }
}
