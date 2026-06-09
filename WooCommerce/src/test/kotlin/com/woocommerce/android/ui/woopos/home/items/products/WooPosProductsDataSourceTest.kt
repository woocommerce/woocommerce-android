package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogProductSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogVariationSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosCatalogFileBlockedException
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncRequirement
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncStatusChecker
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsWooBelowCatalogFixVersion
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import kotlin.test.Test

@Suppress("UnusedFlow")
@ExperimentalCoroutinesApi
class WooPosProductsDataSourceTest {

    private val remoteDataSource: WooPosProductsRemoteDataSource = mock()
    private val localDbDataSource: WooPosProductsInDbDataSource = mock()
    private val syncStatusChecker: WooPosFullSyncStatusChecker = mock()
    private val syncRepository: WooPosLocalCatalogSyncRepository = mock {
        on { syncState }.thenReturn(MutableStateFlow(null))
    }
    private val isWooBelowCatalogFixVersion: WooPosIsWooBelowCatalogFixVersion = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Test
    fun `given local catalog disabled, when prepopulate cache, then uses remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.fetchFirstProductsPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(remoteDataSource).prepopulateCache()
        verify(remoteDataSource).fetchFirstProductsPage(false)
    }

    @Test
    fun `given sync not required, when prepopulate cache, then uses local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired(lastSyncTimestamp = 0L)
        )
        whenever(localDbDataSource.fetchFirstProductsPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(localDbDataSource).fetchFirstProductsPage(false)
    }

    @Test
    fun `given sync overdue, when prepopulate cache, then uses local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NonBlockingRequired(lastSyncTimestamp = 0L, isOverdue = true)
        )
        whenever(localDbDataSource.fetchFirstProductsPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(localDbDataSource).fetchFirstProductsPage(false)
    }

    @Test
    fun `given blocking sync required, when prepopulate cache, then uses local db data source and triggers sync`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.BlockingRequired
        )
        whenever(localDbDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        whenever(localDbDataSource.fetchFirstProductsPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(localDbDataSource).prepopulateCache()
        verify(localDbDataSource).fetchFirstProductsPage(false)
    }

    @Test
    fun `given blocking sync required and prepopulate fails, when prepopulate cache, then emits failed status`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.BlockingRequired
        )
        val errorMessage = "Failed to prepopulate cache"
        whenever(localDbDataSource.prepopulateCache()).thenReturn(
            Result.failure(Exception(errorMessage))
        )
        val sut = createSut()

        // WHEN
        val result = sut.prepopulateCache().toList()

        // THEN
        val status = result.last()
        assertThat(status).isInstanceOf(WooPosProductsDataSource.WooPosPrepopulatingDataStatus.Failed::class.java)
        verify(localDbDataSource).prepopulateCache()
    }

    @Test
    fun `given catalog blocked and woo at or above fix version, when prepopulate cache, then failed is server permissions error`() =
        runTest {
            // GIVEN
            whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
                WooPosFullSyncRequirement.BlockingRequired
            )
            whenever(localDbDataSource.prepopulateCache()).thenReturn(
                Result.failure(WooPosCatalogFileBlockedException())
            )
            whenever(isWooBelowCatalogFixVersion()).thenReturn(false)
            val sut = createSut()

            // WHEN
            val result = sut.prepopulateCache().toList()

            // THEN
            val status = result.last() as WooPosProductsDataSource.WooPosPrepopulatingDataStatus.Failed
            assertThat(status.isServerPermissionsError).isTrue()
            assertThat(sut.didFallBackDueToCatalogBlock()).isFalse()
            verify(remoteDataSource, never()).prepopulateCache()
        }

    @Test
    fun `given catalog blocked and woo below fix version, when prepopulate cache, then falls back to remote`() =
        runTest {
            // GIVEN
            whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
                WooPosFullSyncRequirement.BlockingRequired
            )
            whenever(localDbDataSource.prepopulateCache()).thenReturn(
                Result.failure(WooPosCatalogFileBlockedException())
            )
            whenever(isWooBelowCatalogFixVersion()).thenReturn(true)
            whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
            val sut = createSut()

            // WHEN
            val result = sut.prepopulateCache().toList()

            // THEN
            assertThat(result.last())
                .isInstanceOf(WooPosProductsDataSource.WooPosPrepopulatingDataStatus.Completed::class.java)
            assertThat(sut.didFallBackDueToCatalogBlock()).isTrue()
            assertThat(sut.getCurrentSyncStrategy()).isEqualTo(WooPosProductsDataSource.SyncStrategy.REMOTE)
            verify(remoteDataSource).prepopulateCache()
        }

    @Test
    fun `given catalog blocked and woo below fix version and remote also fails, when prepopulate cache, then emits failed`() =
        runTest {
            // GIVEN
            whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
                WooPosFullSyncRequirement.BlockingRequired
            )
            whenever(localDbDataSource.prepopulateCache()).thenReturn(
                Result.failure(WooPosCatalogFileBlockedException())
            )
            whenever(isWooBelowCatalogFixVersion()).thenReturn(true)
            whenever(remoteDataSource.prepopulateCache()).thenReturn(
                Result.failure(IOException("No network connection"))
            )
            val sut = createSut()

            // WHEN
            val result = sut.prepopulateCache().toList()

            // THEN
            assertThat(result.last())
                .isInstanceOf(WooPosProductsDataSource.WooPosPrepopulatingDataStatus.Failed::class.java)
        }

    @Test
    fun `given generic failure, when prepopulate cache, then does not fall back to remote`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.BlockingRequired
        )
        whenever(localDbDataSource.prepopulateCache()).thenReturn(
            Result.failure(IOException("Download failed with code: 404"))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateCache().toList()

        // THEN
        assertThat(sut.didFallBackDueToCatalogBlock()).isFalse()
        verify(remoteDataSource, never()).prepopulateCache()
    }

    @Test
    fun `when fall back to remote due to catalog block, then uses remote and completes`() = runTest {
        // GIVEN
        whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        val sut = createSut()

        // WHEN
        val result = sut.fallBackToRemoteDueToCatalogBlock().toList()

        // THEN
        assertThat(result.last())
            .isInstanceOf(WooPosProductsDataSource.WooPosPrepopulatingDataStatus.Completed::class.java)
        assertThat(sut.didFallBackDueToCatalogBlock()).isTrue()
        assertThat(sut.getCurrentSyncStrategy()).isEqualTo(WooPosProductsDataSource.SyncStrategy.REMOTE)
        verify(remoteDataSource).prepopulateCache()
    }

    @Test
    fun `given prepopulate fails with generic exception, when prepopulate cache, then failed is not server permissions error`() =
        runTest {
            // GIVEN
            whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
                WooPosFullSyncRequirement.BlockingRequired
            )
            whenever(localDbDataSource.prepopulateCache()).thenReturn(
                Result.failure(IOException("Download failed with code: 404"))
            )
            val sut = createSut()

            // WHEN
            val result = sut.prepopulateCache().toList()

            // THEN
            val status = result.last() as WooPosProductsDataSource.WooPosPrepopulatingDataStatus.Failed
            assertThat(status.isServerPermissionsError).isFalse()
        }

    @Test
    fun `given sync error, when prepopulate cache, then no active source is set`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.Error("No network connection")
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateCache().toList()

        // THEN
        try {
            sut.fetchFirstPage(false)
            error("Expected exception")
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("Data source not selected")
        }
    }

    @Test
    fun `given local catalog disabled, when load more products, then delegates to remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.loadMoreProducts()).thenReturn(Result.success(emptyList()))
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.loadMore()

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(remoteDataSource).loadMoreProducts()
    }

    @Test
    fun `given sync not required, when load more products, then delegates to local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired(lastSyncTimestamp = 0L)
        )
        whenever(localDbDataSource.loadMoreProducts()).thenReturn(Result.success(emptyList()))
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.loadMore()

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(localDbDataSource).loadMoreProducts()
    }

    @Test
    fun `given local catalog disabled, when has more products pages, then delegates to remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.hasMoreProductsPages).thenReturn(true)
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.hasMorePages

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `when reset state, then resets all data sources`() = runTest {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.resetVariationsListHandler()

        // THEN
        verify(remoteDataSource).resetVariationsListHandler()
        verify(localDbDataSource).resetVariationsListHandler()
    }

    @Test
    fun `given local catalog disabled, when refresh products, then delegates to remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.refreshProducts()).thenReturn(
            Result.success(
                ProductsResult.Remote(Result.success(emptyList()))
            )
        )
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.refreshProducts()

        // THEN
        verify(remoteDataSource).refreshProducts()
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given sync not required, when refresh products, then delegates to local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired(lastSyncTimestamp = 0L)
        )
        whenever(localDbDataSource.refreshProducts()).thenReturn(
            Result.success(
                PosLocalCatalogProductSyncResult(PosLocalCatalogSyncResult.Success(0, 0, 0))
            )
        )
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.refreshProducts()

        // THEN
        verify(localDbDataSource).refreshProducts()
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given local catalog disabled, when refresh variations, then delegates to remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.refreshVariations(123L)).thenReturn(
            Result.success(
                VariationsResult.Remote(Result.success(emptyList()))
            )
        )
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.refreshVariations(123L)

        // THEN
        verify(remoteDataSource).refreshVariations(123L)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given sync not required, when refresh variations, then delegates to local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired(lastSyncTimestamp = 0L)
        )
        whenever(localDbDataSource.refreshVariations(123L)).thenReturn(
            Result.success(
                PosLocalCatalogVariationSyncResult(PosLocalCatalogSyncResult.Success(0, 0, 0))
            )
        )
        val sut = createSut()
        sut.prepopulateCache().toList()

        // WHEN
        val result = sut.refreshVariations(123L)

        // THEN
        verify(localDbDataSource).refreshVariations(123L)
        assertThat(result.isSuccess).isTrue()
    }

    private fun createSut() = WooPosProductsDataSource(
        remoteDataSource = remoteDataSource,
        localDbDataSource = localDbDataSource,
        syncStatusChecker = syncStatusChecker,
        syncRepository = syncRepository,
        isWooBelowCatalogFixVersion = isWooBelowCatalogFixVersion,
    )
}
