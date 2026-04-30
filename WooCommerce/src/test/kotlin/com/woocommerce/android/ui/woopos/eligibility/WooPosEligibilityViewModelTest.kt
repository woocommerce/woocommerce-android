package com.woocommerce.android.ui.woopos.eligibility

import android.net.Uri
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosSupportedCountries
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryCode
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryName
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.IneligibleUIShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
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
    private val mockStoreCountryProvider: WooPosGetStoreCountryName = mock()
    private val mockStoreCountryCodeProvider: WooPosGetStoreCountryCode = mock()
    private val supportedCountries: WooPosSupportedCountries = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    init {
        whenever(mockResourceProvider.getString(any())).thenReturn("Test suggestion text")
        whenever(mockResourceProvider.getString(any(), any())).thenReturn("Test suggestion text with params")
        whenever(mockResourceProvider.getString(any(), any(), any()))
            .thenReturn("Test suggestion text with country and currency")
        runBlocking {
            whenever(mockStoreCountryProvider()).doReturn("United States")
            whenever(mockStoreCountryCodeProvider()).doReturn("us")
            whenever(
                supportedCountries.supportedCountryCurrencyPairs()
            ).thenReturn(listOf("us" to "usd", "gb" to "gbp"))
        }
    }

    @Test
    fun `given POS is eligible on retry, when retry tapped, then navigation event is emitted`() = runTest {
        // GIVEN
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.Launchable)
        val sut = createSut()
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled)
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
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider,
            supportedCountries,
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
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider,
            supportedCountries,
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
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider,
            supportedCountries,
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

    @Test
    fun `given DE store and primary expansion flag on, when ineligible due to unsupported currency, then message uses EUR`() = runTest {
        // GIVEN
        whenever(supportedCountries.supportedCountryCurrencyPairs()).thenReturn(
            listOf("us" to "usd", "gb" to "gbp", "de" to "eur"),
        )
        val sut = createSut()
        whenever(mockStoreCountryProvider()).thenReturn("Germany")
        whenever(mockStoreCountryCodeProvider()).thenReturn("de")

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)
        advanceUntilIdle()

        // THEN
        verify(mockResourceProvider).getString(
            eq(com.woocommerce.android.R.string.woopos_eligibility_reason_unsupported_currency_country_pair),
            eq("Germany"),
            eq("EUR"),
        )
    }

    @Test
    fun `given DE store and primary expansion flag off, when ineligible due to unsupported currency, then generic message used`() = runTest {
        // GIVEN
        whenever(supportedCountries.supportedCountryCurrencyPairs()).thenReturn(
            listOf("us" to "usd", "gb" to "gbp"),
        )
        val sut = createSut()
        whenever(mockStoreCountryProvider()).thenReturn("Germany")
        whenever(mockStoreCountryCodeProvider()).thenReturn("de")

        // WHEN
        sut.initialize(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)
        advanceUntilIdle()

        // THEN
        verify(mockResourceProvider).getString(
            eq(com.woocommerce.android.R.string.woopos_eligibility_reason_unsupported_currency_generic),
        )
    }

    private fun mockUriParse(): MockedStatic<Uri> {
        return mockStatic(Uri::class.java).apply {
            `when`<Uri> { Uri.parse(any()) }.thenReturn(mock())
        }
    }

    private suspend fun createSut(): WooPosEligibilityViewModel {
        whenever(mockStoreCountryProvider()).thenReturn("United States")
        whenever(mockStoreCountryCodeProvider()).thenReturn("us")
        return WooPosEligibilityViewModel(
            canBeLaunchedInTab,
            mockAnalyticsTracker,
            mockResourceProvider,
            mockSelectedSite,
            mockWooCommerceStore,
            mockCiabSiteGateKeeper,
            mockStoreCountryProvider,
            mockStoreCountryCodeProvider,
            supportedCountries,
        )
    }
}
