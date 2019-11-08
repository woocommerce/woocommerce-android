package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.OrderDetailFragment.Companion.REFUND_REQUEST_CODE
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_summary.*
import javax.inject.Inject

class RefundSummaryFragment : DaggerFragment(), BackPressListener {
    companion object {
        const val REFUND_SUCCESS_KEY = "refund-success-key"
    }
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

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

        initializeViewModel()
    }

    private fun initializeViewModel() {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(IssueRefundViewModel::class.java).also {
            initializeViews(it)
            setupObservers(it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.showSnackbarMessageWithUndo.observe(this, Observer { message ->
            uiMessageResolver.getUndoSnack(message, "", actionListener = View.OnClickListener {
                viewModel.onUndoTapped()
            }).also {
                it.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        viewModel.onProceedWithRefund()
                    }
                })
                it.show()
            }
        })

        viewModel.showSnackbarMessage.observe(this, Observer { message ->
            uiMessageResolver.showSnack(message)
        })

        viewModel.isSummaryFormEnabled.observe(this, Observer {
            refundSummary_btnRefund.isEnabled = it
            refundSummary_reason.isEnabled = it
        })

        viewModel.formattedRefundAmount.observe(this, Observer {
            refundSummary_refundAmount.text = it
        })

        viewModel.previousRefunds.observe(this, Observer {
            refundSummary_previouslyRefunded.text = it
        })

        viewModel.refundMethod.observe(this, Observer {
            refundSummary_method.text = it
        })

        viewModel.isManualRefundDescriptionVisible.observe(this, Observer { visible ->
            refundSummary_methodDescription.visibility = if (visible) View.VISIBLE else View.GONE
        })

        viewModel.exitAfterRefund.observe(this, Observer {
            val bundle = Bundle()
            bundle.putBoolean(REFUND_SUCCESS_KEY, it)

            requireActivity().navigateBackWithResult(
                    REFUND_REQUEST_CODE,
                    bundle,
                    R.id.nav_host_fragment_main,
                    R.id.orderDetailFragment
            )
        })
    }

    private fun initializeViews(viewModel: IssueRefundViewModel) {
        refundSummary_btnRefund.setOnClickListener {
            viewModel.onRefundConfirmed(refundSummary_reason.text.toString())
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        findNavController().popBackStack(R.id.orderDetailFragment, false)
        return false
    }
}
