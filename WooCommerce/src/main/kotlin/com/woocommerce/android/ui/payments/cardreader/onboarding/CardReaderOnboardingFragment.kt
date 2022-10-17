package com.woocommerce.android.ui.payments.cardreader.onboarding

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingCodDisabledBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingGenericErrorBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingLoadingBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingNetworkErrorBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingSelectPaymentGatewayBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingStripeBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingUnsupportedBinding
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingWcpayBinding
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.ToastUtils
import java.math.BigDecimal

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
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is CardReaderOnboardingViewModel.OnboardingEvent.NavigateToSupport -> {
                    requireActivity().startHelpActivity(HelpActivity.Origin.CARD_READER_ONBOARDING)
                }
                is CardReaderOnboardingViewModel.OnboardingEvent.NavigateToUrlInWPComWebView -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(urlToLoad = event.url)
                    )
                }
                is CardReaderOnboardingViewModel.OnboardingEvent.NavigateToUrlInGenericWebView -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is CardReaderOnboardingViewModel.OnboardingEvent.ContinueToHub -> {
                    findNavController().navigate(
                        CardReaderOnboardingFragmentDirections
                            .actionCardReaderOnboardingFragmentToCardReaderHubFragment(
                                event.cardReaderFlowParam,
                            )
                    )
                }
                is CardReaderOnboardingViewModel.OnboardingEvent.ContinueToConnection -> {
                    findNavController().navigate(
                        CardReaderOnboardingFragmentDirections
                            .actionCardReaderOnboardingFragmentToCardReaderConnectDialogFragment(
                                event.cardReaderFlowParam
                            )
                    )
                }
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                else -> event.isHandled = false
            }
        }

        viewModel.viewStateData.observe(
            viewLifecycleOwner
        ) { state ->
            showOnboardingLayout(binding, state)
        }
    }

    private fun showOnboardingLayout(
        binding: FragmentCardReaderOnboardingBinding,
        state: CardReaderOnboardingViewModel.OnboardingViewState
    ) {

        val layout = if (binding.container.tag != state.layoutRes) {
            binding.container.removeAllViews()
            val layout = LayoutInflater.from(requireActivity()).inflate(state.layoutRes, binding.container, false)
            binding.container.addView(layout)
            layout
        } else {
            binding.container[0]
        }
        binding.container.tag = state.layoutRes
        displayOnboardingState(layout, state)
    }

    private fun displayOnboardingState(
        layout: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState,
    ) {
        when (state) {
            is CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState ->
                showGenericErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState ->
                showNetworkErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.LoadingState ->
                showLoadingState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState ->
                showCountryNotSupportedState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.WCPayError ->
                showWCPayErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.StripeAcountError ->
                showStripeAccountError(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.StripeExtensionError ->
                showStripeExtensionErrorState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.SelectPaymentPluginState ->
                showPaymentPluginSelectionState(layout, state)
            is CardReaderOnboardingViewModel.OnboardingViewState.CashOnDeliveryDisabledState ->
                showCashOnDeliveryDisabledState(layout, state)
        }.exhaustive
    }

    private fun showCashOnDeliveryDisabledState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.CashOnDeliveryDisabledState
    ) {
        val binding = FragmentCardReaderOnboardingCodDisabledBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.cashOnDeliveryHintLabel)
        UiHelpers.setTextOrHide(binding.skipCashOnDelivery, state.skipCashOnDeliveryButtonLabel)
        UiHelpers.setTextOrHide(binding.enableCashOnDelivery, state.enableCashOnDeliveryButtonLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, state.cardIllustration)

        if (state.shouldShowProgress) {
            binding.enableCashOnDelivery.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            binding.enableCashOnDelivery.text = ""
        } else {
            binding.enableCashOnDelivery.isEnabled = true
            binding.progressBar.visibility = View.GONE
            when (state.cashOnDeliveryEnabledSuccessfully) {
                true -> state.onCashOnDeliveryEnabledSuccessfully.invoke()
                false -> {
                    ToastUtils.showToast(
                        requireContext(),
                        R.string.card_reader_onboarding_cash_on_delivery_enable_failure
                    )
                }
                null -> {}
            }
        }

        binding.skipCashOnDelivery.setOnClickListener {
            state.onSkipCashOnDeliveryClicked.invoke()
        }
        binding.enableCashOnDelivery.setOnClickListener {
            state.onEnableCashOnDeliveryClicked.invoke()
        }
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showPaymentPluginSelectionState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.SelectPaymentPluginState
    ) {
        var selectedPluginType: PluginType = PluginType.WOOCOMMERCE_PAYMENTS
        val binding = FragmentCardReaderOnboardingSelectPaymentGatewayBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.hintLabel, state.choosePluginHintLabel)
        UiHelpers.setTextOrHide(binding.selectWcPayButton, state.selectWcPayButtonLabel)
        UiHelpers.setTextOrHide(binding.selectStripeButton, state.selectStripeButtonLabel)
        UiHelpers.setTextOrHide(binding.confirmPaymentMethod, state.confirmPaymentMethodButtonLabel)
        binding.cardIllustration.setImageResource(state.cardIllustration)
        binding.icSelectWcPay.setImageResource(state.icWcPayLogo)
        binding.icCheckmarkWcPay.setImageResource(state.icCheckmarkWcPay)

        binding.selectWcPayButton.setOnClickListener {
            selectedPluginType = PluginType.WOOCOMMERCE_PAYMENTS
            binding.selectWcPayButton.strokeColor =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.woo_purple_60))
            binding.icCheckmarkWcPay.visibility = View.VISIBLE
            binding.icCheckmarkStripe.visibility = View.GONE
            binding.selectStripeButton.strokeColor =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.select_payment_gateway_stroke))
        }
        binding.selectStripeButton.setOnClickListener {
            selectedPluginType = PluginType.STRIPE_EXTENSION_GATEWAY
            binding.selectStripeButton.strokeColor =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.woo_purple_60))
            binding.icCheckmarkWcPay.visibility = View.GONE
            binding.icCheckmarkStripe.visibility = View.VISIBLE
            binding.selectWcPayButton.strokeColor =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.select_payment_gateway_stroke))
        }
        binding.confirmPaymentMethod.setOnClickListener {
            state.onConfirmPaymentMethodClicked.invoke(selectedPluginType)
        }
    }

    private fun showLoadingState(
        view: View,
        state: CardReaderOnboardingViewModel.OnboardingViewState.LoadingState,
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
        state: CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedErrorState
    ) {
        val binding = FragmentCardReaderOnboardingUnsupportedBinding.bind(view)
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
}

sealed class CardReaderOnboardingParams : Parcelable {
    abstract val cardReaderFlowParam: CardReaderFlowParam

    @Parcelize
    data class Check(
        override val cardReaderFlowParam: CardReaderFlowParam,
        val pluginType: PluginType? = null
    ) : CardReaderOnboardingParams()

    @Parcelize
    data class Failed(
        override val cardReaderFlowParam: CardReaderFlowParam,
        val onboardingState: CardReaderOnboardingState
    ) : CardReaderOnboardingParams()
}

sealed class CardReaderFlowParam : Parcelable {
    @Parcelize
    object CardReadersHub : CardReaderFlowParam()

    sealed class PaymentOrRefund : CardReaderFlowParam() {
        abstract val orderId: Long

        @Parcelize
        data class Payment(
            override val orderId: Long,
            val paymentType: PaymentType
        ) : PaymentOrRefund() {
            enum class PaymentType {
                SIMPLE, ORDER
            }
        }

        @Parcelize
        data class Refund(override val orderId: Long, val refundAmount: BigDecimal) : PaymentOrRefund()
    }
}
