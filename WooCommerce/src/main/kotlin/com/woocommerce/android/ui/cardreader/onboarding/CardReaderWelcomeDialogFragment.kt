package com.woocommerce.android.ui.cardreader.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.CardReaderWelcomeDialogBinding
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderWelcomeViewModel.CardReaderWelcomeDialogEvent.NavigateToOnboardingFlow
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderWelcomeDialogFragment : DialogFragment(R.layout.card_reader_welcome_dialog) {
    val viewModel: CardReaderWelcomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = CardReaderWelcomeDialogBinding.bind(view)
        initObservers(binding)
    }

    private fun initObservers(binding: CardReaderWelcomeDialogBinding) {
        viewModel.viewState.observe(viewLifecycleOwner) { viewState ->
            UiHelpers.setImageOrHideInLandscape(binding.illustration, viewState.img)
            UiHelpers.setTextOrHide(binding.headerLabel, viewState.header)
            UiHelpers.setTextOrHide(binding.text, viewState.text)
            UiHelpers.setTextOrHide(binding.actionBtn, viewState.buttonLabel)
            binding.actionBtn.setOnClickListener {
                viewState.buttonAction.invoke()
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToOnboardingFlow -> {
                    findNavController()
                        .navigate(
                            CardReaderWelcomeDialogFragmentDirections
                                .actionCardReaderWelcomeDialogFragmentToCardReaderConnectDialogFragment(
                                    event.cardReaderFlowParam
                                )
                        )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
