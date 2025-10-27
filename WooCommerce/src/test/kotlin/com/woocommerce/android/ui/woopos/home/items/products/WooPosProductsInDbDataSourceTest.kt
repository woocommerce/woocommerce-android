package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformInstantCatalogFullSync
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.math.BigDecimal

class WooPosProductsInDbDataSourceTest {

    private lateinit var sut: WooPosProductsInDbDataSource
    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val mapper: WooPosProductModelMapper = mock()
    private val performInstantCatalogFullSync: WooPosPerformInstantCatalogFullSync = mock()

    private val siteModel = SiteModel().apply {
        id = 1
    }

    @Before
    fun setUp() {
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        sut = WooPosProductsInDbDataSource(
            posLocalCatalogStore = posLocalCatalogStore,
            selectedSite = selectedSite,
            productMapper = mapper,
            performInstantCatalogFullSync = performInstantCatalogFullSync,
            variationMapper = mock(),
            localCatalogSyncRepository = mock(),
        )
    }

    @Test
    fun `given products in database, when fetchFirstProductsPage called, then emits products from db`() = runTest {
        // Given
        val productEntity = createProductEntity(remoteId = 123L, name = "Test Product")
        val productModel = createProductModel(remoteId = 123L, name = "Test Product")

        whenever(posLocalCatalogStore.observeProducts(any()))
            .thenReturn(flowOf(Result.success(listOf(productEntity))))
        whenever(mapper.fromEntity(productEntity))
            .thenReturn(productModel)

        // When
        val results = sut.fetchFirstProductsPage(forceRefresh = false).toList()

        // Then
        assertThat(results).hasSize(1)
        val result = results.first()
        assertThat(result).isInstanceOf(ProductsResult.Remote::class.java)

        val remoteResult = result as ProductsResult.Remote
        assertThat(remoteResult.productsResult.isSuccess).isTrue()
        assertThat(remoteResult.productsResult.getOrThrow()).containsExactly(productModel)
    }

    @Test
    fun `given products in database, when fetchFirstProductsPage called, then returns all products`() = runTest {
        // Given
        val product1 = createProductEntity(remoteId = 1L, name = "Apple iPhone", sku = "ABC123")
        val product2 = createProductEntity(remoteId = 2L, name = "Samsung Galaxy", sku = "XYZ789")
        val model1 = createProductModel(remoteId = 1L, name = "Apple iPhone", sku = "ABC123")
        val model2 = createProductModel(remoteId = 2L, name = "Samsung Galaxy", sku = "XYZ789")

        whenever(posLocalCatalogStore.observeProducts(any()))
            .thenReturn(flowOf(Result.success(listOf(product1, product2))))
        whenever(mapper.fromEntity(product1)).thenReturn(model1)
        whenever(mapper.fromEntity(product2)).thenReturn(model2)

        // When
        val results = sut.fetchFirstProductsPage(forceRefresh = false).toList()

        // Then
        assertThat(results).hasSize(1)
        val result = results.first()
        assertThat(result).isInstanceOf(ProductsResult.Remote::class.java)

        val remoteResult = result as ProductsResult.Remote
        assertThat(remoteResult.productsResult.isSuccess).isTrue()
        assertThat(remoteResult.productsResult.getOrThrow()).containsExactly(model1, model2)
    }

    @Test
    fun `given no site selected, when fetchFirstProductsPage called, then returns empty list`() = runTest {
        // Given
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // When
        val results = sut.fetchFirstProductsPage(forceRefresh = false).toList()

        // Then
        assertThat(results).hasSize(1)
        val result = results.first()
        assertThat(result).isInstanceOf(ProductsResult.Remote::class.java)

        val remoteResult = result as ProductsResult.Remote
        assertThat(remoteResult.productsResult.isSuccess).isTrue()
        assertThat(remoteResult.productsResult.getOrThrow()).isEmpty()
    }

    @Test
    fun `given db error, when fetchFirstProductsPage called, then returns empty list`() = runTest {
        // Given
        whenever(posLocalCatalogStore.observeProducts(any()))
            .thenReturn(flowOf(Result.failure(Exception("DB Error"))))

        // When
        val results = sut.fetchFirstProductsPage(forceRefresh = false).toList()

        // Then
        assertThat(results).hasSize(1)
        val result = results.first()
        assertThat(result).isInstanceOf(ProductsResult.Remote::class.java)

        val remoteResult = result as ProductsResult.Remote
        assertThat(remoteResult.productsResult.isSuccess).isTrue()
        assertThat(remoteResult.productsResult.getOrThrow()).isEmpty()
    }

    @Test
    fun `when loadMore called, then returns empty list`() = runTest {
        // When
        val result = sut.loadMoreProducts()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `when hasMorePages called, then returns false`() {
        // Then
        assertThat(sut.hasMoreProductsPages).isFalse()
    }

    private fun createProductEntity(
        remoteId: Long,
        name: String,
        sku: String = ""
    ): WooPosProductEntity {
        return WooPosProductEntity(
            localSiteId = LocalOrRemoteId.LocalId(1),
            remoteId = LocalOrRemoteId.RemoteId(remoteId),
            name = name,
            sku = sku,
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

    private fun createProductModel(
        remoteId: Long,
        name: String,
        sku: String? = null
    ): WooPosProductModel {
        return WooPosProductModel(
            remoteId = remoteId,
            parentId = 0,
            name = name,
            sku = sku ?: "",
            globalUniqueId = "",
            type = WooPosProductModel.WooPosProductType.SIMPLE,
            status = WooPosProductModel.WooPosProductStatus.PUBLISH,
            pricing = WooPosProductModel.WooPosPricing.RegularPricing(BigDecimal.TEN),
            description = "",
            shortDescription = "",
            isDownloadable = false,
            images = emptyList(),
            attributes = emptyList(),
            categories = emptyList(),
            tags = emptyList(),
            lastModified = "2024-01-01T00:00:00Z",
            variationIds = emptyList()
        )
    }
}
