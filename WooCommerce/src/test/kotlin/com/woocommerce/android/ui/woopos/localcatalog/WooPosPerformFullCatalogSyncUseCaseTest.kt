package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
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

class WooPosPerformFullCatalogSyncUseCaseTest {

    private val syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val selectedSite: SelectedSite = mock()
    private val syncScheduler: WooPosLocalCatalogSyncScheduler = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()

    private val useCase = WooPosPerformInstantCatalogFullSync(
        syncRepository = syncRepository,
        syncTimestampManager = syncTimestampManager,
        syncScheduler = syncScheduler,
        selectedSite = selectedSite,
        wooPosLogWrapper = wooPosLogWrapper
    )

    @Test
    fun `given no site selected, when invoke called, then returns Failed`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        whenever(syncScheduler.isOneTimeWorkRunning()).thenReturn(false)

        // WHEN
        val result = useCase().first()

        // THEN
        assertThat(result).isInstanceOf(WooPosFullSyncStatus.Failed::class.java)
        assertThat((result as WooPosFullSyncStatus.Failed).error).isEqualTo("No site selected")
    }

    @Test
    fun `given sync succeeds, when invoke called, then blocks and returns Success`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 10,
            variationsSynced = 5,
            syncDurationMs = 1000L
        )

        whenever(selectedSite.getOrNull()).thenReturn(site)
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
    fun `given sync fails, when invoke called, then blocks and returns Failed`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Failure.UnexpectedError("Network error")

        whenever(selectedSite.getOrNull()).thenReturn(site)
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
}
