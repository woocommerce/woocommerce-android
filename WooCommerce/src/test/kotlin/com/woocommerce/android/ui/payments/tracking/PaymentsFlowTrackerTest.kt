package com.woocommerce.android.ui.payments.tracking

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_CTA_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_CTA_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_STEP_SKIPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_AUTO_CONNECTION_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_READER_DISCOVERED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.DISABLE_CASH_ON_DELIVERY_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ENABLE_CASH_ON_DELIVERY_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ENABLE_CASH_ON_DELIVERY_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CASH_ON_DELIVERY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.ChoosePaymentGatewayProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.OnboardingCtaReasonTapped
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.CashOnDeliverySource.ONBOARDING
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.CashOnDeliverySource.PAYMENTS_HUB
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class PaymentsFlowTrackerTest : BaseUnitTest() {
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

    private val paymentsFlowTracker = PaymentsFlowTracker(
        trackerWrapper,
        appPrefsWrapper,
        selectedSite,
        cardReaderTrackingInfoProvider
    )

    @Test
    fun `when track learn more invoked, then CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingLearnMoreTapped()

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED),
                any()
            )
        }

    @Test
    fun `given battery level is null, when event is tracked, then it shouldn't contain battery level property`() =
        testBlocking {
            assertNull(cardReaderTrackingInfoProvider.trackingInfo.cardReaderBatteryLevel)

            val props = mutableMapOf<String, Any>()
            paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS, props)

            assertFalse(props.containsKey("card_reader_battery_level"))
        }

    @Test
    fun `given battery level is not null, when event is tracked, then it should contain battery level property`() =
        testBlocking {
            val cardReaderTrackingInfo: TrackingInfo = mock {
                on { cardReaderBatteryLevel } doReturn 0.5F // 50 %
            }
            whenever(cardReaderTrackingInfoProvider.trackingInfo).thenReturn(cardReaderTrackingInfo)
            val props = mutableMapOf<String, Any>()

            paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS, props)

            assertTrue(props.containsKey("card_reader_battery_level"))
            val batteryLevelProperty = props["card_reader_battery_level"]
            assertEquals("50 %", batteryLevelProperty)
        }

    @Test
    fun `when track payment gateway invoked with wcpay, then track event with proper gateway`() =
        testBlocking {
            paymentsFlowTracker.trackPaymentGatewaySelected(WOOCOMMERCE_PAYMENTS)

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.CARD_PRESENT_PAYMENT_GATEWAY_SELECTED),
                check { assertThat(it[AnalyticsTracker.KEY_PAYMENT_GATEWAY]).isEqualTo("woocommerce-payments") }
            )
        }

    @Test
    fun `when track payment gateway invoked with stripe, then track event with proper gateway`() =
        testBlocking {
            paymentsFlowTracker.trackPaymentGatewaySelected(STRIPE_EXTENSION_GATEWAY)

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.CARD_PRESENT_PAYMENT_GATEWAY_SELECTED),
                check { assertThat(it[AnalyticsTracker.KEY_PAYMENT_GATEWAY]).isEqualTo("woocommerce-stripe-gateway") }
            )
        }

    @Test
    fun `given cod disabled state, when skip is tapped, then CARD_PRESENT_ONBOARDING_STEP_SKIPPED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingSkippedState(
                CardReaderOnboardingState.CashOnDeliveryDisabled(
                    countryCode = "US",
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = "4.0.0"
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_STEP_SKIPPED),
                check {
                    assertThat(it["reason"]).isEqualTo("cash_on_delivery_disabled")
                    assertThat(it["remind_later"]).isEqualTo(false)
                }
            )
        }

    @Test
    fun `given CASH_ON_DELIVERY_TAPPED, when trackOnbardingCtaTapped, then cash_on_delivery_disabled tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.CASH_ON_DELIVERY_TAPPED)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_CTA_TAPPED),
                check { assertThat(it["reason"]).isEqualTo("cash_on_delivery_disabled") }
            )
        }

    @Test
    fun `given PLUGIN_INSTALL_TAPPED state, when trackOnbardingCtaTapped, then plugin_install_tapped tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.PLUGIN_INSTALL_TAPPED)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_CTA_TAPPED),
                check { assertThat(it["reason"]).isEqualTo("plugin_install_tapped") }
            )
        }

    @Test
    fun `given CASH_ON_DELIVERY_TAPPED, when trackOnboardingCtaFailed, then cash_on_delivery_disabled tracked with description`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingCtaFailed(
                OnboardingCtaReasonTapped.CASH_ON_DELIVERY_TAPPED,
                "errorDescription"
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_CTA_FAILED),
                check {
                    assertThat(it["reason"]).isEqualTo("cash_on_delivery_disabled")
                    assertThat(it["error_description"]).isEqualTo("errorDescription")
                }
            )
        }

    @Test
    fun `given PLUGIN_INSTALL_TAPPED, when trackOnboardingCtaFailed, then plugin_install_tapped tracked with description`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingCtaFailed(
                OnboardingCtaReasonTapped.PLUGIN_INSTALL_TAPPED,
                "errorDescription"
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_CTA_FAILED),
                check {
                    assertThat(it["reason"]).isEqualTo("plugin_install_tapped")
                    assertThat(it["error_description"]).isEqualTo("errorDescription")
                }
            )
        }

    @Test
    fun `given enable cod is tapped, when success, then ENABLE_CASH_ON_DELIVERY_SUCCESS tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryEnabledSuccess(
                ONBOARDING
            )

            verify(trackerWrapper).track(eq(ENABLE_CASH_ON_DELIVERY_SUCCESS), any())
        }

    @Test
    fun `given enable cod is tapped from onboarding, when success, then proper source is tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryEnabledSuccess(
                ONBOARDING
            )
            val captor = argumentCaptor<Map<String, String>>()

            verify(trackerWrapper).track(
                eq(ENABLE_CASH_ON_DELIVERY_SUCCESS),
                captor.capture()
            )
            assertThat(captor.firstValue[KEY_CASH_ON_DELIVERY_SOURCE]).isEqualTo(ONBOARDING.toString())
        }

    @Test
    fun `given enable cod is tapped from payments hub, when success, then then proper source is tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryEnabledSuccess(
                PAYMENTS_HUB
            )
            val captor = argumentCaptor<Map<String, String>>()

            verify(trackerWrapper).track(
                eq(ENABLE_CASH_ON_DELIVERY_SUCCESS),
                captor.capture()
            )
            assertThat(captor.firstValue[KEY_CASH_ON_DELIVERY_SOURCE]).isEqualTo(PAYMENTS_HUB.toString())
        }

    @Test
    fun `given enable cod is tapped, when failure, then ENABLE_CASH_ON_DELIVERY_FAILED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryEnabledFailure(
                ONBOARDING,
                "COD failed. Please try again later."
            )

            verify(trackerWrapper).track(
                stat = eq(ENABLE_CASH_ON_DELIVERY_FAILED),
                properties = any(),
                errorType = any(),
                errorContext = eq(null),
                errorDescription = eq("COD failed. Please try again later.")
            )
        }

    @Test
    fun `given track cod toggled invoked, then PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryToggled(isEnabled = true)

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED),
                any()
            )
        }

    @Test
    fun `given track cod toggled invoked with true, then PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryToggled(isEnabled = true)
            val captor = argumentCaptor<Map<String, Boolean>>()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED),
                captor.capture()
            )
            assertThat(captor.firstValue[AnalyticsTracker.KEY_IS_ENABLED]).isEqualTo(true)
        }

    @Test
    fun `given track cod toggled invoked with false, then PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryToggled(isEnabled = false)
            val captor = argumentCaptor<Map<String, Boolean>>()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED),
                captor.capture()
            )
            assertThat(captor.firstValue[AnalyticsTracker.KEY_IS_ENABLED]).isEqualTo(false)
        }

    @Test
    fun `given disable cod method is invoked, when success, then DISABLE_CASH_ON_DELIVERY_SUCCESS tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryDisabledSuccess(
                ONBOARDING
            )

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.DISABLE_CASH_ON_DELIVERY_SUCCESS),
                any()
            )
        }

    @Test
    fun `given disable cod method is invoked, when success & source is payments_hub, then event tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryDisabledSuccess(
                PAYMENTS_HUB
            )
            val captor = argumentCaptor<Map<String, String>>()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.DISABLE_CASH_ON_DELIVERY_SUCCESS),
                captor.capture()
            )
            assertThat(captor.firstValue[KEY_CASH_ON_DELIVERY_SOURCE]).isEqualTo(
                PAYMENTS_HUB.toString()
            )
        }

    @Test
    fun `given disable cod method is invoked, when failure, then DISABLE_CASH_ON_DELIVERY_FAILED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryDisabledFailure(
                ONBOARDING,
                "Disabling COD failed. Please try again later."
            )

            verify(trackerWrapper).track(
                stat = eq(DISABLE_CASH_ON_DELIVERY_FAILED),
                properties = any(),
                errorType = any(),
                errorContext = eq(null),
                errorDescription = eq("Disabling COD failed. Please try again later.")
            )
        }

    @Test
    fun `given disable cod method is invoked, when failure & source is payments_hub, then event tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryDisabledFailure(
                PAYMENTS_HUB,
                "Disabling COD failed. Please try again later."
            )
            val captor = argumentCaptor<Map<String, String>>()

            verify(trackerWrapper).track(
                eq(DISABLE_CASH_ON_DELIVERY_FAILED),
                captor.capture(),
                errorType = any(),
                errorContext = eq(null),
                errorDescription = eq("Disabling COD failed. Please try again later.")
            )
            assertThat(captor.firstValue[KEY_CASH_ON_DELIVERY_SOURCE]).isEqualTo(
                PAYMENTS_HUB.toString()
            )
        }

    @Test
    fun `given learn more cod method is invoked, then proper event is tracked`() =
        testBlocking {
            paymentsFlowTracker.trackCashOnDeliveryLearnMoreTapped()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED_LEARN_MORE_TAPPED),
                any()
            )
        }

    @Test
    fun `when onboarding state cod disabled, then reason=cash_on_delivery tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(
                CardReaderOnboardingState.CashOnDeliveryDisabled(
                    countryCode = "US",
                    preferredPlugin = WOOCOMMERCE_PAYMENTS,
                    version = "4.0.0"
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("cash_on_delivery_disabled") }
            )
        }

    @Test
    fun `when onboarding state GenericError, then reason=generic_error tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.GenericError)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED), check { assertThat(it["reason"]).isEqualTo("generic_error") }
            )
        }

    @Test
    fun `when onboarding state StoreCountryNotSupported, then reason=country_not_supported tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.StoreCountryNotSupported(""))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("country_not_supported") }
            )
        }

    @Test
    fun `when onboarding state PluginIsNotSupportedInTheCountry woo, then wcpay_is_not_supported_in_CA tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.WcpayNotInstalled)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_not_installed") }
            )
        }

    @Test
    fun `when onboarding state WcpayNotActivated, then reason=wcpay_not_activated tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.WcpayNotActivated)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("wcpay_not_activated") }
            )
        }

    @Test
    fun `when onboarding state PluginUnsupportedVersion WCPay, then reason=wcpay_unsupported_version tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(
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
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountOverdueRequirement(mock()))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_overdue_requirements") }
            )
        }

    @Test
    fun `when onboarding state StripeAccountUnderReview, then reason=account_under_review tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountUnderReview(mock()))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_under_review") }
            )
        }

    @Test
    fun `when onboarding state StripeAccountRejected, then reason=account_rejected tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountRejected(mock()))

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("account_rejected") }
            )
        }

    @Test
    fun `when state ChoosePaymentGatewayProvider, then reason=multiple_payment_providers_conflict tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(
                ChoosePaymentGatewayProvider
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("multiple_payment_providers_conflict") }
            )
        }

    @Test
    fun `when wcpay in test mode with live account, then wcpay_in_test_mode_with_live_account`() =
        testBlocking {
            paymentsFlowTracker
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
            paymentsFlowTracker
                .trackOnboardingState(
                    CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount(STRIPE_EXTENSION_GATEWAY)
                )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_NOT_COMPLETED),
                check { assertThat(it["reason"]).isEqualTo("stripe_extension_in_test_mode_with_live_account") }
            )
        }

    @Test
    fun `given OnboardingCompleted with wcpay, when onboarding trackOnboardingState, then event tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(
                CardReaderOnboardingState.OnboardingCompleted(
                    WOOCOMMERCE_PAYMENTS,
                    PLUGIN_VERSION,
                    COUNTRY_CODE
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_COMPLETED),
                any()
            )
        }

    @Test
    fun `given OnboardingCompleted with stripe, when onboarding trackOnboardingState, then event tracked`() =
        testBlocking {
            paymentsFlowTracker.trackOnboardingState(
                CardReaderOnboardingState.OnboardingCompleted(
                    STRIPE_EXTENSION_GATEWAY,
                    PLUGIN_VERSION,
                    COUNTRY_CODE
                )
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_ONBOARDING_COMPLETED),
                any()
            )
        }

    @Test
    fun `when track software update started, then CARD_READER_SOFTWARE_UPDATE_STARTED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackSoftwareUpdateStarted(true)

            verify(trackerWrapper).track(
                eq(CARD_READER_SOFTWARE_UPDATE_STARTED),
                any()
            )
        }

    @Test
    fun `given update required, when track software update started, then event with REQUIRED_UPDATE tracked`() =
        testBlocking {
            paymentsFlowTracker.trackSoftwareUpdateStarted(requiredUpdate = true)

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
            paymentsFlowTracker.trackSoftwareUpdateStarted(requiredUpdate = false)

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
            paymentsFlowTracker.trackSoftwareUpdateFailed(Failed(mock(), dummyMessage), requiredUpdate = false)

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
            paymentsFlowTracker.trackSoftwareUpdateCancelled(false)

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
            paymentsFlowTracker.trackSoftwareUpdateCancelled(requiredUpdate = true)

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
            paymentsFlowTracker.trackSoftwareUpdateCancelled(requiredUpdate = false)

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
            paymentsFlowTracker.trackAutoConnectionStarted()

            verify(trackerWrapper).track(eq(CARD_READER_AUTO_CONNECTION_STARTED), any())
        }

    @Test
    fun `when scanning fails, then CARD_READER_DISCOVERY_FAILED tracked`() =
        testBlocking {
            val dummyErrorMgs = "dummy error"
            paymentsFlowTracker.trackReaderDiscoveryFailed(dummyErrorMgs)

            verify(trackerWrapper).track(
                eq(CARD_READER_DISCOVERY_FAILED), any(), any(), anyOrNull(), eq(dummyErrorMgs)
            )
        }

    @Test
    fun `when reader found, then CARD_READER_DISCOVERY_READER_DISCOVERED tracked`() =
        testBlocking {
            val dummyCount = 99
            paymentsFlowTracker.trackReadersDiscovered(dummyCount)

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
            paymentsFlowTracker.trackReadersDiscovered(dummyCount)

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
            paymentsFlowTracker.trackFetchingLocationFailed(dummyErrorMgs)

            verify(trackerWrapper).track(
                eq(CARD_READER_LOCATION_FAILURE),
                any(),
                eq("PaymentsFlowTracker"),
                eq(null),
                eq(dummyErrorMgs)
            )
        }

    @Test
    fun `given US country, when tracking, then US property tracked`() {
        val countryCode = "US"
        whenever(cardReaderTrackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(country = countryCode))

        paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

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

        paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

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

        paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

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

        paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

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

        paymentsFlowTracker.track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)

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
            paymentsFlowTracker.trackFetchingLocationSucceeded()

            verify(trackerWrapper).track(eq(CARD_READER_LOCATION_SUCCESS), any())
        }

    @Test
    fun `when payment fails, then CARD_PRESENT_COLLECT_PAYMENT_FAILED tracked`() =
        testBlocking {
            val dummyMessage = "error msg"
            paymentsFlowTracker.trackPaymentFailed(dummyMessage)

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED), any(), any(), anyOrNull(), eq(dummyMessage)
            )
        }

    @Test
    fun `given CardReadTimeOut error type, when payment fails, then CARD_PRESENT_COLLECT_PAYMENT_FAILED tracked with readable error type`() =
        testBlocking {
            val dummyMessage = "error msg"
            paymentsFlowTracker.trackPaymentFailed(
                dummyMessage,
                CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED),
                any(),
                any(),
                errorType = eq("CardPaymentStatus\$CardPaymentStatusErrorType\$CardReadTimeOut"),
                errorDescription = eq(dummyMessage)
            )
        }

    @Test
    fun `given CardNotSupported error type, when payment fails, then CARD_PRESENT_COLLECT_PAYMENT_FAILED tracked with readable error type`() =
        testBlocking {
            val dummyMessage = "error msg"
            paymentsFlowTracker.trackPaymentFailed(
                dummyMessage,
                CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CardNotSupported
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED),
                any(),
                any(),
                errorType = eq(
                    "CardPaymentStatus" +
                        "\$CardPaymentStatusErrorType" +
                        "\$DeclinedByBackendError" +
                        "\$CardDeclined" +
                        "\$CardNotSupported"
                ),
                errorDescription = eq(dummyMessage)
            )
        }

    @Test
    fun `given Server error type, when payment fails, then CARD_PRESENT_COLLECT_PAYMENT_FAILED tracked with readable error type`() =
        testBlocking {
            val dummyMessage = "error msg"
            val dummyServerError = "server error"
            paymentsFlowTracker.trackPaymentFailed(
                dummyMessage,
                CardPaymentStatus.CardPaymentStatusErrorType.Server(dummyServerError)
            )

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED),
                any(),
                any(),
                errorType = eq(
                    "Server(errorMessage=$dummyServerError)"
                ),
                errorDescription = eq(dummyMessage)
            )
        }

    @Test
    fun `when payment completed, then CARD_PRESENT_COLLECT_PAYMENT_SUCCESS tracked`() =
        testBlocking {
            paymentsFlowTracker.trackPaymentSucceeded()

            verify(trackerWrapper).track(eq(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS), any())
        }

    @Test
    fun `when user clicks on print receipt button, then RECEIPT_PRINT_TAPPED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackPrintReceiptTapped()

            verify(trackerWrapper).track(eq(RECEIPT_PRINT_TAPPED), any())
        }

    @Test
    fun `when OS accepts the print request, then RECEIPT_PRINT_SUCCESS tracked`() {
        paymentsFlowTracker.trackPrintReceiptSucceeded()

        verify(trackerWrapper).track(eq(RECEIPT_PRINT_SUCCESS), any())
    }

    @Test
    fun `when OS refuses the print request, then RECEIPT_PRINT_FAILED tracked`() {
        paymentsFlowTracker.trackPrintReceiptFailed()

        verify(trackerWrapper).track(eq(RECEIPT_PRINT_FAILED), any())
    }

    @Test
    fun `when manually cancels the print request, then RECEIPT_PRINT_CANCELED tracked`() {
        paymentsFlowTracker.trackPrintReceiptCancelled()

        verify(trackerWrapper).track(eq(RECEIPT_PRINT_CANCELED), any())
    }

    @Test
    fun `when user clicks on send receipt button, then RECEIPT_EMAIL_TAPPED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackEmailReceiptTapped()

            verify(trackerWrapper).track(eq(RECEIPT_EMAIL_TAPPED), any())
        }

    @Test
    fun `when user cancels payment, then CARD_PRESENT_COLLECT_PAYMENT_CANCELLED tracked`() =
        testBlocking {
            val currentPaymentState = "dummy state"
            paymentsFlowTracker.trackPaymentCancelled(currentPaymentState)

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
            paymentsFlowTracker.trackCollectPaymentTapped(any())

            verify(trackerWrapper).track(eq(PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED), any())
        }

    @Test
    fun `given wcpay is preferred plugin, when event tracked, then wcpay plugin slug tracked`() =
        testBlocking {
            whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
                .thenReturn(WOOCOMMERCE_PAYMENTS)

            paymentsFlowTracker.track(mock())

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

            paymentsFlowTracker.track(mock())

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

            paymentsFlowTracker.track(mock())

            verify(trackerWrapper).track(
                any(),
                check { assertThat(it["plugin_slug"]).isEqualTo("unknown") }
            )
        }

    @Test
    fun `when select bt reader tapped, then bluetooth reader selection tracked`() =
        testBlocking {
            paymentsFlowTracker.trackSelectReaderTypeBluetoothTapped()

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED),
                any()
            )
        }

    @Test
    fun `when select built in reader tapped, then tpp reader selection tracked`() =
        testBlocking {
            paymentsFlowTracker.trackSelectReaderTypeBuiltInTapped()

            verify(trackerWrapper).track(
                eq(CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED),
                any()
            )
        }

    @Test
    fun `when payment failed and contact support button tapped, then CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackPaymentFailedContactSupportTapped()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED),
                any()
            )
        }

    @Test
    fun `when trackPaymentFailedContactSupportTapped, then CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED tracked`() {
        paymentsFlowTracker.trackPaymentFailedContactSupportTapped()

        verify(trackerWrapper).track(
            eq(AnalyticsEvent.CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED),
            any()
        )
    }

    @Test
    fun `when track optional card reader update shown invoked, then CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN tracked`() =
        testBlocking {
            paymentsFlowTracker.trackSoftwareUpdateAlertShown()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN),
                any()
            )
        }

    @Test
    fun `when track optional card reader update install clicked invoked, then CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED tracked`() =
        testBlocking {
            paymentsFlowTracker.trackSoftwareUpdateAlertInstallClicked()

            verify(trackerWrapper).track(
                eq(AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED),
                any()
            )
        }

    @Test
    fun `when trackPaymentsFlowFailed invoked, then PAYMENTS_FLOW_FAILED tracked`() =
        testBlocking {
            // WHEN
            paymentsFlowTracker.trackPaymentsFlowFailed(
                source = "source",
                flow = "flow",
            )

            // THEN
            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_FLOW_FAILED),
                check {
                    assertThat(it["source"]).isEqualTo("source")
                    assertThat(it["flow"]).isEqualTo("flow")
                },
            )
        }

    @Test
    fun `when trackPaymentsFlowCanceled invoked, then PAYMENTS_FLOW_CANCELED tracked`() =
        testBlocking {
            // WHEN
            paymentsFlowTracker.trackPaymentsFlowCanceled(
                flow = "flow",
            )

            // THEN
            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_FLOW_CANCELED),
                check {
                    assertThat(it["flow"]).isEqualTo("flow")
                },
            )
        }

    @Test
    fun `given cardReaderType and timeElapsed, when trackPaymentsFlowCollect invoked, then PAYMENTS_FLOW_COLLECT tracked with type and time`() =
        testBlocking {
            // WHEN
            paymentsFlowTracker.trackPaymentsFlowCollect(
                flow = "flow",
                paymentMethod = "paymentMethod",
                orderId = 1L,
                cardReaderType = "cardReaderType",
                timeElapsed = 2L,
            )

            // THEN
            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_FLOW_COLLECT),
                check {
                    assertThat(it["flow"]).isEqualTo("flow")
                    assertThat(it["payment_method"]).isEqualTo("paymentMethod")
                    assertThat(it["order_id"]).isEqualTo(1L)
                    assertThat(it["card_reader_type"]).isEqualTo("cardReaderType")
                    assertThat(it["milliseconds_since_order_add_new"]).isEqualTo("2")
                },
            )
        }

    @Test
    fun `when trackPaymentsFlowCollect invoked, then PAYMENTS_FLOW_COLLECT tracked without time and card reader type`() =
        testBlocking {
            // WHEN
            paymentsFlowTracker.trackPaymentsFlowCollect(
                flow = "flow",
                paymentMethod = "paymentMethod",
                orderId = 1L,
                cardReaderType = null,
                timeElapsed = null,
            )

            // THEN
            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_FLOW_COLLECT),
                check {
                    assertThat(it["flow"]).isEqualTo("flow")
                    assertThat(it["payment_method"]).isEqualTo("paymentMethod")
                    assertThat(it["order_id"]).isEqualTo(1L)
                    assertThat(it["card_reader_type"]).isNull()
                    assertThat(it["milliseconds_since_order_add_new"]).isNull()
                },
            )
        }

    @Test
    fun `when trackPaymentsFlowCompleted invoked, then PAYMENTS_FLOW_COMPLETED tracked`() =
        testBlocking {
            // WHEN
            paymentsFlowTracker.trackPaymentsFlowCompleted(
                flow = "flow",
                paymentMethod = "paymentMethod",
                orderId = 1L,
                amount = "1$",
                amountNormalized = 2L,
            )

            // THEN
            verify(trackerWrapper).track(
                eq(AnalyticsEvent.PAYMENTS_FLOW_COMPLETED),
                check {
                    assertThat(it["flow"]).isEqualTo("flow")
                    assertThat(it["payment_method"]).isEqualTo("paymentMethod")
                    assertThat(it["order_id"]).isEqualTo(1L)
                    assertThat(it["amount"]).isEqualTo("1$")
                    assertThat(it["amount_normalized"]).isEqualTo(2L)
                },
            )
        }
}
