package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@ExperimentalCoroutinesApi
class WooPosFullSyncStatusCheckerTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val localCatalogStore: WooPosLocalCatalogStore = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported = mock()
    private val time: DateTimeProvider = mock()

    private val siteModel = SiteModel().apply {
        id = 123
        siteId = 456L
    }

    companion object {
        private const val NOW = 1609459200000L // 2021-01-01 00:00:00 UTC
    }

    private fun createSut() = WooPosFullSyncStatusChecker(
        syncTimestampManager = syncTimestampManager,
        selectedSite = selectedSite,
        networkStatus = networkStatus,
        localCatalogStore = localCatalogStore,
        isLocalCatalogSupported = isLocalCatalogSupported,
        wooPosLogWrapper = wooPosLogWrapper,
        time = time
    )

    @Before
    fun setup() = runTest {
        val recentTimestamp = NOW - 1.days.inWholeMilliseconds
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isLocalCatalogSupported()).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(time.now()).thenReturn(NOW)
        whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
            .thenReturn(Result.success(15))
    }

    @Test
    fun `given local catalog not supported, when checkSyncRequirement called, then should return LocalCatalogDisabled`() =
        runTest {
            // GIVEN
            whenever(isLocalCatalogSupported()).thenReturn(false)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isInstanceOf(WooPosFullSyncRequirement.LocalCatalogDisabled::class.java)
        }

    @Test
    fun `given no site selected, when checkSyncRequirement called, then should return Error`() = runTest {
        // GIVEN
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
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)

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
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(null)
            whenever(networkStatus.isConnected()).thenReturn(false)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isInstanceOf(WooPosFullSyncRequirement.Error::class.java)
            assertThat((result as WooPosFullSyncRequirement.Error).message).isEqualTo("No network connection")
        }

    @Test
    fun `given sync overdue and network connected, when checkSyncRequirement called, then should return NonBlockingRequired with isOverdue true`() =
        runTest {
            // GIVEN
            val overdueTimestamp = NOW - 8.days.inWholeMilliseconds
            whenever(time.now()).thenReturn(NOW)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(true)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(
                result
            ).isEqualTo(WooPosFullSyncRequirement.NonBlockingRequired(overdueTimestamp, isOverdue = true))
        }

    @Test
    fun `given sync overdue and no network, when checkSyncRequirement called, then should return NonBlockingRequired with isOverdue true`() =
        runTest {
            // GIVEN
            val overdueTimestamp = NOW - 8.days.inWholeMilliseconds
            whenever(time.now()).thenReturn(NOW)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(false)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(
                result
            ).isEqualTo(WooPosFullSyncRequirement.NonBlockingRequired(overdueTimestamp, isOverdue = true))
        }

    @Test
    fun `given sync overdue with empty catalog and no network, when checkSyncRequirement called, then should return NonBlockingRequired with isOverdue true`() =
        runTest {
            // GIVEN
            val overdueTimestamp = NOW - 8.days.inWholeMilliseconds
            whenever(time.now()).thenReturn(NOW)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(0))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(
                result
            ).isEqualTo(WooPosFullSyncRequirement.NonBlockingRequired(overdueTimestamp, isOverdue = true))
        }

    @Test
    fun `given sync not overdue, when checkSyncRequirement called, then should return NotRequired`() =
        runTest {
            // GIVEN
            val recentTimestamp = NOW - 10.hours.inWholeMilliseconds
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired(recentTimestamp))
        }

    @Test
    fun `given sync at exact threshold, when checkSyncRequirement called, then should return NonBlockingRequired with isOverdue false`() =
        runTest {
            // GIVEN
            val exactThresholdTimestamp = NOW - 7.days.inWholeMilliseconds
            whenever(time.now()).thenReturn(NOW)
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(exactThresholdTimestamp)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(
                result
            ).isEqualTo(WooPosFullSyncRequirement.NonBlockingRequired(exactThresholdTimestamp, isOverdue = false))
        }

    @Test
    fun `given product count fetch fails, when checkSyncRequirement called, then should treat as empty catalog`() =
        runTest {
            // GIVEN
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.failure(Exception("Database error")))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isInstanceOf(WooPosFullSyncRequirement.NonBlockingRequired::class.java)
        }
}
