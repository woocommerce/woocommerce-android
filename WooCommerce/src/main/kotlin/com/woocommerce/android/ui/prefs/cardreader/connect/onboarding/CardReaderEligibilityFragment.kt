package com.woocommerce.android.ui.prefs.cardreader.connect.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderEligibilityBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.connect.onboarding.CardReaderEligibilityViewModel.ViewState.CountryNotSupportedState
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderEligibilityFragment : BaseFragment(R.layout.fragment_card_reader_eligibility) {
    val viewModel: CardReaderEligibilityViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderEligibilityBinding.bind(view)

        observeViewState(binding)
    }

    private fun observeViewState(binding: FragmentCardReaderEligibilityBinding) {
        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { state ->
                // We presume we are later going to have different states
                when (state) {
                    is CountryNotSupportedState -> {
                        UiHelpers.setTextOrHide(binding.eligibilityHeader, state.headerLabel)
                        UiHelpers.setImageOrHide(binding.eligibilityIllustration, state.illustration)
                        UiHelpers.setTextOrHide(binding.eligibilityHint, state.hintLabel)
                        UiHelpers.setTextOrHide(binding.eligibilityHelp, state.contactSupportLabel)
                        UiHelpers.setTextOrHide(binding.eligibilityLearnMore, state.learnMoreLabel)
                        binding.eligibilityHelp.setOnClickListener {
                            state.onContactSupportActionClicked?.invoke()
                        }
                        binding.eligibilityLearnMore.setOnClickListener {
                            state.onLearnMoreActionClicked?.invoke()
                        }
                    }
                }
            }
        )
    }
}
