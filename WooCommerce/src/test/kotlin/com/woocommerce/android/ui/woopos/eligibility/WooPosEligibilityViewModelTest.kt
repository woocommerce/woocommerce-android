package com.woocommerce.android.ui.woopos.eligibility

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchabilityRefreshPolicy.ForceRefresh
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryCode
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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
        whenever(canBeLaunchedInTab(ForceRefresh)).thenReturn(WooPosLaunchability.Launchable)
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
        whenever(canBeLaunchedInTab(ForceRefresh)).thenReturn(
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
        whenever(canBeLaunchedInTab(ForceRefresh)).thenReturn(WooPosLaunchability.NotLaunchable(reason))
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
        whenever(canBeLaunchedInTab(ForceRefresh)).thenReturn(WooPosLaunchability.NotLaunchable(retryReason))
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
    fun `given an unsupported country, when initialized, then the country copy names it`() = runTest {
        // GIVEN
        whenever(mockGetStoreCountryCode()).thenReturn("de")
        whenever(mockGetStoreCountryDisplayName("de")).thenReturn("Germany")
        val sut = createSut()

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCountry)

        // THEN
        verify(mockResourceProvider).getString(R.string.woopos_eligibility_reason_unsupported_country, "Germany")
    }

    @Test
    fun `given an unsupported and unknown country, when initialized, then the generic country copy is used`() =
        runTest {
            // GIVEN
            whenever(mockGetStoreCountryCode()).thenReturn(null)
            val sut = createSut()

            // WHEN
            sut.initialize(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCountry)

            // THEN
            verify(mockResourceProvider)
                .getString(R.string.woopos_eligibility_reason_unsupported_country_generic)
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

    @Test
    fun `given initialized screen, when initialized again, then shown is tracked once`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        val sut = createSut()
        sut.initialize(reason)

        // WHEN
        sut.initialize(reason)

        // THEN
        verify(mockAnalyticsTracker).track(IneligibleUIShown(reason))
    }

    @Test
    fun `given failed retry, when initialized again, then retry state and tracking are preserved`() = runTest {
        // GIVEN
        val initialReason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        val retryReason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion
        whenever(canBeLaunchedInTab(ForceRefresh)).thenReturn(WooPosLaunchability.NotLaunchable(retryReason))
        val sut = createSut()
        sut.initialize(initialReason)
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()
        val retryState = sut.retryState.value

        // WHEN
        sut.initialize(initialReason)

        // THEN
        assertThat(sut.retryState.value).isSameAs(retryState)
        verify(mockAnalyticsTracker).track(IneligibleUIShown(initialReason))
        verify(mockAnalyticsTracker).track(IneligibleUIShown(retryReason))
    }

    @Test
    fun `given same ineligibility, when retried and visited again, then each result is tracked`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        whenever(canBeLaunchedInTab(ForceRefresh)).thenReturn(WooPosLaunchability.NotLaunchable(reason))
        val sut = createSut()
        sut.initialize(reason)

        // WHEN
        sut.retryEligibilityCheckTapped()
        advanceUntilIdle()
        createSut().initialize(reason)

        // THEN
        verify(mockAnalyticsTracker, times(3)).track(IneligibleUIShown(reason))
        verify(mockAnalyticsTracker).track(IneligibleUIRetryTapped(reason))
    }

    @Test
    fun `given overlapping initialization, when text resolves, then shown is tracked once`() = runTest {
        // GIVEN
        val countryCode = CompletableDeferred<String?>()
        whenever(mockGetStoreCountryCode()).doSuspendableAnswer { countryCode.await() }
        val reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCountry
        val sut = createSut()
        launch { sut.initialize(reason) }
        launch { sut.initialize(reason) }
        runCurrent()

        // WHEN
        countryCode.complete(null)
        advanceUntilIdle()

        // THEN
        assertThat(sut.retryState.value).isInstanceOf(WooPosEligibilityRetryState.Ineligible::class.java)
        verify(mockAnalyticsTracker).track(IneligibleUIShown(reason))
    }

    @Test
    fun `given cancelled initialization, when initialized again, then state and tracking complete`() = runTest {
        // GIVEN
        val countryCode = CompletableDeferred<String?>()
        whenever(mockGetStoreCountryCode()).doSuspendableAnswer { countryCode.await() }
        val reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCountry
        val sut = createSut()
        val initialization = launch { sut.initialize(reason) }
        runCurrent()
        initialization.cancelAndJoin()

        // WHEN
        countryCode.complete(null)
        sut.initialize(reason)

        // THEN
        assertThat(sut.retryState.value).isInstanceOf(WooPosEligibilityRetryState.Ineligible::class.java)
        verify(mockAnalyticsTracker).track(IneligibleUIShown(reason))
    }

    @Test
    fun `given pending initial tracking, when initialization is cancelled, then tracking completes`() = runTest {
        // GIVEN
        val reason = WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
        val trackingGate = CompletableDeferred<Unit>()
        val tracked = CompletableDeferred<Unit>()
        whenever(mockAnalyticsTracker.track(IneligibleUIShown(reason))).doSuspendableAnswer {
            trackingGate.await()
            tracked.complete(Unit)
            Unit
        }
        val sut = createSut()
        val initialization = launch { sut.initialize(reason) }
        runCurrent()

        // WHEN
        initialization.cancelAndJoin()
        trackingGate.complete(Unit)
        advanceUntilIdle()

        // THEN
        assertThat(tracked.isCompleted).isTrue()
        verify(mockAnalyticsTracker).track(IneligibleUIShown(reason))
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
