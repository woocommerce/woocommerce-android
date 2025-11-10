package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.ConnectionType
import com.woocommerce.android.ui.woopos.util.WooPosConnectionTypeProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncRepositoryTest : BaseUnitTest() {
    private lateinit var sut: WooPosLocalCatalogSyncRepository
    private var posSyncAction: WooPosSyncAction = mock()
    private var posCheckCatalogSizeAction: WooPosCheckCatalogSizeAction = mock()
    private var syncTimestampManager: WooPosSyncTimestampManager = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private lateinit var dispatchers: CoroutineDispatchers
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()
    private var dateTimeProvider: DateTimeProvider = mock()
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private var connectionTypeProvider: WooPosConnectionTypeProvider = mock()
    private var analyticsTracker: WooPosAnalyticsTracker = mock()

    @Before
    fun setup() = runTest {
        dispatchers = CoroutineDispatchers(
            main = UnconfinedTestDispatcher(),
            io = UnconfinedTestDispatcher(),
            computation = UnconfinedTestDispatcher()
        )

        sut = WooPosLocalCatalogSyncRepository(
            posSyncAction = posSyncAction,
            posCheckCatalogSizeAction = posCheckCatalogSizeAction,
            syncTimestampManager = syncTimestampManager,
            dispatchers = dispatchers,
            logger = logger,
            preferencesRepository = preferencesRepository,
            posLocalCatalogStore = posLocalCatalogStore,
            dateTimeProvider = dateTimeProvider,
            analyticsTracker = analyticsTracker,
            connectionTypeProvider = connectionTypeProvider,
        )

        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }

        // Default: catalog size is acceptable
        givenCatalogSizeAcceptable()
        whenever(connectionTypeProvider.getConnectionType()).thenReturn(ConnectionType.WIFI)
        whenever(posLocalCatalogStore.getProductCount(any())).thenReturn(Result.success(9))
        whenever(posLocalCatalogStore.getVariationCount(any())).thenReturn(Result.success(9))
    }

    @Test
    fun `when full sync succeeds, then returns success`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = productsSynced,
                    variationsSynced = 50,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `when full sync succeeds, then stores both last sync and last full sync timestamps`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = productsSynced,
                    variationsSynced = 50,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
        verify(syncTimestampManager).storeVariationsLastSyncTimestamp(any())
        verify(syncTimestampManager).storeFullSyncLastCompletedTimestamp(any())
    }

    @Test
    fun `when full sync succeeds, then does not disable periodic sync`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = productsSynced,
                    variationsSynced = 50,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

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
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.CatalogTooLarge(totalPages, maxPages))

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
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.CatalogTooLarge(totalPages, maxPages))

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.CatalogTooLarge::class.java)
    }

    @Test
    fun `when full sync fails with unexpected error, then returns UnexpectedError failure`() = testBlocking {
        // GIVEN
        val errorMessage = "Network timeout"
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.UnexpectedError(errorMessage))

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.UnexpectedError::class.java)
    }

    @Test
    fun `when incremental sync succeeds, then returns success`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = productsSynced,
                    variationsSynced = 50,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `when incremental sync succeeds, then stores timestamp`() = testBlocking {
        // GIVEN
        val productsSynced = 150
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = productsSynced,
                    variationsSynced = 50,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        sut.syncLocalCatalogIncremental(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
        verify(syncTimestampManager).storeVariationsLastSyncTimestamp(any())
    }

    @Test
    fun `when incremental sync fails with catalog too large, then returns CatalogTooLarge failure`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = WooPosLocalCatalogSyncRepository.MAX_PAGES_PER_FULL_SYNC
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.CatalogTooLarge(totalPages, maxPages))

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.CatalogTooLarge::class.java)
    }

    @Test
    fun `when incremental sync fails with unexpected error, then returns UnexpectedError failure`() = testBlocking {
        // GIVEN
        val errorMessage = "Network timeout"
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.UnexpectedError(errorMessage))

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.UnexpectedError::class.java)
    }

    @Test
    fun `given catalog size exceeds limit, when sync starts, then returns CatalogTooLarge`() = testBlocking {
        // GIVEN
        givenCatalogTooLarge("Catalog too large: 1100 items")

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.CatalogTooLarge::class.java)
        val failure = result as PosLocalCatalogSyncResult.Failure.CatalogTooLarge
        assertThat(failure.error).contains("1100 items")
    }

    @Test
    fun `given catalog size is exactly at limit, when sync starts, then proceeds with sync`() = testBlocking {
        // GIVEN
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = 600,
                    variationsSynced = 400,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `given products count fetch fails, when sync starts, then proceeds with sync anyway`() = testBlocking {
        // GIVEN
        givenCatalogSizeUnknown()
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = 100,
                    variationsSynced = 50,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `given variations count fetch fails, when sync starts, then proceeds with sync anyway`() = testBlocking {
        // GIVEN
        givenCatalogSizeUnknown()
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = 500,
                    variationsSynced = 200,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    private fun givenCatalogSizeAcceptable() = runBlocking {
        whenever(posCheckCatalogSizeAction.execute(any(), anyOrNull(), any()))
            .thenReturn(WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.SizeAcceptable)
    }

    private fun givenCatalogSizeUnknown() = runBlocking {
        whenever(posCheckCatalogSizeAction.execute(any(), anyOrNull(), any()))
            .thenReturn(WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.SizeUnknown)
    }

    private fun givenCatalogTooLarge(errorMessage: String) = runBlocking {
        whenever(posCheckCatalogSizeAction.execute(any(), anyOrNull(), any()))
            .thenReturn(WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.CatalogTooLarge(errorMessage))
    }
}
