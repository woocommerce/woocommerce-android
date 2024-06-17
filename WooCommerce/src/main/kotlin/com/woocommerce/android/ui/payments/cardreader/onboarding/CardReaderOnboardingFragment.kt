package com.woocommerce.android.ui.payments.cardreader.onboarding

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.get
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
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
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.ToastUtils
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderOnboardingFragment : BaseFragment(R.layout.fragment_card_reader_onboarding) {
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    val viewModel: CardReaderOnboardingViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCardReaderOnboardingBinding.bind(view)
        setupToolbar(binding)
        initObservers(binding)
    }

    private fun setupToolbar(binding: FragmentCardReaderOnboardingBinding) {
        binding.toolbar.title = resources.getString(R.string.card_reader_onboarding_title)
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.ic_back_24dp
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun initObservers(binding: FragmentCardReaderOnboardingBinding) {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is CardReaderOnboardingEvent.NavigateToSupport -> {
                    requireActivity().startHelpActivity(HelpOrigin.CARD_READER_ONBOARDING)
                }
                is CardReaderOnboardingEvent.NavigateToUrlInWPComWebView -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(urlToLoad = event.url)
                    )
                }
                is CardReaderOnboardingEvent.NavigateToUrlInGenericWebView -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is CardReaderOnboardingEvent.ContinueToHub -> {
                    findNavController().navigate(
                        CardReaderOnboardingFragmentDirections
                            .actionCardReaderOnboardingFragmentToCardReaderHubFragment(
                                event.cardReaderFlowParam,
                            )
                    )
                }
                is CardReaderOnboardingEvent.ContinueToConnection -> {
                    findNavController().navigate(
                        CardReaderOnboardingFragmentDirections
                            .actionCardReaderOnboardingFragmentToCardReaderConnectDialogFragment(
                                event.cardReaderFlowParam,
                                event.cardReaderType
                            )
                    )
                }
                is MultiLiveEvent.Event.ShowUiStringSnackbar -> uiMessageResolver.showSnack(event.message)
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
        state: CardReaderOnboardingViewState
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
        state: CardReaderOnboardingViewState,
    ) {
        when (state) {
            is CardReaderOnboardingViewState.GenericErrorState ->
                showGenericErrorState(layout, state)
            is CardReaderOnboardingViewState.NoConnectionErrorState ->
                showNetworkErrorState(layout, state)
            is CardReaderOnboardingViewState.LoadingState ->
                showLoadingState(layout, state)
            is CardReaderOnboardingViewState.UnsupportedErrorState ->
                showCountryNotSupportedState(layout, state)
            is CardReaderOnboardingViewState.WCPayError ->
                showWCPayErrorState(layout, state)
            is CardReaderOnboardingViewState.StripeAccountError ->
                showStripeAccountError(layout, state)
            is CardReaderOnboardingViewState.StripeExtensionError ->
                showStripeExtensionErrorState(layout, state)
            is CardReaderOnboardingViewState.SelectPaymentPluginState ->
                showPaymentPluginSelectionState(layout, state)
            is CardReaderOnboardingViewState.CashOnDeliveryDisabledState ->
                showCashOnDeliveryDisabledState(layout, state)
        }
    }

    private fun showCashOnDeliveryDisabledState(
        view: View,
        state: CardReaderOnboardingViewState.CashOnDeliveryDisabledState
    ) {
        val binding = FragmentCardReaderOnboardingCodDisabledBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.cashOnDeliveryHintLabel)
        UiHelpers.setTextOrHide(binding.skipCashOnDelivery, state.skipCashOnDeliveryButtonLabel)
        UiHelpers.setTextOrHide(binding.enableCashOnDelivery, state.enableCashOnDeliveryButtonLabel)
        UiHelpers.setTextOrHide(binding.textSupport, state.contactSupportLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustration, state.cardIllustration)

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
        binding.textSupport.setOnClickListener {
            state.onContactSupportActionClicked.invoke()
        }
    }

    private fun showPaymentPluginSelectionState(
        view: View,
        state: CardReaderOnboardingViewState.SelectPaymentPluginState
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
        state: CardReaderOnboardingViewState.LoadingState,
    ) {
        val binding = FragmentCardReaderOnboardingLoadingBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeaderTv, state.headerLabel)
        UiHelpers.setTextOrHide(binding.hintTv, state.hintLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustrationIv, state.illustration)
    }

    private fun showGenericErrorState(
        view: View,
        state: CardReaderOnboardingViewState.GenericErrorState
    ) {
        val binding = FragmentCardReaderOnboardingGenericErrorBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textSupport, state.contactSupportLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustration, state.illustration)
        binding.textSupport.setOnClickListener {
            state.onContactSupportActionClicked.invoke()
        }
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showNetworkErrorState(
        view: View,
        state: CardReaderOnboardingViewState.NoConnectionErrorState
    ) {
        val binding = FragmentCardReaderOnboardingNetworkErrorBinding.bind(view)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustration, state.illustration)
        binding.buttonRetry.setOnClickListener {
            state.onRetryButtonActionClicked.invoke()
        }
    }

    private fun showStripeAccountError(
        view: View,
        state: CardReaderOnboardingViewState.StripeAccountError
    ) {
        val binding = FragmentCardReaderOnboardingStripeBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustration, state.illustration)

        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreButton.label)
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }

        UiHelpers.setTextOrHide(binding.textSupport, state.contactSupportButton.label)
        binding.textSupport.setOnClickListener {
            state.onContactSupportActionClicked.invoke()
        }

        UiHelpers.setTextOrHide(binding.primaryButton, state.actionButtonPrimary?.label)
        binding.primaryButton.setWhiteIcon(state.actionButtonPrimary?.icon)
        state.actionButtonPrimary?.action?.let { onButtonActionClicked ->
            binding.primaryButton.setOnClickListener {
                onButtonActionClicked.invoke()
            }
        }

        UiHelpers.setTextOrHide(binding.secondaryButton, state.actionButtonSecondary?.label)
        binding.secondaryButton.setWhiteIcon(state.actionButtonSecondary?.icon)
        state.actionButtonSecondary?.action?.let { onButtonActionClicked ->
            binding.secondaryButton.setOnClickListener {
                onButtonActionClicked.invoke()
            }
        }
    }

    private fun showWCPayErrorState(
        view: View,
        state: CardReaderOnboardingViewState.WCPayError
    ) {
        val binding = FragmentCardReaderOnboardingWcpayBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustration, state.illustration)

        UiHelpers.setTextOrHide(binding.primaryButton, state.actionButtonPrimary.label)
        binding.primaryButton.setWhiteIcon(state.actionButtonPrimary.icon)
        binding.primaryButton.setOnClickListener {
            state.actionButtonPrimary.action.invoke()
        }

        UiHelpers.setTextOrHide(binding.secondaryButton, state.actionButtonSecondary?.label)
        binding.secondaryButton.setWhiteIcon(state.actionButtonSecondary?.icon)
        binding.secondaryButton.setOnClickListener {
            state.actionButtonSecondary?.action?.invoke()
        }

        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreButton.label)
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showStripeExtensionErrorState(
        view: View,
        state: CardReaderOnboardingViewState.StripeExtensionError
    ) {
        val binding = FragmentCardReaderOnboardingWcpayBinding.bind(view)
        UiHelpers.setTextOrHide(binding.textHeader, state.headerLabel)
        UiHelpers.setTextOrHide(binding.textLabel, state.hintLabel)
        binding.primaryButton.visibility = View.GONE
        UiHelpers.setTextOrHide(binding.secondaryButton, state.refreshButtonLabel)
        UiHelpers.setTextOrHide(binding.learnMoreContainer.learnMore, state.learnMoreLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(binding.illustration, state.illustration)
        binding.secondaryButton.setOnClickListener {
            state.refreshButtonAction.invoke()
        }
        binding.learnMoreContainer.learnMore.setOnClickListener {
            state.onLearnMoreActionClicked.invoke()
        }
    }

    private fun showCountryNotSupportedState(
        view: View,
        state: CardReaderOnboardingViewState.UnsupportedErrorState
    ) {
        val binding = FragmentCardReaderOnboardingUnsupportedBinding.bind(view)
        UiHelpers.setTextOrHide(binding.unsupportedCountryHeader, state.headerLabel)
        UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(
            binding.unsupportedCountryIllustration,
            state.illustration
        )
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

    private fun MaterialButton.setWhiteIcon(@DrawableRes icon: Int?) {
        icon?.let {
            setIconResource(it)
            iconTint = ColorStateList.valueOf(getColor(context, R.color.woo_white))
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
            iconSize = resources.getDimensionPixelSize(R.dimen.major_125)
        }
    }
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
    data class CardReadersHub(
        val openInHub: OpenInHub = OpenInHub.NONE,
    ) : CardReaderFlowParam() {
        enum class OpenInHub {
            TAP_TO_PAY_SUMMARY, NONE
        }
    }

    @Parcelize
    data object WooPosConnection : CardReaderFlowParam()

    sealed class PaymentOrRefund : CardReaderFlowParam() {
        abstract val orderId: Long

        @Parcelize
        data class Payment(
            override val orderId: Long,
            val paymentType: PaymentType
        ) : PaymentOrRefund() {
            enum class PaymentType {
                SIMPLE,
                ORDER,
                ORDER_CREATION,
                TRY_TAP_TO_PAY,
                WOO_POS,
            }
        }

        @Parcelize
        data class Refund(override val orderId: Long, val refundAmount: BigDecimal) : PaymentOrRefund()
    }
}

@Parcelize
enum class CardReaderType : Parcelable {
    BUILT_IN,
    EXTERNAL,
}
