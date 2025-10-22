package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncWorkerTest : BaseUnitTest() {

    private var context: Context = mock()
    private var workerParams: WorkerParameters = mock()
    private var accountRepository: com.woocommerce.android.ui.login.AccountRepository = mock()
    private var selectedSite: com.woocommerce.android.tools.SelectedSite = mock()
    private var featureFlagM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private var syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()
    private var wooPosTabShouldBeVisible: WooPosTabShouldBeVisible = mock()
    private var timeProvider: DateTimeProviderInterface = object : DateTimeProviderInterface {
        override fun now(): Long = CURRENT_TIME_MILLIS
    }

    companion object {
        private const val CURRENT_TIME_MILLIS = 1704067200000L // 2024-01-01 00:00:00 UTC
    }

    private val successResponse = PosLocalCatalogSyncResult.Success(
        productsSynced = 10,
        variationsSynced = 0,
        syncDurationMs = 1000
    )
    private val catalogTooLargeResponse = PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
        error = "Catalog too large: 29 pages exceed maximum of 10 pages",
        totalPages = 29,
        maxPages = 10
    )
    private val incrementalSuccessResponse = PosLocalCatalogSyncResult.Success(
        productsSynced = 5,
        variationsSynced = 2,
        syncDurationMs = 500
    )
    private val incrementalFailureResponse = PosLocalCatalogSyncResult.Failure.UnexpectedError("Incremental sync error")

    @Before
    fun setup() = testBlocking {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
            name = "Test Site"
            url = "https://test.com"
        }

        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(wooPosTabShouldBeVisible.invoke()).thenReturn(Result.success(true))
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(any())).thenReturn(true)
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(null)
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(successResponse)
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalSuccessResponse)
    }

    private fun createWorker(
        currentTimeInMillis: DateTimeProviderInterface = timeProvider
    ): WooPosLocalCatalogSyncWorker {
        return WooPosLocalCatalogSyncWorker(
            appContext = context,
            workerParams = workerParams,
            accountRepository = accountRepository,
            selectedSite = selectedSite,
            featureFlagM1Enabled = featureFlagM1Enabled,
            preferencesRepository = preferencesRepository,
            syncRepository = syncRepository,
            logger = logger,
            timeProvider = currentTimeInMillis,
            wooPosTabShouldBeVisible = wooPosTabShouldBeVisible,
        )
    }

    @Test
    fun `given user logged in and site available, when sync succeeds, then returns success`() = testBlocking {
        // GIVEN
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository).syncLocalCatalogIncremental(eq(site))
    }

    @Test
    fun `when feature flag disabled, then returns failure`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(false)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
    }

    @Test
    fun `when user not logged in, then returns failure`() = testBlocking {
        // GIVEN
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
    }

    @Test
    fun `when no site selected, then returns failure`() = testBlocking {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
    }

    @Test
    fun `when periodic sync disabled, then returns failure`() = testBlocking {
        // GIVEN
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(false)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
    }

    @Test
    fun `when POS not used in 31 days, then returns success without sync`() = testBlocking {
        // GIVEN
        val thirtyOneDaysAgo = CURRENT_TIME_MILLIS - (31 * 24 * 60 * 60 * 1000L)
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(thirtyOneDaysAgo)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
    }

    @Test
    fun `when sync fails with catalog too large, then returns failure without failure`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(catalogTooLargeResponse)

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `when sync fails with unexpected error, then returns failure with retry`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError(""))
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `given full sync succeeds, when incremental sync succeeds, then returns success and calls both syncs`() = testBlocking {
        // GIVEN
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository).syncLocalCatalogIncremental(eq(site))
    }

    @Test
    fun `given full sync succeeds, when incremental sync fails, then returns success but logs error`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalFailureResponse)

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository).syncLocalCatalogIncremental(eq(site))
        verify(logger).d("Local catalog INCREMENTAL sync failed.")
    }

    @Test
    fun `given full sync fails, when worker executes, then incremental sync is not called`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError("Full sync failed"))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
    }

    @Test
    fun `given full sync fails with catalog too large, when worker executes, then incremental sync is not called`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(catalogTooLargeResponse)

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
    }

    @Test
    fun `given POS tab is not available, when sync is attempted, then returns success without syncing`() = testBlocking {
        // GIVEN
        whenever(wooPosTabShouldBeVisible.invoke()).thenReturn(Result.success(false))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
    }

    @Test
    fun `given POS tab availability check fails, when sync is attempted, then continues to attempt sync`() = testBlocking {
        // GIVEN
        whenever(wooPosTabShouldBeVisible.invoke()).thenReturn(Result.failure(Exception("Failed to check POS tab")))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository).syncLocalCatalogIncremental(eq(site))
    }
}
