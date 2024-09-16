package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

class WooPosTotalsRepositoryTest {
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val getProductById: WooPosGetProductById = mock()

    private val dateUtils: DateUtils = mock()

    private lateinit var repository: WooPosTotalsRepository

    private val product1 = Product(
        remoteId = 1L, name = "Product 1", price = BigDecimal(10),
        sku = "SKU1", attributes = emptyList(),
        parentId = 0L, description = "", shortDescription = "", slug = "", type = "",
        status = null, catalogVisibility = null, isFeatured = false,
        stockStatus = ProductStockStatus.InsufficientStock, backorderStatus = ProductBackorderStatus.No,
        dateCreated = Date(), firstImageUrl = null, totalSales = 0L, reviewsAllowed = false,
        isVirtual = false, ratingCount = 0, averageRating = 0.0f, permalink = "", externalUrl = "",
        buttonText = "", salePrice = BigDecimal.ZERO, regularPrice = BigDecimal.ZERO,
        taxClass = "", isStockManaged = false, stockQuantity = 0.0, shippingClass = "",
        shippingClassId = 0L, isDownloadable = false, downloads = emptyList(),
        downloadLimit = 0L, downloadExpiry = 0, purchaseNote = "", numVariations = 0,
        images = emptyList(), saleEndDateGmt = null, saleStartDateGmt = null,
        isSoldIndividually = false, taxStatus = ProductTaxStatus.Taxable,
        isSaleScheduled = false, isPurchasable = false, menuOrder = 0, categories = emptyList(),
        tags = emptyList(), groupedProductIds = emptyList(), crossSellProductIds = emptyList(),
        upsellProductIds = emptyList(), variationIds = emptyList(), length = 0f, width = 0f,
        height = 0f, weight = 0f, subscription = null, isSampleProduct = false, specialStockStatus = null,
        isConfigurable = false, minAllowedQuantity = null, maxAllowedQuantity = null,
        bundleMinSize = null, bundleMaxSize = null, groupOfQuantity = null,
        combineVariationQuantities = null
    )

    @Test
    fun `given empty product list, when createOrderWithProducts called, then return error`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = emptyList<Long>()

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(productIds) }

        // THEN
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `given product ids without duplicates, when createOrderWithProducts, then items all quantity one`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L, 2L, 3L)

        whenever(getProductById(1L)).thenReturn(product1)
        whenever(getProductById(2L)).thenReturn(product1)
        whenever(getProductById(3L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(productIds = productIds)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(3)
        assertThat(orderCapture.lastValue.items.map { it.quantity }).containsOnly(1f)
        assertThat(orderCapture.lastValue.items[0].name).isEqualTo(product1.name)
    }

    @Test
    fun `given product id, when createOrderWithProducts, then item name matches original product`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L)

        whenever(getProductById(1L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(productIds = productIds)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(1)
        assertThat(orderCapture.lastValue.items[0].name).isEqualTo(product1.name)
    }

    @Test
    fun `given product ids with duplicates, when createOrderWithProducts, then items quantity is correct`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L, 1L, 2L, 3L, 3L, 3L)

        whenever(getProductById(1L)).thenReturn(product1)
        whenever(getProductById(2L)).thenReturn(product1)
        whenever(getProductById(3L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(productIds = productIds)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(3)
        assertThat(orderCapture.lastValue.items.map { it.quantity }).containsExactly(2f, 1f, 3f)
    }

    @Test
    fun `given product ids, when createOrder with some invalid ids, then return failure`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L, -1L, 3L)
        val mockOrder: Order = mock()
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), eq(""))).thenReturn(Result.success(mockOrder))

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(productIds) }

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Invalid product ID: -1")
        verify(orderCreateEditRepository, never()).createOrUpdateOrder(any(), eq(""))
    }
}
