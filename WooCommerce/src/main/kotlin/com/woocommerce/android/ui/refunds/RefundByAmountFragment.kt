package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRefundByAmountBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.HideValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowValidationError
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import javax.inject.Inject

class RefundByAmountFragment : BaseFragment(R.layout.fragment_refund_by_amount) {
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var _binding: FragmentRefundByAmountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) {
        viewModelFactory.get()
    }
    
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
            new.currency?.takeIfNotEqualTo(old?.currency) {
                binding.issueRefundRefundAmount.initView(new.currency, new.decimals, currencyFormatter)
            }
            new.enteredAmount.takeIfNotEqualTo(old?.enteredAmount) {
                binding.issueRefundRefundAmount.setValue(new.enteredAmount)
            }
            new.isNextButtonEnabled?.takeIfNotEqualTo(old?.isNextButtonEnabled) {
                binding.issueRefundBtnNextFromAmount.isEnabled = it
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowValidationError -> binding.issueRefundRefundAmount.error = event.message
                is HideValidationError -> binding.issueRefundRefundAmount.error = null
                else -> event.isHandled = false
            }
        })

        binding.issueRefundRefundAmount.value.observe(viewLifecycleOwner, Observer {
            viewModel.onManualRefundAmountChanged(it)
        })
    }
}
