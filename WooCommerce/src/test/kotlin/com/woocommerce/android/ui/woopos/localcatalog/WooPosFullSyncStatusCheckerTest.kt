package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import kotlin.test.Test
import kotlin.time.Duration.Companion.days

@ExperimentalCoroutinesApi
class WooPosFullSyncStatusCheckerTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private val localCatalogStore: WooPosLocalCatalogStore = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()

    private val siteModel = SiteModel().apply {
        id = 123
        siteId = 456L
    }

    private fun createSut() = WooPosFullSyncStatusChecker(
        syncTimestampManager = syncTimestampManager,
        selectedSite = selectedSite,
        networkStatus = networkStatus,
        wooPosLocalCatalogM1Enabled = wooPosLocalCatalogM1Enabled,
        localCatalogStore = localCatalogStore,
        wooPosLogWrapper = wooPosLogWrapper
    )

    @Test
    fun `given feature flag disabled, when checkSyncRequirement called, then should return NotRequired`() = runTest {
        // GIVEN
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(false)

        val sut = createSut()

        // WHEN
        val result = sut.checkSyncRequirement()

        // THEN
        assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired)
    }

    @Test
    fun `given no site selected, when checkSyncRequirement called, then should return Error`() = runTest {
        // GIVEN
        whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        val sut = createSut()

        // WHEN
        val result = sut.checkSyncRequirement()

        // THEN
        assertThat(result).isInstanceOf(WooPosFullSyncRequirement.Error::class.java)
        assertThat((result as WooPosFullSyncRequirement.Error).message).isEqualTo("No site selected")
    }

    @Test
    fun `given never synced before and network connected, when checkSyncRequirement called, then should return BlockingRequired`() =
        runTest {
            // GIVEN
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(0))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.BlockingRequired)
        }

    @Test
    fun `given never synced before and no network, when checkSyncRequirement called, then should return Error`() =
        runTest {
            // GIVEN
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(localCatalogStore.getProductCount(any())).thenReturn(Result.success(0))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isInstanceOf(WooPosFullSyncRequirement.Error::class.java)
            assertThat((result as WooPosFullSyncRequirement.Error).message).isEqualTo("No network connection")
        }

    @Test
    fun `given sync overdue and network connected, when checkSyncRequirement called, then should return Overdue`() =
        runTest {
            // GIVEN
            val overdueTimestamp = System.currentTimeMillis() - 8.days.inWholeMilliseconds
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(10))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.Overdue)
        }

    @Test
    fun `given sync overdue and no network, when checkSyncRequirement called, then should return Overdue`() =
        runTest {
            // GIVEN
            val overdueTimestamp = System.currentTimeMillis() - 8.days.inWholeMilliseconds
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(5))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.Overdue)
        }

    @Test
    fun `given sync overdue with empty catalog and no network, when checkSyncRequirement called, then should return Overdue`() =
        runTest {
            // GIVEN
            val overdueTimestamp = System.currentTimeMillis() - 8.days.inWholeMilliseconds
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(0))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.Overdue)
        }

    @Test
    fun `given sync not overdue, when checkSyncRequirement called, then should return NotRequired`() =
        runTest {
            // GIVEN
            val recentTimestamp = System.currentTimeMillis() - 1.days.inWholeMilliseconds
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(15))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired)
        }

    @Test
    fun `given sync at exact threshold, when checkSyncRequirement called, then should return Overdue`() =
        runTest {
            // GIVEN
            val exactThresholdTimestamp = System.currentTimeMillis() - 7.days.inWholeMilliseconds
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(exactThresholdTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(20))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.Overdue)
        }

    @Test
    fun `given product count fetch fails, when checkSyncRequirement called, then should treat as empty catalog`() =
        runTest {
            // GIVEN
            val recentTimestamp = System.currentTimeMillis() - 1.days.inWholeMilliseconds
            whenever(wooPosLocalCatalogM1Enabled()).thenReturn(true)
            whenever(selectedSite.getOrNull()).thenReturn(siteModel)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.failure(Exception("Database error")))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired)
        }
}
