package com.woocommerce.android.ui.orders.cardreader.statuschecker

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderStatusCheckerDialogBinding
import com.woocommerce.android.extensions.exhaustive
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderStatusCheckerDialogFragment : DialogFragment(R.layout.card_reader_status_checker_dialog) {
    val viewModel: CardReaderStatusCheckerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.run {
            window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog_NoAnimation
            setCanceledOnTouchOutside(false)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObservers()

        val binding = CardReaderStatusCheckerDialogBinding.bind(view)
        startAnimation(binding)
        initClickListeners(binding)
    }

    private fun initObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding -> {
                    findNavController()
                        .navigate(
                            CardReaderStatusCheckerDialogFragmentDirections
                                .actionCardReaderStatusCheckerDialogFragmentToCardReaderOnboardingFragment(
                                    event.cardReaderFlowParam
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

    private fun startAnimation(binding: CardReaderStatusCheckerDialogBinding) {
        (binding.illustration.drawable as? AnimatedVectorDrawable)?.start()
    }

    private fun initClickListeners(binding: CardReaderStatusCheckerDialogBinding) {
        binding.secondaryActionBtn.setOnClickListener { dismiss() }
    }
}
