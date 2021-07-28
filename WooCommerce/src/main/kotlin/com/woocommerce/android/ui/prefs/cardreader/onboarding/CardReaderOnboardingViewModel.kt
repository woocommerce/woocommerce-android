package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CardReaderOnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderChecker: CardReaderOnboardingChecker
) : ScopedViewModel(savedState) {
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private val viewState = MutableLiveData<OnboardingViewState>()
    val viewStateData: LiveData<OnboardingViewState> = viewState

    init {
        refreshState()
    }

    private fun refreshState() {
        // TODO
        viewState.value = OnboardingViewState.WCPayError.WCPayNotSetupState(::refreshState, ::onLearnMoreClicked)
        return
        launch {
            viewState.value = OnboardingViewState.LoadingState
            when (val state = cardReaderChecker.getOnboardingState()) {
                CardReaderOnboardingState.OnboardingCompleted -> exitFlow()
                is CardReaderOnboardingState.CountryNotSupported ->
                    viewState.value = OnboardingViewState.UnsupportedCountryState(
                        convertCountryCodeToCountry(state.countryCode),
                        ::onContactSupportClicked,
                        ::onLearnMoreClicked
                    )
                CardReaderOnboardingState.WcpayNotInstalled ->
                    viewState.value =
                        OnboardingViewState.WCPayError.WCPayNotInstalledState(::refreshState, ::onLearnMoreClicked)
                CardReaderOnboardingState.WcpayUnsupportedVersion ->
                    viewState.value =
                        OnboardingViewState.WCPayError.WCPayUnsupportedVersionState(
                            ::refreshState,
                            ::onLearnMoreClicked
                        )
                CardReaderOnboardingState.WcpayNotActivated ->
                    viewState.value =
                        OnboardingViewState.WCPayError.WCPayNotActivatedState(::refreshState, ::onLearnMoreClicked)
                CardReaderOnboardingState.WcpaySetupNotCompleted ->
                    viewState.value =
                        OnboardingViewState.WCPayError.WCPayNotSetupState(::refreshState, ::onLearnMoreClicked)
                CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount ->
                    viewState.value = OnboardingViewState.WCPayInTestModeWithLiveAccountState
                CardReaderOnboardingState.StripeAccountUnderReview ->
                    viewState.value = OnboardingViewState.WCPayAccountUnderReviewState
                // TODO cardreader Pass due date to the state
                CardReaderOnboardingState.StripeAccountPendingRequirement ->
                    viewState.value = OnboardingViewState.WCPayAccountPendingRequirementsState("", ::exitFlow)
                CardReaderOnboardingState.StripeAccountOverdueRequirement ->
                    viewState.value = OnboardingViewState.WCPayAccountOverdueRequirementsState
                CardReaderOnboardingState.StripeAccountRejected ->
                    viewState.value = OnboardingViewState.WCPayAccountRejectedState
                CardReaderOnboardingState.GenericError ->
                    viewState.value = OnboardingViewState.GenericErrorState
                CardReaderOnboardingState.NoConnectionError ->
                    viewState.value = OnboardingViewState.NoConnectionErrorState
            }
        }
    }

    fun onCancelClicked() {
        WooLog.e(WooLog.T.CARD_READER, "Onboarding flow interrupted by the user.")
        exitFlow()
    }

    private fun onContactSupportClicked() {
        triggerEvent(OnboardingEvent.NavigateToSupport)
    }

    private fun onLearnMoreClicked() {
        triggerEvent(OnboardingEvent.ViewLearnMore)
    }

    private fun exitFlow() {
        triggerEvent(Event.Exit)
    }

    private fun convertCountryCodeToCountry(countryCode: String?) =
        Locale("", countryCode.orEmpty()).displayName

    sealed class OnboardingEvent : Event() {
        object ViewLearnMore : OnboardingEvent() {
            const val url = AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        }

        object NavigateToSupport : Event()
    }

    sealed class OnboardingViewState(@LayoutRes val layoutRes: Int) {
        object LoadingState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_loading)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.please_wait)
            @DrawableRes
            val illustration: Int = R.drawable.img_payment_onboarding_loading
        }

        // TODO cardreader Update layout resource
        object GenericErrorState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            // TODO cardreader implement generic error state
        }

        // TODO cardreader Update layout resource
        object NoConnectionErrorState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            // TODO cardreader implement no connection error state
        }

        class UnsupportedCountryState(
            val countryDisplayName: String,
            val onContactSupportActionClicked: (() -> Unit),
            val onLearnMoreActionClicked: (() -> Unit)
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_unsupported_country) {
            val headerLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_header,
                params = listOf(UiString.UiStringText(countryDisplayName))
            )
            val illustration = R.drawable.img_products_error
            val hintLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_hint
            )
            val contactSupportLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_contact_support,
                containsHtml = true
            )
            val learnMoreLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
                containsHtml = true
            )
        }

        // TODO cardreader Update layout resource
        object WCPayAccountUnderReviewState :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_hint)
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes
            val illustration: Int = R.drawable.img_products_error
        }

        // TODO cardreader Update layout resource
        object WCPayAccountRejectedState :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_hint)
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes
            val illustration: Int = R.drawable.img_products_error
        }

        // TODO cardreader Update layout resource
        object WCPayAccountOverdueRequirementsState :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_hint)
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes
            val illustration: Int = R.drawable.img_products_error
        }

        // TODO cardreader Update layout resource
        data class WCPayAccountPendingRequirementsState(val dueDate: String, val dismissButtonAction: () -> Unit) :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_pending_requirements_header)
            val hintLabel: UiString =
                UiString.UiStringRes(
                    R.string.card_reader_onboarding_account_pending_requirements_hint,
                    listOf(UiString.UiStringText(dueDate))
                )
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes
            val illustration: Int = R.drawable.img_products_error
            val dismissButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_pending_requirements_dismiss_button)
        }

        // TODO cardreader Update layout resource
        object WCPayInTestModeWithLiveAccountState :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_hint)
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes
            val illustration: Int = R.drawable.img_products_error
        }

        sealed class WCPayError(
            val headerLabel: UiString,
            val hintLabel: UiString,
            val learnMoreLabel: UiString,
            @DrawableRes val illustration: Int,
            val refreshButtonLabel: UiString
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
            abstract val refreshButtonAction: () -> Unit
            abstract val onLearnMoreActionClicked: (() -> Unit)

            data class WCPayNotInstalledState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                illustration = R.drawable.img_woo_payments,
                refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_refresh_button)
            )

            data class WCPayNotActivatedState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                illustration = R.drawable.img_woo_payments,
                refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_refresh_button)
            )

            data class WCPayNotSetupState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                illustration = R.drawable.img_woo_payments,
                refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button)
            )

            data class WCPayUnsupportedVersionState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                illustration = R.drawable.img_woo_payments,
                refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button)
            )
        }
    }
}
