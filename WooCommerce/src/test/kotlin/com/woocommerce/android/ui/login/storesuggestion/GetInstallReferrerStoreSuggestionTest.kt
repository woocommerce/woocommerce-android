package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class GetInstallReferrerStoreSuggestionTest : BaseUnitTest() {
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val installReferrerSource: InstallReferrerSource = mock()
    private val currentTimeProvider: CurrentTimeProvider = mock {
        on { currentDate() } doReturn Date(TimeUnit.SECONDS.toMillis(NOW_SECONDS))
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val sut = GetInstallReferrerStoreSuggestion(
        featureFlagRepository = featureFlagRepository,
        appPrefsWrapper = appPrefsWrapper,
        installReferrerSource = installReferrerSource,
        installReferrerParser = InstallReferrerParser(),
        currentTimeProvider = currentTimeProvider,
        analyticsTrackerWrapper = analyticsTrackerWrapper,
    )

    @Before
    fun setUp() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.LOGIN_STORE_SUGGESTION)).thenReturn(true)
        whenever(appPrefsWrapper.isInstallReferrerChecked).thenReturn(false)
        givenReferrer(VALID_REFERRER)
    }

    @Test
    fun `given feature flag disabled, when invoked, then returns null`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.LOGIN_STORE_SUGGESTION)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given feature flag disabled, when invoked, then referrer is not read`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.LOGIN_STORE_SUGGESTION)).thenReturn(false)

        // WHEN
        sut()

        // THEN
        verifyNoInteractions(installReferrerSource)
    }

    @Test
    fun `given feature flag disabled, when invoked, then nothing is tracked`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.LOGIN_STORE_SUGGESTION)).thenReturn(false)

        // WHEN
        sut()

        // THEN
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given feature flag disabled, when invoked, then referrer is not marked as checked`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.LOGIN_STORE_SUGGESTION)).thenReturn(false)

        // WHEN
        sut()

        // THEN
        verify(appPrefsWrapper, never()).isInstallReferrerChecked = any()
    }

    @Test
    fun `given referrer already checked, when invoked, then returns null`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.isInstallReferrerChecked).thenReturn(true)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given referrer already checked, when invoked, then referrer is not read`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.isInstallReferrerChecked).thenReturn(true)

        // WHEN
        sut()

        // THEN
        verifyNoInteractions(installReferrerSource)
    }

    @Test
    fun `given referrer already checked, when invoked, then nothing is tracked`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.isInstallReferrerChecked).thenReturn(true)

        // WHEN
        sut()

        // THEN
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given happy path, when invoked, then referrer is marked as checked after reading it`() = testBlocking {
        // WHEN
        sut()

        // THEN
        inOrder(installReferrerSource, appPrefsWrapper) {
            verify(installReferrerSource).fetch()
            verify(appPrefsWrapper).isInstallReferrerChecked = true
        }
    }

    @Test
    fun `given caller cancelled while fetching, when invoked, then referrer is still marked as checked`() =
        testBlocking {
            // WHEN
            invokeAndCancelWhileFetching()

            // THEN
            verify(appPrefsWrapper).isInstallReferrerChecked = true
        }

    @Test
    fun `given caller cancelled while fetching, when invoked, then result is still tracked`() = testBlocking {
        // WHEN
        invokeAndCancelWhileFetching()

        // THEN
        verifyTracked(result = "valid", hasSiteUrl = true, campaign = CAMPAIGN)
    }

    @Test
    fun `given happy path, when invoked, then returns store suggestion`() = testBlocking {
        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(StoreSuggestion(siteUrl = SITE_URL, displayAddress = SITE_ADDRESS))
    }

    @Test
    fun `given http site url, when invoked, then display address has no scheme`() = testBlocking {
        // GIVEN
        givenReferrer(referrer(siteUrl = "http://$SITE_ADDRESS"))

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(StoreSuggestion(siteUrl = "http://$SITE_ADDRESS", displayAddress = SITE_ADDRESS))
    }

    @Test
    fun `given happy path, when invoked, then valid result is tracked`() = testBlocking {
        // WHEN
        sut()

        // THEN
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED,
            mapOf(
                AnalyticsTracker.KEY_RESULT to "valid",
                AnalyticsTracker.KEY_HAS_SITE_URL to true,
                AnalyticsTracker.KEY_UTM_CAMPAIGN to CAMPAIGN,
            )
        )
    }

    @Test
    fun `given happy path, when invoked, then tracked properties do not contain the site address`() = testBlocking {
        // WHEN
        sut()

        // THEN
        val captor = argumentCaptor<Map<String, *>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED), captor.capture())
        assertThat(captor.firstValue.values.map { it.toString() })
            .noneMatch { it.contains(SITE_ADDRESS) }
    }

    @Test
    fun `given invalid site url, when invoked, then tracked properties do not contain the site address`() =
        testBlocking {
            // GIVEN
            givenReferrer(referrer(siteUrl = "https://$SITE_ADDRESS?query=1"))

            // WHEN
            sut()

            // THEN
            val captor = argumentCaptor<Map<String, *>>()
            verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED), captor.capture())
            assertThat(captor.firstValue.values.map { it.toString() })
                .noneMatch { it.contains(SITE_ADDRESS) }
        }

    @Test
    fun `given click older than 24 hours, when invoked, then returns null`() = testBlocking {
        // GIVEN
        givenReferrer(VALID_REFERRER, clickTimestampSeconds = NOW_SECONDS - MAX_AGE_SECONDS - 1)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given click older than 24 hours, when invoked, then stale result is tracked`() = testBlocking {
        // GIVEN
        givenReferrer(VALID_REFERRER, clickTimestampSeconds = NOW_SECONDS - MAX_AGE_SECONDS - 1)

        // WHEN
        sut()

        // THEN
        verifyTracked(result = "stale", hasSiteUrl = true, campaign = CAMPAIGN)
    }

    @Test
    fun `given click timestamp is zero, when invoked, then stale result is tracked`() = testBlocking {
        // GIVEN
        givenReferrer(VALID_REFERRER, clickTimestampSeconds = 0L)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(result = "stale", hasSiteUrl = true, campaign = CAMPAIGN)
    }

    @Test
    fun `given click timestamp in the future, when invoked, then returns store suggestion`() = testBlocking {
        // GIVEN
        givenReferrer(VALID_REFERRER, clickTimestampSeconds = NOW_SECONDS + ONE_HOUR_SECONDS)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(StoreSuggestion(siteUrl = SITE_URL, displayAddress = SITE_ADDRESS))
        verifyTracked(result = "valid", hasSiteUrl = true, campaign = CAMPAIGN)
    }

    @Test
    fun `given click exactly 24 hours ago, when invoked, then returns store suggestion`() = testBlocking {
        // GIVEN
        givenReferrer(VALID_REFERRER, clickTimestampSeconds = NOW_SECONDS - MAX_AGE_SECONDS)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(StoreSuggestion(siteUrl = SITE_URL, displayAddress = SITE_ADDRESS))
        verifyTracked(result = "valid", hasSiteUrl = true, campaign = CAMPAIGN)
    }

    @Test
    fun `given referrer without site url, when invoked, then no site url result is tracked`() = testBlocking {
        // GIVEN
        givenReferrer("utm_source=google-play&utm_campaign=$CAMPAIGN")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(result = "no_site_url", hasSiteUrl = false, campaign = CAMPAIGN)
    }

    @Test
    fun `given referrer with invalid site url, when invoked, then invalid site url result is tracked`() =
        testBlocking {
            // GIVEN
            givenReferrer(referrer(siteUrl = "javascript:alert(1)"))

            // WHEN
            val result = sut()

            // THEN
            assertThat(result).isNull()
            verifyTracked(result = "invalid_site_url", hasSiteUrl = true, campaign = CAMPAIGN)
        }

    @Test
    fun `given invalid and stale referrer, when invoked, then invalid site url result is tracked`() = testBlocking {
        // GIVEN
        givenReferrer(referrer(siteUrl = "https://localhost"), clickTimestampSeconds = 0L)

        // WHEN
        sut()

        // THEN
        verifyTracked(result = "invalid_site_url", hasSiteUrl = true, campaign = CAMPAIGN)
    }

    @Test
    fun `given blank referrer, when invoked, then no referrer result is tracked`() = testBlocking {
        // GIVEN
        givenReferrer("")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(result = "no_referrer", hasSiteUrl = false, campaign = null)
    }

    @Test
    fun `given referrer unavailable, when invoked, then unavailable result is tracked`() = testBlocking {
        // GIVEN
        whenever(installReferrerSource.fetch()).thenReturn(InstallReferrerResult.Unavailable)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(result = "unavailable", hasSiteUrl = false, campaign = null)
    }

    @Test
    fun `given referrer error, when invoked, then error result is tracked without campaign`() = testBlocking {
        // GIVEN
        whenever(installReferrerSource.fetch()).thenReturn(InstallReferrerResult.Error)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(result = "error", hasSiteUrl = false, campaign = null)
    }

    private suspend fun givenReferrer(referrer: String, clickTimestampSeconds: Long = NOW_SECONDS - ONE_HOUR_SECONDS) {
        whenever(installReferrerSource.fetch()).thenReturn(
            InstallReferrerResult.Available(referrer = referrer, clickTimestampSeconds = clickTimestampSeconds)
        )
    }

    private suspend fun TestScope.invokeAndCancelWhileFetching() {
        val fetchResult = CompletableDeferred<InstallReferrerResult>()
        whenever(installReferrerSource.fetch()).doSuspendableAnswer { fetchResult.await() }

        val job = launch { sut() }
        job.cancel()
        fetchResult.complete(
            InstallReferrerResult.Available(
                referrer = VALID_REFERRER,
                clickTimestampSeconds = NOW_SECONDS - ONE_HOUR_SECONDS,
            )
        )
        advanceUntilIdle()
    }

    private fun verifyTracked(result: String, hasSiteUrl: Boolean, campaign: String?) {
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED,
            buildMap {
                put(AnalyticsTracker.KEY_RESULT, result)
                put(AnalyticsTracker.KEY_HAS_SITE_URL, hasSiteUrl)
                campaign?.let { put(AnalyticsTracker.KEY_UTM_CAMPAIGN, it) }
            }
        )
    }

    private companion object {
        const val CAMPAIGN = "store_login"
        const val SITE_ADDRESS = "example.com"
        const val SITE_URL = "https://$SITE_ADDRESS"
        const val NOW_SECONDS = 1_760_000_000L
        val ONE_HOUR_SECONDS = TimeUnit.HOURS.toSeconds(1)
        val MAX_AGE_SECONDS = TimeUnit.HOURS.toSeconds(24)
        val VALID_REFERRER = referrer(siteUrl = SITE_URL)

        fun referrer(siteUrl: String) =
            "utm_source=google-play&utm_campaign=$CAMPAIGN&site_url=${URLEncoder.encode(siteUrl, "UTF-8")}"
    }
}
