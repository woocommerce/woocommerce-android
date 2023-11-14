package com.woocommerce.android.ui.payments.refunds

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRefundSummaryBinding
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RefundSummaryFragment : BaseFragment(R.layout.fragment_refund_summary), BackPressListener {
    companion object {
        const val REFUND_ORDER_NOTICE_KEY = "refund_order_notice"
        const val KEY_INTERAC_SUCCESS = "interac_refund_success"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_refunds)

    private var _binding: FragmentRefundSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRefundSummaryBinding.bind(view)

        initializeViews()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.getSnack(event.message, *event.args).show()
                is Exit -> navigateBackWithNotice(REFUND_ORDER_NOTICE_KEY, R.id.orderDetailFragment)
                is ShowRefundConfirmation -> {
                    val action =
                        RefundSummaryFragmentDirections.actionRefundSummaryFragmentToRefundConfirmationDialog(
                            title = event.title,
                            message = event.message,
                            positiveButtonTitle = event.confirmButtonTitle
                        )
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        }

        viewModel.refundSummaryStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isFormEnabled?.takeIfNotEqualTo(old?.isFormEnabled) {
                binding.refundSummaryBtnRefund.isEnabled = new.isFormEnabled
                binding.refundSummaryReason.isEnabled = new.isFormEnabled
            }
            new.isSubmitButtonEnabled.takeIfNotEqualTo(old?.isSubmitButtonEnabled) {
                binding.refundSummaryBtnRefund.isEnabled = new.isSubmitButtonEnabled
            }
            new.refundAmount?.takeIfNotEqualTo(old?.refundAmount) {
                binding.refundSummaryRefundAmount.text = it
            }
            new.previouslyRefunded?.takeIfNotEqualTo(old?.previouslyRefunded) {
                binding.refundSummaryPreviouslyRefunded.text = it
            }
            new.refundMethod?.takeIfNotEqualTo(old?.refundMethod) {
                binding.refundSummaryMethod.text = it
            }
            new.isMethodDescriptionVisible?.takeIfNotEqualTo(old?.isMethodDescriptionVisible) { visible ->
                if (visible) {
                    binding.refundSummaryMethodDescription.show()
                } else {
                    binding.refundSummaryMethodDescription.hide()
                }
            }
            handleDialogNotice(
                KEY_INTERAC_SUCCESS,
                entryId = R.id.refundSummaryFragment
            ) {
                viewModel.refund()
            }
        }
    }

    private fun initializeViews() {
        binding.refundSummaryBtnRefund.setOnClickListener {
            viewModel.onRefundIssued(binding.refundSummaryReason.text.toString())
        }

        binding.refundSummaryReason.doOnTextChanged { _, _, _, _ ->
            val maxLength = binding.refundSummaryReasonLayout.counterMaxLength
            viewModel.onRefundSummaryTextChanged(maxLength, binding.refundSummaryReason.length())
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
