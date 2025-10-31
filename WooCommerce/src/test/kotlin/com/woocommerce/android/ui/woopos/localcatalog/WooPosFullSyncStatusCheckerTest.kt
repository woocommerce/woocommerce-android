package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
    private val localCatalogStore: WooPosLocalCatalogStore = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()
    private val prefsRepo: WooPosPreferencesRepository = mock()
    private val checkCatalogSizeAction: WooPosCheckCatalogSizeAction = mock()
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported = mock()

    private val siteModel = SiteModel().apply {
        id = 123
        siteId = 456L
    }

    private fun createSut() = WooPosFullSyncStatusChecker(
        syncTimestampManager = syncTimestampManager,
        selectedSite = selectedSite,
        networkStatus = networkStatus,
        localCatalogStore = localCatalogStore,
        prefsRepo = prefsRepo,
        checkCatalogSizeAction = checkCatalogSizeAction,
        isLocalCatalogSupported = isLocalCatalogSupported,
        wooPosLogWrapper = wooPosLogWrapper
    )

    @Before
    fun setup() = runTest {
        val recentTimestamp = System.currentTimeMillis() - 1.days.inWholeMilliseconds
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isLocalCatalogSupported(siteModel.siteId)).thenReturn(true)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
            .thenReturn(Result.success(15))
        whenever(checkCatalogSizeAction.execute(siteModel, null, 1000))
            .thenReturn(WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.SizeAcceptable)
    }

    @Test
    fun `given local catalog not supported, when checkSyncRequirement called, then should return LocalCatalogDisabled`() =
        runTest {
            // GIVEN
            whenever(isLocalCatalogSupported(siteModel.siteId)).thenReturn(false)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isInstanceOf(WooPosFullSyncRequirement.LocalCatalogDisabled::class.java)
        }

    @Test(expected = IllegalStateException::class)
    fun `given no site selected, when checkSyncRequirement called, then should throw error`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        val sut = createSut()

        // WHEN
        sut.checkSyncRequirement()

        // THEN
        // Exception is expected
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
    fun `given sync overdue and network connected, when checkSyncRequirement called, then should return Overdue`() =
        runTest {
            // GIVEN
            val overdueTimestamp = System.currentTimeMillis() - 8.days.inWholeMilliseconds
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(true)

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
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(overdueTimestamp)
            whenever(networkStatus.isConnected()).thenReturn(false)

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
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)

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
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(exactThresholdTimestamp)

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
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.failure(Exception("Database error")))

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired)
        }

    @Test
    fun `given empty catalog and catalog too large, when checkSyncRequirement called, then should disable sync and return LocalCatalogDisabled`() =
        runTest {
            // GIVEN
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(0))
            whenever(checkCatalogSizeAction.execute(siteModel, null, 1000))
                .thenReturn(
                    WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.CatalogTooLarge("Too many products")
                )

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            verify(prefsRepo).disablePeriodicSyncForSite(siteModel.siteId)
            assertThat(result).isInstanceOf(WooPosFullSyncRequirement.LocalCatalogDisabled::class.java)
        }

    @Test
    fun `given empty catalog and catalog size acceptable, when checkSyncRequirement called, then should check sync requirement normally`() =
        runTest {
            // GIVEN
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(0))
            whenever(checkCatalogSizeAction.execute(siteModel, null, 1000))
                .thenReturn(WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.SizeAcceptable)
            val recentTimestamp = System.currentTimeMillis() - 1.days.inWholeMilliseconds
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired)
        }

    @Test
    fun `given empty catalog and catalog size unknown, when checkSyncRequirement called, then should check sync requirement normally`() =
        runTest {
            // GIVEN
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(0))
            whenever(checkCatalogSizeAction.execute(siteModel, null, 1000))
                .thenReturn(WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.SizeUnknown)
            val recentTimestamp = System.currentTimeMillis() - 1.days.inWholeMilliseconds
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)

            val sut = createSut()

            // WHEN
            val result = sut.checkSyncRequirement()

            // THEN
            assertThat(result).isEqualTo(WooPosFullSyncRequirement.NotRequired)
        }

    @Test
    fun `given non-empty catalog, when checkSyncRequirement called, then should not check catalog size`() =
        runTest {
            // GIVEN
            whenever(localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(siteModel.id)))
                .thenReturn(Result.success(100))
            val recentTimestamp = System.currentTimeMillis() - 1.days.inWholeMilliseconds
            whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(recentTimestamp)

            val sut = createSut()

            // WHEN
            sut.checkSyncRequirement()

            // THEN
            verify(checkCatalogSizeAction, never()).execute(any(), any(), any())
        }
}
