package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_AUTO_CONNECTION_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_READER_DISCOVERED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class CardReaderTrackerTest : BaseUnitTest() {
    companion object {
        private const val REQUIRED_UPDATE = "Required"
        private const val OPTIONAL_UPDATE = "Optional"
        private const val COUNTRY_CODE = "US"
        private const val CURRENCY = "USD"
        private const val PAYMENT_METHOD_TYPE = "card"
        private const val CARD_READER_MODEL = "CHIPPER_2X"
        private const val PLUGIN_VERSION = "4.0.0"
    }

    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock() {
        on(it.getCardReaderPreferredPlugin(anyInt(), anyLong(), anyLong())).thenReturn(WOOCOMMERCE_PAYMENTS)
    }
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val cardReaderTrackingInfoProvider: CardReaderTrackingInfoProvider = mock() {
        on { trackingInfo }.thenReturn(
            TrackingInfo(
                country = COUNTRY_CODE,
                currency = CURRENCY,
                paymentMethodType = PAYMENT_METHOD_TYPE,
                cardReaderModel = CARD_READER_MODEL,
            )
        )
    }

    private val cardReaderTracker = CardReaderTracker(
        trackerWrapper,
        appPrefsWrapper,
        selectedSite,
        cardReaderTrackingInfoProvider
    )

    @Test
    fun `when track learn more invoked, then CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingLearnMoreTapped()

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED),
                any()
            )
        }

    @Test
    fun `when onboarding state GenericError, then reason=generic_error tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.GenericError)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED), check { assertThat(it["reason"]).isEqualTo("generic_error") }
            )
        }

    @Test
    fun `when onboarding state StoreCountryNotSupported, then reason=country_not_supported tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StoreCountryNotSupported(""))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("country_not_supported") }
            )
        }

    @Test
    fun `when onboarding state PluginIsNotSupportedInTheCountry woo, then wcpay_is_not_supported_in_CA tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.PluginIsNotSupportedInTheCountry(
                    WOOCOMMERCE_PAYMENTS,
                    "CA"
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_is_not_supported_in_CA") }
            )
        }

    @Test
    fun `when onboarding state PluginIsNotSupportedInTheCountry str, then stripe_extension_is_not_supported_in_US`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.PluginIsNotSupportedInTheCountry(
                    STRIPE_EXTENSION_GATEWAY,
                    "US"
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("stripe_extension_is_not_supported_in_US") }
            )
        }

    @Test
    fun `when onboarding state StripeAccountCountryNotSupported, then reason=account_country_not_supported tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.StripeAccountCountryNotSupported(
                    mock(),
                    ""
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_country_not_supported") }
            )
        }

    @Test
    fun `when onboarding state WcpayNotInstalled, then reason=wcpay_not_installed tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.WcpayNotInstalled)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_not_installed") }
            )
        }

    @Test
    fun `when onboarding state WcpayNotActivated, then reason=wcpay_not_activated tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.WcpayNotActivated)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_not_activated") }
            )
        }

    @Test
    fun `when onboarding state PluginUnsupportedVersion WCPay, then reason=wcpay_unsupported_version tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.PluginUnsupportedVersion(WOOCOMMERCE_PAYMENTS)
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_unsupported_version") }
            )
        }

    @Test
    fun `when onboarding PluginUnsupportedVersion Stripe, then reason=stripe_extension_unsupported_version tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("stripe_extension_unsupported_version") }
            )
        }

    @Test
    fun `when onboarding state SetupNotCompleted WCPay, then reason=wcpay_not_setup tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.SetupNotCompleted(WOOCOMMERCE_PAYMENTS)
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_not_setup") }
            )
        }

    @Test
    fun `when onboarding state SetupNotCompleted Stripe, then reason=stripe_extension_not_setup tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.SetupNotCompleted(STRIPE_EXTENSION_GATEWAY)
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("stripe_extension_not_setup") }
            )
        }

    @Test
    fun `when onboarding StripeAccountPendingRequirement WCPay, then reason=account_pending_requirements tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.StripeAccountPendingRequirement(
                    null,
                    WOOCOMMERCE_PAYMENTS,
                    PLUGIN_VERSION,
                    COUNTRY_CODE
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_pending_requirements") }
            )
        }

    @Test
    fun `when onboarding StripeAccountPendingRequirement Stripe, then reason=account_pending_requirements tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.StripeAccountPendingRequirement(
                    null,
                    WOOCOMMERCE_PAYMENTS,
                    PLUGIN_VERSION,
                    COUNTRY_CODE
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_pending_requirements") }
            )
        }

    @Test
    fun `when onboarding state StripeAccountOverdueRequirement, then reason=account_overdue_requirements tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountOverdueRequirement(mock()))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_overdue_requirements") }
            )
        }

    @Test
    fun `when onboarding state StripeAccountUnderReview, then reason=account_under_review tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountUnderReview(mock()))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_under_review") }
            )
        }

    @Test
    fun `when onboarding state StripeAccountRejected, then reason=account_rejected tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountRejected(mock()))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_rejected") }
            )
        }

    @Test
    fun `when wcpay in test mode with live account, then wcpay_in_test_mode_with_live_account`() =
        testBlocking {
            cardReaderTracker
                .trackOnboardingState(
                    CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount(WOOCOMMERCE_PAYMENTS)
                )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_in_test_mode_with_live_account") }
            )
        }

    @Test
    fun `when stripe in test mode with live account, then stripe_extension_in_test_mode_with_live_account`() =
        testBlocking {
            cardReaderTracker
                .trackOnboardingState(
                    CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount(STRIPE_EXTENSION_GATEWAY)
                )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("stripe_extension_in_test_mode_with_live_account") }
            )
        }

    @Test
    fun `when onboarding state OnboardingCompleted WCPay, then event NOT tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.OnboardingCompleted(
                    WOOCOMMERCE_PAYMENTS,
                    PLUGIN_VERSION,
                    COUNTRY_CODE
                )
            )

            verify(trackerWrapper, never()).track(any(), any())
        }

    @Test
    fun `when onboarding state OnboardingCompleted Stripe, then event NOT tracked`() =
        testBlocking {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.OnboardingCompleted(STRIPE_EXTENSION_GATEWAY, PLUGIN_VERSION, COUNTRY_CODE)
            )

            verify(trackerWrapper, never()).track(any(), any())
        }

    @Test
    fun `when track software update started, then CARD_READER_SOFTWARE_UPDATE_STARTED tracked`() =
        testBlocking {
            cardReaderTracker.trackSoftwareUpdateStarted(true)

            verify(trackerWrapper).track(
                eq(CARD_READER_SOFTWARE_UPDATE_STARTED),
                any()
            )
        }

    @Test
    fun `given update required, when track software update started, then event with REQUIRED_UPDATE tracked`() =
        testBlocking {
            cardReaderTracker.trackSoftwareUpdateStarted(requiredUpdate = true)

            verify(trackerWrapper).track(
                any(),
                properties = check {
                    assertThat(it[AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE]).isEqualTo(REQUIRED_UPDATE)
                }
            )
        }

    @Test
    fun `given update not required, when track software update started, then event with OPTIONAL_UPDATE tracked`() =
        testBlocking {
            cardReaderTracker.trackSoftwareUpdateStarted(requiredUpdate = false)

            verify(trackerWrapper).track(
                any(),
                check {
                    assertThat(it[AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE]).isEqualTo(OPTIONAL_UPDATE)
                }
            )
        }

    @Test
    fun `when track software update failed, then CARD_READER_SOFTWARE_UPDATE_FAILED tracked`() =
        testBlocking {
            val dummyMessage = "abcd"
            cardReaderTracker.trackSoftwareUpdateFailed(Failed(mock(), dummyMessage), requiredUpdate = false)

            verify(trackerWrapper).track(
                eq(CARD_READER_SOFTWARE_UPDATE_FAILED),
                any(),
                any(),
                eq(null),
                eq(dummyMessage)
            )
        }

    @Test
    fun `when software update canceled, then CARD_READER_SOFTWARE_UPDATE_FAILED tracked`() =
        testBlocking {
            cardReaderTracker.trackSoftwareUpdateCancelled(false)

            verify(trackerWrapper).track(
                stat = eq(CARD_READER_SOFTWARE_UPDATE_FAILED),
                properties = any(),
                errorContext = any(),
                errorType = anyOrNull(),
                errorDescription = any()
            )
        }

    @Test
    fun `given required update, when software update canceled, then required update property tracked`() =
        testBlocking {
            cardReaderTracker.trackSoftwareUpdateCancelled(requiredUpdate = true)

            verify(trackerWrapper).track(
                any(),
                properties = check {
                    assertThat(it[AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE]).isEqualTo(REQUIRED_UPDATE)
                },
                errorContext = any(),
                errorType = anyOrNull(),
                errorDescription = any()
            )
        }

    @Test
    fun `given optional update, when software update canceled, then optional update property tracked`() =
        testBlocking {
            cardReaderTracker.trackSoftwareUpdateCancelled(requiredUpdate = false)

            verify(trackerWrapper).track(
                any(),
                properties = check {
                    assertThat(it[AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE]).isEqualTo(OPTIONAL_UPDATE)
                },
                errorContext = any(),
                errorType = anyOrNull(),
                errorDescription = any()
            )
        }

    @Test
    fun `when auto connection started, then CARD_READER_AUTO_CONNECTION_STARTED tracked`() =
        testBlocking {
            cardReaderTracker.trackAutoConnectionStarted()

            verify(trackerWrapper).track(eq(CARD_READER_AUTO_CONNECTION_STARTED), any())
        }

    @Test
    fun `when scanning fails, then CARD_READER_DISCOVERY_FAILED tracked`() =
        testBlocking {
            val dummyErrorMgs = "dummy error"
            cardReaderTracker.trackReaderDiscoveryFailed(dummyErrorMgs)

            verify(trackerWrapper).track(
                eq(CARD_READER_DISCOVERY_FAILED), any(), any(), anyOrNull(), eq(dummyErrorMgs)
            )
        }

    @Test
    fun `when reader found, then CARD_READER_DISCOVERY_READER_DISCOVERED tracked`() =
        testBlocking {
            val dummyCount = 99
            cardReaderTracker.trackReadersDiscovered(dummyCount)

            verify(trackerWrapper)
                .track(
                    eq(CARD_READER_DISCOVERY_READER_DISCOVERED),
                    any()
                )
        }

    @Test
    fun `when reader found, then reader_count tracked`() =
        testBlocking {
            val dummyCount = 99
            cardReaderTracker.trackReadersDiscovered(dummyCount)

            verify(trackerWrapper)
                .track(
                    any(),
                    check { assertThat(it["reader_count"]).isEqualTo(dummyCount) }
                )
        }

    @Test
    fun `when location fetching fails, then CARD_READER_LOCATION_FAILURE tracked`() =
        testBlocking {
            val dummyErrorMgs = "dummy error"
            cardReaderTracker.trackFetchingLocationFailed(dummyErrorMgs)

            verify(trackerWrapper).track(
                eq(CARD_READER_LOCATION_FAILURE),
                any(),
                eq("CardReaderTracker"),
                eq(null),
                eq(dummyErrorMgs)
            )
        }

    @Test
    fun `given US country, when tracking, then US property tracked`() {
        val countryCode = "US"
        whenever(cardReaderTrackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(country = countryCode))

        cardReaderTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

        val captor = argumentCaptor<Map<String, Any>>()
        verify(trackerWrapper).track(
            eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS),
            captor.capture(),
        )
        assertThat(captor.firstValue["country"]).isEqualTo(countryCode)
    }

    @Test
    fun `given CA country, when tracking, then CA property tracked`() {
        val countryCode = "CA"
        whenever(cardReaderTrackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(country = countryCode))

        cardReaderTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

        val captor = argumentCaptor<Map<String, Any>>()
        verify(trackerWrapper).track(
            eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS),
            captor.capture(),
        )
        assertThat(captor.firstValue["country"]).isEqualTo(countryCode)
    }

    @Test
    fun `given GBR currency, when tracking, then GBR currency property tracked`() {
        val currency = "GBR"
        whenever(cardReaderTrackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(currency = currency))

        cardReaderTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

        val captor = argumentCaptor<Map<String, Any>>()
        verify(trackerWrapper).track(
            eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS),
            captor.capture(),
        )
        assertThat(captor.firstValue["currency"]).isEqualTo(currency)
    }

    @Test
    fun `given card paymentMethodType, when tracking, then card paymentMethodType property tracked`() {
        val paymentMethodType = "card"
        whenever(cardReaderTrackingInfoProvider.trackingInfo)
            .thenReturn(TrackingInfo(paymentMethodType = paymentMethodType))

        cardReaderTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

        val captor = argumentCaptor<Map<String, Any>>()
        verify(trackerWrapper).track(
            eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS),
            captor.capture(),
        )
        assertThat(captor.firstValue["payment_method_type"]).isEqualTo(paymentMethodType)
    }

    @Test
    fun `given m2 cardReaderModel, when tracking, then m2 cardReaderModel property tracked`() {
        val cardReaderModel = "M2"
        whenever(cardReaderTrackingInfoProvider.trackingInfo)
            .thenReturn(TrackingInfo(cardReaderModel = cardReaderModel))

        cardReaderTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

        val captor = argumentCaptor<Map<String, Any>>()
        verify(trackerWrapper).track(
            eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS),
            captor.capture(),
        )
        assertThat(captor.firstValue["card_reader_model"]).isEqualTo(cardReaderModel)
    }

    @Test
    fun `when location fetching succeeds, then CARD_READER_LOCATION_SUCCESS tracked`() =
        testBlocking {
            cardReaderTracker.trackFetchingLocationSucceeded()

            verify(trackerWrapper).track(eq(CARD_READER_LOCATION_SUCCESS), any())
        }

    @Test
    fun `when payment fails, then CARD_PRESENT_COLLECT_PAYMENT_FAILED tracked`() =
        testBlocking {
            val dummyMessage = "error msg"
            cardReaderTracker.trackPaymentFailed(dummyMessage)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED), any(), any(), anyOrNull(), eq(dummyMessage)
            )
        }

    @Test
    fun `when payment completed, then CARD_PRESENT_COLLECT_PAYMENT_SUCCESS tracked`() =
        testBlocking {
            cardReaderTracker.trackPaymentSucceeded()

            verify(trackerWrapper).track(eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS), any())
        }

    @Test
    fun `when user clicks on print receipt button, then RECEIPT_PRINT_TAPPED tracked`() =
        testBlocking {
            cardReaderTracker.trackPrintReceiptTapped()

            verify(trackerWrapper).track(eq(RECEIPT_PRINT_TAPPED), any())
        }

    @Test
    fun `when OS accepts the print request, then RECEIPT_PRINT_SUCCESS tracked`() {
        cardReaderTracker.trackPrintReceiptSucceeded()

        verify(trackerWrapper).track(eq(RECEIPT_PRINT_SUCCESS), any())
    }

    @Test
    fun `when OS refuses the print request, then RECEIPT_PRINT_FAILED tracked`() {
        cardReaderTracker.trackPrintReceiptFailed()

        verify(trackerWrapper).track(eq(RECEIPT_PRINT_FAILED), any())
    }

    @Test
    fun `when manually cancels the print request, then RECEIPT_PRINT_CANCELED tracked`() {
        cardReaderTracker.trackPrintReceiptCancelled()

        verify(trackerWrapper).track(eq(RECEIPT_PRINT_CANCELED), any())
    }

    @Test
    fun `when user clicks on send receipt button, then RECEIPT_EMAIL_TAPPED tracked`() =
        testBlocking {
            cardReaderTracker.trackEmailReceiptTapped()

            verify(trackerWrapper).track(eq(RECEIPT_EMAIL_TAPPED), any())
        }

    @Test
    fun `when user cancels payment, then CARD_PRESENT_COLLECT_PAYMENT_CANCELLED tracked`() =
        testBlocking {
            val currentPaymentState = "dummy state"
            cardReaderTracker.trackPaymentCancelled(currentPaymentState)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                any(),
                any(),
                anyOrNull(),
                eq("User manually cancelled the payment during state $currentPaymentState")
            )
        }

    @Test
    fun `when user taps collect payment button, then CARD_PRESENT_COLLECT_PAYMENT_TAPPED tracked`() =
        testBlocking {
            cardReaderTracker.trackCollectPaymentTapped()

            verify(trackerWrapper).track(eq(CARD_PRESENT_COLLECT_PAYMENT_TAPPED), any())
        }

    @Test
    fun `given wcpay is preferred plugin, when event tracked, then wcpay plugin slug tracked`() =
        testBlocking {
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(WOOCOMMERCE_PAYMENTS)

            cardReaderTracker.track(mock())

            verify(trackerWrapper).track(
                any(),
                check { assertThat(it["plugin_slug"]).isEqualTo("woocommerce-payments") }
            )
        }

    @Test
    fun `given stripe is preferred plugin, when event tracked, then stripe plugin slug tracked`() =
        testBlocking {
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(STRIPE_EXTENSION_GATEWAY)

            cardReaderTracker.track(mock())

            verify(trackerWrapper).track(
                any(),
                check { assertThat(it["plugin_slug"]).isEqualTo("woocommerce-stripe") }
            )
        }

    @Test
    fun `given preferred plugin not stored, when event tracked, then unknown slug tracked`() =
        testBlocking {
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(null)

            cardReaderTracker.track(mock())

            verify(trackerWrapper).track(
                any(),
                check { assertThat(it["plugin_slug"]).isEqualTo("unknown") }
            )
        }
}
