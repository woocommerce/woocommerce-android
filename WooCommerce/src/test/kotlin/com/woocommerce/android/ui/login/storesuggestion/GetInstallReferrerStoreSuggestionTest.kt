package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class GetInstallReferrerStoreSuggestionTest : BaseUnitTest() {
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val installReferrerSource: InstallReferrerSource = mock()
    private val registrableDomainResolver: RegistrableDomainResolver = mock {
        on { resolve(HOST) } doReturn REGISTRABLE_DOMAIN
    }
    private val currentTimeProvider: CurrentTimeProvider = mock {
        on { currentDate() } doReturn Date(TimeUnit.SECONDS.toMillis(NOW))
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val sitePickerRepository: SitePickerRepository = mock()
    private val storeSuggestionRollout: StoreSuggestionRollout = mock()

    private val sut = GetInstallReferrerStoreSuggestion(
        appPrefsWrapper = appPrefsWrapper,
        installReferrerSource = installReferrerSource,
        installReferrerParser = InstallReferrerParser(),
        registrableDomainResolver = registrableDomainResolver,
        currentTimeProvider = currentTimeProvider,
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        dispatchers = coroutinesTestRule.testDispatchers,
        sitePickerRepository = sitePickerRepository,
        storeSuggestionRollout = storeSuggestionRollout,
    )

    @Before
    fun setUp() = testBlocking {
        whenever(storeSuggestionRollout.isInRollout()).thenReturn(true)
        whenever(appPrefsWrapper.isInstallReferrerChecked).thenReturn(false)
        givenReferrer(referrer())
        givenSiteInfo(Result.success(siteInfo(isJetpackConnected = true)))
    }

    @Test
    fun `given referrer not checked yet, when invoked, then referrer is read and tracked without a build type check`() =
        testBlocking {
            // WHEN
            sut()

            // THEN
            verify(installReferrerSource).fetch()
            verify(appPrefsWrapper).isInstallReferrerChecked = true
            assertThat(trackedResult()).isEqualTo("valid")
        }

    @Test
    fun `given out of rollout and qualified store, when invoked, then no suggestion but tracked with is_in_rollout false and wpcom_login_check qualified`() =
        testBlocking {
            // GIVEN
            whenever(storeSuggestionRollout.isInRollout()).thenReturn(false)

            // WHEN
            val result = sut()

            // THEN
            assertThat(result).isNull()
            verify(sitePickerRepository).fetchSiteInfo(SITE_URL)
            verifyTracked(
                result = "valid",
                hasSiteHost = true,
                campaign = CAMPAIGN,
                wpComLoginCheck = "qualified",
                isInRollout = false,
            )
        }

    @Test
    fun `given in rollout and qualified store, when invoked, then returns suggestion and tracks is_in_rollout true`() =
        testBlocking {
            // GIVEN
            whenever(storeSuggestionRollout.isInRollout()).thenReturn(true)

            // WHEN
            val result = sut()

            // THEN
            assertThat(result?.siteUrl).isEqualTo(SITE_URL)
            verify(storeSuggestionRollout, times(1)).isInRollout()
            verifyTracked(
                result = "valid",
                hasSiteHost = true,
                campaign = CAMPAIGN,
                wpComLoginCheck = "qualified",
                isInRollout = true,
            )
        }

    @Test
    fun `given out of rollout and non valid referrer, when invoked, then tracks is_in_rollout false`() = testBlocking {
        // GIVEN
        whenever(storeSuggestionRollout.isInRollout()).thenReturn(false)
        whenever(installReferrerSource.fetch()).thenReturn(InstallReferrerResult.Unavailable)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(
            result = "unavailable",
            hasSiteHost = false,
            campaign = null,
            wpComLoginCheck = null,
            isInRollout = false,
        )
    }

    @Test
    fun `given referrer already checked, when invoked, then nothing is read or tracked`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.isInstallReferrerChecked).thenReturn(true)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyNoInteractions(installReferrerSource, analyticsTrackerWrapper, storeSuggestionRollout)
    }

    @Test
    fun `given valid referrer, when invoked, then returns suggestion and tracks valid result without the host`() =
        testBlocking {
            // WHEN
            val result = sut()

            // THEN
            assertThat(result).isEqualTo(
                StoreSuggestion(siteUrl = "https://$HOST", host = HOST, registrableDomain = REGISTRABLE_DOMAIN)
            )
            verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "qualified")
            assertThat(trackedValues()).noneMatch { it.contains(HOST) }
        }

    @Test
    fun `when invoked, then referrer is marked as checked before it is read`() = testBlocking {
        // WHEN
        sut()

        // THEN
        inOrder(appPrefsWrapper, installReferrerSource) {
            verify(appPrefsWrapper).isInstallReferrerChecked = true
            verify(installReferrerSource).fetch()
        }
    }

    @Test
    fun `given fetch throws, when invoked, then referrer is still marked as checked`() = testBlocking {
        // GIVEN
        whenever(installReferrerSource.fetch()).thenThrow(IllegalStateException())

        // WHEN
        runCatching { sut() }

        // THEN
        verify(appPrefsWrapper).isInstallReferrerChecked = true
    }

    @Test
    fun `given caller cancelled while fetching, when fetch finishes, then result is still tracked`() = testBlocking {
        // GIVEN
        val fetchResult = CompletableDeferred<InstallReferrerResult>()
        whenever(installReferrerSource.fetch()).doSuspendableAnswer { fetchResult.await() }

        // WHEN
        val job = launch { sut() }
        job.cancel()
        fetchResult.complete(available(referrer()))
        advanceUntilIdle()

        // THEN
        verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "qualified")
    }

    @Test
    fun `given each referrer outcome, when invoked, then the matching result is tracked`() = testBlocking {
        NON_VALID_REFERRERS.forEach { (fetchResult, expected) ->
            // GIVEN
            clearInvocations(analyticsTrackerWrapper)
            whenever(installReferrerSource.fetch()).thenReturn(fetchResult)

            // WHEN
            val result = sut()

            // THEN
            assertThat(result).describedAs(expected).isNull()
            assertThat(trackedResult()).describedAs(expected).isEqualTo(expected)
        }
    }

    @Test
    fun `given each non valid referrer, when invoked, then the store is not checked or tracked as checked`() =
        testBlocking {
            NON_VALID_REFERRERS.forEach { (fetchResult, expected) ->
                // GIVEN
                clearInvocations(analyticsTrackerWrapper)
                whenever(installReferrerSource.fetch()).thenReturn(fetchResult)

                // WHEN
                sut()

                // THEN
                assertThat(trackedProperties().keys)
                    .describedAs(expected)
                    .doesNotContain(AnalyticsTracker.KEY_WPCOM_LOGIN_CHECK)
            }
            verifyNoInteractions(sitePickerRepository)
        }

    @Test
    fun `given store is hosted on WPCom, when invoked, then returns suggestion and tracks qualified`() = testBlocking {
        // GIVEN
        givenSiteInfo(Result.success(siteInfo(isWPCom = true)))

        // WHEN
        val result = sut()

        // THEN
        assertThat(result?.siteUrl).isEqualTo(SITE_URL)
        verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "qualified")
    }

    @Test
    fun `given store is connected with Jetpack, when invoked, then returns suggestion and tracks qualified`() =
        testBlocking {
            // GIVEN
            givenSiteInfo(Result.success(siteInfo(isJetpackConnected = true)))

            // WHEN
            val result = sut()

            // THEN
            assertThat(result?.siteUrl).isEqualTo(SITE_URL)
            verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "qualified")
        }

    @Test
    fun `given store has Jetpack but no connection, when invoked, then returns null and tracks not qualified`() =
        testBlocking {
            // GIVEN
            givenSiteInfo(Result.success(siteInfo(hasJetpack = true)))

            // WHEN
            val result = sut()

            // THEN
            assertThat(result).isNull()
            verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "not_qualified")
        }

    @Test
    fun `given site info request fails, when invoked, then returns null and tracks failed`() = testBlocking {
        // GIVEN
        givenSiteInfo(Result.failure(Exception()))

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
        verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "failed")
    }

    @Test
    fun `given site info never returns, when invoked and the limit passes, then returns null and tracks timed out`() =
        testBlocking {
            // GIVEN
            whenever(sitePickerRepository.fetchSiteInfo(SITE_URL)).doSuspendableAnswer { awaitCancellation() }

            // WHEN
            val result = async { sut() }
            advanceTimeBy(CHECK_LIMIT_MILLIS + 1)

            // THEN
            assertThat(result.getCompleted()).isNull()
            verifyTracked(result = "valid", hasSiteHost = true, campaign = CAMPAIGN, wpComLoginCheck = "timed_out")
        }

    @Test
    fun `given site info never returns, when invoked and the limit has not passed, then nothing is tracked yet`() =
        testBlocking {
            // GIVEN
            whenever(sitePickerRepository.fetchSiteInfo(SITE_URL)).doSuspendableAnswer { awaitCancellation() }

            // WHEN
            val result = async { sut() }
            advanceTimeBy(CHECK_LIMIT_MILLIS - 1)

            // THEN
            assertThat(result.isCompleted).isFalse()
            verifyNoInteractions(analyticsTrackerWrapper)
        }

    @Test
    fun `given click and install times, when invoked, then only fresh referrers are suggested`() = testBlocking {
        val cases = listOf(
            Triple(0L, 0L, false),
            Triple(NOW - MAX_AGE, NOW, true),
            Triple(NOW - MAX_AGE - 1, NOW, false),
            Triple(NOW + MAX_SKEW, NOW + MAX_SKEW, true),
            Triple(NOW + MAX_SKEW + 1, NOW + MAX_SKEW + 1, false),
            Triple(CLICK, CLICK, true),
            Triple(CLICK, CLICK - 1, false),
        )

        cases.forEach { (click, installBegin, isFresh) ->
            // GIVEN
            whenever(installReferrerSource.fetch()).thenReturn(available(referrer(), click, installBegin))

            // WHEN
            val result = sut()

            // THEN
            assertThat(result != null).describedAs("click=$click installBegin=$installBegin").isEqualTo(isFresh)
        }
    }

    @Test
    fun `given each campaign, when invoked, then only known campaigns are tracked as is`() = testBlocking {
        val cases = mapOf(
            "deeplinks_promote_app" to "deeplinks_promote_app",
            "deeplinks_payments" to "deeplinks_payments",
            "deeplinks_orders_details" to "deeplinks_orders_details",
            "something_else" to "other",
        )

        cases.forEach { (campaign, expected) ->
            // GIVEN
            clearInvocations(analyticsTrackerWrapper)
            givenReferrer(referrer(campaign = campaign))

            // WHEN
            sut()

            // THEN
            verifyTracked(result = "valid", hasSiteHost = true, campaign = expected, wpComLoginCheck = "qualified")
        }
    }

    private suspend fun givenReferrer(referrer: String) {
        whenever(installReferrerSource.fetch()).thenReturn(available(referrer))
    }

    private suspend fun givenSiteInfo(result: Result<ConnectSiteInfoPayload>) {
        whenever(sitePickerRepository.fetchSiteInfo(SITE_URL)).thenReturn(result)
    }

    private fun trackedValues(): List<String> = trackedProperties().values.map { it.toString() }

    private fun trackedResult(): Any? = trackedProperties()[AnalyticsTracker.KEY_RESULT]

    private fun trackedProperties(): Map<String, *> {
        val captor = argumentCaptor<Map<String, *>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED), captor.capture())
        return captor.firstValue
    }

    private fun verifyTracked(
        result: String,
        hasSiteHost: Boolean,
        campaign: String?,
        wpComLoginCheck: String?,
        isInRollout: Boolean = true,
    ) {
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED),
            eq(
                buildMap {
                    put(AnalyticsTracker.KEY_RESULT, result)
                    put(AnalyticsTracker.KEY_HAS_SITE_HOST, hasSiteHost)
                    campaign?.let { put(AnalyticsTracker.KEY_UTM_CAMPAIGN, it) }
                    wpComLoginCheck?.let { put(AnalyticsTracker.KEY_WPCOM_LOGIN_CHECK, it) }
                    put(AnalyticsTracker.KEY_IS_IN_ROLLOUT, isInRollout)
                }
            )
        )
    }

    private companion object {
        const val CAMPAIGN = "deeplinks_promote_app"
        const val HOST = "shop.store.com"
        const val SITE_URL = "https://$HOST"
        const val REGISTRABLE_DOMAIN = "store.com"
        const val NOW = 1_760_000_000L
        const val MAX_REFERRER_LENGTH = 2_048
        const val CHECK_LIMIT_MILLIS = 3_000L
        val CLICK = NOW - TimeUnit.HOURS.toSeconds(1)
        val MAX_AGE = TimeUnit.HOURS.toSeconds(24)
        val MAX_SKEW = TimeUnit.MINUTES.toSeconds(5)

        fun referrer(siteHost: String = HOST, campaign: String = CAMPAIGN) =
            "utm_source=google-play&utm_campaign=$campaign&site_host=$siteHost"

        fun available(referrer: String, click: Long = CLICK, installBegin: Long = click) =
            InstallReferrerResult.Available(
                referrer = referrer,
                clickTimestampSeconds = click,
                installBeginTimestampSeconds = installBegin,
            )

        fun siteInfo(isWPCom: Boolean = false, hasJetpack: Boolean = false, isJetpackConnected: Boolean = false) =
            ConnectSiteInfoPayload(
                url = SITE_URL,
                exists = true,
                isWordPress = true,
                hasJetpack = hasJetpack || isJetpackConnected,
                isJetpackActive = hasJetpack || isJetpackConnected,
                isJetpackConnected = isJetpackConnected,
                isWPCom = isWPCom,
            )

        val NON_VALID_REFERRERS = listOf(
            InstallReferrerResult.Unavailable to "unavailable",
            InstallReferrerResult.Error to "error",
            available("") to "no_referrer",
            available(referrer() + "&utm_content=" + "a".repeat(MAX_REFERRER_LENGTH)) to "too_long",
            available("utm_campaign=$CAMPAIGN") to "no_site_host",
            available(referrer(siteHost = "https%3A%2F%2F$HOST")) to "invalid_site_host",
            available(referrer(), click = NOW - TimeUnit.HOURS.toSeconds(25)) to "stale",
        )
    }
}
