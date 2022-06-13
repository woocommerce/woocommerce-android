package com.woocommerce.android.ui.cardreader.statuschecker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.exhaustive
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderStatusCheckerDialogFragment : DialogFragment(R.layout.card_reader_status_checker_dialog) {
    val viewModel: CardReaderStatusCheckerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.let {
            it.setCanceledOnTouchOutside(false)
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
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
                                    event.cardReaderOnboardingParams
                                )
                        )
                }
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderTutorialDialogFragment(
                                    event.cardReaderFlowParam
                                )
                        )
                }
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToWelcome -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderWelcomeDialogFragment(
                                    event.cardReaderFlowParam
                                )
                        )
                }
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderConnectDialogFragment(
                                    event.cardReaderFlowParam
                                )
                        )
                }
                else -> event.isHandled = false
            }.exhaustive
        }
    }
}
