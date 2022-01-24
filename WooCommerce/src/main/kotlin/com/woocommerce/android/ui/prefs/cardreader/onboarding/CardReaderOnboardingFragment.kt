package com.woocommerce.android.ui.prefs.cardreader.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingGenericErrorBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingLoadingBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingNetworkErrorBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingStripeBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingUnsupportedCountryBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingWcpayBinding
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
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
                    is CardReaderOnboardingViewModel.OnboardingEvent.NavigateToSupport -> {
                        requireActivity().startHelpActivity(HelpActivity.Origin.CARD_READER_ONBOARDING)
                    }
                    is CardReaderOnboardingViewModel.OnboardingEvent.ViewLearnMore -> {
                        ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
                    }
                    is CardReaderOnboardingViewModel.OnboardingEvent.Continue -> {
                        val inSettingsGraph = findNavController().graph.id == R.id.nav_graph_settings
                        if (inSettingsGraph) {
                            findNavController().navigate(
                                R.id.action_cardReaderOnboardingFragment_to_cardReaderHubFragment
                            )
                        } else {
                            navigateBackWithNotice(KEY_READER_ONBOARDING_SUCCESS)
                        }
                    }
                    is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
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
            is CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState ->
                showGenericErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState ->
                showNetworkErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.LoadingState ->
                showLoadingState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedCountryState ->
                showCountryNotSupportedState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayError ->
                showWCPayErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError ->
                showStripeAccountError(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError ->
                showStripeExtensionErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.WcPayAndStripeInstalledState ->
                showBothPluginsInstalledState(layout, state)
        }.exhaustive
    }

    private fun showBothPluginsInstalledState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.WcPayAndStripeInstalledState
    ) {
        val binding = FragmentCardReaderOnboardingStripeBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.illustration)
    }

    private fun showLoadingState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.LoadingState
    ) {
        val binding = FragmentCardReaderOnboardingLoadingBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeaderTv, state.headerLabel)
        UiHelpers.setTextOrHide(binding.hintTv, state.hintLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustrationIv, state.illustration)
    }

    private fun showGenericErrorState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState
    ) {
        val binding = FragmentCardReaderOnboardingGenericErrorBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textSupport, state.contactSupportLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.illustration)
        binding.textSupport.setOnClickListener {
            state.onContactSupportActionClicked.invoke()
        }
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showNetworkErrorState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState
    ) {
        val binding = FragmentCardReaderOnboardingNetworkErrorBinding.bind(view)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.illustration)
        binding.buttonRetry.setOnClickListener {
            state.onRetryButtonActionClicked.invoke()
        }
    }

    private fun showStripeAccountError(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError
    ) {
        val binding = FragmentCardReaderOnboardingStripeBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setTextOrHide(binding.textSupport, state.contactSupportLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.illustration)
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
        binding.textSupport.setOnClickListener {
            state.onContactSupportActionClicked.invoke()
        }

        UiHelpers.setTextOrHide(binding.button, state.buttonLabel)
        state.onButtonActionClicked?.let { onButtonActionClicked ->
            binding.button.setOnClickListener {
                onButtonActionClicked.invoke()
            }
        }
    }

    private fun showWCPayErrorState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.WCPayError
    ) {
        val binding = FragmentCardReaderOnboardingWcpayBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        UiHelpers.setTextOrHide(binding.refreshButton, state.refreshButtonLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.illustration)
        binding.refreshButton.setOnClickListener {
            state.refreshButtonAction.invoke()
        }
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showStripeExtensionErrorState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError
    ) {
        val binding = FragmentCardReaderOnboardingWcpayBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        UiHelpers.setTextOrHide(binding.refreshButton, state.refreshButtonLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.illustration)
        binding.refreshButton.setOnClickListener {
            state.refreshButtonAction.invoke()
        }
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showCountryNotSupportedState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedCountryState
    ) {
        val binding = FragmentCardReaderOnboardingUnsupportedCountryBinding.bind(view)
        UiHelpers.setTextOrHide(binding.unsupportedCountryHeader, state.headerLabel)
        UiHelpers.setImageOrHideInLandscape(binding.unsupportedCountryIllustration, state.illustration)
        UiHelpers.setTextOrHide(binding.unsupportedCountryHint, state.hintLabel)
        UiHelpers.setTextOrHide(binding.unsupportedCountryHelp, state.contactSupportLabel)
        UiHelpers.setTextOrHide(binding.unsupportedCountryLearnMoreContainer.learnMore, state.learnMoreLabel)
        binding.unsupportedCountryHelp.setOnClickListener {
            state.onContactSupportActionClicked.invoke()
        }
        binding.unsupportedCountryLearnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    override fun getFragmentTitle() = resources.getString(R.string.card_reader_onboarding_title)

    companion object {
        const val KEY_READER_ONBOARDING_SUCCESS = "key_reader_onboarding_success"
    }
}
