package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosFullSyncCheckUseCaseTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    companion object {
        private val TWENTY_FIVE_HOURS_MILLIS = TimeUnit.HOURS.toMillis(25)
        private val TWO_HOURS_MILLIS = TimeUnit.HOURS.toMillis(2)
    }

    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val syncScheduler: WooPosLocalCatalogSyncScheduler = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()

    private val useCase = WooPosFullSyncCheckUseCase(
        syncTimestampManager = syncTimestampManager,
        syncScheduler = syncScheduler,
        selectedSite = selectedSite,
        networkStatus = networkStatus,
        wooPosLocalCatalogM1Enabled = wooPosLocalCatalogM1Enabled,
        wooPosLogWrapper = wooPosLogWrapper
    )

    @Test
    fun `given feature flag disabled, when checkAndTriggerSyncIfNeeded called, then does not trigger sync`() =
        runTest {
            // GIVEN
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(false)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler, never()).triggerManualFullCatalogSync()
        }

    @Test
    fun `given no site selected, when checkAndTriggerSyncIfNeeded called, then does not trigger sync`() =
        runTest {
            // GIVEN
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(null)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler, never()).triggerManualFullCatalogSync()
        }

    @Test
    fun `given no network connection, when checkAndTriggerSyncIfNeeded called, then does not trigger sync`() =
        runTest {
            // GIVEN
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(mock<SiteModel>())
            whenever(networkStatus.isConnected()).thenReturn(false)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler, never()).triggerManualFullCatalogSync()
        }

    @Test
    fun `given sync is running, when checkAndTriggerSyncIfNeeded called, then does not trigger sync`() =
        runTest {
            // GIVEN
            givenAllPrerequisitesMet()
            whenever(syncScheduler.isPeriodicWorkRunning()).thenReturn(true)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler, never()).triggerManualFullCatalogSync()
        }

    @Test
    fun `given no previous sync, when checkAndTriggerSyncIfNeeded called, then triggers sync`() =
        runTest {
            // GIVEN
            givenAllPrerequisitesMet()
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler).triggerManualFullCatalogSync()
        }

    @Test
    fun `given sync older than 24 hours, when checkAndTriggerSyncIfNeeded called, then triggers sync`() =
        runTest {
            // GIVEN
            val currentTime = System.currentTimeMillis()
            val twentyFiveHoursAgo = currentTime - TWENTY_FIVE_HOURS_MILLIS
            givenAllPrerequisitesMet()
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(twentyFiveHoursAgo)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler).triggerManualFullCatalogSync()
        }

    @Test
    fun `given sync newer than 24 hours, when checkAndTriggerSyncIfNeeded called, then does not trigger sync`() =
        runTest {
            // GIVEN
            val currentTime = System.currentTimeMillis()
            val twoHoursAgo = currentTime - TWO_HOURS_MILLIS
            givenAllPrerequisitesMet()
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(twoHoursAgo)

            // WHEN
            useCase.checkAndTriggerSyncIfNeeded()

            // THEN
            verify(syncScheduler, never()).triggerManualFullCatalogSync()
        }

    private fun givenAllPrerequisitesMet() {
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(mock<SiteModel>())
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(syncScheduler.isPeriodicWorkRunning()).thenReturn(false)
        whenever(syncScheduler.isOneTimeWorkRunning()).thenReturn(false)
    }
}
