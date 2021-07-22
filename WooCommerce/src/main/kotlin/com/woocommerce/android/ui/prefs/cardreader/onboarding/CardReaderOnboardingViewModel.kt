package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderOnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private lateinit var cardReaderChecker: CardReaderOnboardingChecker

    private val viewState = MutableLiveData<OnboardingViewState>()
    val viewStateData: LiveData<OnboardingViewState> = viewState

    init {
        startFlow()
    }

    private fun startFlow() {
        // TODO
    }

    private fun onCancelClicked() {
        WooLog.e(WooLog.T.CARD_READER, "Onboarding flow interrupted by the user.")
        exitFlow()
    }

    private fun exitFlow() {
        triggerEvent(Event.Exit)
    }

    sealed class OnboardingViewState(@LayoutRes layoutRes: Int) {
        object LoadingState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.payment_onboarding_loading)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.please_wait)
            @DrawableRes val illustration: Int = R.drawable.img_payment_onboarding_loading
        }

        // TODO cardreader Update layout resource
        object GenericErrorState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            // TODO cardreader implement generic error state
        }

        // TODO cardreader Update layout resource
        object NoConnectionErrorState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            // TODO cardreader implement no connection error state
        }

        // TODO cardreader Update layout resource
        object UnsupportedCountryState : OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            // TODO cardreader implement unsupported country state - !already in progress!
        }

        // TODO cardreader Update layout resource
        data class WCPayNotInstalledState(val refreshButtonAction: () -> Unit) :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_hint)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes val illustration: Int = R.drawable.img_woocommerce_payments
            val refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_refresh_button)
        }

        // TODO cardreader Update layout resource
        data class WCPayNotActivatedState(val refreshButtonAction: () -> Unit) :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_hint)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes val illustration: Int = R.drawable.img_woocommerce_payments
            val refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_refresh_button)
        }

        // TODO cardreader Update layout resource
        data class WCPayNotSetupState(val refreshButtonAction: () -> Unit) :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_hint)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes val illustration: Int = R.drawable.img_woocommerce_payments
            val refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button)
        }

        // TODO cardreader Update layout resource
        data class WCPayUnsupportedVersionState(val refreshButtonAction: () -> Unit) :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_hint)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes val illustration: Int = R.drawable.img_woocommerce_payments
            val refreshButtonLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button)
        }

        // TODO cardreader Update layout resource
        object WCPayAccountUnderReviewState :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_hint)
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes val illustration: Int = R.drawable.img_products_error
        }

        // TODO cardreader Update layout resource
        object WCPayAccountRestrictedState :
            OnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
            val headerLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_restricted_header)
            val hintLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_account_restricted_hint)
            val contactSupportLabel: UiString =
                UiString.UiStringRes(R.string.card_reader_onboarding_contact_support)
            val learnMoreLabel =
                UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true)
            @DrawableRes val illustration: Int = R.drawable.img_products_error
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
            @DrawableRes val illustration: Int = R.drawable.img_products_error
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
            @DrawableRes val illustration: Int = R.drawable.img_products_error
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
            @DrawableRes val illustration: Int = R.drawable.img_products_error
        }
    }
}
