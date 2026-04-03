package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity

@RunWith(RobolectricTestRunner::class)
class WooPosSearchableFtsDaoTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: WooPosSearchableFtsDao

    @Before
    fun setUp() {
        sut = databaseRule.db.posSearchableFtsDao
    }

    @Test
    fun `when searching by product name, then matching products are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Blue T-Shirt"),
            createProduct(itemId = "2", name = "Red Hoodie"),
            createProduct(itemId = "3", name = "Green T-Shirt"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "shirt*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(2)
        assertThat(results.map { it.itemId }).containsExactlyInAnyOrder("1", "3")
    }

    @Test
    fun `when searching by SKU, then matching products are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Product A", sku = "SKU-ABC-123"),
            createProduct(itemId = "2", name = "Product B", sku = "SKU-XYZ-456"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "ABC*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("1")
    }

    @Test
    fun `when searching by barcode, then matching products are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Product A", barcode = "1234567890"),
            createProduct(itemId = "2", name = "Product B", barcode = "0987654321"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "123456*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("1")
    }

    @Test
    fun `when searching by attribute values, then matching variations are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createVariation(itemId = "10", parentProductId = "1", name = "T-Shirt", attributeValues = "blue medium"),
            createVariation(itemId = "11", parentProductId = "1", name = "T-Shirt", attributeValues = "red large"),
            createVariation(itemId = "12", parentProductId = "1", name = "T-Shirt", attributeValues = "blue small"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "blue*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(2)
        assertThat(results.map { it.itemId }).containsExactlyInAnyOrder("10", "12")
    }

    @Test
    fun `given product with hyphenated name, when searching, then individual tokens match`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "SHIRT-1234", sku = "A-B-C"),
        )
        sut.insertAll(entries)

        // WHEN
        val shirtResults = sut.search(LOCAL_SITE_ID, "shirt*", limit = 10, offset = 0)
        val numberResults = sut.search(LOCAL_SITE_ID, "1234*", limit = 10, offset = 0)
        val letterResults = sut.search(LOCAL_SITE_ID, "B*", limit = 10, offset = 0)

        // THEN
        assertThat(shirtResults).hasSize(1)
        assertThat(numberResults).hasSize(1)
        assertThat(letterResults).hasSize(1)
    }

    @Test
    fun `when searching with multiple terms, then only entries matching all terms are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createVariation(itemId = "1", parentProductId = "100", name = "Gant T-Shirt", attributeValues = "blue medium"),
            createVariation(itemId = "2", parentProductId = "100", name = "Gant T-Shirt", attributeValues = "red medium"),
            createVariation(itemId = "3", parentProductId = "101", name = "Nike Hoodie", attributeValues = "blue large"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "gant* blue*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("1")
    }

    @Test
    fun `when deleting all for site, then all entries for that site are removed`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(siteId = "1", itemId = "1", name = "Product A"),
            createProduct(siteId = "1", itemId = "2", name = "Product B"),
            createProduct(siteId = "2", itemId = "3", name = "Product C"),
        )
        sut.insertAll(entries)

        // WHEN
        sut.deleteAllForSite("1")

        // THEN
        val site1Results = sut.search("1", "product*", limit = 10, offset = 0)
        val site2Results = sut.search("2", "product*", limit = 10, offset = 0)
        assertThat(site1Results).isEmpty()
        assertThat(site2Results).hasSize(1)
    }

    @Test
    fun `when deleting products, then only products are removed and variations remain`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Product One"),
            createVariation(itemId = "10", parentProductId = "1", name = "Product One", attributeValues = "blue"),
            createProduct(itemId = "2", name = "Product Two"),
        )
        sut.insertAll(entries)

        // WHEN
        sut.deleteProducts(LOCAL_SITE_ID, listOf("1", "2"))

        // THEN
        val results = sut.search(LOCAL_SITE_ID, "product* OR one* OR two* OR blue*", limit = 10, offset = 0)
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("10")
    }

    @Test
    fun `when deleting variations, then only variations are removed and products remain`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Product One"),
            createVariation(itemId = "10", parentProductId = "1", name = "Product One", attributeValues = "blue"),
            createVariation(itemId = "11", parentProductId = "1", name = "Product One", attributeValues = "red"),
        )
        sut.insertAll(entries)

        // WHEN
        sut.deleteVariations(LOCAL_SITE_ID, listOf("10", "11"))

        // THEN
        val results = sut.search(LOCAL_SITE_ID, "product* OR one* OR blue* OR red*", limit = 10, offset = 0)
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("1")
    }

    @Test
    fun `when paginating search results, then correct pages are returned`() = runTest {
        // GIVEN
        val entries = (1..10).map { i ->
            createProduct(itemId = "$i", name = "Product $i")
        }
        sut.insertAll(entries)

        // WHEN
        val page1 = sut.search(LOCAL_SITE_ID, "product*", limit = 3, offset = 0)
        val page2 = sut.search(LOCAL_SITE_ID, "product*", limit = 3, offset = 3)
        val page3 = sut.search(LOCAL_SITE_ID, "product*", limit = 3, offset = 6)
        val page4 = sut.search(LOCAL_SITE_ID, "product*", limit = 3, offset = 9)

        // THEN
        assertThat(page1).hasSize(3)
        assertThat(page2).hasSize(3)
        assertThat(page3).hasSize(3)
        assertThat(page4).hasSize(1)
    }

    @Test
    fun `when searching with different cases, then results are case insensitive`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "UPPERCASE PRODUCT"),
            createProduct(itemId = "2", name = "lowercase product"),
            createProduct(itemId = "3", name = "MixedCase Product"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "product*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(3)
    }

    @Test
    fun `when searching, then both products and variations are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Gant T-Shirt"),
            createVariation(itemId = "10", parentProductId = "1", name = "Gant T-Shirt", attributeValues = "blue small"),
            createVariation(itemId = "11", parentProductId = "1", name = "Gant T-Shirt", attributeValues = "blue medium"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "gant*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(3)
        assertThat(results.map { it.itemId }).containsExactlyInAnyOrder("1", "10", "11")
    }

    @Test
    fun `given 1 product inserted twice, when searching, then 2 duplicates are returned`() = runTest {
        // GIVEN
        val product = createProduct(itemId = "1", name = "Blue Shirt")
        sut.insertAll(listOf(product))
        sut.insertAll(listOf(product))

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "shirt*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(2)
    }

    @Test
    fun `when searching for non-existent term, then empty list is returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Blue Shirt"),
            createProduct(itemId = "2", name = "Red Hoodie"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "pants*", limit = 10, offset = 0)

        // THEN
        assertThat(results).isEmpty()
    }

    @Test
    fun `when searching empty table, then empty list is returned`() = runTest {
        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "shirt*", limit = 10, offset = 0)

        // THEN
        assertThat(results).isEmpty()
    }

    @Test
    fun `given 1 product with empty sku and barcode, when searching by name, then product is found`() = runTest {
        // GIVEN
        val product = WooPosSearchableFtsEntity(
            localSiteId = LOCAL_SITE_ID,
            itemId = "1",
            parentProductId = "",
            name = "Simple Product",
            sku = "",
            barcode = "",
            attributeValues = "",
        )
        sut.insertAll(listOf(product))

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "simple*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("1")
    }

    @Test
    fun `when searching for different site, then no results from other sites are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(siteId = "1", itemId = "1", name = "Blue Shirt"),
            createProduct(siteId = "2", itemId = "2", name = "Blue Pants"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search("1", "blue*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("1")
    }

    @Test
    fun `given 2 products with unicode characters, when searching normalized term, then products are found`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Café Latte"),
            createProduct(itemId = "2", name = "Naïve Art Print"),
        )
        sut.insertAll(entries)

        // WHEN
        val cafeResults = sut.search(LOCAL_SITE_ID, "cafe*", limit = 10, offset = 0)
        val naiveResults = sut.search(LOCAL_SITE_ID, "naive*", limit = 10, offset = 0)

        // THEN
        assertThat(cafeResults).hasSize(1)
        assertThat(naiveResults).hasSize(1)
    }

    @Test
    fun `given 3 products with 2 deleted, when searching, then only 1 product is returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Blue Shirt"),
            createProduct(itemId = "2", name = "Blue Pants"),
            createProduct(itemId = "3", name = "Blue Hat"),
        )
        sut.insertAll(entries)
        sut.deleteProducts(LOCAL_SITE_ID, listOf("1", "2"))

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "blue*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results[0].itemId).isEqualTo("3")
    }

    @Test
    fun `when deleting non-existent products, then no error occurs`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Blue Shirt"),
        )
        sut.insertAll(entries)

        // WHEN
        sut.deleteProducts(LOCAL_SITE_ID, listOf("999", "888"))

        // THEN
        val results = sut.search(LOCAL_SITE_ID, "shirt*", limit = 10, offset = 0)
        assertThat(results).hasSize(1)
    }

    @Test
    fun `when searching with OR operator, then entries matching any term are returned`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Blue Shirt"),
            createProduct(itemId = "2", name = "Red Pants"),
            createProduct(itemId = "3", name = "Green Hat"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "shirt* OR pants*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(2)
        assertThat(results.map { it.itemId }).containsExactlyInAnyOrder("1", "2")
    }

    @Test
    fun `when searching, then products are ranked before variations`() = runTest {
        // GIVEN
        val entries = listOf(
            createVariation(itemId = "10", parentProductId = "1", name = "T-Shirt", attributeValues = "blue"),
            createProduct(itemId = "1", name = "T-Shirt"),
            createVariation(itemId = "11", parentProductId = "1", name = "T-Shirt", attributeValues = "red"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "shirt*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(3)
        assertThat(results[0].itemId).isEqualTo("1")
        assertThat(results[0].parentProductId).isEmpty()
    }

    @Test
    fun `when searching, then shorter names are ranked first`() = runTest {
        // GIVEN
        val entries = listOf(
            createProduct(itemId = "1", name = "Blue T-Shirt Extra Long Name"),
            createProduct(itemId = "2", name = "Blue T-Shirt"),
            createProduct(itemId = "3", name = "Blue"),
        )
        sut.insertAll(entries)

        // WHEN
        val results = sut.search(LOCAL_SITE_ID, "blue*", limit = 10, offset = 0)

        // THEN
        assertThat(results).hasSize(3)
        assertThat(results.map { it.itemId }).containsExactly("3", "2", "1")
    }

    private fun createProduct(
        siteId: String = LOCAL_SITE_ID,
        itemId: String,
        name: String = "Test Product",
        sku: String = "SKU-$itemId",
        barcode: String = "BARCODE-$itemId",
        attributeValues: String = "",
    ) = WooPosSearchableFtsEntity(
        localSiteId = siteId,
        itemId = itemId,
        parentProductId = "",
        name = name,
        sku = sku,
        barcode = barcode,
        attributeValues = attributeValues,
    )

    private fun createVariation(
        siteId: String = LOCAL_SITE_ID,
        itemId: String,
        parentProductId: String,
        name: String,
        sku: String = "VAR-SKU-$itemId",
        barcode: String = "VAR-BARCODE-$itemId",
        attributeValues: String,
    ) = WooPosSearchableFtsEntity(
        localSiteId = siteId,
        itemId = itemId,
        parentProductId = parentProductId,
        name = name,
        sku = sku,
        barcode = barcode,
        attributeValues = attributeValues,
    )

    companion object {
        private const val LOCAL_SITE_ID = "1"
    }
}
