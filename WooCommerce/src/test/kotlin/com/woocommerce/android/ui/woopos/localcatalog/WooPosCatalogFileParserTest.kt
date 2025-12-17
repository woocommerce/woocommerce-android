package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import java.io.File

@ExperimentalCoroutinesApi
class WooPosCatalogFileParserTest : BaseUnitTest() {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var sut: WooPosCatalogFileParser
    private val logger: WooPosLogWrapper = mock()
    private val gson = Gson()
    private val localSiteId = LocalOrRemoteId.LocalId(1)

    @Before
    fun setup() {
        sut = WooPosCatalogFileParser(
            logger = logger,
            gson = gson,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when parsing simple product, then returns correct product data`() = testBlocking {
        // GIVEN
        val file = createTestFile("simple.json", WooPosCatalogFileParserTestData.SIMPLE_PRODUCT_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isSuccess).isTrue
        val data = result.getOrNull()!!
        assertThat(data.products).hasSize(1)
        assertThat(data.variations).isEmpty()
    }

    @Test
    fun `when parsing simple product, then product has correct attributes`() = testBlocking {
        // GIVEN
        val file = createTestFile("simple.json", WooPosCatalogFileParserTestData.SIMPLE_PRODUCT_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        val product = result.getOrNull()!!.products[0]
        assertThat(product.remoteId.value).isEqualTo(123)
        assertThat(product.name).isEqualTo("Test Product")
        assertThat(product.price).isEqualTo("19.99")
        assertThat(product.sku).isEqualTo("TEST-SKU-001")
        assertThat(product.stockStatus).isEqualTo("instock")
        assertThat(product.stockQuantity).isEqualTo(100.0)
    }

    @Test
    fun `when parsing variable product with variations, then returns all products and variations`() = testBlocking {
        // GIVEN
        val file = createTestFile(
            "variable.json",
            WooPosCatalogFileParserTestData.VARIABLE_PRODUCT_WITH_VARIATIONS_JSON
        )

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isSuccess).isTrue
        val data = result.getOrNull()!!
        assertThat(data.products).hasSize(1)
        assertThat(data.variations).hasSize(2)
    }

    @Test
    fun `when parsing variable product, then product has correct attributes`() = testBlocking {
        // GIVEN
        val file = createTestFile(
            "variable.json",
            WooPosCatalogFileParserTestData.VARIABLE_PRODUCT_WITH_VARIATIONS_JSON
        )

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        val product = result.getOrNull()!!.products[0]
        assertThat(product.remoteId.value).isEqualTo(200)
        assertThat(product.name).isEqualTo("Variable T-Shirt")
        assertThat(product.sku).isEqualTo("VAR-SHIRT")
    }

    @Test
    fun `when parsing variations, then variations have correct attributes`() = testBlocking {
        // GIVEN
        val file = createTestFile(
            "variable.json",
            WooPosCatalogFileParserTestData.VARIABLE_PRODUCT_WITH_VARIATIONS_JSON
        )

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        val variations = result.getOrNull()!!.variations

        val variation1 = variations[0]
        assertThat(variation1.remoteVariationId.value).isEqualTo(301)
        assertThat(variation1.remoteProductId.value).isEqualTo(200)
        assertThat(variation1.sku).isEqualTo("VAR-SHIRT-S")
        assertThat(variation1.stockQuantity).isEqualTo(50.0)

        val variation2 = variations[1]
        assertThat(variation2.remoteVariationId.value).isEqualTo(302)
        assertThat(variation2.remoteProductId.value).isEqualTo(200)
        assertThat(variation2.sku).isEqualTo("VAR-SHIRT-M")
        assertThat(variation2.stockQuantity).isEqualTo(75.0)
    }

    @Test
    fun `when parsing mixed product types, then separates products by type correctly`() = testBlocking {
        // GIVEN
        val file = createTestFile("mixed.json", WooPosCatalogFileParserTestData.MIXED_PRODUCT_TYPES_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        val data = result.getOrNull()!!
        assertThat(data.products).hasSize(3)
        assertThat(data.variations).hasSize(1)

        val simpleProducts = data.products.filter { it.type == "simple" }
        assertThat(simpleProducts).hasSize(2)
        assertThat(simpleProducts.map { it.remoteId.value }).containsExactlyInAnyOrder(1001L, 1002L)

        val variableProduct = data.products.find { it.type == "variable" }
        assertThat(variableProduct?.remoteId?.value).isEqualTo(2001)
    }

    @Test
    fun `when parsing unknown product type, then skips unknown items`() = testBlocking {
        // GIVEN
        val file = createTestFile("unknown.json", WooPosCatalogFileParserTestData.WITH_UNKNOWN_TYPE_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        val data = result.getOrNull()!!
        assertThat(data.products).hasSize(1)
        assertThat(data.variations).hasSize(1)
        assertThat(data.products[0].remoteId.value).isEqualTo(100)
        assertThat(data.variations[0].remoteVariationId.value).isEqualTo(200)
    }

    @Test
    fun `when parsing empty catalog, then returns empty collections`() = testBlocking {
        // GIVEN
        val file = createTestFile("empty.json", WooPosCatalogFileParserTestData.EMPTY_CATALOG_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isSuccess).isTrue
        val data = result.getOrNull()!!
        assertThat(data.products).isEmpty()
        assertThat(data.variations).isEmpty()
    }

    @Test
    fun `when parsing malformed json, then returns failure`() = testBlocking {
        // GIVEN
        val file = createTestFile("malformed.json", WooPosCatalogFileParserTestData.MALFORMED_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isFailure).isTrue
    }

    @Test
    fun `when parsing item without data field, then skips invalid item`() = testBlocking {
        // GIVEN
        val file = createTestFile("missing_data.json", WooPosCatalogFileParserTestData.MISSING_DATA_FIELD_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isSuccess).isTrue
        val data = result.getOrNull()!!
        assertThat(data.products).hasSize(1)
        assertThat(data.products[0].remoteId.value).isEqualTo(123)
    }

    @Test
    fun `when parsing item without type field, then skips invalid item`() = testBlocking {
        // GIVEN
        val file = createTestFile("missing_type.json", WooPosCatalogFileParserTestData.MISSING_TYPE_FIELD_JSON)

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isSuccess).isTrue
        val data = result.getOrNull()!!
        assertThat(data.products).hasSize(1)
        assertThat(data.products[0].remoteId.value).isEqualTo(123)
    }

    @Test
    fun `when file does not exist, then returns failure`() = testBlocking {
        // GIVEN
        val file = File("/non/existent/file.json")

        // WHEN
        val result = sut.parseCatalogFile(file, localSiteId)

        // THEN
        assertThat(result.isFailure).isTrue
    }

    private fun createTestFile(fileName: String, content: String): File {
        val file = temporaryFolder.newFile(fileName)
        file.writeText(content)
        return file
    }
}
