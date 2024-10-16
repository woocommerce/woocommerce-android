package com.woocommerce.android.ui.payments.cardreader.statuschecker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.PaymentsBaseDialogFragment
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_PREPARE_FOR_CARD_PAYMENT_REQUEST_KEY
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderStatusCheckerDialogFragment : PaymentsBaseDialogFragment(R.layout.card_reader_status_checker_dialog) {
    val viewModel: CardReaderStatusCheckerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setCanceledOnTouchOutside(false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initObservers()
    }

    private fun initObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderOnboardingFragment(
                                    event.cardReaderOnboardingParams,
                                    event.cardReaderType
                                )
                        )
                }
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderTutorialDialogFragment(
                                    event.cardReaderFlowParam,
                                    event.cardReaderType
                                )
                        )
                }
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToWelcome -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderWelcomeDialogFragment(
                                    event.cardReaderFlowParam,
                                    event.cardReaderType
                                )
                        )
                }
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderConnectDialogFragment(
                                    event.cardReaderFlowParam,
                                    event.cardReaderType,
                                )
                        )
                }

                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NotifyPOSReadyToCollectPayment -> {
                    // TODO: if called from POS, exit with result "ready to collect payment"
                    parentFragmentManager.setFragmentResult(
                        WooPosCardReaderActivity.WOO_POS_PREPARE_FOR_CARD_PAYMENT_REQUEST_KEY,
                        Bundle().apply {
                            putParcelable(WOO_POS_PREPARE_FOR_CARD_PAYMENT_REQUEST_KEY, WooPosCardReaderState.ReadyForPayment)
                        },
                    )
                    dismiss()
                }
                else -> event.isHandled = false
            }
        }
    }
}
