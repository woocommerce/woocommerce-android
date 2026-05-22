package com.woocommerce.android.ui.woopos.eligibility

import android.net.Uri
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosEligibilityViewModelTest {

    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()
    private val mockAnalyticsTracker: WooPosAnalyticsTracker = mock()
    private val mockResourceProvider: ResourceProvider = mock()
    private val mockSelectedSite: SelectedSite = mock()
    private val mockWooCommerceStore: WooCommerceStore = mock()
    private val mockCiabSiteGateKeeper: CIABSiteGateKeeper = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    init {
        whenever(mockResourceProvider.getString(any())).thenReturn("Test suggestion text")
        whenever(mockResourceProvider.getString(any(), any())).thenReturn("Test suggestion text with params")
        whenever(mockResourceProvider.getString(any(), any(), any()))
            .thenReturn("Test suggestion text with country and currency")
    }

    @Test
    fun `given POS is eligible on retry, when retry tapped, then navigation event is emitted`() = runTest {
        // GIVEN
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.Launchable)
        val sut = createSut()
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable)
        val navigated = mutableListOf<Unit>()
        val job = launch { sut.navigateToPos.collect { navigated.add(it) } }

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()

        // THEN
        assertThat(navigated).hasSize(1)
        job.cancel()
    }

    @Test
    fun `given POS is ineligible on retry, should update state to Ineligible with suggestion text`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
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
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
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
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        val tracker: WooPosAnalyticsTracker = mock()
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
        )

        // WHEN
        sut.initialize(reason)

        // THEN
        verify(tracker).track(IneligibleUIShown(reason))
    }

    @Test
    fun `given ineligible state, when retryEligibilityCheckTapped is called, then IneligibleUIRetryTapped event is tracked`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.NotLaunchable(reason))
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
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
        val initialReason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        val retryReason = WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.NotLaunchable(retryReason))
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
        )

        sut.initialize(initialReason)
        reset(tracker)

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()

        // THEN
        verify(tracker).track(IneligibleUIRetryTapped(initialReason))
        verify(tracker).track(IneligibleUIShown(retryReason))
    }

    @Test
    fun `given CIAB plan upgrade reason, when initialized, then state is CiabPlanUpgradeRequired`() = runTest {
        mockUriParse().use {
            // GIVEN
            val reason = WooPosLaunchability.NonLaunchabilityReason.CiabPlanUpgradeRequired
            whenever(mockCiabSiteGateKeeper.buildPlanUpgradeUrl()).thenReturn("https://example.com")
            val sut = createSut()

            // WHEN
            sut.initialize(reason)

            // THEN
            assertThat(sut.retryState.value)
                .isInstanceOf(WooPosEligibilityRetryState.CiabPlanUpgradeRequired::class.java)
        }
    }

    @Test
    fun `given learn more tapped, when onResumed and becomes eligible, then navigation is emitted`() = runTest {
        mockUriParse().use {
            // GIVEN
            val reason = WooPosLaunchability.NonLaunchabilityReason.CiabPlanUpgradeRequired
            whenever(mockCiabSiteGateKeeper.buildPlanUpgradeUrl()).thenReturn("https://example.com")
            whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.Launchable)
            val sut = createSut()
            sut.initialize(reason)
            val navigated = mutableListOf<Unit>()
            val job = launch { sut.navigateToPos.collect { navigated.add(it) } }

            // WHEN
            sut.learnMoreTapped()
            sut.onResumed()
            advanceUntilIdle()

            // THEN
            assertThat(navigated).hasSize(1)
            job.cancel()
        }
    }

    private fun mockUriParse(): MockedStatic<Uri> {
        return mockStatic(Uri::class.java).apply {
            `when`<Uri> { Uri.parse(any()) }.thenReturn(mock())
        }
    }

    private fun createSut(): WooPosEligibilityViewModel {
        return WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            mockAnalyticsTracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
        )
    }
}
