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
class WooPosProductsSearchInDbDataSourceTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val productMapper: WooPosProductModelMapper = mock()

    private lateinit var sut: WooPosProductsSearchInDbDataSource

    private val siteModel = SiteModel().apply { id = 123 }
    private val siteId = LocalOrRemoteId.LocalId(123)
    private val defaultProduct = generateWooPosProduct()
    private val defaultEntity = createProductEntity(1L)

    private val firstPageEntities = (1..15).map { createProductEntity(it.toLong()) }
    private val firstPageProducts = (1..15).map { generateWooPosProduct(productId = it.toLong()) }
    private val secondPageEntities = (16..20).map { createProductEntity(it.toLong()) }
    private val secondPageProducts = (16..20).map { generateWooPosProduct(productId = it.toLong()) }

    @Before
    fun setup() = runTest {
        // Setup happy path mocks
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(productMapper.fromEntity(any())).thenReturn(defaultProduct)

        // Setup mapping for pagination test data
        firstPageEntities.forEachIndexed { index, entity ->
            whenever(productMapper.fromEntity(entity)).thenReturn(firstPageProducts[index])
        }
        secondPageEntities.forEachIndexed { index, entity ->
            whenever(productMapper.fromEntity(entity)).thenReturn(secondPageProducts[index])
        }

        // Setup happy path search results
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(firstPageEntities))
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 15))
            .thenReturn(Result.success(secondPageEntities))

        sut = WooPosProductsSearchInDbDataSource(
            posLocalCatalogStore = posLocalCatalogStore,
            selectedSite = selectedSite,
            productMapper = productMapper
        )
    }

    @Test
    fun `when searchProducts called, then returns first page of results`() = runTest {
        // WHEN
        val result = sut.searchProducts("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(15)
        assertThat(result.getOrThrow().map { it.remoteId }).containsExactlyInAnyOrder(
            *((1L..15L).toList().toTypedArray())
        )
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
    fun `when loadMore called after initial search, then returns accumulated results`() = runTest {
        // GIVEN
        sut.searchProducts("query")

        // WHEN
        val result = sut.loadMore("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(20) // 15 from first page + 5 from second page
    }

    @Test
    fun `when loadMore called, then accumulated results contain all products`() = runTest {
        // GIVEN
        sut.searchProducts("query")

        // WHEN
        val result = sut.loadMore("query")

        // THEN
        val productIds = result.getOrThrow().map { it.remoteId }
        assertThat(productIds).containsExactlyInAnyOrder(*((1L..20L).toList().toTypedArray()))
    }

    @Test
    fun `given last page has less than page size, when loadMore called, then hasMorePages returns false`() = runTest {
        // GIVEN
        sut.searchProducts("query")

        // WHEN
        sut.loadMore("query")

        // THEN
        assertThat(sut.hasMorePages).isFalse()
    }

    @Test
    fun `given no more pages, when loadMore called, then returns current accumulated results`() = runTest {
        // GIVEN
        whenever(posLocalCatalogStore.searchProducts(siteId, "query", 15, 0))
            .thenReturn(Result.success(listOf(defaultEntity)))
        sut.searchProducts("query")

        // WHEN
        val result = sut.loadMore("query")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(1) // Returns the accumulated single item
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
    fun `when new search started, then resets accumulated results`() = runTest {
        // GIVEN
        sut.searchProducts("query")
        sut.loadMore("query")
        val singleEntity = listOf(createProductEntity(99L))
        val singleProduct = generateWooPosProduct(productId = 99L)
        whenever(productMapper.fromEntity(singleEntity[0])).thenReturn(singleProduct)
        whenever(posLocalCatalogStore.searchProducts(siteId, "newQuery", 15, 0))
            .thenReturn(Result.success(singleEntity))

        // WHEN
        val result = sut.searchProducts("newQuery")

        // THEN
        assertThat(result.getOrThrow()).hasSize(1)
        assertThat(result.getOrThrow()[0].remoteId).isEqualTo(99L)
    }

    @Test
    fun `when new search started, then resets hasMorePages flag`() = runTest {
        // GIVEN
        sut.searchProducts("query")
        assertThat(sut.hasMorePages).isTrue()
        val singleEntity = listOf(createProductEntity(99L))
        whenever(posLocalCatalogStore.searchProducts(siteId, "query2", 15, 0))
            .thenReturn(Result.success(singleEntity))

        // WHEN
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

