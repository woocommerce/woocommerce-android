package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPerformLocalCatalogIncrementalSyncTest {

    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        main = testDispatcher,
        computation = testDispatcher,
        io = testDispatcher
    )

    private lateinit var sut: WooPosPerformLocalCatalogIncrementalSync

    @Before
    fun setUp() {
        sut = WooPosPerformLocalCatalogIncrementalSync(
            localCatalogSyncRepository = localCatalogSyncRepository,
            selectedSite = selectedSite,
            networkStatus = networkStatus,
            isLocalCatalogSupported = isLocalCatalogSupported,
            wooPosLogWrapper = wooPosLogWrapper,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `given local catalog not supported, when execute called, then returns null`() = runTest(testDispatcher) {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported(site.localId())).thenReturn(false)

        // WHEN
        val result = sut.execute(WooPosIncrementalSyncReason.ON_POS_HOME)

        // THEN
        assertThat(result).isNull()
        verify(wooPosLogWrapper).d("Skipping sync on POS home: Local catalog not supported for site")
    }

    @Test
    fun `given no network connection, when execute called, then returns null`() = runTest(testDispatcher) {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(false)

        // WHEN
        val result = sut.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)

        // THEN
        assertThat(result).isNull()
        verify(wooPosLogWrapper).d("Skipping sync after successful payment: No network connection")
    }

    @Test
    fun `given no site selected, when execute called, then returns null`() = runTest(testDispatcher) {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = sut.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)

        // THEN
        assertThat(result).isNull()
        verify(wooPosLogWrapper).d("Skipping sync periodic hourly: No site selected")
    }

    @Test
    fun `given sync succeeds, when execute called, then returns success result`() = runTest(testDispatcher) {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 15,
            variationsSynced = 8,
            syncDurationMs = 2500L
        )

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported(site.localId())).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN
        val result = sut.execute(WooPosIncrementalSyncReason.ON_POS_HOME)

        // THEN
        assertThat(result).isEqualTo(syncResult)
        verify(wooPosLogWrapper).d("Starting incremental sync on POS home")
        verify(wooPosLogWrapper).d(
            "Sync on POS home completed successfully: 15 products, 8 variations synced in 2500ms"
        )
    }

    @Test
    fun `given sync fails with unexpected error, when execute called, then returns failure result`() = runTest(
        testDispatcher
    ) {
        // GIVEN
        val site = SiteModel().apply { id = 456 }
        val syncResult = PosLocalCatalogSyncResult.Failure.UnexpectedError("Network timeout")

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported(site.localId())).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN
        val result = sut.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)

        // THEN
        assertThat(result).isEqualTo(syncResult)
        verify(wooPosLogWrapper).d("Starting incremental sync after successful payment")
        verify(wooPosLogWrapper).e("Sync after successful payment failed: Network timeout")
    }

    @Test
    fun `given sync fails with catalog too large, when execute called, then returns failure result`() = runTest(
        testDispatcher
    ) {
        // GIVEN
        val site = SiteModel().apply { id = 789 }
        val syncResult = PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
            error = "Catalog exceeds limit",
            totalPages = 15,
            maxPages = 3
        )

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported(site.localId())).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN
        val result = sut.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)

        // THEN
        assertThat(result).isEqualTo(syncResult)
        verify(wooPosLogWrapper).d("Starting incremental sync periodic hourly")
        verify(wooPosLogWrapper).e("Sync periodic hourly failed: Catalog exceeds limit")
    }

    @Test
    fun `given different sync reasons, when execute called, then logs correct reason description`() = runTest(
        testDispatcher
    ) {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 5,
            variationsSynced = 2,
            syncDurationMs = 1000L
        )

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported(site.localId())).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN & THEN
        sut.execute(WooPosIncrementalSyncReason.ON_POS_HOME)
        verify(wooPosLogWrapper).d("Starting incremental sync on POS home")

        sut.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)
        verify(wooPosLogWrapper).d("Starting incremental sync after successful payment")

        sut.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
        verify(wooPosLogWrapper).d("Starting incremental sync periodic hourly")
    }
}
