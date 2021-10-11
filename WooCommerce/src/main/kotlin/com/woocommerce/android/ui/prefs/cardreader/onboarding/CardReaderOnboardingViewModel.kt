package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.formatToMMMMdd
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val UNIX_TO_JAVA_TIMESTAMP_OFFSET = 1000L

@HiltViewModel
class CardReaderOnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderChecker: CardReaderOnboardingChecker,
    private val trackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private val viewState = MutableLiveData<OnboardingViewState>()
    val viewStateData: LiveData<OnboardingViewState> = viewState

    init {
        refreshState()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun refreshState() {
        launch {
            viewState.value = OnboardingViewState.LoadingState
            val state = cardReaderChecker.getOnboardingState()
            trackState(state)
            when (state) {
                CardReaderOnboardingState.OnboardingCompleted ->
                    triggerEvent(OnboardingEvent.Continue)
                is CardReaderOnboardingState.StoreCountryNotSupported ->
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
                    viewState.value = OnboardingViewState.WCStripeError.WCPayInTestModeWithLiveAccountState(
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        onLearnMoreActionClicked = ::onLearnMoreClicked
                    )
                CardReaderOnboardingState.StripeAccountUnderReview ->
                    viewState.value = OnboardingViewState.WCStripeError.WCPayAccountUnderReviewState(
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        onLearnMoreActionClicked = ::onLearnMoreClicked
                    )
                is CardReaderOnboardingState.StripeAccountPendingRequirement ->
                    viewState.value = OnboardingViewState.WCStripeError
                        .WCPayAccountPendingRequirementsState(
                            onContactSupportActionClicked = ::onContactSupportClicked,
                            onLearnMoreActionClicked = ::onLearnMoreClicked,
                            onButtonActionClicked = ::onSkipPendingRequirementsClicked,
                            dueDate = formatDueDate(state)
                        )
                CardReaderOnboardingState.StripeAccountOverdueRequirement ->
                    viewState.value = OnboardingViewState.WCStripeError.WCPayAccountOverdueRequirementsState(
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        onLearnMoreActionClicked = ::onLearnMoreClicked
                    )
                CardReaderOnboardingState.StripeAccountRejected ->
                    viewState.value = OnboardingViewState.WCStripeError.WCPayAccountRejectedState(
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        onLearnMoreActionClicked = ::onLearnMoreClicked
                    )
                CardReaderOnboardingState.GenericError ->
                    viewState.value = OnboardingViewState.GenericErrorState(
                        onContactSupportActionClicked = ::onContactSupportClicked,
                        onLearnMoreActionClicked = ::onLearnMoreClicked
                    )
                CardReaderOnboardingState.NoConnectionError ->
                    viewState.value = OnboardingViewState.NoConnectionErrorState(
                        onRetryButtonActionClicked = ::refreshState
                    )
                is CardReaderOnboardingState.StripeAccountCountryNotSupported ->
                    viewState.value = OnboardingViewState.UnsupportedCountryState(
                        convertCountryCodeToCountry(state.countryCode),
                        ::onContactSupportClicked,
                        ::onLearnMoreClicked
                    )
            }.exhaustive
        }
    }

    private fun trackState(state: CardReaderOnboardingState) {
        getTrackingReason(state)?.let {
            trackerWrapper.track(AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to it))
        }
    }

    @Suppress("ComplexMethod")
    private fun getTrackingReason(state: CardReaderOnboardingState): String? =
        when (state) {
            CardReaderOnboardingState.OnboardingCompleted -> null
            is CardReaderOnboardingState.StoreCountryNotSupported -> "country_not_supported"
            CardReaderOnboardingState.StripeAccountOverdueRequirement -> "account_overdue_requirements"
            is CardReaderOnboardingState.StripeAccountPendingRequirement -> "account_pending_requirements"
            CardReaderOnboardingState.StripeAccountRejected -> "account_rejected"
            CardReaderOnboardingState.StripeAccountUnderReview -> "account_under_review"
            is CardReaderOnboardingState.StripeAccountCountryNotSupported -> "account_country_not_supported"
            CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount -> "wcpay_in_test_mode_with_live_account"
            CardReaderOnboardingState.WcpayNotActivated -> "wcpay_not_activated"
            CardReaderOnboardingState.WcpayNotInstalled -> "wcpay_not_installed"
            CardReaderOnboardingState.WcpaySetupNotCompleted -> "wcpay_not_setup"
            CardReaderOnboardingState.WcpayUnsupportedVersion -> "wcpay_unsupported_version"
            CardReaderOnboardingState.GenericError -> "generic_error"
            CardReaderOnboardingState.NoConnectionError -> "no_connection_error"
        }

    fun onCancelClicked() {
        WooLog.e(WooLog.T.CARD_READER, "Onboarding flow interrupted by the user.")
        exitFlow()
    }

    private fun onContactSupportClicked() {
        triggerEvent(OnboardingEvent.NavigateToSupport)
    }

    private fun onLearnMoreClicked() {
        trackerWrapper.track(AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED)
        triggerEvent(OnboardingEvent.ViewLearnMore)
    }

    private fun onSkipPendingRequirementsClicked() {
        triggerEvent(OnboardingEvent.Continue)
    }

    private fun exitFlow() {
        triggerEvent(Event.Exit)
    }

    private fun convertCountryCodeToCountry(countryCode: String?) =
        Locale("", countryCode.orEmpty()).displayName

    private fun formatDueDate(state: CardReaderOnboardingState.StripeAccountPendingRequirement) =
        state.dueDate?.let { Date(it * UNIX_TO_JAVA_TIMESTAMP_OFFSET).formatToMMMMdd() } ?: ""

    sealed class OnboardingEvent : Event() {
        object ViewLearnMore : OnboardingEvent() {
            const val url = AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        }

        object NavigateToSupport : Event()

        object Continue : Event()
    }

    sealed class OnboardingViewState(@LayoutRes val layoutRes: Int) {
        object LoadingState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_loading)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.please_wait)

            @DrawableRes
            val illustration: Int = R.drawable.img_hot_air_balloon
        }

        class GenericErrorState(
            val onContactSupportActionClicked: (() -> Unit),
            val onLearnMoreActionClicked: (() -> Unit)
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_generic_error) {
            val contactSupportLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_contact_support,
                containsHtml = true
            )
            val learnMoreLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
                containsHtml = true
            )
            val illustration = R.drawable.img_products_error
        }

        class NoConnectionErrorState(
            val onRetryButtonActionClicked: (() -> Unit)
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_network_error) {
            val illustration = R.drawable.ic_woo_error_state
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
            val illustration = R.drawable.img_hot_air_balloon
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

        sealed class WCStripeError(
            val headerLabel: UiString,
            val hintLabel: UiString,
            val buttonLabel: UiString? = null
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_stripe) {
            abstract val onContactSupportActionClicked: (() -> Unit)
            abstract val onLearnMoreActionClicked: (() -> Unit)
            open val onButtonActionClicked: (() -> Unit?)? = null

            @DrawableRes
            val illustration = R.drawable.img_products_error
            val contactSupportLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support, containsHtml = true)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)

            data class WCPayAccountUnderReviewState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : WCStripeError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_hint),
            )

            data class WCPayAccountRejectedState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : WCStripeError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_hint)
            )

            data class WCPayAccountOverdueRequirementsState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : WCStripeError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_hint)
            )

            data class WCPayInTestModeWithLiveAccountState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit
            ) : WCStripeError(
                headerLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_header),
                hintLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_hint)
            )

            data class WCPayAccountPendingRequirementsState(
                override val onContactSupportActionClicked: () -> Unit,
                override val onLearnMoreActionClicked: () -> Unit,
                override val onButtonActionClicked: () -> Unit,
                val dueDate: String
            ) : WCStripeError(
                headerLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_account_pending_requirements_header),
                hintLabel = UiString.UiStringRes(
                    R.string.card_reader_onboarding_account_pending_requirements_hint,
                    listOf(UiString.UiStringText(dueDate))
                ),
                buttonLabel = UiString.UiStringRes(R.string.skip)
            )
        }

        sealed class WCPayError(
            val headerLabel: UiString,
            val hintLabel: UiString,
            val learnMoreLabel: UiString,
            val refreshButtonLabel: UiString
        ) : OnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
            abstract val refreshButtonAction: () -> Unit
            abstract val onLearnMoreActionClicked: (() -> Unit)

            @DrawableRes
            val illustration = R.drawable.img_woo_payments

            data class WCPayNotInstalledState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_refresh_button)
            )

            data class WCPayNotActivatedState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_refresh_button)
            )

            data class WCPayNotSetupState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button)
            )

            data class WCPayUnsupportedVersionState(
                override val refreshButtonAction: () -> Unit,
                override val onLearnMoreActionClicked: (() -> Unit)
            ) : WCPayError(
                headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_header),
                hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_hint),
                learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                refreshButtonLabel = UiString
                    .UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button)
            )
        }
    }
}
