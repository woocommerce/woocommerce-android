package com.woocommerce.android.ui.payments.refunds

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRefundByAmountBinding
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.HideValidationError
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowValidationError
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RefundByAmountFragment : BaseFragment(R.layout.fragment_refund_by_amount) {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var _binding: FragmentRefundByAmountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IssueRefundViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_refunds)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRefundByAmountBinding.bind(view)

        initializeViews()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeViews() {
        binding.issueRefundBtnNextFromAmount.setOnClickListener {
            viewModel.onNextButtonTappedFromAmounts()
        }
    }

    private fun setupObservers() {
        viewModel.refundByAmountStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.availableForRefund?.takeIfNotEqualTo(old?.availableForRefund) {
                binding.issueRefundTxtAvailableForRefund.text = it
            }
            new.enteredAmount.takeIfNotEqualTo(old?.enteredAmount) {
                binding.issueRefundRefundAmount.setValue(new.enteredAmount)
            }
            new.isNextButtonEnabled?.takeIfNotEqualTo(old?.isNextButtonEnabled) {
                binding.issueRefundBtnNextFromAmount.isEnabled = it
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowValidationError -> binding.issueRefundRefundAmount.error = event.message
                is HideValidationError -> binding.issueRefundRefundAmount.error = null
                else -> event.isHandled = false
            }
        }

        binding.issueRefundRefundAmount.value.filterNotNull().observe(viewLifecycleOwner) {
            viewModel.onManualRefundAmountChanged(it)
        }
    }
}
