package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosFtsSearchResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosProductsSearchInDbDataSourceTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private val selectedSite: SelectedSite = mock()

    private lateinit var sut: WooPosProductsSearchInDbDataSource

    private val siteModel = SiteModel().apply { id = 123 }
    private val siteId = LocalOrRemoteId.LocalId(123)

    private val firstPageFtsResults = (1..15).map {
        WooPosFtsSearchResult.Product(createProductEntity(it.toLong()))
    }
    private val secondPageFtsResults = (16..20).map {
        WooPosFtsSearchResult.Product(createProductEntity(it.toLong()))
    }

    @Before
    fun setup() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        whenever(posLocalCatalogStore.searchProductsFts(siteId, "query", 15, 0))
            .thenReturn(Result.success(firstPageFtsResults))
        whenever(posLocalCatalogStore.searchProductsFts(siteId, "query", 15, 15))
            .thenReturn(Result.success(secondPageFtsResults))

        sut = WooPosProductsSearchInDbDataSource(
            posLocalCatalogStore = posLocalCatalogStore,
            selectedSite = selectedSite,
            productMapper = WooPosProductModelMapper(mock()),
            logger = mock<WooPosLogWrapper>(),
        )
    }

    @Test
    fun `when searchProducts called, then returns first page of results`() = runTest {
        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().products).hasSize(15)
    }

    @Test
    fun `given full page of results, when searchProducts called, then hasMorePages returns true`() = runTest {
        // WHEN
        sut.searchProducts("query")

        // THEN
        assertThat(sut.hasMorePages).isTrue()
    }

    @Test
    fun `given partial page of results, when searchProducts called, then hasMorePages returns false`() = runTest {
        // GIVEN
        whenever(posLocalCatalogStore.searchProductsFts(siteId, "query", 15, 0))
            .thenReturn(Result.success(listOf(WooPosFtsSearchResult.Product(createProductEntity(1L)))))

        // WHEN
        sut.searchProducts("query")

        // THEN
        assertThat(sut.hasMorePages).isFalse()
    }

    @Test
    fun `when loadMore called after initial search, then returns accumulated results`() = runTest {
        // GIVEN
        sut.searchProducts("query")

        // WHEN
        val result = sut.loadMore("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(20)
    }

    @Test
    fun `given no site selected, when searchProducts called, then returns failure`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `given store error, when searchProducts called, then returns failure`() = runTest {
        // GIVEN
        val error = Exception("Database error")
        whenever(posLocalCatalogStore.searchProductsFts(siteId, "query", 15, 0))
            .thenReturn(Result.failure(error))

        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    @Test
    fun `when searchProducts returns fts results with variations, then maps them correctly`() = runTest {
        // GIVEN
        val ftsResults = listOf(
            WooPosFtsSearchResult.Product(createProductEntity(1L)),
            WooPosFtsSearchResult.Variation(
                entity = createVariationEntity(2L, parentProductId = 10L),
                parentProductName = "Parent"
            ),
        )
        whenever(posLocalCatalogStore.searchProductsFts(siteId, "query", 15, 0))
            .thenReturn(Result.success(ftsResults))

        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().products).hasSize(2)
    }

    private fun createVariationEntity(
        remoteId: Long,
        parentProductId: Long = 0L
    ) = WooPosVariationEntity(
        localSiteId = siteId,
        remoteProductId = LocalOrRemoteId.RemoteId(parentProductId),
        remoteVariationId = LocalOrRemoteId.RemoteId(remoteId),
        price = "10.00",
        status = "publish",
    )

    private fun createProductEntity(
        remoteId: Long,
        name: String = "Test Product"
    ) = WooPosProductEntity(
        localSiteId = siteId,
        remoteId = LocalOrRemoteId.RemoteId(remoteId),
        name = name,
        sku = "",
        globalUniqueId = "",
        type = "simple",
        price = "10.00",
        downloadable = false,
        images = "",
        attributes = "",
        parentId = 0,
        status = "publish",
        regularPrice = "10.00",
        salePrice = "",
        onSale = false,
        description = "",
        shortDescription = "",
        manageStock = false,
        stockQuantity = null,
        stockStatus = "instock",
        backordersAllowed = false,
        backordered = false,
        categories = "",
        tags = "",
        dateModified = "2024-01-01T00:00:00Z",
        variations = ""
    )
}
