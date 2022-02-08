package com.woocommerce.android.ui.prefs.cardreader

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.*
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class CardReaderTrackerTest : BaseUnitTest() {
    companion object {
        private const val REQUIRED_UPDATE = "Required"
        private const val OPTIONAL_UPDATE = "Optional"
    }

    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private val cardReaderTracker = CardReaderTracker(trackerWrapper)

    @Test
    fun `when track learn more invoked, then CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingLearnMoreTapped()

            verify(trackerWrapper).track(CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED)
        }

    @Test
    fun `when onboarding state GenericError, then reason=generic_error tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.GenericError)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "generic_error")
            )
        }

    @Test
    fun `when onboarding state StoreCountryNotSupported, then reason=country_not_supported tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StoreCountryNotSupported(""))

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "country_not_supported")
            )
        }

    @Test
    fun `when onboarding state StripeAccountCountryNotSupported, then reason=account_country_not_supported tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountCountryNotSupported(""))

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_country_not_supported")
            )
        }

    @Test
    fun `when onboarding state WcpayNotInstalled, then reason=wcpay_not_installed tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.WcpayNotInstalled)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "wcpay_not_installed")
            )
        }

    @Test
    fun `when onboarding state WcpayNotActivated, then reason=wcpay_not_activated tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.WcpayNotActivated)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "wcpay_not_activated")
            )
        }

    @Test
    fun `when onboarding state PluginUnsupportedVersion WCPay, then reason=wcpay_unsupported_version tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.PluginUnsupportedVersion(WOOCOMMERCE_PAYMENTS)
            )

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "wcpay_unsupported_version")
            )
        }

    @Test
    fun `when onboarding PluginUnsupportedVersion Stripe, then reason=stripe_extension_unsupported_version tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.STRIPE_EXTENSION_GATEWAY)
            )

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "stripe_extension_unsupported_version")
            )
        }

    @Test
    fun `when onboarding state SetupNotCompleted WCPay, then reason=wcpay_not_setup tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.SetupNotCompleted(WOOCOMMERCE_PAYMENTS)
            )

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "wcpay_not_setup")
            )
        }

    @Test
    fun `when onboarding state SetupNotCompleted Stripe, then reason=stripe_extension_not_setup tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.SetupNotCompleted(PluginType.STRIPE_EXTENSION_GATEWAY)
            )

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "stripe_extension_not_setup")
            )
        }

    @Test
    fun `when onboarding StripeAccountPendingRequirement WCPay, then reason=account_pending_requirements tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.StripeAccountPendingRequirement(null, WOOCOMMERCE_PAYMENTS)
            )

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_pending_requirements")
            )
        }

    @Test
    fun `when onboarding StripeAccountPendingRequirement Stripe, then reason=account_pending_requirements tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.StripeAccountPendingRequirement(null, WOOCOMMERCE_PAYMENTS)
            )

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_pending_requirements")
            )
        }

    @Test
    fun `when onboarding state StripeAccountOverdueRequirement, then reason=account_overdue_requirements tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountOverdueRequirement)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "account_overdue_requirements")
            )
        }

    @Test
    fun `when onboarding state StripeAccountUnderReview, then reason=account_under_review tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountUnderReview)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "account_under_review")
            )
        }

    @Test
    fun `when onboarding state StripeAccountRejected, then reason=account_rejected tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.StripeAccountRejected)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to "account_rejected")
            )
        }

    @Test
    fun `when onboarding PluginInTestModeWithLiveStripeAccount, then reason=wcpay_in_test_mode_with_live_account`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)

            verify(trackerWrapper).track(
                CARD_PRESENT_ONBOARDING_NOT_COMPLETED,
                mapOf("reason" to "wcpay_in_test_mode_with_live_account")
            )
        }

    @Test
    fun `when onboarding state OnboardingCompleted WCPay, then event NOT tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.OnboardingCompleted(WOOCOMMERCE_PAYMENTS))

            verify(trackerWrapper, never()).track(any(), any())
        }

    @Test
    fun `when onboarding state OnboardingCompleted Stripe, then event NOT tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackOnboardingState(
                CardReaderOnboardingState.OnboardingCompleted(PluginType.STRIPE_EXTENSION_GATEWAY)
            )

            verify(trackerWrapper, never()).track(any(), any())
        }

    @Test
    fun `when track software update started, then CARD_READER_SOFTWARE_UPDATE_STARTED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackSoftwareUpdateStarted(true)

            verify(trackerWrapper).track(
                CARD_READER_SOFTWARE_UPDATE_STARTED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE
                )
            )
        }

    @Test
    fun `given update required, when track software update started, then event with REQUIRED_UPDATE tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackSoftwareUpdateStarted(requiredUpdate = true)

            verify(trackerWrapper).track(
                CARD_READER_SOFTWARE_UPDATE_STARTED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE
                )
            )
        }

    @Test
    fun `given update not required, when track software update started, then event with OPTIONAL_UPDATE tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackSoftwareUpdateStarted(requiredUpdate = false)

            verify(trackerWrapper).track(
                CARD_READER_SOFTWARE_UPDATE_STARTED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE
                )
            )
        }

    @Test
    fun `when track software update failed, then CARD_READER_SOFTWARE_UPDATE_FAILED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val dummyMessage = "abcd"
            cardReaderTracker.trackSoftwareUpdateFailed(Failed(mock(), dummyMessage), requiredUpdate = false)

            verify(trackerWrapper).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderTracker",
                    AnalyticsTracker.KEY_ERROR_DESC to dummyMessage
                )
            )
        }

    @Test
    fun `when software update canceled, then CARD_READER_SOFTWARE_UPDATE_FAILED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackSoftwareUpdateCancelled(false)

            verify(trackerWrapper).track(
                eq(CARD_READER_SOFTWARE_UPDATE_FAILED),
                any()
            )
        }

    @Test
    fun `given required update, when software update canceled, then required update property tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackSoftwareUpdateCancelled(requiredUpdate = true)

            verify(trackerWrapper).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderTracker",
                    AnalyticsTracker.KEY_ERROR_DESC to "User manually cancelled the flow"
                )
            )
        }

    @Test
    fun `given optional update, when software update canceled, then optional update property tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackSoftwareUpdateCancelled(requiredUpdate = false)

            verify(trackerWrapper).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderTracker",
                    AnalyticsTracker.KEY_ERROR_DESC to "User manually cancelled the flow"
                )
            )
        }

    @Test
    fun `when auto connection started, then CARD_READER_AUTO_CONNECTION_STARTED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackAutoConnectionStarted()

            verify(trackerWrapper).track(CARD_READER_AUTO_CONNECTION_STARTED)
        }

    @Test
    fun `when scanning fails, then CARD_READER_DISCOVERY_FAILED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val dummyErrorMgs = "dummy error"
            cardReaderTracker.trackReaderDiscoveryFailed(dummyErrorMgs)

            verify(trackerWrapper).track(
                eq(CARD_READER_DISCOVERY_FAILED), anyOrNull(), anyOrNull(), eq(dummyErrorMgs)
            )
        }

    @Test
    fun `when reader found, then CARD_READER_DISCOVERY_READER_DISCOVERED tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val dummyCount = 99
            cardReaderTracker.trackReadersDiscovered(dummyCount)

            verify(trackerWrapper)
                .track(CARD_READER_DISCOVERY_READER_DISCOVERED, mapOf("reader_count" to dummyCount))
        }

    @Test
    fun `when location fetching fails, then CARD_READER_LOCATION_FAILURE tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val dummyErrorMgs = "dummy error"
            cardReaderTracker.trackFetchingLocationFailed(dummyErrorMgs)

            verify(trackerWrapper).track(
                CARD_READER_LOCATION_FAILURE,
                "CardReaderTracker",
                null,
                dummyErrorMgs
            )
        }

    @Test
    fun `when location fetching succeeds, then CARD_READER_LOCATION_SUCCESS tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            cardReaderTracker.trackFetchingLocationSucceeded()

            verify(trackerWrapper).track(CARD_READER_LOCATION_SUCCESS)
        }
}
