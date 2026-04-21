package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooPosProductsInMemoryCacheTest {

    private lateinit var cache: WooPosProductsInMemoryCache

    @Before
    fun setup() {
        cache = WooPosProductsInMemoryCache()
    }

    @Test
    fun `when cache is empty, getAll returns empty list`() = runTest {
        // WHEN
        val result = cache.getAll()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `when product is added, getAll returns all products`() = runTest {
        // GIVEN
        val products = listOf(createTestProduct(1), createTestProduct(2))

        // WHEN
        cache.addAll(products)
        val result = cache.getAll()

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyInAnyOrderElementsOf(products)
    }

    @Test
    fun `when product is added, getProductById returns correct product`() = runTest {
        // GIVEN
        val product1 = createTestProduct(1)
        val product2 = createTestProduct(2)
        cache.addAll(listOf(product1, product2))

        // WHEN
        val result = cache.getProductById(1)

        // THEN
        assertThat(result).isEqualTo(product1)
    }

    @Test
    fun `when product is not in cache, getProductById returns null`() = runTest {
        // GIVEN
        val product = createTestProduct(1)
        cache.addAll(listOf(product))

        // WHEN
        val result = cache.getProductById(999)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `when cache is cleared, getAll returns empty list`() = runTest {
        // GIVEN
        val products = listOf(createTestProduct(1), createTestProduct(2))
        cache.addAll(products)

        // WHEN
        cache.clear()
        val result = cache.getAll()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `when adding product with existing id, it should replace old product`() = runTest {
        // GIVEN
        val product1 = createTestProduct(1, "Product 1")
        val product1Updated = createTestProduct(1, "Product 1 Updated")

        // WHEN
        cache.addAll(listOf(product1))
        cache.addAll(listOf(product1Updated))
        val result = cache.getProductById(1)

        // THEN
        assertThat(result).isEqualTo(product1Updated)
        assertThat(result?.name).isEqualTo("Product 1 Updated")
    }

    @Test
    fun `when adding more products than max cache size, oldest products should be removed`() = runTest {
        // GIVEN
        val products = (1..10005L).map { createTestProduct(it) }

        // WHEN
        cache.addAll(products)
        val firstProduct = cache.getProductById(1)
        val lastProduct = cache.getProductById(10005)

        // THEN
        // The first products should have been removed to maintain the cache size limit
        assertThat(firstProduct).isNull()
        assertThat(lastProduct).isNotNull
        assertThat(cache.getAll()).hasSize(2_000) // MAX_CACHE_SIZE from the implementation
    }

    @Test
    fun `when multiple threads access cache concurrently, data remains consistent`() = runTest {
        // GIVEN
        val initialProducts = (1..100L).map { createTestProduct(it) }
        cache.addAll(initialProducts)

        // WHEN - simulate concurrent access from multiple coroutines
        val concurrentOperations = (101..200L).map { id ->
            async {
                val product = createTestProduct(id)
                cache.addAll(listOf(product))
                cache.getProductById(id)
            }
        }

        // THEN - all operations should complete without errors and data should be consistent
        val results = concurrentOperations.awaitAll()
        assertThat(results).hasSize(100)
        assertThat(results).doesNotContainNull()

        // Verify the cache contains all products
        val allCachedProducts = cache.getAll()
        assertThat(allCachedProducts).hasSize(200)
    }

    private fun createTestProduct(
        id: Long,
        name: String = "Test Product $id"
    ): WooPosProductModel =
        WooPosProductModel(
            remoteId = id,
            parentId = null,
            name = name,
            sku = "SKU-$id",
            pricing = WooPosProductModel.WooPosPricing.RegularPricing(price = BigDecimal.TEN),
            globalUniqueId = "global-$id",
            type = WooPosProductModel.WooPosProductType.Simple,
            status = WooPosProductModel.WooPosProductStatus.PUBLISH,
            description = "Description for $name",
            shortDescription = "Short description for $name",
            isDownloadable = false,
            lastModified = "2020-01-01T00:00:00Z",
        )
}
