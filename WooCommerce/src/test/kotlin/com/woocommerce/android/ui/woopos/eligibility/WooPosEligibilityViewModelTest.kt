package com.woocommerce.android.ui.woopos.eligibility

import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosEligibilityViewModelTest {

    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()
    private val mockAnalyticsTracker: WooPosAnalyticsTracker = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Test
    fun `given POS is eligible on retry, should update state to Eligible`() = runTest {
        // GIVEN
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.Launchable)
        val sut = createSut()

        // WHEN
        sut.retryEligibilityCheckTapped()

        // THEN
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()
        assertThat(sut.retryState.value).isEqualTo(WooPosEligibilityRetryState.Eligible)
    }

    @Test
    fun `given POS is ineligible on retry, should update state to Ineligible with reason`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(
            WooPosLaunchability.NotLaunchable(reason)
        )
        val sut = createSut()

        // WHEN
        sut.retryEligibilityCheckTapped()

        // THEN
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()
        assertThat(sut.retryState.value).isEqualTo(WooPosEligibilityRetryState.Ineligible(reason))
    }

    @Test
    fun `initialize should set state to Ineligible with provided reason`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        val sut = createSut()

        // WHEN
        sut.initialize(reason)

        // THEN
        assertThat(sut.retryState.value).isEqualTo(WooPosEligibilityRetryState.Ineligible(reason))
    }

    @Test
    fun `given ineligible reason, when initialize is called, then IneligibleUIShown event is tracked`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        val tracker: WooPosAnalyticsTracker = mock()
        val sut = WooPosEligibilityViewModel(canBeLaunchedInTab, tracker)

        // WHEN
        sut.initialize(reason)

        // THEN
        verify(tracker).track(IneligibleUIShown(reason))
    }

    @Test
    fun `given ineligible state, when retryEligibilityCheckTapped is called, then IneligibleUIRetryTapped event is tracked`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.NotLaunchable(reason))
        val sut = WooPosEligibilityViewModel(canBeLaunchedInTab, tracker)

        sut.initialize(reason)

        reset(tracker)

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()

        // THEN
        verify(tracker).track(IneligibleUIRetryTapped(reason))
    }

    private fun createSut() = WooPosEligibilityViewModel(canBeLaunchedInTab, mockAnalyticsTracker)
}
