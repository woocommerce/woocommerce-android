package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncRepositoryTest : BaseUnitTest() {
    private lateinit var sut: PosLocalCatalogSyncRepository
    private var posSyncProductsAction: WooPosSyncProductsAction = mock()
    private var syncTimestampManager: WooPosSyncTimestampManager = mock()
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

        sut = PosLocalCatalogSyncRepository(
            posSyncProductsAction = posSyncProductsAction,
            syncTimestampManager = syncTimestampManager,
            dispatchers = dispatchers,
            logger = logger,
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
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced))

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `when full sync succeeds, then stores timestamp`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncProductsAction.execute(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced))

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
    }

    @Test
    fun `when full sync fails with catalog too large, then returns CatalogTooLarge failure`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = PosLocalCatalogSyncRepository.MAX_PAGES_PER_FULL_SYNC
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
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced))

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
            .thenReturn(WooPosSyncProductsAction.WooPosSyncProductsResult.Success(productsSynced))

        // WHEN
        sut.syncLocalCatalogIncremental(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
    }

    @Test
    fun `when incremental sync fails with catalog too large, then returns CatalogTooLarge failure`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = PosLocalCatalogSyncRepository.MAX_PAGES_PER_FULL_SYNC
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
