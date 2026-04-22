package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncSkipped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncSkipReason
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
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
    private var selectedSite: com.woocommerce.android.tools.SelectedSite = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private var syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()
    private var syncStatusChecker: WooPosFullSyncStatusChecker = mock()
    private var analyticsTracker: WooPosAnalyticsTracker = mock()
    private var syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val mockTimeProvider: DateTimeProvider = mock {
        whenever(it.now()).thenReturn(CURRENT_TIME_MILLIS)
    }

    companion object {
        private const val CURRENT_TIME_MILLIS = 1704067200000L // 2024-01-01 00:00:00 UTC
    }

    private val successResponse = PosLocalCatalogSyncResult.Success(
        productsSynced = 10,
        variationsSynced = 0,
        syncDurationMs = 1000
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

        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(WooPosFullSyncRequirement.BlockingRequired)

        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(null)
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(successResponse)
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalSuccessResponse)
    }

    private fun createWorker(): WooPosLocalCatalogSyncWorker {
        return WooPosLocalCatalogSyncWorker(
            appContext = context,
            workerParams = workerParams,
            selectedSite = selectedSite,
            preferencesRepository = preferencesRepository,
            syncRepository = syncRepository,
            logger = logger,
            timeProvider = mockTimeProvider,
            syncStatusChecker = syncStatusChecker,
            analyticsTracker = analyticsTracker,
            syncTimestampManager = syncTimestampManager,
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
    fun `when no site selected, then returns retry and tracks event`() = testBlocking {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement())
            .thenReturn(WooPosFullSyncRequirement.Error("No site selected"))
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = SyncSkipReason.CHECKING_SYNC_REQUIREMENT_FAILED
            )
        )
    }

    @Test
    fun `when site is null after validation, then returns failure and tracks event`() = testBlocking {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = SyncSkipReason.SITE_NOT_SELECTED
            )
        )
    }

    @Test
    fun `when POS not used in 31 days, then returns success without sync and tracks event`() = testBlocking {
        // GIVEN
        val thirtyOneDaysAgo = CURRENT_TIME_MILLIS - (31 * 24 * 60 * 60 * 1000L)
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(thirtyOneDaysAgo)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = SyncSkipReason.POS_NOT_OPENED_30_DAYS
            )
        )
    }

    @Test
    fun `given POS never opened and never synced, when worker runs, then proceeds with sync`() = testBlocking {
        // GIVEN
        whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(null)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)
        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(analyticsTracker, never()).track(any())
    }

    @Test
    fun `given POS never opened and full sync already completed, when worker runs, then skips sync`() =
        testBlocking {
            // GIVEN
            whenever(preferencesRepository.getLastUsedTimestamp()).thenReturn(null)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp())
                .thenReturn(CURRENT_TIME_MILLIS)
            val worker = createWorker()

            // WHEN
            val result = worker.doWork()
            coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

            // THEN
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verify(syncRepository, never()).syncLocalCatalogFull(any())
            verify(analyticsTracker).track(
                LocalCatalogSyncSkipped(
                    syncType = SyncType.FULL,
                    skipReason = SyncSkipReason.POS_NOT_OPENED_30_DAYS
                )
            )
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
    fun `given sync not required, when validateSyncStatus is called, then returns success without syncing and tracks event`() = testBlocking {
        // GIVEN
        val lastSyncTimestamp = CURRENT_TIME_MILLIS - (3 * 24 * 60 * 60 * 1000L) // 3 days ago
        whenever(syncStatusChecker.checkSyncRequirement())
            .thenReturn(WooPosFullSyncRequirement.NotRequired(lastSyncTimestamp))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = SyncSkipReason.SYNC_NOT_REQUIRED
            )
        )
    }

    @Test
    fun `given local catalog disabled, when validateSyncStatus is called, then returns success without syncing and tracks event`() = testBlocking {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement())
            .thenReturn(WooPosFullSyncRequirement.LocalCatalogDisabled("Catalog too large"))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = SyncSkipReason.LOCAL_CATALOG_DISABLED
            )
        )
    }

    @Test
    fun `given no network connection, when validateSyncStatus is called, then returns retry and tracks event`() = testBlocking {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement())
            .thenReturn(WooPosFullSyncRequirement.Error("No network connection"))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = SyncSkipReason.CHECKING_SYNC_REQUIREMENT_FAILED
            )
        )
    }

    @Test
    fun `given blocking sync required, when validateSyncStatus is called, then proceeds with sync`() = testBlocking {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement())
            .thenReturn(WooPosFullSyncRequirement.BlockingRequired)

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository).syncLocalCatalogIncremental(eq(site))
    }

    @Test
    fun `given sync overdue, when validateSyncStatus is called, then proceeds with sync`() = testBlocking {
        // GIVEN
        val lastSyncTimestamp = CURRENT_TIME_MILLIS - (8 * 24 * 60 * 60 * 1000L) // 8 days ago
        whenever(syncStatusChecker.checkSyncRequirement())
            .thenReturn(WooPosFullSyncRequirement.NonBlockingRequired(lastSyncTimestamp, isOverdue = true))

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site))
        verify(syncRepository).syncLocalCatalogIncremental(eq(site))
    }
}
