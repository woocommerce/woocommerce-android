package com.woocommerce.android.ui.woopos.cardreader.connection

import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToLocalizedFullMonthDay
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WooPosOnboardingErrorMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun map(
        state: CardReaderOnboardingState,
        onDismiss: () -> Unit,
        onRetry: () -> Unit,
        onSkipPendingRequirements: () -> Unit,
    ): Result {
        return when (state) {
            is CardReaderOnboardingState.StoreCountryNotSupported ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_country_not_supported_header,
                            convertCountryCodeToCountry(state.countryCode)
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_country_not_supported_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.PluginIsNotSupportedInTheCountry -> {
                val headerRes = when (state.preferredPlugin) {
                    WOOCOMMERCE_PAYMENTS ->
                        R.string.card_reader_onboarding_wcpay_unsupported_in_country_header
                    STRIPE_EXTENSION_GATEWAY ->
                        R.string.card_reader_onboarding_stripe_unsupported_in_country_header
                }
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            headerRes,
                            convertCountryCodeToCountry(state.countryCode)
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_country_not_supported_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )
            }

            is CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_header
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_wcpay_in_test_mode_with_live_account_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.StripeAccountUnderReview ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_account_under_review_header
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_account_under_review_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.StripeAccountPendingRequirement -> {
                val message = if (state.dueDate != null) {
                    val formattedDate = Date(state.dueDate * MILLIS_PER_SECOND).formatToLocalizedFullMonthDay()
                    resourceProvider.getString(
                        R.string.card_reader_onboarding_account_pending_requirements_hint,
                        formattedDate
                    )
                } else {
                    resourceProvider.getString(
                        R.string.card_reader_onboarding_account_pending_requirements_without_date_hint
                    )
                }
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_account_pending_requirements_header
                        ),
                        message = message,
                        primaryButton = WooPosCardReaderConnectionState.OnboardingError.PrimaryButton(
                            label = resourceProvider.getString(R.string.skip),
                            onClick = onSkipPendingRequirements,
                        ),
                        onDismissClicked = onDismiss,
                    )
                )
            }

            is CardReaderOnboardingState.StripeAccountRejected ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_account_rejected_header
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_account_rejected_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.StripeAccountCountryNotSupported ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_stripe_account_in_unsupported_country,
                            convertCountryCodeToCountry(state.countryCode)
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_country_not_supported_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.GenericError ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_generic_error_header
                        ),
                        message = resourceProvider.getString(
                            R.string.card_reader_onboarding_generic_error_hint
                        ),
                        primaryButton = null,
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.NoConnectionError ->
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(
                            R.string.card_reader_onboarding_generic_error_header
                        ),
                        message = resourceProvider.getString(R.string.error_generic_network),
                        primaryButton = WooPosCardReaderConnectionState.OnboardingError.PrimaryButton(
                            label = resourceProvider.getString(R.string.retry),
                            onClick = onRetry,
                        ),
                        onDismissClicked = onDismiss,
                    )
                )

            is CardReaderOnboardingState.PluginUnsupportedVersion -> {
                val (headerRes, hintRes) = when (state.preferredPlugin) {
                    WOOCOMMERCE_PAYMENTS -> Pair(
                        R.string.card_reader_onboarding_wcpay_unsupported_version_header,
                        R.string.card_reader_onboarding_wcpay_unsupported_version_hint,
                    )
                    STRIPE_EXTENSION_GATEWAY -> Pair(
                        R.string.card_reader_onboarding_stripe_extension_unsupported_version_header,
                        R.string.card_reader_onboarding_stripe_extension_unsupported_version_hint,
                    )
                }
                Result.DialogError(
                    WooPosCardReaderConnectionState.OnboardingError(
                        title = resourceProvider.getString(headerRes),
                        message = resourceProvider.getString(hintRes),
                        primaryButton = WooPosCardReaderConnectionState.OnboardingError.PrimaryButton(
                            label = resourceProvider.getString(R.string.retry),
                            onClick = onRetry,
                        ),
                        onDismissClicked = onDismiss,
                    )
                )
            }

            is CardReaderOnboardingState.OnboardingCompleted,
            is CardReaderOnboardingState.WcpayNotInstalled,
            is CardReaderOnboardingState.WcpayNotActivated,
            is CardReaderOnboardingState.SetupNotCompleted,
            is CardReaderOnboardingState.StripeAccountOverdueRequirement,
            is CardReaderOnboardingState.ChoosePaymentGatewayProvider,
            is CardReaderOnboardingState.CashOnDeliveryDisabled ->
                Result.FullScreenRequired
        }
    }

    sealed interface Result {
        data class DialogError(
            val error: WooPosCardReaderConnectionState.OnboardingError
        ) : Result

        data object FullScreenRequired : Result
    }

    private fun convertCountryCodeToCountry(countryCode: String?) =
        Locale.Builder().setRegion(countryCode.orEmpty()).build().displayName

    companion object {
        private const val MILLIS_PER_SECOND = 1000L
    }
}
