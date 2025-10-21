package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
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
    private var preconditionsChecker: WooPosLocalCatalogSyncPreconditionsChecker = mock()
    private var syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

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

        whenever(preconditionsChecker.checkPreconditions())
            .thenReturn(WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed(site))
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(successResponse)
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalSuccessResponse)
    }

    private fun createWorker(): WooPosLocalCatalogSyncWorker {
        return WooPosLocalCatalogSyncWorker(
            appContext = context,
            workerParams = workerParams,
            preconditionsChecker = preconditionsChecker,
            syncRepository = syncRepository,
            logger = logger,
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
    fun `when preconditions fail, then returns skip result`() = testBlocking {
        // GIVEN
        whenever(preconditionsChecker.checkPreconditions())
            .thenReturn(
                WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip(
                    workerResult = ListenableWorker.Result.failure(),
                    reason = "Precondition failed"
                )
            )

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(syncRepository, never()).syncLocalCatalogFull(any())
        verify(syncRepository, never()).syncLocalCatalogIncremental(any())
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
}
