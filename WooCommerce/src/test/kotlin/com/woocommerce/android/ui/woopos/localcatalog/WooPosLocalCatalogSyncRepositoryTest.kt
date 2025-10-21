package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncRepositoryTest : BaseUnitTest() {
    private lateinit var sut: WooPosLocalCatalogSyncRepository
    private var posSyncProductsAction: WooPosSyncProductsAction = mock()
    private var posSyncVariationsAction: WooPosSyncVariationsAction = mock()
    private var syncTimestampManager: WooPosSyncTimestampManager = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private lateinit var dispatchers: CoroutineDispatchers
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

    @Before
    fun setup() {
        dispatchers = CoroutineDispatchers(
            main = UnconfinedTestDispatcher(),
            io = UnconfinedTestDispatcher(),
            computation = UnconfinedTestDispatcher()
        )

        sut = WooPosLocalCatalogSyncRepository(
            posSyncProductsAction = posSyncProductsAction,
            posSyncVariationsAction = posSyncVariationsAction,
            syncTimestampManager = syncTimestampManager,
            dispatchers = dispatchers,
            logger = logger,
            preferencesRepository = preferencesRepository,
        )

        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
    }

    @Test
    fun `when full sync succeeds, then returns success`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced, "2024-01-01T12:00:00Z")
            )
        whenever(posSyncVariationsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncVariationsAction.WooPosSyncVariationsResult.Success(50, "2024-01-01T12:00:00Z"))

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `when full sync succeeds, then stores both last sync and last full sync timestamps`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced, "2024-01-01T12:00:00Z")
            )
        whenever(posSyncVariationsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncVariationsAction.WooPosSyncVariationsResult.Success(50, "2024-01-01T12:00:00Z"))

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
        verify(syncTimestampManager).storeFullSyncLastCompletedTimestamp(any())
    }

    @Test
    fun `when full sync succeeds, then does not disable periodic sync`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced, "2024-01-01T12:00:00Z")
            )
        whenever(posSyncVariationsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncVariationsAction.WooPosSyncVariationsResult.Success(50, "2024-01-01T12:00:00Z"))

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(preferencesRepository, never()).disablePeriodicSyncForSite(any())
    }

    @Test
    fun `when full sync fails, then disables periodic sync`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = WooPosLocalCatalogSyncRepository.MAX_PAGES_PER_FULL_SYNC
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Failed.CatalogTooLarge(totalPages, maxPages))

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(preferencesRepository).disablePeriodicSyncForSite(any())
    }

    @Test
    fun `when full sync fails with catalog too large, then returns CatalogTooLarge failure`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = WooPosLocalCatalogSyncRepository.MAX_PAGES_PER_FULL_SYNC
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Failed.CatalogTooLarge(totalPages, maxPages))

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.CatalogTooLarge::class.java)
    }

    @Test
    fun `when full sync fails with unexpected error, then returns UnexpectedError failure`() = testBlocking {
        // GIVEN
        val errorMessage = "Network timeout"
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Failed.UnexpectedError(errorMessage))

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.UnexpectedError::class.java)
    }

    @Test
    fun `when incremental sync succeeds, then returns success`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced, "2024-01-01T12:00:00Z")
            )
        whenever(posSyncVariationsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncVariationsAction.WooPosSyncVariationsResult.Success(50, "2024-01-01T12:00:00Z"))

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `when incremental sync succeeds, then stores timestamp`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced, "2024-01-01T12:00:00Z")
            )
        whenever(posSyncVariationsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncVariationsAction.WooPosSyncVariationsResult.Success(50, "2024-01-01T12:00:00Z"))

        // WHEN
        sut.syncLocalCatalogIncremental(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
    }

    @Test
    fun `when incremental sync fails with catalog too large, then returns CatalogTooLarge failure`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = WooPosLocalCatalogSyncRepository.MAX_PAGES_PER_FULL_SYNC
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Failed.CatalogTooLarge(totalPages, maxPages))

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.CatalogTooLarge::class.java)
    }

    @Test
    fun `when incremental sync fails with unexpected error, then returns UnexpectedError failure`() = testBlocking {
        // GIVEN
        val errorMessage = "Network timeout"
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Failed.UnexpectedError(errorMessage))

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.UnexpectedError::class.java)
    }
}
