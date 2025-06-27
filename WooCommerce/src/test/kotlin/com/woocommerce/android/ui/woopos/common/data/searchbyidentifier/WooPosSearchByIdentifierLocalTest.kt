package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import kotlin.test.assertTrue

class WooPosSearchByIdentifierLocalTest {

    private lateinit var sut: WooPosSearchByIdentifierLocal
    private val productsCache: WooPosProductsCache = mock {
        onBlocking { getProductById(any()) }.thenReturn(ProductTestUtils.generateWCProductModel().toAppModel())
    }
    private val variationsCache: WooPosVariationsLRUCache = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierLocal(productsCache, variationsCache)
    }

    @Test
    fun `given product with matching global unique id, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "1234567890123"
        val product = createProduct(globalUniqueId = identifier)
        whenever(productsCache.getAll()).thenReturn(listOf(product))
        whenever(variationsCache.getAll()).thenReturn(emptyList<ProductVariation>())

        // WHEN
        val result = sut(identifier)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Success(product), result)
    }

    @Test
    fun `given no matching products, when search called, then return failure`() = runTest {
        // GIVEN
        val identifier = "NOTFOUND"
        whenever(productsCache.getAll()).thenReturn(emptyList())
        whenever(variationsCache.getAll()).thenReturn(emptyList<ProductVariation>())

        // WHEN
        val result = sut(identifier)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound), result)
    }

    @Test
    fun `given product with lowercase global unique id, when search with uppercase, then return product`() = runTest {
        // GIVEN
        val identifier = "ABC123"
        val product = createProduct(globalUniqueId = "abc123")
        whenever(productsCache.getAll()).thenReturn(listOf(product))
        whenever(variationsCache.getAll()).thenReturn(emptyList<ProductVariation>())

        // WHEN
        val result = sut(identifier)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Success(product), result)
    }

    @Test
    fun `given variation with matching global unique id, when search called, then return variation`() = runTest {
        // GIVEN
        val identifier = "VAR123456"
        val productId = 1L
        val variationId = 10L
        val product = createProduct(remoteId = productId).copy(type = ProductType.VARIABLE.value)
        val variation: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(variationId)
            on { remoteProductId }.thenReturn(productId)
            on { globalUniqueId }.thenReturn(identifier)
            on { sku }.thenReturn("")
        }
        whenever(productsCache.getAll()).thenReturn(listOf(product))
        whenever(productsCache.getProductById(productId)).thenReturn(product)
        whenever(variationsCache.getAll()).thenReturn(listOf(variation))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        assertEquals(variation, result.variation)
        assertEquals(product, result.parentProduct)
    }

    @Test
    fun `given multiple variations with one matching, when search called, then return correct variation`() = runTest {
        // GIVEN
        val identifier = "MATCH-VAR"
        val productId = 1L
        val product = createProduct(remoteId = productId).copy(type = ProductType.VARIABLE.value)
        val variation1: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(10L)
            on { remoteProductId }.thenReturn(productId)
            on { globalUniqueId }.thenReturn("OTHER-VAR")
            on { sku }.thenReturn("OTHER-SKU")
        }
        val variation2: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(20L)
            on { remoteProductId }.thenReturn(productId)
            on { globalUniqueId }.thenReturn(identifier)
            on { sku }.thenReturn("")
        }
        whenever(productsCache.getAll()).thenReturn(listOf(product))
        whenever(productsCache.getProductById(productId)).thenReturn(product)
        whenever(variationsCache.getAll()).thenReturn(listOf(variation1, variation2))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        assertEquals(variation2, result.variation)
        assertEquals(product, result.parentProduct)
    }

    @Test
    fun `given variation with case insensitive match, when search called, then return variation`() = runTest {
        // GIVEN
        val identifier = "VAR-UPPER"
        val productId = 1L
        val product = createProduct(remoteId = productId).copy(type = ProductType.VARIABLE.value)
        val variation: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(10L)
            on { remoteProductId }.thenReturn(productId)
            on { globalUniqueId }.thenReturn("var-upper")
            on { sku }.thenReturn("")
        }
        whenever(productsCache.getAll()).thenReturn(listOf(product))
        whenever(productsCache.getProductById(productId)).thenReturn(product)
        whenever(variationsCache.getAll()).thenReturn(listOf(variation))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        assertEquals(variation, result.variation)
        assertEquals(product, result.parentProduct)
    }

    @Suppress("LongMethod")
    private fun createProduct(
        remoteId: Long = 1,
        name: String = "Test Product",
        sku: String = "",
        globalUniqueId: String = ""
    ) = Product(
        remoteId = remoteId,
        parentId = 0,
        name = name,
        description = "",
        shortDescription = "",
        slug = "",
        type = ProductType.SIMPLE.value,
        status = ProductStatus.PUBLISH,
        catalogVisibility = ProductCatalogVisibility.VISIBLE,
        isFeatured = false,
        stockStatus = ProductStockStatus.InStock,
        backorderStatus = ProductBackorderStatus.No,
        dateCreated = Date(),
        firstImageUrl = null,
        totalSales = 0,
        reviewsAllowed = true,
        isVirtual = false,
        ratingCount = 0,
        averageRating = 0f,
        permalink = "",
        externalUrl = "",
        buttonText = "",
        price = BigDecimal.TEN,
        salePrice = null,
        regularPrice = BigDecimal.TEN,
        taxClass = Product.TAX_CLASS_DEFAULT,
        isStockManaged = false,
        stockQuantity = 0.0,
        sku = sku,
        globalUniqueId = globalUniqueId,
        shippingClass = "",
        shippingClassId = 0,
        isDownloadable = false,
        downloads = emptyList(),
        downloadLimit = 0,
        downloadExpiry = 0,
        purchaseNote = "",
        numVariations = 0,
        images = emptyList(),
        attributes = emptyList(),
        saleEndDateGmt = null,
        saleStartDateGmt = null,
        isSoldIndividually = false,
        taxStatus = ProductTaxStatus.Taxable,
        isSaleScheduled = false,
        isPurchasable = true,
        menuOrder = 0,
        categories = emptyList(),
        tags = emptyList(),
        groupedProductIds = emptyList(),
        crossSellProductIds = emptyList(),
        upsellProductIds = emptyList(),
        variationIds = emptyList(),
        length = 0f,
        width = 0f,
        height = 0f,
        weight = 0f,
        isSampleProduct = false,
        specialStockStatus = null,
        isConfigurable = false,
        minAllowedQuantity = null,
        maxAllowedQuantity = null,
        bundleMinSize = null,
        bundleMaxSize = null,
        groupOfQuantity = null,
        combineVariationQuantities = null,
        password = null
    )
}
