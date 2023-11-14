package com.woocommerce.android.ui.payments.refunds

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RefundConfirmationDialog : ConfirmationDialog() {
    private val viewModel: IssueRefundViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_refunds)

    override fun returnResult(result: Boolean) {
        viewModel.onRefundConfirmed(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(
            this
        ) { event ->
            when (event) {
                is IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen -> {
                    val action =
                        RefundConfirmationDialogDirections.actionRefundConfirmationDialogToCardReaderFlow(
                            cardReaderFlowParam = CardReaderFlowParam.PaymentOrRefund.Refund(
                                event.orderId,
                                event.refundAmount
                            )
                        )
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        }
    }
}
