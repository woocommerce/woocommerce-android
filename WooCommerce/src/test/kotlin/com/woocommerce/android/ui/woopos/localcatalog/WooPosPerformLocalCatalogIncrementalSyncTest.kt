package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncSkipped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncSkipReason
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPerformLocalCatalogIncrementalSyncTest {

    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private fun createSut() = WooPosPerformLocalCatalogIncrementalSync(
        localCatalogSyncRepository = localCatalogSyncRepository,
        selectedSite = selectedSite,
        networkStatus = networkStatus,
        isLocalCatalogSupported = isLocalCatalogSupported,
        wooPosLogWrapper = wooPosLogWrapper,
        analyticsTracker = analyticsTracker,
        appCoroutineScope = TestScope(testDispatcher)
    )

    @Test
    fun `given local catalog not supported, when execute called, then skips sync and tracks event`() = runTest(
        testScheduler
    ) {
        // GIVEN
        val sut = createSut()
        val site = SiteModel().apply { id = 123 }
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported()).thenReturn(false)

        // WHEN
        sut.execute(WooPosIncrementalSyncReason.ON_POS_PRODUCT_LIST)
        testScheduler.advanceUntilIdle()

        // THEN
        verify(wooPosLogWrapper).d("Skipping sync on POS product list: Local catalog not supported for site")
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.INCREMENTAL,
                skipReason = SyncSkipReason.LOCAL_CATALOG_DISABLED
            )
        )
        verifyNoInteractions(localCatalogSyncRepository)
    }

    @Test
    fun `given no network connection, when execute called, then skips sync and tracks event`() = runTest(
        testScheduler
    ) {
        // GIVEN
        val sut = createSut()
        whenever(networkStatus.isConnected()).thenReturn(false)

        // WHEN
        sut.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)
        testScheduler.advanceUntilIdle()

        // THEN
        verify(wooPosLogWrapper).d("Skipping sync after successful payment: No network connection")
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.INCREMENTAL,
                skipReason = SyncSkipReason.CHECKING_SYNC_REQUIREMENT_FAILED
            )
        )
        verifyNoInteractions(localCatalogSyncRepository)
    }

    @Test
    fun `given no site selected, when execute called, then skips sync and tracks event`() = runTest(testScheduler) {
        // GIVEN
        val sut = createSut()
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        sut.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
        testScheduler.advanceUntilIdle()

        // THEN
        verify(wooPosLogWrapper).d("Skipping sync periodic hourly: No site selected")
        verify(analyticsTracker).track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.INCREMENTAL,
                skipReason = SyncSkipReason.SITE_NOT_SELECTED
            )
        )
        verifyNoInteractions(localCatalogSyncRepository)
    }

    @Test
    fun `given sync succeeds, when execute called, then performs sync successfully`() = runTest(testScheduler) {
        // GIVEN
        val sut = createSut()
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 15,
            variationsSynced = 8,
            syncDurationMs = 2500L
        )

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported()).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN
        sut.execute(WooPosIncrementalSyncReason.ON_POS_PRODUCT_LIST)
        testScheduler.advanceUntilIdle()

        // THEN
        verify(wooPosLogWrapper).d("Starting incremental sync on POS product list")
        verify(wooPosLogWrapper).d(
            "Sync on POS product list completed successfully: 15 products, 8 variations synced in 2500ms"
        )
    }

    @Test
    fun `given sync fails with unexpected error, when execute called, then logs failure`() = runTest(testScheduler) {
        // GIVEN
        val sut = createSut()
        val site = SiteModel().apply { id = 456 }
        val syncResult = PosLocalCatalogSyncResult.Failure.UnexpectedError("Network timeout")

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported()).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN
        sut.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)
        testScheduler.advanceUntilIdle()

        // THEN
        verify(wooPosLogWrapper).d("Starting incremental sync after successful payment")
        verify(wooPosLogWrapper).e("Sync after successful payment failed: Network timeout")
    }

    @Test
    fun `given different sync reasons, when execute called, then logs correct reason description`() = runTest(
        testScheduler
    ) {
        // GIVEN
        val sut = createSut()
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 5,
            variationsSynced = 2,
            syncDurationMs = 1000L
        )

        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(isLocalCatalogSupported()).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(site)).thenReturn(syncResult)

        // WHEN & THEN
        sut.execute(WooPosIncrementalSyncReason.ON_POS_PRODUCT_LIST)
        testScheduler.advanceUntilIdle()
        verify(wooPosLogWrapper).d("Starting incremental sync on POS product list")

        sut.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)
        testScheduler.advanceUntilIdle()
        verify(wooPosLogWrapper).d("Starting incremental sync after successful payment")

        sut.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
        testScheduler.advanceUntilIdle()
        verify(wooPosLogWrapper).d("Starting incremental sync periodic hourly")
    }
}
