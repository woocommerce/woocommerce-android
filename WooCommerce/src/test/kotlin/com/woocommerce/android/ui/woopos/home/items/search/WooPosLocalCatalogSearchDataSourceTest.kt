package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLocalCatalogSearchDataSourceTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val productMapper: WooPosProductModelMapper = mock()

    private lateinit var sut: WooPosLocalCatalogSearchDataSource

    private val siteModel = SiteModel().apply { id = 123 }
    private val siteId = LocalOrRemoteId.LocalId(123)
    private val defaultProduct = generateWooPosProduct()
    private val defaultEntity = createProductEntity(1L)

    @Before
    fun setup() {
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(productMapper.fromEntity(any())).thenReturn(defaultProduct)

        sut = WooPosLocalCatalogSearchDataSource(
            posLocalCatalogStore = posLocalCatalogStore,
            selectedSite = selectedSite,
            productMapper = productMapper
        )
    }

    @Test
    fun `given successful search, when searchProducts called, then returns mapped products`() = runTest {
        // GIVEN
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(listOf(defaultEntity)))

        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(1)
    }

    @Test
    fun `given full page of results, when searchProducts called, then hasMorePages returns true`() = runTest {
        // GIVEN
        val entities = (1..15).map { createProductEntity(it.toLong()) }
        whenever(productMapper.fromEntity(any())).thenReturn(generateWooPosProduct())
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(entities))

        // WHEN
        sut.searchProducts("query")

        // THEN
        assertThat(sut.hasMorePages).isTrue()
    }

    @Test
    fun `given partial page of results, when searchProducts called, then hasMorePages returns false`() = runTest {
        // GIVEN
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(listOf(defaultEntity)))

        // WHEN
        sut.searchProducts("query")

        // THEN
        assertThat(sut.hasMorePages).isFalse()
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
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.failure(error))

        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    @Test
    fun `given more pages available, when loadMore called, then loads next page`() = runTest {
        // GIVEN
        val firstPageEntities = (1..15).map { createProductEntity(it.toLong()) }
        val secondPageEntities = listOf(createProductEntity(16L))
        whenever(productMapper.fromEntity(any())).thenReturn(generateWooPosProduct())
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(firstPageEntities))
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 15))
            .thenReturn(Result.success(secondPageEntities))

        // WHEN
        sut.searchProducts("query")
        val result = sut.loadMore("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(1)
    }

    @Test
    fun `given no more pages, when loadMore called, then returns empty list`() = runTest {
        // GIVEN
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(listOf(defaultEntity)))

        // WHEN
        sut.searchProducts("query")
        val result = sut.loadMore("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `given products returned, when searchProducts called, then products are sorted by name`() = runTest {
        // GIVEN
        val entities = listOf(
            createProductEntity(1L, "Zebra"),
            createProductEntity(2L, "Apple"),
            createProductEntity(3L, "Banana")
        )
        val products = listOf(
            generateWooPosProduct(productId = 1L, productName = "Zebra"),
            generateWooPosProduct(productId = 2L, productName = "Apple"),
            generateWooPosProduct(productId = 3L, productName = "Banana")
        )
        entities.forEachIndexed { index, entity ->
            whenever(productMapper.fromEntity(entity)).thenReturn(products[index])
        }
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(entities))

        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.getOrThrow().map { it.name }).containsExactly("Apple", "Banana", "Zebra")
    }

    @Test
    fun `given multiple searches, when searchProducts called, then pagination resets`() = runTest {
        // GIVEN
        val fullPageEntities = (1..15).map { createProductEntity(it.toLong()) }
        val singleEntity = listOf(createProductEntity(1L))
        whenever(productMapper.fromEntity(any())).thenReturn(generateWooPosProduct())
        whenever(posLocalCatalogStore.searchProducts(siteId, "query1", 15, 0))
            .thenReturn(Result.success(fullPageEntities))
        whenever(posLocalCatalogStore.searchProducts(siteId, "query2", 15, 0))
            .thenReturn(Result.success(singleEntity))

        // WHEN
        sut.searchProducts("query1")
        assertThat(sut.hasMorePages).isTrue()
        sut.searchProducts("query2")

        // THEN
        assertThat(sut.hasMorePages).isFalse()
    }

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
