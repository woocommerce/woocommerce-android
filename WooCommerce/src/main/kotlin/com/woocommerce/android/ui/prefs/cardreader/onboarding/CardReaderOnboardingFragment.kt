package com.woocommerce.android.ui.prefs.cardreader.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingLoadingBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingUnsupportedCountryBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderOnboardingFragment : BaseFragment(R.layout.fragment_card_reader_onboarding) {
    val viewModel: CardReaderOnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCardReaderOnboardingBinding.bind(view)
        initObservers(binding)
    }

    private fun initObservers(binding: FragmentCardReaderOnboardingBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is MultiLiveEvent.Event.Exit -> navigateBackWithNotice(KEY_READER_ONBOARDING_RESULT)
                    else -> event.isHandled = false
                }
            }
        )

        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { state ->
                showOnboardingLayout(binding, state)
            }
        )
    }

    private fun showOnboardingLayout(
        binding: FragmentCardReaderOnboardingBinding,
        state: CardReaderOnboardingViewModel.OnboardingViewState
    ) {
        binding.container.removeAllViews()
        val layout = LayoutInflater.from(requireActivity()).inflate(state.layoutRes, binding.container, false)
        binding.container.addView(layout)
        when (state) {
            is CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.LoadingState ->
                showLoadingState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedCountryState ->
                showCountryNotSupportedState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayAccountOverdueRequirementsState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayAccountPendingRequirementsState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayAccountRejectedState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayAccountUnderReviewState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayInTestModeWithLiveAccountState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayNotActivatedState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayNotInstalledState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayNotSetupState -> TODO()
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayUnsupportedVersionState -> TODO()
        }
    }

    private fun showLoadingState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.LoadingState
    ) {
        val binding = FragmentCardReaderOnboardingLoadingBinding.bind(view)
    }

    private fun showCountryNotSupportedState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedCountryState
    ) {
        val binding = FragmentCardReaderOnboardingUnsupportedCountryBinding.bind(view)
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

    override fun getFragmentTitle() = resources.getString(R.string.card_reader_onboarding_title)

    companion object {
        const val KEY_READER_ONBOARDING_RESULT = "key_reader_onboarding_result"
    }
}
