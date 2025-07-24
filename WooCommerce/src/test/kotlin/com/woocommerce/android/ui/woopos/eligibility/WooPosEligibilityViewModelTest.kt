package com.woocommerce.android.ui.woopos.eligibility

import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryCode
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryName
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosEligibilityViewModelTest {

    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()
    private val mockAnalyticsTracker: WooPosAnalyticsTracker = mock()
    private val mockResourceProvider: ResourceProvider = mock()
    private val mockStoreCountryProvider: WooPosGetStoreCountryName = mock()
    private val mockStoreCountryCodeProvider: WooPosGetStoreCountryCode = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    init {
        whenever(mockResourceProvider.getString(any())).thenReturn("Test suggestion text")
        whenever(mockResourceProvider.getString(any(), any())).thenReturn("Test suggestion text with params")
        runBlocking {
            whenever(mockStoreCountryProvider()).doReturn("United States")
            whenever(mockStoreCountryCodeProvider()).doReturn("us")
        }
    }

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
    fun `given POS is ineligible on retry, should update state to Ineligible with suggestion text`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(
            WooPosLaunchability.NotLaunchable(reason)
        )
        val sut = createSut()
        sut.initialize(reason)

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()

        // THEN
        val currentState = sut.retryState.value as WooPosEligibilityRetryState.Ineligible
        assertThat(currentState.suggestionText).isNotEmpty()
    }

    @Test
    fun `initialize should set state to Ineligible with suggestion text`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        val sut = createSut()

        // WHEN
        sut.initialize(reason)

        // THEN
        val currentState = sut.retryState.value as WooPosEligibilityRetryState.Ineligible
        assertThat(currentState.suggestionText).isNotEmpty()
    }

    @Test
    fun `given ineligible reason, when initialize is called, then IneligibleUIShown event is tracked`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(mockStoreCountryProvider()).thenReturn("United States")
        whenever(mockStoreCountryCodeProvider()).thenReturn("us")
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider
        )

        // WHEN
        sut.initialize(reason)

        // THEN
        verify(tracker).track(IneligibleUIShown(reason))
    }

    @Test
    fun `given ineligible state, when retryEligibilityCheckTapped is called, then IneligibleUIRetryTapped event is tracked`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.NotLaunchable(reason))
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider
        )

        sut.initialize(reason)
        reset(tracker)

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()

        // THEN
        verify(tracker).track(IneligibleUIRetryTapped(reason))
    }

    @Test
    fun `given retry results in different ineligible reason, then IneligibleUIShown event is tracked for new reason`() = runTest {
        // GIVEN
        val initialReason = WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
        val retryReason = WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.NotLaunchable(retryReason))
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider
        )

        sut.initialize(initialReason)
        reset(tracker) // Clear the initial IneligibleUIShown event

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()

        // THEN
        verify(tracker).track(IneligibleUIRetryTapped(initialReason))
        verify(tracker).track(IneligibleUIShown(retryReason))
    }

    private suspend fun createSut(): WooPosEligibilityViewModel {
        whenever(mockStoreCountryProvider()).thenReturn("United States")
        whenever(mockStoreCountryCodeProvider()).thenReturn("us")
        return WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            mockAnalyticsTracker,
            mockResourceProvider,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider
        )
    }
}
