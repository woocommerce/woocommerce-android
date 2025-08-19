package org.wordpress.android.fluxc.persistence.dao.pos

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductModel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PosProductsDaoTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    private lateinit var sut: PosProductsDao

    @Before
    fun setUp() {
        sut = databaseRule.db.posProductsDao
    }

    @Test
    fun `when upserting single product, then product is stored`() = runTest {
        // GIVEN
        val product = generatePosProduct(remoteId = 100L)

        // WHEN
        sut.upsertProducts(listOf(product))

        // THEN
        val storedProduct = sut.getProduct(product.localSiteId.value, product.remoteId.value)
        assertNotNull(storedProduct)
        assertEquals(product.name, storedProduct.name)
        assertEquals(product.sku, storedProduct.sku)
        assertEquals(product.price, storedProduct.price)
    }

    @Test
    fun `given existing product, when upserting with changes, then product is updated`() = runTest {
        // GIVEN
        val product = generatePosProduct(remoteId = 100L)
        sut.upsertProducts(listOf(product))

        // WHEN
        val updatedProduct = product.copy(
            name = "Updated Product",
            price = "29.99",
            stockQuantity = 5.0
        )
        sut.upsertProducts(listOf(updatedProduct))

        // THEN
        val storedProduct = sut.getProduct(product.localSiteId.value, product.remoteId.value)
        assertNotNull(storedProduct)
        assertEquals("Updated Product", storedProduct.name)
        assertEquals("29.99", storedProduct.price)
        assertEquals(5.0, storedProduct.stockQuantity)
    }

    @Test
    fun `when upserting multiple products, then all products are stored`() = runTest {
        // GIVEN
        val products = listOf(
            generatePosProduct(remoteId = 100L),
            generatePosProduct(remoteId = 101L),
            generatePosProduct(remoteId = 102L)
        )

        // WHEN
        sut.upsertProducts(products)

        // THEN
        val storedProducts = sut.observeAllProducts(products.first().localSiteId.value).first()
        assertEquals(3, storedProducts.size)
        assertTrue(storedProducts.any { it.remoteId.value == 100L })
        assertTrue(storedProducts.any { it.remoteId.value == 101L })
        assertTrue(storedProducts.any { it.remoteId.value == 102L })
    }

    @Test
    fun `given products from multiple sites, when querying by site, then only site products returned`() = runTest {
        // GIVEN
        val site1Products = listOf(
            generatePosProduct(siteId = 1, remoteId = 100L),
            generatePosProduct(siteId = 1, remoteId = 101L)
        )
        val site2Products = listOf(
            generatePosProduct(siteId = 2, remoteId = 100L),
            generatePosProduct(siteId = 2, remoteId = 102L)
        )
        sut.upsertProducts(site1Products + site2Products)

        // WHEN
        val site1Results = sut.observeAllProducts(1).first()
        val site2Results = sut.observeAllProducts(2).first()

        // THEN
        assertEquals(2, site1Results.size)
        assertEquals(2, site2Results.size)
        assertTrue(site1Results.all { it.localSiteId.value == 1 })
        assertTrue(site2Results.all { it.localSiteId.value == 2 })
    }

    @Test
    fun `when getting non-existent product, then null is returned`() = runTest {
        // WHEN
        val result = sut.getProduct(1, 999L)

        // THEN
        assertNull(result)
    }

    @Test
    fun `given existing product, when getting by id, then product is returned`() = runTest {
        // GIVEN
        val product = generatePosProduct(remoteId = 100L)
        sut.upsertProducts(listOf(product))

        // WHEN
        val result = sut.getProduct(product.localSiteId.value, product.remoteId.value)

        // THEN
        assertNotNull(result)
        assertEquals(product.remoteId, result.remoteId)
        assertEquals(product.name, result.name)
        assertEquals(product.sku, result.sku)
    }

    @Test
    fun `given existing product, when deleting product, then product is removed`() = runTest {
        // GIVEN
        val product = generatePosProduct(remoteId = 100L)
        sut.upsertProducts(listOf(product))

        // WHEN
        sut.deleteProduct(product.localSiteId.value, product.remoteId.value)

        // THEN
        val result = sut.getProduct(product.localSiteId.value, product.remoteId.value)
        assertNull(result)
    }

    @Test
    fun `given multiple products, when deleting one product, then only that product is removed`() = runTest {
        // GIVEN
        val products = listOf(
            generatePosProduct(remoteId = 100L),
            generatePosProduct(remoteId = 101L),
            generatePosProduct(remoteId = 102L)
        )
        sut.upsertProducts(products)

        // WHEN
        sut.deleteProduct(products.first().localSiteId.value, 101L)

        // THEN
        val remainingProducts = sut.observeAllProducts(products.first().localSiteId.value).first()
        assertEquals(2, remainingProducts.size)
        assertTrue(remainingProducts.any { it.remoteId.value == 100L })
        assertTrue(remainingProducts.any { it.remoteId.value == 102L })
        assertTrue(remainingProducts.none { it.remoteId.value == 101L })
    }

    @Test
    fun `when deleting non-existent product, then no error occurs`() = runTest {
        // WHEN & THEN - Should not throw exception
        sut.deleteProduct(1, 999L)
    }

    @Test
    fun `when observing products, then flow emits current products`() = runTest {
        // GIVEN
        val products = listOf(
            generatePosProduct(remoteId = 100L, name = "Product A"),
            generatePosProduct(remoteId = 101L, name = "Product B")
        )
        sut.upsertProducts(products)

        // WHEN
        val results = sut.observeAllProducts(products.first().localSiteId.value).first()

        // THEN
        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "Product A" })
        assertTrue(results.any { it.name == "Product B" })
    }

    @Test
    fun `given empty database, when observing products, then empty list is emitted`() = runTest {
        // WHEN
        val results = sut.observeAllProducts(1).first()

        // THEN
        assertTrue(results.isEmpty())
    }

    @Test
    fun `given products with decimal stock quantities, when storing, then decimal values preserved`() = runTest {
        // GIVEN
        val product = generatePosProduct(
            remoteId = 100L,
            stockQuantity = 4.5
        )

        // WHEN
        sut.upsertProducts(listOf(product))

        // THEN
        val storedProduct = sut.getProduct(product.localSiteId.value, product.remoteId.value)
        assertNotNull(storedProduct)
        assertEquals(4.5, storedProduct.stockQuantity)
    }

    @Test
    fun `given products with complex data, when storing, then all fields are preserved`() = runTest {
        // GIVEN
        val product = generatePosProduct(
            remoteId = 100L,
            name = "Complex Product",
            sku = "COMPLEX-SKU",
            categories = """[{"id":1,"name":"Electronics"}]""",
            tags = """[{"id":1,"name":"Sale"}]""",
            attributes = """[{"id":1,"name":"Color","options":["Red","Blue"]}]""",
            images = """[{"id":1,"src":"http://example.com/image.jpg"}]"""
        )

        // WHEN
        sut.upsertProducts(listOf(product))

        // THEN
        val storedProduct = sut.getProduct(product.localSiteId.value, product.remoteId.value)
        assertNotNull(storedProduct)
        assertEquals(product.categories, storedProduct.categories)
        assertEquals(product.tags, storedProduct.tags)
        assertEquals(product.attributes, storedProduct.attributes)
        assertEquals(product.images, storedProduct.images)
    }

    private fun generatePosProduct(
        siteId: Int = 1,
        remoteId: Long = 1L,
        name: String = "Test Product $remoteId",
        sku: String = "SKU-$remoteId",
        globalUniqueId: String = "gid://wordpress/Product/$remoteId",
        type: String = "simple",
        price: String = "19.99",
        status: String = "publish",
        stockStatus: String = "instock",
        stockQuantity: Double = 10.0,
        categories: String = "",
        tags: String = "",
        attributes: String = "",
        images: String = "",
        dateModified: String = "2024-01-01T10:00:00Z"
    ) = WCPosProductModel(
        localSiteId = LocalId(siteId),
        remoteId = RemoteId(remoteId),
        name = name,
        sku = sku,
        globalUniqueId = globalUniqueId,
        type = type,
        price = price,
        downloadable = false,
        images = images,
        attributes = attributes,
        parentId = 0L,
        status = status,
        regularPrice = price,
        salePrice = "",
        onSale = false,
        description = "Product description",
        shortDescription = "Short desc",
        manageStock = true,
        stockQuantity = stockQuantity,
        stockStatus = stockStatus,
        backordersAllowed = false,
        backordered = false,
        categories = categories,
        tags = tags,
        dateModified = dateModified
    )
}
