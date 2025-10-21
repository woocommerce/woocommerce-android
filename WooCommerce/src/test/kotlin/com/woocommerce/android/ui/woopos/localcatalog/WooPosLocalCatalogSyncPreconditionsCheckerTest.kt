package com.woocommerce.android.ui.woopos.localcatalog

import androidx.work.ListenableWorker
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.time.Duration.Companion.days

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncPreconditionsCheckerTest : BaseUnitTest() {

    private var accountRepository: AccountRepository = mock()
    private var selectedSite: SelectedSite = mock()
    private var featureFlagM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private var logger: WooPosLogWrapper = mock()

    private lateinit var site: SiteModel
    private lateinit var checker: WooPosLocalCatalogSyncPreconditionsChecker

    @Before
    fun setup() {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
            name = "Test Site"
            url = "https://test.com"
        }

        checker = WooPosLocalCatalogSyncPreconditionsChecker(
            accountRepository = accountRepository,
            selectedSite = selectedSite,
            featureFlagM1Enabled = featureFlagM1Enabled,
            preferencesRepository = preferencesRepository,
            logger = logger
        )
    }

    @Test
    fun `given feature flag disabled, when check preconditions called, then skip with failure result`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(false)

        // WHEN
        val result = checker.checkPreconditions()

        // THEN
        assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip::class.java)
        val skipResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip
        assertThat(skipResult.workerResult).isEqualTo(ListenableWorker.Result.failure())
        assertThat(skipResult.reason).contains("Feature flag disabled")
    }

    @Test
    fun `given user not logged in, when check preconditions called, then skip with failure result`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)

        // WHEN
        val result = checker.checkPreconditions()

        // THEN
        assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip::class.java)
        val skipResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip
        assertThat(skipResult.workerResult).isEqualTo(ListenableWorker.Result.failure())
        assertThat(skipResult.reason).contains("User not logged in")
    }

    @Test
    fun `given no site selected, when check preconditions called, then skip with failure result`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = checker.checkPreconditions()

        // THEN
        assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip::class.java)
        val skipResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip
        assertThat(skipResult.workerResult).isEqualTo(ListenableWorker.Result.failure())
        assertThat(skipResult.reason).contains("No selected WooCommerce site")
    }

    @Test
    fun `given periodic sync disabled for site, when check preconditions called, then skip with failure result`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(site)
            whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(false)

            // WHEN
            val result = checker.checkPreconditions()

            // THEN
            assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip::class.java)
            val skipResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip
            assertThat(skipResult.workerResult).isEqualTo(ListenableWorker.Result.failure())
            assertThat(skipResult.reason).contains("Periodic sync permanently disabled")
        }

    @Test
    fun `given POS not used in last 30 days, when check preconditions called, then skip with success result`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(site)
            whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(true)

            val thirtyOneDaysAgo = System.currentTimeMillis() - 31.days.inWholeMilliseconds
            whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(thirtyOneDaysAgo)

            // WHEN
            val result = checker.checkPreconditions()

            // THEN
            assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip::class.java)
            val skipResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip
            assertThat(skipResult.workerResult).isEqualTo(ListenableWorker.Result.success())
            assertThat(skipResult.reason).contains("POS not used in the last 30 days")
        }

    @Test
    fun `given POS used exactly 30 days ago, when check preconditions called, then proceed with site`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(site)
            whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(true)

            val exactlyThirtyDaysAgo = System.currentTimeMillis() - 30.days.inWholeMilliseconds
            whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(exactlyThirtyDaysAgo)

            // WHEN
            val result = checker.checkPreconditions()

            // THEN
            assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed::class.java)
            val proceedResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed
            assertThat(proceedResult.site).isEqualTo(site)
        }

    @Test
    fun `given POS used 29 days ago, when check preconditions called, then proceed with site`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(true)

        val twentyNineDaysAgo = System.currentTimeMillis() - 29.days.inWholeMilliseconds
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(twentyNineDaysAgo)

        // WHEN
        val result = checker.checkPreconditions()

        // THEN
        assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed::class.java)
        val proceedResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed
        assertThat(proceedResult.site).isEqualTo(site)
    }

    @Test
    fun `given POS never used, when check preconditions called, then proceed with site`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(true)
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(null)

        // WHEN
        val result = checker.checkPreconditions()

        // THEN
        assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed::class.java)
        val proceedResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed
        assertThat(proceedResult.site).isEqualTo(site)
    }

    @Test
    fun `given all checks pass, when check preconditions called, then proceed with site`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(true)

        val yesterday = System.currentTimeMillis() - 1.days.inWholeMilliseconds
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(yesterday)

        // WHEN
        val result = checker.checkPreconditions()

        // THEN
        assertThat(result).isInstanceOf(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed::class.java)
        val proceedResult = result as WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed
        assertThat(proceedResult.site).isEqualTo(site)
    }
}
