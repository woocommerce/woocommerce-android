package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncRequirement
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncStatusChecker
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@Suppress("UnusedFlow")
@ExperimentalCoroutinesApi
class WooPosProductsDataSourceTest {

    private val remoteDataSource: WooPosProductsRemoteDataSource = mock()
    private val localDbDataSource: WooPosProductsInDbDataSource = mock()
    private val syncStatusChecker: WooPosFullSyncStatusChecker = mock()

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
            WooPosFullSyncRequirement.NotRequired
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
            WooPosFullSyncRequirement.Overdue
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
    fun `given sync error, when prepopulate cache, then no active source is set`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.Error("Sync error")
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
            WooPosFullSyncRequirement.NotRequired
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

    private fun createSut() = WooPosProductsDataSource(
        remoteDataSource = remoteDataSource,
        localDbDataSource = localDbDataSource,
        syncStatusChecker = syncStatusChecker
    )
}
