package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.ConnectionType
import com.woocommerce.android.ui.woopos.util.WooPosConnectionTypeProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncRepositoryTest : BaseUnitTest() {
    private lateinit var sut: WooPosLocalCatalogSyncRepository
    private var posSyncAction: WooPosSyncAction = mock()
    private var posFileBasedSyncAction: WooPosFileBasedSyncAction = mock()
    private var syncTimestampManager: WooPosSyncTimestampManager = mock()
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
            posFileBasedSyncAction = posFileBasedSyncAction,
            syncTimestampManager = syncTimestampManager,
            dispatchers = dispatchers,
            logger = logger,
            posLocalCatalogStore = posLocalCatalogStore,
            dateTimeProvider = dateTimeProvider,
            analyticsTracker = analyticsTracker,
            connectionTypeProvider = connectionTypeProvider,
        )

        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }

        whenever(connectionTypeProvider.getConnectionType()).thenReturn(ConnectionType.WIFI)
        whenever(posLocalCatalogStore.getProductCount(any())).thenReturn(Result.success(9))
        whenever(posLocalCatalogStore.getVariationCount(any())).thenReturn(Result.success(9))
    }

    @Test
    fun `when full sync succeeds, then returns success`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncSucceeds()

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
    }

    @Test
    fun `when full sync succeeds, then stores both last sync and last full sync timestamps`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncSucceeds()
        whenever(syncTimestampManager.parseTimestampFromApi(any())).thenReturn(123456L)

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(syncTimestampManager).storeProductsLastSyncTimestamp(any())
        verify(syncTimestampManager).storeVariationsLastSyncTimestamp(any())
        verify(syncTimestampManager).storeFullSyncLastCompletedTimestamp(any())
    }

    @Test
    fun `when full sync fails with network error, then returns NetworkError failure`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncFails(
            PosLocalCatalogSyncResult.Failure.NetworkError("Network timeout")
        )

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.NetworkError::class.java)
    }

    @Test
    fun `when full sync fails with unexpected error, then returns UnexpectedError failure`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncFails(
            PosLocalCatalogSyncResult.Failure.UnexpectedError("Unexpected error")
        )

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
    fun `when incremental sync fails with catalog too large, then returns UnexpectedError failure`() = testBlocking {
        // GIVEN
        val totalPages = 15
        val maxPages = 10
        whenever(posSyncAction.syncCatalog(any(), anyOrNull(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.CatalogTooLarge(totalPages, maxPages))

        // WHEN
        val result = sut.syncLocalCatalogIncremental(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Failure.UnexpectedError::class.java)
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
    fun `when file-based full sync succeeds, then returns success with correct counts`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncSucceeds(productsSynced = 600, variationsSynced = 400)

        // WHEN
        val result = sut.syncLocalCatalogFull(site)

        // THEN
        assertThat(result).isInstanceOf(PosLocalCatalogSyncResult.Success::class.java)
        val success = result as PosLocalCatalogSyncResult.Success
        assertThat(success.productsSynced).isEqualTo(600)
        assertThat(success.variationsSynced).isEqualTo(400)
    }

    @Test
    fun `when full sync starts, then tracks sync started event`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncSucceeds()

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.LocalCatalogSyncStarted>())
    }

    @Test
    fun `when full sync completes successfully, then tracks sync completed event`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncSucceeds()

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.LocalCatalogSyncCompleted>())
    }

    @Test
    fun `when full sync fails, then tracks sync failed event`() = testBlocking {
        // GIVEN
        givenFileBasedFullSyncFails(
            PosLocalCatalogSyncResult.Failure.UnexpectedError("Sync failed")
        )

        // WHEN
        sut.syncLocalCatalogFull(site)

        // THEN
        verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.LocalCatalogSyncFailed>())
    }

    @Test
    fun `when full sync fails with catalog file blocked, then tracks catalog_file_blocked`() =
        testBlocking {
            // GIVEN
            givenFileBasedFullSyncFails(
                PosLocalCatalogSyncResult.Failure.CatalogFileBlocked(
                    error = "Catalog file blocked by the host"
                )
            )

            // WHEN
            sut.syncLocalCatalogFull(site)

            // THEN
            verify(analyticsTracker).track(
                argThat {
                    this is WooPosAnalyticsEvent.Event.LocalCatalogSyncFailed &&
                        errorType == WooPosAnalyticsEventConstant.SyncErrorType.CATALOG_FILE_BLOCKED
                }
            )
        }

    @Test
    fun `when incremental sync starts, then tracks sync started event`() = testBlocking {
        // GIVEN
        givenIncrementalSyncSucceeds()

        // WHEN
        sut.syncLocalCatalogIncremental(site)

        // THEN
        verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.LocalCatalogSyncStarted>())
    }

    @Test
    fun `when incremental sync completes successfully, then tracks sync completed event`() = testBlocking {
        // GIVEN
        givenIncrementalSyncSucceeds()

        // WHEN
        sut.syncLocalCatalogIncremental(site)

        // THEN
        verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.LocalCatalogSyncCompleted>())
    }

    @Test
    fun `when incremental sync fails, then tracks sync failed event`() = testBlocking {
        // GIVEN
        whenever(syncTimestampManager.getProductsLastSyncTimestamp()).thenReturn(123456L)
        whenever(syncTimestampManager.formatTimestampForApi(any())).thenReturn("2024-01-01T12:00:00Z")
        whenever(posSyncAction.syncCatalog(any(), any(), any(), any()))
            .thenReturn(WooPosSyncResult.Failed.UnexpectedError("Network error"))

        // WHEN
        sut.syncLocalCatalogIncremental(site)

        // THEN
        verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.LocalCatalogSyncFailed>())
    }

    private suspend fun givenFileBasedFullSyncSucceeds(
        productsSynced: Int = 150,
        variationsSynced: Int = 50,
        syncDurationMs: Long = 1000L,
        lastModifiedDate: String? = "2024-01-01T12:00:00Z"
    ) {
        whenever(posFileBasedSyncAction.syncCatalog(any()))
            .thenReturn(
                WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success(
                    result = PosLocalCatalogSyncResult.Success(
                        productsSynced = productsSynced,
                        variationsSynced = variationsSynced,
                        syncDurationMs = syncDurationMs
                    ),
                    lastModifiedDate = lastModifiedDate
                )
            )
    }

    private suspend fun givenFileBasedFullSyncFails(
        failure: PosLocalCatalogSyncResult.Failure
    ) {
        whenever(posFileBasedSyncAction.syncCatalog(any()))
            .thenReturn(
                WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure(result = failure)
            )
    }

    private suspend fun givenIncrementalSyncSucceeds() {
        whenever(syncTimestampManager.getProductsLastSyncTimestamp()).thenReturn(123456L)
        whenever(syncTimestampManager.formatTimestampForApi(any())).thenReturn("2024-01-01T12:00:00Z")
        whenever(posSyncAction.syncCatalog(any(), any(), any(), any()))
            .thenReturn(
                WooPosSyncResult.Success(
                    productsSynced = 20,
                    variationsSynced = 10,
                    productsServerDate = "2024-01-01T12:00:00Z",
                    variationsServerDate = "2024-01-01T12:00:00Z"
                )
            )
    }
}
