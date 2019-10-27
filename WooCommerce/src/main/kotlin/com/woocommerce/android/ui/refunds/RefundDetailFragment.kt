package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_detail.*
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class RefundDetailFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val navArgs: RefundDetailFragmentArgs by navArgs()
    private val viewModel: RefundDetailViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_refund_detail, container, false)
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
        setupObservers(viewModel)
        viewModel.start(navArgs.orderId, navArgs.refundId)
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers(viewModel: RefundDetailViewModel) {
        viewModel.viewState.observe(this, Observer {
            activity?.title = it.screenTitle
            refundDetail_refundAmount.text = it.refundAmount
            refundDetail_refundMethod.text = it.refundMethod
            if (it.refundReason.isNullOrEmpty()) {
                refundDetail_reasonCard.hide()
            } else {
                refundDetail_reasonCard.show()
                refundDetail_refundReason.text = it.refundReason
            }
        })
    }
}
