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
    private var accountRepository: AccountRepository = mock()
    private var selectedSite: SelectedSite = mock()
    private var syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()
    private var featureFlagM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()

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
        }

        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(any())).thenReturn(true)
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(successResponse)
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalSuccessResponse)
    }

    private fun createWorker(): WooPosLocalCatalogSyncWorker {
        return WooPosLocalCatalogSyncWorker(
            appContext = context,
            workerParams = workerParams,
            accountRepository = accountRepository,
            selectedSite = selectedSite,
            syncRepository = syncRepository,
            logger = logger,
            featureFlagM1Enabled = featureFlagM1Enabled,
            preferencesRepository = preferencesRepository,
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
    }

    @Test
    fun `when user is not logged in, then returns failure`() = testBlocking {
        // GIVEN
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `given user logged in, when no site is selected, then returns failure`() = testBlocking {
        // GIVEN
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        val worker = createWorker()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `when periodic sync is disabled for site, then returns failure and skips sync`() = testBlocking {
        // GIVEN
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)).thenReturn(false)

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
    fun `when login state changes between calls, then behavior changes accordingly`() = testBlocking {
        // GIVEN
        val worker = createWorker()

        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
        val result1 = worker.doWork()

        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val result2 = worker.doWork()

        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 100,
                    variationsSynced = 0,
                    syncDurationMs = 1000L
                )
            )
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalSuccessResponse)
        val result3 = worker.doWork()

        // THEN
        assertThat(result1).isEqualTo(ListenableWorker.Result.failure()) // Not logged in
        assertThat(result2).isEqualTo(ListenableWorker.Result.failure()) // No site
        assertThat(result3).isEqualTo(ListenableWorker.Result.success()) // Successful sync
    }

    @Test
    fun `when site changes between calls, then worker uses current site`() = testBlocking {
        // GIVEN
        val site1 = SiteModel().apply {
            id = 1
            siteId = 123L
            name = "Site 1"
        }
        val site2 = SiteModel().apply {
            id = 2
            siteId = 456L
            name = "Site 2"
        }

        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(syncRepository.syncLocalCatalogFull(any()))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 50,
                    variationsSynced = 5,
                    syncDurationMs = 800L
                )
            )
        whenever(syncRepository.syncLocalCatalogIncremental(any()))
            .thenReturn(incrementalSuccessResponse)

        val worker = createWorker()

        whenever(selectedSite.getOrNull()).thenReturn(site1)
        val result1 = worker.doWork()

        whenever(selectedSite.getOrNull()).thenReturn(site2)
        val result2 = worker.doWork()

        // THEN
        assertThat(result1).isEqualTo(ListenableWorker.Result.success())
        assertThat(result2).isEqualTo(ListenableWorker.Result.success())
        verify(syncRepository).syncLocalCatalogFull(eq(site1))
        verify(syncRepository).syncLocalCatalogFull(eq(site2))
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
    fun `when sync fails with catalog too large, then disables periodic sync for site`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(catalogTooLargeResponse)

        val worker = createWorker()

        // WHEN
        worker.doWork()

        // THEN
        verify(preferencesRepository).disablePeriodicSyncForSite(site.siteId)
    }

    @Test
    fun `when sync fails with unexpected error, then does not disable periodic sync`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError("Network error"))

        val worker = createWorker()

        // WHEN
        worker.doWork()

        // THEN
        verify(preferencesRepository, never()).disablePeriodicSyncForSite(any())
    }

    @Test
    fun `when sync succeeds, then does not disable periodic sync`() = testBlocking {
        // GIVEN
        whenever(syncRepository.syncLocalCatalogFull(site))
            .thenReturn(successResponse)
        whenever(syncRepository.syncLocalCatalogIncremental(site))
            .thenReturn(incrementalSuccessResponse)

        val worker = createWorker()

        // WHEN
        worker.doWork()

        // THEN
        verify(preferencesRepository, never()).disablePeriodicSyncForSite(any())
    }
}
