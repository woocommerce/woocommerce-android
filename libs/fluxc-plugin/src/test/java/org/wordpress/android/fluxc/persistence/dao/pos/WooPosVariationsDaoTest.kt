package org.wordpress.android.fluxc.persistence.dao.pos

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
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class WooPosVariationsDaoTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: WooPosVariationsDao

    @Before
    fun setUp() {
        sut = databaseRule.db.posVariationsDao
    }

    @Test
    fun `when upserting single variation, then variation is stored`() = runTest {
        // GIVEN
        val variation = generatePosVariation(productId = 100L, variationId = 1001L)

        // WHEN
        sut.upsertVariation(variation)

        // THEN
        val storedVariation = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )
        assertNotNull(storedVariation)
        assertEquals(variation.variationName, storedVariation.variationName)
        assertEquals(variation.sku, storedVariation.sku)
        assertEquals(variation.price, storedVariation.price)
        assertEquals(variation.stockQuantity, storedVariation.stockQuantity)
    }

    @Test
    fun `given existing variation, when upserting with changes, then variation is updated`() = runTest {
        // GIVEN
        val variation = generatePosVariation(productId = 100L, variationId = 1001L)
        sut.upsertVariation(variation)

        // WHEN
        val updatedVariation = variation.copy(
            variationName = "Updated Variation",
            price = "39.99",
            stockQuantity = 15.0,
            stockStatus = "outofstock"
        )
        sut.upsertVariation(updatedVariation)

        // THEN
        val storedVariation = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )
        assertNotNull(storedVariation)
        assertEquals("Updated Variation", storedVariation.variationName)
        assertEquals("39.99", storedVariation.price)
        assertEquals(15.0, storedVariation.stockQuantity)
        assertEquals("outofstock", storedVariation.stockStatus)
    }

    @Test
    fun `when upserting multiple variations, then all variations are stored`() = runTest {
        // GIVEN
        val productId = 100L
        val variations = listOf(
            generatePosVariation(productId = productId, variationId = 1001L),
            generatePosVariation(productId = productId, variationId = 1002L),
            generatePosVariation(productId = productId, variationId = 1003L)
        )

        // WHEN
        sut.upsertVariations(variations)

        // THEN
        val storedVariations = sut.observeVariationsForProduct(
            variations.first().localSiteId.value,
            productId
        ).first()
        assertEquals(3, storedVariations.size)
        assertTrue(storedVariations.any { it.remoteVariationId.value == 1001L })
        assertTrue(storedVariations.any { it.remoteVariationId.value == 1002L })
        assertTrue(storedVariations.any { it.remoteVariationId.value == 1003L })
    }

    @Test
    fun `given variations from multiple products, when querying by product, then only product variations returned`() = runTest {
        // GIVEN
        val product1Variations = listOf(
            generatePosVariation(productId = 100L, variationId = 1001L),
            generatePosVariation(productId = 100L, variationId = 1002L)
        )
        val product2Variations = listOf(
            generatePosVariation(productId = 200L, variationId = 2001L),
            generatePosVariation(productId = 200L, variationId = 2002L)
        )
        sut.upsertVariations(product1Variations + product2Variations)

        // WHEN
        val product1Results = sut.observeVariationsForProduct(1, 100L).first()
        val product2Results = sut.observeVariationsForProduct(1, 200L).first()

        // THEN
        assertEquals(2, product1Results.size)
        assertEquals(2, product2Results.size)
        assertTrue(product1Results.all { it.remoteProductId.value == 100L })
        assertTrue(product2Results.all { it.remoteProductId.value == 200L })
    }

    @Test
    fun `given variations from multiple sites, when querying by site, then only site variations returned`() = runTest {
        // GIVEN
        val site1Variations = listOf(
            generatePosVariation(siteId = 1, productId = 100L, variationId = 1001L),
            generatePosVariation(siteId = 1, productId = 100L, variationId = 1002L)
        )
        val site2Variations = listOf(
            generatePosVariation(siteId = 2, productId = 100L, variationId = 1001L),
            generatePosVariation(siteId = 2, productId = 100L, variationId = 1003L)
        )
        sut.upsertVariations(site1Variations + site2Variations)

        // WHEN
        val site1Results = sut.observeVariationsForProduct(1, 100L).first()
        val site2Results = sut.observeVariationsForProduct(2, 100L).first()

        // THEN
        assertEquals(2, site1Results.size)
        assertEquals(2, site2Results.size)
        assertTrue(site1Results.all { it.localSiteId.value == 1 })
        assertTrue(site2Results.all { it.localSiteId.value == 2 })
    }

    @Test
    fun `when getting non-existent variation, then null is returned`() = runTest {
        // WHEN
        val result = sut.getVariation(1, 100L, 999L)

        // THEN
        assertNull(result)
    }

    @Test
    fun `given existing variation, when getting by id, then variation is returned`() = runTest {
        // GIVEN
        val variation = generatePosVariation(productId = 100L, variationId = 1001L)
        sut.upsertVariation(variation)

        // WHEN
        val result = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )

        // THEN
        assertNotNull(result)
        assertEquals(variation.remoteVariationId, result.remoteVariationId)
        assertEquals(variation.variationName, result.variationName)
        assertEquals(variation.sku, result.sku)
    }

    @Test
    fun `given variations for site, when deleting all site variations, then all are removed`() = runTest {
        // GIVEN
        val variations = listOf(
            generatePosVariation(productId = 100L, variationId = 1001L),
            generatePosVariation(productId = 100L, variationId = 1002L),
            generatePosVariation(productId = 200L, variationId = 2001L)
        )
        sut.upsertVariations(variations)

        // WHEN
        sut.deleteAllVariationsForSite(LocalId(1))

        // THEN
        val product1Results = sut.observeVariationsForProduct(1, 100L).first()
        val product2Results = sut.observeVariationsForProduct(1, 200L).first()
        assertTrue(product1Results.isEmpty())
        assertTrue(product2Results.isEmpty())
    }

    @Test
    fun `given multiple variations, when batch deleting by ids, then all specified variations are removed`() = runTest {
        // GIVEN
        val productId = 100L
        val variations = listOf(
            generatePosVariation(productId = productId, variationId = 1001L),
            generatePosVariation(productId = productId, variationId = 1002L),
            generatePosVariation(productId = productId, variationId = 1003L),
            generatePosVariation(productId = productId, variationId = 1004L)
        )
        sut.upsertVariations(variations)

        // WHEN
        sut.deleteVariationsByIds(LocalId(1), listOf(RemoteId(1001L), RemoteId(1003L)))

        // THEN
        val remainingVariations = sut.observeVariationsForProduct(1, productId).first()
        assertEquals(2, remainingVariations.size)
        assertTrue(remainingVariations.any { it.remoteVariationId.value == 1002L })
        assertTrue(remainingVariations.any { it.remoteVariationId.value == 1004L })
        assertTrue(remainingVariations.none { it.remoteVariationId.value == 1001L })
        assertTrue(remainingVariations.none { it.remoteVariationId.value == 1003L })
    }

    @Test
    fun `when batch deleting variations with empty list, then no variations are removed`() = runTest {
        // GIVEN
        val productId = 100L
        val variations = listOf(
            generatePosVariation(productId = productId, variationId = 1001L),
            generatePosVariation(productId = productId, variationId = 1002L)
        )
        sut.upsertVariations(variations)

        // WHEN
        sut.deleteVariationsByIds(LocalId(1), emptyList())

        // THEN
        val remainingVariations = sut.observeVariationsForProduct(1, productId).first()
        assertEquals(2, remainingVariations.size)
    }

    @Test
    fun `given variations from multiple sites, when batch deleting by ids for one site, then only that site variations are removed`() = runTest {
        // GIVEN
        val productId = 100L
        val site1Variations = listOf(
            generatePosVariation(siteId = 1, productId = productId, variationId = 1001L),
            generatePosVariation(siteId = 1, productId = productId, variationId = 1002L)
        )
        val site2Variations = listOf(
            generatePosVariation(siteId = 2, productId = productId, variationId = 1001L),
            generatePosVariation(siteId = 2, productId = productId, variationId = 1002L)
        )
        sut.upsertVariations(site1Variations + site2Variations)

        // WHEN
        sut.deleteVariationsByIds(LocalId(1), listOf(RemoteId(1001L), RemoteId(1002L)))

        // THEN
        val site1Results = sut.observeVariationsForProduct(1, productId).first()
        val site2Results = sut.observeVariationsForProduct(2, productId).first()
        assertTrue(site1Results.isEmpty())
        assertEquals(2, site2Results.size)
    }

    @Test
    fun `given variations for multiple products, when batch deleting for products, then all variations for those products are removed`() = runTest {
        // GIVEN
        val product1Variations = listOf(
            generatePosVariation(productId = 100L, variationId = 1001L),
            generatePosVariation(productId = 100L, variationId = 1002L)
        )
        val product2Variations = listOf(
            generatePosVariation(productId = 200L, variationId = 2001L),
            generatePosVariation(productId = 200L, variationId = 2002L)
        )
        val product3Variations = listOf(
            generatePosVariation(productId = 300L, variationId = 3001L),
            generatePosVariation(productId = 300L, variationId = 3002L)
        )
        sut.upsertVariations(product1Variations + product2Variations + product3Variations)

        // WHEN
        sut.deleteVariationsForProducts(LocalId(1), listOf(RemoteId(100L), RemoteId(300L)))

        // THEN
        val product1Results = sut.observeVariationsForProduct(1, 100L).first()
        val product2Results = sut.observeVariationsForProduct(1, 200L).first()
        val product3Results = sut.observeVariationsForProduct(1, 300L).first()
        assertTrue(product1Results.isEmpty())
        assertEquals(2, product2Results.size)
        assertTrue(product3Results.isEmpty())
    }

    @Test
    fun `when batch deleting variations for products with empty list, then no variations are removed`() = runTest {
        // GIVEN
        val variations = listOf(
            generatePosVariation(productId = 100L, variationId = 1001L),
            generatePosVariation(productId = 200L, variationId = 2001L)
        )
        sut.upsertVariations(variations)

        // WHEN
        sut.deleteVariationsForProducts(LocalId(1), emptyList())

        // THEN
        val product1Results = sut.observeVariationsForProduct(1, 100L).first()
        val product2Results = sut.observeVariationsForProduct(1, 200L).first()
        assertEquals(1, product1Results.size)
        assertEquals(1, product2Results.size)
    }

    @Test
    fun `when observing variations for product, then flow emits current variations`() = runTest {
        // GIVEN
        val productId = 100L
        val variations = listOf(
            generatePosVariation(productId = productId, variationId = 1001L, variationName = "Size M"),
            generatePosVariation(productId = productId, variationId = 1002L, variationName = "Size L")
        )
        sut.upsertVariations(variations)

        // WHEN
        val results = sut.observeVariationsForProduct(1, productId).first()

        // THEN
        assertEquals(2, results.size)
        assertTrue(results.any { it.variationName == "Size M" })
        assertTrue(results.any { it.variationName == "Size L" })
    }

    @Test
    fun `given empty database, when observing variations, then empty list is emitted`() = runTest {
        // WHEN
        val results = sut.observeVariationsForProduct(1, 100L).first()

        // THEN
        assertTrue(results.isEmpty())
    }

    @Test
    fun `given variations with decimal stock quantities, when storing, then decimal values preserved`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            stockQuantity = 7.5
        )

        // WHEN
        sut.upsertVariation(variation)

        // THEN
        val storedVariation = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )
        assertNotNull(storedVariation)
        assertEquals(7.5, storedVariation.stockQuantity)
    }

    @Test
    fun `given variations with complex attributes, when storing, then all fields are preserved`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            variationName = "Complex Variation",
            sku = "COMPLEX-VAR-SKU",
            attributesJson = """[{"id":1,"name":"Size","option":"Large"},{"id":2,"name":"Color","option":"Blue"}]""",
            imageUrl = "http://example.com/variation-image.jpg",
            description = "This is a complex variation with multiple attributes"
        )

        // WHEN
        sut.upsertVariation(variation)

        // THEN
        val storedVariation = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )
        assertNotNull(storedVariation)
        assertEquals(variation.attributesJson, storedVariation.attributesJson)
        assertEquals(variation.imageUrl, storedVariation.imageUrl)
        assertEquals(variation.description, storedVariation.description)
    }

    @Test
    fun `given variations are sorted by name, when observing product variations, then returned in order`() = runTest {
        // GIVEN
        val productId = 100L
        val variations = listOf(
            generatePosVariation(productId = productId, variationId = 1003L, variationName = "C - Large"),
            generatePosVariation(productId = productId, variationId = 1001L, variationName = "A - Small"),
            generatePosVariation(productId = productId, variationId = 1002L, variationName = "B - Medium")
        )
        sut.upsertVariations(variations)

        // WHEN
        val results = sut.observeVariationsForProduct(1, productId).first()

        // THEN
        assertEquals(3, results.size)
        assertEquals("A - Small", results[0].variationName)
        assertEquals("B - Medium", results[1].variationName)
        assertEquals("C - Large", results[2].variationName)
    }

    @Test
    fun `given variations with boolean flags, when storing, then flags are preserved`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            manageStock = true,
            backordered = false,
            downloadable = true
        )

        // WHEN
        sut.upsertVariation(variation)

        // THEN
        val storedVariation = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )
        assertNotNull(storedVariation)
        assertEquals(true, storedVariation.manageStock)
        assertEquals(false, storedVariation.backordered)
        assertEquals(true, storedVariation.downloadable)
    }

    @Test
    fun `given variations with different types, when observing, then only variation type is returned`() = runTest {
        // GIVEN
        val productId = 100L
        val variations = listOf(
            generatePosVariation(productId = productId, variationId = 1001L, type = "variation"),
            generatePosVariation(productId = productId, variationId = 1002L, type = "subscription_variation"),
            generatePosVariation(productId = productId, variationId = 1003L, type = "variation")
        )
        sut.upsertVariations(variations)

        // WHEN
        val results = sut.observeVariationsForProduct(1, productId).first()

        // THEN
        assertEquals(2, results.size)
        assertTrue(results.all { it.type == "variation" })
        assertTrue(results.any { it.remoteVariationId.value == 1001L })
        assertTrue(results.any { it.remoteVariationId.value == 1003L })
        assertTrue(results.none { it.remoteVariationId.value == 1002L })
    }

    @Test
    fun `given subscription_variation type, when getting by id, then null is returned`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            type = "subscription_variation"
        )
        sut.upsertVariation(variation)

        // WHEN
        val result = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )

        // THEN
        assertNull(result)
    }

    @Test
    fun `given variation type, when getting by id, then variation is returned`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            type = "variation"
        )
        sut.upsertVariation(variation)

        // WHEN
        val result = sut.getVariation(
            variation.localSiteId.value,
            variation.remoteProductId.value,
            variation.remoteVariationId.value
        )

        // THEN
        assertNotNull(result)
        assertEquals("variation", result.type)
    }

    @Test
    fun `given subscription_variation type, when finding by identifier, then null is returned`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            globalUniqueId = "test-identifier",
            type = "subscription_variation"
        )
        sut.upsertVariation(variation)

        // WHEN
        val result = sut.findVariationByIdentifier(LocalId(1), "test-identifier")

        // THEN
        assertNull(result)
    }

    @Test
    fun `given variation type, when finding by identifier, then variation is returned`() = runTest {
        // GIVEN
        val variation = generatePosVariation(
            productId = 100L,
            variationId = 1001L,
            globalUniqueId = "test-identifier",
            type = "variation"
        )
        sut.upsertVariation(variation)

        // WHEN
        val result = sut.findVariationByIdentifier(LocalId(1), "test-identifier")

        // THEN
        assertNotNull(result)
        assertEquals("variation", result.type)
    }

    private fun generatePosVariation(
        siteId: Int = 1,
        productId: Long = 1L,
        variationId: Long = 1L,
        dateModified: String = "2024-01-01T10:00:00Z",
        sku: String = "VAR-SKU-$variationId",
        globalUniqueId: String = "gid://wordpress/ProductVariation/$variationId",
        variationName: String = "Test Variation $variationId",
        price: String = "29.99",
        regularPrice: String = "29.99",
        salePrice: String = "",
        description: String = "Variation description",
        stockQuantity: Double = 10.0,
        stockStatus: String = "instock",
        manageStock: Boolean = true,
        backordered: Boolean = false,
        attributesJson: String = """[{"id":1,"name":"Size","option":"Medium"}]""",
        imageUrl: String = "",
        status: String = "publish",
        lastUpdated: String = "2024-01-01T10:00:00Z",
        downloadable: Boolean = false,
        type: String = "variation"
    ) = WooPosVariationEntity(
        localSiteId = LocalId(siteId),
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        dateModified = dateModified,
        sku = sku,
        globalUniqueId = globalUniqueId,
        variationName = variationName,
        price = price,
        regularPrice = regularPrice,
        salePrice = salePrice,
        description = description,
        stockQuantity = stockQuantity,
        stockStatus = stockStatus,
        manageStock = manageStock,
        backordered = backordered,
        attributesJson = attributesJson,
        imageUrl = imageUrl,
        status = status,
        lastUpdated = lastUpdated,
        downloadable = downloadable,
        type = type
    )
}
