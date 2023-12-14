package com.woocommerce.android.ui.payments.cardreader.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString

sealed class CardReaderOnboardingViewState(@LayoutRes val layoutRes: Int) {
    object LoadingState : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_loading) {
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
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_generic_error) {
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

    data class SelectPaymentPluginState(
        val onConfirmPaymentMethodClicked: ((PluginType) -> Unit),
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_select_payment_gateway) {
        val cardIllustration = R.drawable.ic_credit_card_give
        val headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_payment_provider)
        val choosePluginHintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_plugin_hint)

        val selectWcPayButtonLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_wcpayment_button)
        val icWcPayLogo = R.drawable.ic_wcpay
        val icCheckmarkWcPay = R.drawable.ic_menu_action_mode_check
        val selectStripeButtonLabel = UiString.UiStringRes(R.string.card_reader_onboarding_choose_stripe_button)
        val confirmPaymentMethodButtonLabel =
            UiString.UiStringRes(R.string.card_reader_onboarding_confirm_payment_method_button)
    }

    data class CashOnDeliveryDisabledState(
        val onSkipCashOnDeliveryClicked: (() -> Unit),
        val onCashOnDeliveryEnabledSuccessfully: (() -> Unit),
        val onEnableCashOnDeliveryClicked: (() -> Unit),
        val onLearnMoreActionClicked: (() -> Unit),
        val onContactSupportActionClicked: (() -> Unit),
        val shouldShowProgress: Boolean = false,
        val cashOnDeliveryEnabledSuccessfully: Boolean? = null
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_cod_disabled) {
        val cardIllustration = R.drawable.ic_woo_illustrated_icon
        val headerLabel = UiString.UiStringRes(
            R.string.card_reader_onboarding_cash_on_delivery_disabled_error_header
        )
        val cashOnDeliveryHintLabel = UiString.UiStringRes(
            R.string.card_reader_onboarding_cash_on_delivery_disabled_error_hint
        )
        val contactSupportLabel = UiString.UiStringRes(
            R.string.card_reader_onboarding_contact_us,
            containsHtml = true
        )
        val skipCashOnDeliveryButtonLabel = UiString.UiStringRes(
            R.string.skip
        )
        val enableCashOnDeliveryButtonLabel = UiString.UiStringRes(
            R.string.card_reader_onboarding_cash_on_delivery_disabled_button
        )
        val learnMoreLabel = UiString.UiStringRes(
            stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
            containsHtml = true
        )
    }

    class NoConnectionErrorState(
        val onRetryButtonActionClicked: (() -> Unit)
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_network_error) {
        val illustration = R.drawable.ic_woo_error_state
    }

    sealed class UnsupportedErrorState(
        val headerLabel: UiString,
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_unsupported) {
        abstract val onContactSupportActionClicked: (() -> Unit)
        abstract val onLearnMoreActionClicked: (() -> Unit)

        val contactSupportLabel = UiString.UiStringRes(
            stringRes = R.string.card_reader_onboarding_country_not_supported_contact_support,
            containsHtml = true
        )
        val learnMoreLabel = UiString.UiStringRes(
            stringRes = R.string.card_reader_onboarding_country_not_supported_learn_more,
            containsHtml = true
        )
        val illustration = R.drawable.img_hot_air_balloon
        val hintLabel = UiString.UiStringRes(
            stringRes = R.string.card_reader_onboarding_country_not_supported_hint
        )

        data class Country(
            val countryDisplayName: String,
            override val onContactSupportActionClicked: (() -> Unit),
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : UnsupportedErrorState(
            headerLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_country_not_supported_header,
                params = listOf(UiString.UiStringText(countryDisplayName))
            )
        )

        data class StripeInCountry(
            val countryDisplayName: String,
            override val onContactSupportActionClicked: (() -> Unit),
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : UnsupportedErrorState(
            headerLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_stripe_unsupported_in_country_header,
                params = listOf(UiString.UiStringText(countryDisplayName))
            )
        )

        data class WcPayInCountry(
            val countryDisplayName: String,
            override val onContactSupportActionClicked: (() -> Unit),
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : UnsupportedErrorState(
            headerLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_wcpay_unsupported_in_country_header,
                params = listOf(UiString.UiStringText(countryDisplayName))
            )
        )

        data class StripeAccountInUnsupportedCountry(
            val countryDisplayName: String,
            override val onContactSupportActionClicked: (() -> Unit),
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : UnsupportedErrorState(
            headerLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_onboarding_stripe_account_in_unsupported_country,
                params = listOf(UiString.UiStringText(countryDisplayName))
            )
        )
    }

    sealed class StripeAccountError(
        val headerLabel: UiString,
        val hintLabel: UiString,
        val actionButtonPrimary: ActionButton? = null,
        val actionButtonSecondary: ActionButton? = null,
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_stripe) {
        abstract val onContactSupportActionClicked: (() -> Unit)
        abstract val onLearnMoreActionClicked: (() -> Unit)

        @DrawableRes
        val illustration = R.drawable.img_products_error
        val learnMoreButton = ActionButton(
            label = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
            action = { onLearnMoreActionClicked() }
        )
        val contactSupportButton = ActionButton(
            label = UiString.UiStringRes(R.string.card_reader_onboarding_contact_support, containsHtml = true),
            action = { onContactSupportActionClicked() }
        )

        data class StripeAccountUnderReviewState(
            override val onContactSupportActionClicked: () -> Unit,
            override val onLearnMoreActionClicked: () -> Unit
        ) : StripeAccountError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_under_review_hint)
        )

        data class StripeAccountRejectedState(
            override val onContactSupportActionClicked: () -> Unit,
            override val onLearnMoreActionClicked: () -> Unit
        ) : StripeAccountError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_rejected_hint)
        )

        data class StripeAccountOverdueRequirementsState(
            override val onContactSupportActionClicked: () -> Unit,
            override val onLearnMoreActionClicked: () -> Unit,
            val onPrimaryActionClicked: (() -> Unit),
            val onSecondaryActionClicked: (() -> Unit),
        ) : StripeAccountError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_overdue_requirements_hint),
            actionButtonPrimary = ActionButton(
                label = UiString.UiStringRes(
                    R.string.card_reader_onboarding_account_overdue_requirements_take_care_button
                ),
                icon = R.drawable.ic_external,
                action = onPrimaryActionClicked
            ),
            actionButtonSecondary = ActionButton(
                label = UiString.UiStringRes(
                    R.string.card_reader_onboarding_account_overdue_requirements_refresh_button
                ),
                action = onSecondaryActionClicked
            )
        )

        data class PluginInTestModeWithLiveAccountState(
            override val onContactSupportActionClicked: () -> Unit,
            override val onLearnMoreActionClicked: () -> Unit
        ) : StripeAccountError(
            headerLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_header
            ),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_hint)
        )

        data class StripeAccountPendingRequirementsState(
            override val onContactSupportActionClicked: () -> Unit,
            override val onLearnMoreActionClicked: () -> Unit,
            val onPrimaryActionClicked: () -> Unit,
            val dueDate: String?
        ) : StripeAccountError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_account_pending_requirements_header),
            hintLabel = if (dueDate != null) UiString.UiStringRes(
                R.string.card_reader_onboarding_account_pending_requirements_hint,
                listOf(UiString.UiStringText(dueDate))
            ) else UiString.UiStringRes(
                R.string.card_reader_onboarding_account_pending_requirements_without_date_hint,
            ),
            actionButtonPrimary = ActionButton(
                label = UiString.UiStringRes(R.string.skip),
                action = onPrimaryActionClicked
            )
        )
    }

    sealed class WCPayError(
        val headerLabel: UiString,
        val hintLabel: UiString,
        val learnMoreButton: ActionButton,
        val actionButtonPrimary: ActionButton,
        val actionButtonSecondary: ActionButton? = null
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
        abstract val actionButtonActionPrimary: () -> Unit
        abstract val onLearnMoreActionClicked: (() -> Unit)

        @DrawableRes
        val illustration = R.drawable.img_woo_payments

        data class WCPayNotInstalledState(
            override val actionButtonActionPrimary: () -> Unit,
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : WCPayError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_hint),
            learnMoreButton = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                action = onLearnMoreActionClicked
            ),
            actionButtonPrimary = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_installed_install_button),
                action = actionButtonActionPrimary
            )
        )

        data class WCPayNotActivatedState(
            override val actionButtonActionPrimary: () -> Unit,
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : WCPayError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_hint),
            learnMoreButton = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                action = onLearnMoreActionClicked
            ),
            actionButtonPrimary = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_activated_activate_button),
                action = actionButtonActionPrimary
            )
        )

        data class WCPayNotSetupState(
            override val actionButtonActionPrimary: () -> Unit,
            val actionButtonActionSecondary: () -> Unit,
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : WCPayError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_hint),
            learnMoreButton = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                action = onLearnMoreActionClicked
            ),
            actionButtonPrimary = ActionButton(
                UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_go_to_wpadmin_button),
                icon = R.drawable.ic_external,
                action = actionButtonActionPrimary
            ),
            actionButtonSecondary = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button),
                action = actionButtonActionSecondary
            )
        )

        data class WCPayUnsupportedVersionState(
            override val actionButtonActionPrimary: () -> Unit,
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : WCPayError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_unsupported_version_hint),
            learnMoreButton = ActionButton(
                label = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
                action = onLearnMoreActionClicked
            ),
            actionButtonPrimary = ActionButton(
                label = UiString.UiStringRes(
                    R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button
                ),
                action = actionButtonActionPrimary
            )
        )
    }

    sealed class StripeExtensionError(
        val headerLabel: UiString,
        val hintLabel: UiString,
        val learnMoreLabel: UiString,
        val refreshButtonLabel: UiString
    ) : CardReaderOnboardingViewState(R.layout.fragment_card_reader_onboarding_wcpay) {
        abstract val refreshButtonAction: () -> Unit
        abstract val onLearnMoreActionClicked: (() -> Unit)

        @DrawableRes
        val illustration = R.drawable.img_stripe_extension

        data class StripeExtensionNotSetupState(
            override val refreshButtonAction: () -> Unit,
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : StripeExtensionError(
            headerLabel = UiString.UiStringRes(R.string.card_reader_onboarding_stripe_extension_not_setup_header),
            hintLabel = UiString.UiStringRes(R.string.card_reader_onboarding_stripe_extension_not_setup_hint),
            learnMoreLabel = UiString.UiStringRes(R.string.card_reader_onboarding_learn_more, containsHtml = true),
            refreshButtonLabel = UiString.UiStringRes(R.string.card_reader_onboarding_wcpay_not_setup_refresh_button)
        )

        data class StripeExtensionUnsupportedVersionState(
            override val refreshButtonAction: () -> Unit,
            override val onLearnMoreActionClicked: (() -> Unit)
        ) : StripeExtensionError(
            headerLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_stripe_extension_unsupported_version_header
            ),
            hintLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_stripe_extension_unsupported_version_hint
            ),
            learnMoreLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_learn_more, containsHtml = true
            ),
            refreshButtonLabel = UiString.UiStringRes(
                R.string.card_reader_onboarding_wcpay_unsupported_version_refresh_button
            )
        )
    }

    data class ActionButton(
        val label: UiString,
        @DrawableRes val icon: Int? = null,
        val action: () -> Unit
    )
}
