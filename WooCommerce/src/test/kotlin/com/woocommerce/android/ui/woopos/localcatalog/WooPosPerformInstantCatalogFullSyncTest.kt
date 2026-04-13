package com.woocommerce.android.ui.woopos.localcatalog

import androidx.work.WorkInfo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class WooPosPerformInstantCatalogFullSyncTest {

    private val syncRepository: WooPosLocalCatalogSyncRepository = mock()
    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val selectedSite: SelectedSite = mock()
    private val syncScheduler: WooPosLocalCatalogSyncScheduler = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()

    private val sut = WooPosPerformInstantCatalogFullSync(
        syncRepository = syncRepository,
        syncTimestampManager = syncTimestampManager,
        syncScheduler = syncScheduler,
        selectedSite = selectedSite,
        wooPosLogWrapper = wooPosLogWrapper
    )

    @Test
    fun `given no site selected, when invoke called, then returns failure `() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given sync succeeds, when invoke called, then returns success`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Success(
            productsSynced = 10,
            variationsSynced = 5,
            syncDurationMs = 1000L
        )

        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))
        whenever(syncRepository.syncLocalCatalogFull(site)).thenReturn(syncResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given sync fails, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val site = SiteModel().apply { id = 123 }
        val syncResult = PosLocalCatalogSyncResult.Failure.UnexpectedError("Network error")

        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))
        whenever(syncRepository.syncLocalCatalogFull(site)).thenReturn(syncResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given one time worker is running and succeeds, when invoke called, then returns success`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.SUCCEEDED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observeOneTimeWorkInfo()).thenReturn(workInfoFlow)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(System.currentTimeMillis())

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given one time worker is running and fails, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.FAILED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observeOneTimeWorkInfo()).thenReturn(workInfoFlow)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(System.currentTimeMillis())

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `given periodic worker is running and succeeds, when invoke called, then returns success`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.SUCCEEDED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkInfo()).thenReturn(workInfoFlow)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp())
            .thenReturn(System.currentTimeMillis())

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given periodic worker is running and fails, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.FAILED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkInfo()).thenReturn(workInfoFlow)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given one time worker is stopped mid sync, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.ENQUEUED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observeOneTimeWorkInfo()).thenReturn(workInfoFlow)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Background sync worker was stopped: ENQUEUED")
    }

    @Test
    fun `given periodic worker is stopped mid sync, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.BLOCKED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkInfo()).thenReturn(workInfoFlow)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Background sync worker was stopped: BLOCKED")
    }

    @Test
    fun `given worker succeeds but no timestamp found, when invoke called, then returns failure`() = runTest {
        // GIVEN
        val workInfo = mock<WorkInfo>()
        whenever(workInfo.state).thenReturn(WorkInfo.State.SUCCEEDED)
        val workInfoFlow = MutableStateFlow<WorkInfo?>(workInfo)
        whenever(syncScheduler.observeOneTimeWorkStatus()).thenReturn(flowOf(true))
        whenever(syncScheduler.observePeriodicWorkStatus()).thenReturn(flowOf(false))
        whenever(syncScheduler.observeOneTimeWorkInfo()).thenReturn(workInfoFlow)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Worker completed but sync not verified")
    }
}
