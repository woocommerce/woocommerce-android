package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_refund_summary.*
import javax.inject.Inject

class RefundSummaryFragment : BaseFragment(), BackPressListener {
    companion object {
        const val REFUND_ORDER_NOTICE_KEY = "refund_order_notice"
    }
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) {
        viewModelFactory.get()
    }

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
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.getSnack(event.message, *event.args).show()
                is Exit -> navigateBackWithNotice(REFUND_ORDER_NOTICE_KEY, R.id.orderDetailFragment)
                is ShowRefundConfirmation -> {
                    val action = RefundSummaryFragmentDirections.actionRefundSummaryFragmentToRefundConfirmationDialog(
                            event.title, event.message, event.confirmButtonTitle
                    )
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        })

        viewModel.refundSummaryStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isFormEnabled?.takeIfNotEqualTo(old?.isFormEnabled) {
                refundSummary_btnRefund.isEnabled = new.isFormEnabled
                refundSummary_reason.isEnabled = new.isFormEnabled
            }
            new.isSubmitButtonEnabled?.takeIfNotEqualTo(old?.isSubmitButtonEnabled) {
                refundSummary_btnRefund.isEnabled = new.isSubmitButtonEnabled
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
            viewModel.onRefundIssued(refundSummary_reason.text.toString())
        }

        refundSummary_reason.doOnTextChanged { _, _, _, _ ->
            val maxLength = refundSummary_reasonLayout.counterMaxLength
            viewModel.onRefundSummaryTextChanged(maxLength, refundSummary_reason.length())
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        if (viewModel.isRefundInProgress) {
            Toast.makeText(context, R.string.order_refunds_refund_in_progress, Toast.LENGTH_SHORT).show()
        } else {
            findNavController().popBackStack()
        }
        return false
    }
}
