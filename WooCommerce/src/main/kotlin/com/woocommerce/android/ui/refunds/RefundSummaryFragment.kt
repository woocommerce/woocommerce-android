package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.OrderDetailFragment.Companion.REFUND_REQUEST_CODE
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ExitAfterRefund
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_summary.*
import javax.inject.Inject

class RefundSummaryFragment : DaggerFragment(), BackPressListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_refund_summary, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(this, Observer { event ->
            when (event) {
                is ShowSnackbar -> {
                    if (event.undoAction == null) {
                        uiMessageResolver.showSnack(event.message)
                    } else {
                        val snackbar = uiMessageResolver.getUndoSnack(
                                event.message,
                                "",
                                actionListener = View.OnClickListener { event.undoAction.invoke() }
                        )
                        snackbar.addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                viewModel.onProceedWithRefund()
                            }
                        })
                        snackbar.show()
                    }
                }
                is ExitAfterRefund -> {
                    requireActivity().navigateBackWithResult(
                            REFUND_REQUEST_CODE,
                            Bundle(),
                            R.id.nav_host_fragment_main,
                            R.id.orderDetailFragment
                    )
                }
                else -> event.isHandled = false
            }
        })

        viewModel.refundSummaryStateLiveData.observe(this) { old, new ->
            new.isFormEnabled?.takeIfNotEqualTo(old?.isFormEnabled) {
                refundSummary_btnRefund.isEnabled = new.isFormEnabled
                refundSummary_reason.isEnabled = new.isFormEnabled
            }
            new.refundAmount?.takeIfNotEqualTo(old?.refundAmount) { refundSummary_refundAmount.text = it }
            new.previouslyRefunded?.takeIfNotEqualTo(old?.previouslyRefunded) {
                refundSummary_previouslyRefunded.text = it
            }
            new.refundMethod?.takeIfNotEqualTo(old?.refundMethod) { refundSummary_method.text = it }
            new.isMethodDescriptionVisible?.takeIfNotEqualTo(old?.isMethodDescriptionVisible) { visible ->
                if (visible)
                    refundSummary_methodDescription.show()
                else
                    refundSummary_methodDescription.hide()
            }
        }
    }

    private fun initializeViews() {
        refundSummary_btnRefund.setOnClickListener {
            viewModel.onRefundConfirmed(refundSummary_reason.text.toString())
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        findNavController().popBackStack()
        return false
    }
}
