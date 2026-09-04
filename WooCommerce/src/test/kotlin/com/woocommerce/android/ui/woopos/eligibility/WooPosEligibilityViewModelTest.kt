package com.woocommerce.android.ui.woopos.eligibility

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryCode
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
    private val mockGetStoreCountryCode: WooPosGetStoreCountryCode = mock()
    private val mockGetStoreCountryDisplayName: WooPosGetStoreCountryDisplayName = mock()

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
            mockGetStoreCountryCode,
            mockGetStoreCountryDisplayName,
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
            mockGetStoreCountryCode,
            mockGetStoreCountryDisplayName,
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
        val retryReason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion
        val tracker: WooPosAnalyticsTracker = mock()
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.NotLaunchable(retryReason))
        val sut = WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            tracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockGetStoreCountryCode,
            mockGetStoreCountryDisplayName,
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
    fun `given unsupported currency and a known country, when initialized, then the copy names both`() = runTest {
        // GIVEN
        whenever(mockGetStoreCountryCode()).thenReturn("CA")
        whenever(mockGetStoreCountryDisplayName("CA")).thenReturn("Canada")
        val sut = createSut()

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)

        // THEN
        verify(mockResourceProvider).getString(
            R.string.woopos_eligibility_reason_unsupported_currency_country_pair,
            "Canada",
            "CAD"
        )
    }

    @Test
    fun `given unsupported currency and an unknown country, when initialized, then the generic copy is used`() = runTest {
        // GIVEN
        whenever(mockGetStoreCountryCode()).thenReturn("CA")
        whenever(mockGetStoreCountryDisplayName("CA")).thenReturn(null)
        val sut = createSut()

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)

        // THEN
        verify(mockResourceProvider).getString(R.string.woopos_eligibility_reason_unsupported_currency_generic)
    }

    @Test
    fun `given the WooCommerce plugin is missing, when initialized, then the plugin copy is used`() = runTest {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound)

        // THEN
        verify(mockResourceProvider).getString(R.string.woopos_eligibility_reason_woocommerce_plugin_not_found)
    }

    @Test
    fun `given the POS feature switch is disabled, when initialized, then the switch copy is used`() = runTest {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled)

        // THEN
        verify(mockResourceProvider).getString(R.string.woopos_eligibility_reason_feature_switch_disabled)
    }

    private fun createSut(): WooPosEligibilityViewModel {
        return WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            mockAnalyticsTracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockGetStoreCountryCode,
            mockGetStoreCountryDisplayName,
        )
    }
}
