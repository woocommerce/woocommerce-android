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
        whenever(remoteDataSource.prepopulateProductsCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.fetchFirstPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateProductsCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(remoteDataSource).prepopulateProductsCache()
        verify(remoteDataSource).fetchFirstPage(false)
    }

    @Test
    fun `given sync not required, when prepopulate cache, then uses local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired
        )
        whenever(localDbDataSource.fetchFirstPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateProductsCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(localDbDataSource).fetchFirstPage(false)
    }

    @Test
    fun `given sync overdue, when prepopulate cache, then uses local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.Overdue
        )
        whenever(localDbDataSource.fetchFirstPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateProductsCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(localDbDataSource).fetchFirstPage(false)
    }

    @Test
    fun `given blocking sync required, when prepopulate cache, then uses local db data source and triggers sync`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.BlockingRequired
        )
        whenever(localDbDataSource.prepopulateProductsCache()).thenReturn(Result.success(Unit))
        whenever(localDbDataSource.fetchFirstPage(false)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateProductsCache().toList()
        sut.fetchFirstPage(false).toList()

        // THEN
        verify(localDbDataSource).prepopulateProductsCache()
        verify(localDbDataSource).fetchFirstPage(false)
    }

    @Test
    fun `given sync error, when prepopulate cache, then no active source is set`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.Error("Sync error")
        )
        val sut = createSut()

        // WHEN
        sut.prepopulateProductsCache().toList()

        // THEN
        try {
            sut.fetchFirstPage(false)
            error("Expected exception")
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("Data source not selected")
        }
    }

    @Test
    fun `given local catalog disabled, when load more, then delegates to remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateProductsCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.loadMore()).thenReturn(Result.success(emptyList()))
        val sut = createSut()
        sut.prepopulateProductsCache().toList()

        // WHEN
        val result = sut.loadMore()

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(remoteDataSource).loadMore()
    }

    @Test
    fun `given sync not required, when load more, then delegates to local db data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired
        )
        whenever(localDbDataSource.loadMore()).thenReturn(Result.success(emptyList()))
        val sut = createSut()
        sut.prepopulateProductsCache().toList()

        // WHEN
        val result = sut.loadMore()

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(localDbDataSource).loadMore()
    }

    @Test
    fun `given local catalog disabled, when has more pages, then delegates to remote data source`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog disabled")
        )
        whenever(remoteDataSource.prepopulateProductsCache()).thenReturn(Result.success(Unit))
        whenever(remoteDataSource.hasMorePages).thenReturn(true)
        val sut = createSut()
        sut.prepopulateProductsCache().toList()

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
        sut.resetState()

        // THEN
        verify(remoteDataSource).resetState()
        verify(localDbDataSource).resetState()
    }

    private fun createSut() = WooPosProductsDataSource(
        remoteDataSource = remoteDataSource,
        localDbDataSource = localDbDataSource,
        syncStatusChecker = syncStatusChecker
    )
}
