package com.woocommerce.android.ui.payments.cardreader.statuschecker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.PaymentsBaseDialogFragment
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
                else -> event.isHandled = false
            }
        }
    }
}
