package com.woocommerce.android.ui.payments.changeduecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeDueCalculatorFragment : BaseFragment() {

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: ChangeDueCalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewLifecycleOwnerLiveData.observe(this) { viewLifecycleOwner ->
            viewLifecycleOwner?.let { lifecycleOwner ->
                viewModel.event.observe(lifecycleOwner) { event ->
                    when (event) {
                        is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                ChangeDueCalculatorScreen(
                    uiState = uiState,
                    onNavigateUp = viewModel::onBackPressed,
                    onCompleteOrderClick = {
                        val action = ChangeDueCalculatorFragmentDirections
                            .actionChangeDueCalculatorFragmentToSelectPaymentMethodFragment(
                                cardReaderFlowParam = CardReaderFlowParam.PaymentOrRefund.Payment(
                                    viewModel.navArgs.orderId,
                                    CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
                                ),
                                isOrderPaid = true
                            )
                        findNavController().navigate(action)
                    },
                    onAmountReceivedChanged = { viewModel.updateAmountReceived(it) }
                )
            }
        }
    }
}
