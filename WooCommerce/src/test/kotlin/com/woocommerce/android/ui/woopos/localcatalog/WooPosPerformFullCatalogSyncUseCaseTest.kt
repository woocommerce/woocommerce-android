package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore

class WooPosPerformFullCatalogSyncUseCaseTest {

    private val syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private val syncScheduler: WooPosLocalCatalogSyncScheduler = mock()
    private val localCatalogStore: WooPosLocalCatalogStore = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()

    private val useCase = WooPosPerformInitialCatalogFullSync(
        syncRepository = syncRepository,
        syncTimestampManager = syncTimestampManager,
        selectedSite = selectedSite,
        networkStatus = networkStatus,
        wooPosLocalCatalogM1Enabled = wooPosLocalCatalogM1Enabled,
        syncScheduler = syncScheduler,
        localCatalogStore = localCatalogStore,
        wooPosLogWrapper = wooPosLogWrapper
    )

    @Test
    fun `given feature flag disabled, when checkAndPerformFirstTimeSyncIfNeeded called, then returns NotRequired`() = runTest {
        // GIVEN
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(false)

        // WHEN
        val result = useCase().first()

        // THEN
        assertThat(result).isEqualTo(WooPosFullSyncStatus.NotRequired)
    }

    @Test
    fun `given no site selected, when checkAndPerformFirstTimeSyncIfNeeded called, then returns NotRequired`() = runTest {
        // GIVEN
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = useCase().first()

        // THEN
        assertThat(result).isEqualTo(WooPosFullSyncStatus.NotRequired)
    }

    @Test
    fun `given no network connection, when checkAndPerformFirstTimeSyncIfNeeded called, then returns Failed`() = runTest {
        // GIVEN
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(mock<SiteModel>())
        whenever(networkStatus.isConnected()).thenReturn(false)

        // WHEN
        val result = useCase().first()

        // THEN
        assertThat(result).isInstanceOf(WooPosFullSyncStatus.Failed::class.java)
        assertThat((result as WooPosFullSyncStatus.Failed).error).isEqualTo("No network connection")
    }

    @Test
    fun `given recent sync and catalog has products, when invoke called, then returns NotRequired`() = runTest {
        // GIVEN
        val recentTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 // 1 hour ago
        val site = SiteModel().apply { id = 123 }
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)
        whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(10))

        // WHEN
        val result = useCase().first()

        // THEN
        assertThat(result).isEqualTo(WooPosFullSyncStatus.NotRequired)
    }

    @Test
    fun `given no previous sync and sync succeeds, when invoke called, then blocks and returns Success`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 10,
            variationsSynced = 5,
            syncDurationMs = 1000L
        )

        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)
        whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(0))
        whenever(syncScheduler.isOneTimeWorkRunning()).thenReturn(false)
        whenever(syncRepository.syncLocalCatalogFull(site)).thenReturn(syncResult)

        // WHEN
        val results = mutableListOf<WooPosFullSyncStatus>()
        useCase().collect { status ->
            results.add(status)
        }

        // THEN
        assertThat(results).hasSize(2)
        assertThat(results[0]).isEqualTo(WooPosFullSyncStatus.InProgress)
        assertThat(results[1]).isEqualTo(WooPosFullSyncStatus.Success)
        verify(syncTimestampManager).storeFullSyncLastCompletedTimestamp(any())
    }

    @Test
    fun `given no previous sync and sync fails, when invoke called, then blocks and returns Failed`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Failure.UnexpectedError("Network error")

        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)
        whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(0))
        whenever(syncScheduler.isOneTimeWorkRunning()).thenReturn(false)
        whenever(syncRepository.syncLocalCatalogFull(site)).thenReturn(syncResult)

        // WHEN
        val results = mutableListOf<WooPosFullSyncStatus>()
        useCase().collect { status ->
            results.add(status)
        }

        // THEN
        assertThat(results).hasSize(2)
        assertThat(results[0]).isEqualTo(WooPosFullSyncStatus.InProgress)
        assertThat(results[1]).isInstanceOf(WooPosFullSyncStatus.Failed::class.java)
        assertThat((results[1] as WooPosFullSyncStatus.Failed).error).isEqualTo("Network error")
    }

    @Test
    fun `given catalog empty and sync succeeds, when invoke called, then blocks and returns Success`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val recentTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 // 1 hour ago
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 10,
            variationsSynced = 5,
            syncDurationMs = 1000L
        )

        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)
        whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(0)) // Catalog is empty
        whenever(syncScheduler.isOneTimeWorkRunning()).thenReturn(false)
        whenever(syncRepository.syncLocalCatalogFull(site)).thenReturn(syncResult)

        // WHEN
        val results = mutableListOf<WooPosFullSyncStatus>()
        useCase().collect { status ->
            results.add(status)
        }

        // THEN
        assertThat(results).hasSize(2)
        assertThat(results[0]).isEqualTo(WooPosFullSyncStatus.InProgress)
        assertThat(results[1]).isEqualTo(WooPosFullSyncStatus.Success)
        verify(syncTimestampManager).storeFullSyncLastCompletedTimestamp(any())
    }

    @Test
    fun `given sync overdue, when invoke called, then triggers background worker and returns NotRequired`() = runTest {
        // GIVEN
        val overdueTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 25 // 25 hours ago
        val site = SiteModel().apply { id = 123 }
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
        whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(10)) // Catalog has products

        // WHEN
        val result = useCase().first()

        // THEN
        assertThat(result).isEqualTo(WooPosFullSyncStatus.NotRequired)
        verify(syncScheduler).triggerManualFullCatalogSync()
    }
}
