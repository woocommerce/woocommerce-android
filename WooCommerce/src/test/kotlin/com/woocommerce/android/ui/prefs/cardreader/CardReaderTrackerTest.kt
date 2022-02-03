package com.woocommerce.android.ui.prefs.cardreader

import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class CardReaderTrackerTest : BaseUnitTest() {
    private val trackerWrapper: AnalyticsTrackerWrapper = mock {

    }
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
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS)
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
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.SetupNotCompleted(PluginType.WOOCOMMERCE_PAYMENTS))

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
    fun `when onboarding state PluginInTestModeWithLiveStripeAccount, then reason=wcpay_in_test_mode_with_live_account tracked`() =
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
            cardReaderTracker.trackOnboardingState(CardReaderOnboardingState.OnboardingCompleted(PluginType.WOOCOMMERCE_PAYMENTS))

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
}
