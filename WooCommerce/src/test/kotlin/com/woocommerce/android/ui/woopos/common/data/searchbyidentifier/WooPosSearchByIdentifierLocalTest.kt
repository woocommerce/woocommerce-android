package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

class WooPosSearchByIdentifierLocalTest {

    private lateinit var sut: WooPosSearchByIdentifierLocal
    private val productsCache: WooPosProductsCache = mock()
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierLocal(productsCache, checkDigitRemover)
    }

    @Test
    fun `given product with matching global unique id, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "1234567890123"
        val product = createProduct(globalUniqueId = identifier)
        whenever(productsCache.getAll()).thenReturn(listOf(product))

        // WHEN
        val result = sut(identifier, WooPosBarcodeFormat.FormatEAN13)

        // THEN
        assertEquals(product, result)
    }

    @Test
    fun `given product with matching sku, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "SKU123"
        val product = createProduct(sku = identifier)
        whenever(productsCache.getAll()).thenReturn(listOf(product))

        // WHEN
        val result = sut(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertEquals(product, result)
    }

    @Test
    fun `given product with identifier without check digit, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "1234567890123"
        val identifierWithoutCheckDigit = "123456789012"
        val product = createProduct(globalUniqueId = identifierWithoutCheckDigit)
        whenever(productsCache.getAll()).thenReturn(listOf(product))
        whenever(checkDigitRemover(identifier, WooPosBarcodeFormat.FormatEAN13))
            .thenReturn(identifierWithoutCheckDigit)

        // WHEN
        val result = sut(identifier, WooPosBarcodeFormat.FormatEAN13)

        // THEN
        assertEquals(product, result)
    }

    @Test
    fun `given no matching products, when search called, then return null`() = runTest {
        // GIVEN
        val identifier = "NOTFOUND"
        whenever(productsCache.getAll()).thenReturn(emptyList())

        // WHEN
        val result = sut(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertNull(result)
    }

    @Test
    fun `given product with lowercase global unique id, when search with uppercase, then return product`() = runTest {
        // GIVEN
        val identifier = "ABC123"
        val product = createProduct(globalUniqueId = "abc123")
        whenever(productsCache.getAll()).thenReturn(listOf(product))

        // WHEN
        val result = sut(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertEquals(product, result)
    }

    @Test
    fun `given product with lowercase sku, when search with uppercase, then return product`() = runTest {
        // GIVEN
        val identifier = "SKU123"
        val product = createProduct(sku = "sku123")
        whenever(productsCache.getAll()).thenReturn(listOf(product))

        // WHEN
        val result = sut(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertEquals(product, result)
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
