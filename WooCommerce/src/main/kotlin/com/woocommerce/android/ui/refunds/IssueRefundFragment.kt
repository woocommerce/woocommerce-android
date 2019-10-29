package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.Event.ShowRefundSummary
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_issue_refund.*

class IssueRefundFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by activityViewModels { viewModelFactory }
    private val navArgs: IssueRefundFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_issue_refund, container, false)
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
        initializeViews(viewModel)
        setupObservers(viewModel)

        viewModel.initialize(navArgs.orderId)
    }

    private fun initializeViews(viewModel: IssueRefundViewModel) {
        issueRefund_btnNext.setOnClickListener {
            viewModel.onNextButtonTapped()
        }
    }

    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.resetTriggerEvent()

        viewModel.commonViewState.observe(this, Observer {
            issueRefund_btnNext.isEnabled = it.isNextButtonEnabled
            requireActivity().title = it.screenTitle
        })

        viewModel.eventTrigger.observe(this, Observer { event ->
            when (event) {
                is ShowRefundSummary -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundSummaryFragment()
                    findNavController().navigate(action)
                }
                else -> event.isHandled = false
            }
        })
    }
}
