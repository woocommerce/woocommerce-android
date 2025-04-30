package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosCartItemsUpdaterTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(eq(R.string.woopos_cart_changes_in_the_cart)) }.thenReturn("Changes made to items in cart")
    }
    private val formatPrice: WooPosFormatPrice = mock {
        onBlocking { invoke(argThat { this == BigDecimal("10.0") }) }.thenReturn("10.0$")
        onBlocking { invoke(argThat { this == BigDecimal("5.0") }) }.thenReturn("5.0$")
    }
    private val productsCache: WooPosProductsCache = mock()
    private val crashLogger: CrashLogging = mock()
    private val logger: WooLogWrapper = mock()

    private val updater = WooPosCartItemsUpdater(
        childrenToParentEventSender = childrenToParentEventSender,
        resourceProvider = resourceProvider,
        formatPrice = formatPrice,
        productsCache = productsCache,
        wooLogWrapper = logger,
        crashLogger = crashLogger
    )

    @Test
    fun `given cart with simple product, when called, then product is updated`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Old Name",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(simpleProduct)
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Name",
            price = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result).hasSize(1)
        val updatedItem = result[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem.name).isEqualTo("Updated Name")
        assertThat(updatedItem.price).isEqualTo("10.0$")
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Name",
                price = BigDecimal("10.0")
            )
        )
    }

    @Test
    fun `given cart with variation product, when called, then product is updated`() = runTest {
        // GIVEN
        val variationProduct = WooPosCartItemViewState.Product.Variation(
            itemNumber = 1,
            id = 1L,
            variationId = 2L,
            name = "Old Variation",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(variationProduct)
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Variation(
            id = 1L,
            variationId = 2L,
            name = "Updated Variation",
            price = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result).hasSize(1)
        val updatedItem = result[0] as WooPosCartItemViewState.Product.Variation
        assertThat(updatedItem.name).isEqualTo("Updated Variation")
        assertThat(updatedItem.price).isEqualTo("10.0$")
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Variation",
                price = BigDecimal("10.0")
            )
        )
    }

    @Test
    fun `given cart with product, when called, then product is marked as not existing`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product Name",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(simpleProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, emptyList(), emptyList())

        // THEN
        assertThat(result).hasSize(1)
        val updatedItem = result[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem.productDoesNotExist).isTrue()
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
        verify(productsCache).deleteProduct(1L)
    }

    @Test
    fun `given cart with multiple items, when called, then only those are updated`() = runTest {
        // GIVEN
        val simpleProduct1 = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product 1",
            price = "9.0$",
            imageUrl = "url1",
            description = null
        )
        val simpleProduct2 = WooPosCartItemViewState.Product.Simple(
            itemNumber = 2,
            id = 2L,
            name = "Product 2",
            price = "19.0$",
            imageUrl = "url2",
            description = null
        )
        val itemsInCart = listOf(simpleProduct1, simpleProduct2)
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product 1",
            price = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result).hasSize(2)

        val updatedItem1 = result[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem1.name).isEqualTo("Updated Product 1")
        assertThat(updatedItem1.price).isEqualTo("10.0$")

        val updatedItem2 = result[1] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem2.productDoesNotExist).isTrue()

        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Product 1",
                price = BigDecimal("10.0")
            )
        )
        verify(productsCache).deleteProduct(2L)
    }

    @Test
    fun `given cart with multiple identical products, when called, then update respects quantities`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(simpleProduct, simpleProduct)
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product",
            price = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result).hasSize(2)

        val firstItem = result[0] as WooPosCartItemViewState.Product.Simple
        assertThat(firstItem.name).isEqualTo("Updated Product")
        assertThat(firstItem.price).isEqualTo("10.0$")
        assertThat(firstItem.productDoesNotExist).isFalse()

        val secondItem = result[1] as WooPosCartItemViewState.Product.Simple
        assertThat(secondItem.productDoesNotExist).isTrue()

        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Product",
                price = BigDecimal("10.0")
            )
        )
        verify(productsCache).deleteProduct(1L)
    }

    @Test
    fun `given no changes in product info, when called, then cache is not updated and parent is not notified`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product",
            price = "10.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(simpleProduct)
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Product",
            price = BigDecimal("10.0"),
            quantity = 1f
        )

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result).hasSize(1)
        verify(productsCache, never()).updateProduct(any())
        verify(childrenToParentEventSender, never()).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
    }

    @Test
    fun `given product deleted, when called, then cache is updated correctly`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product Name",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(simpleProduct)

        // WHEN
        updater.invoke(itemsInCart, emptyList(), emptyList())

        // THEN
        verify(productsCache).deleteProduct(1L)
    }

    @Test
    fun `given product updated, when called, then cache is updated correctly`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Old Name",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val itemsInCart = listOf(simpleProduct)
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Name",
            price = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Name",
                price = BigDecimal("10.0")
            )
        )
    }

    @Test
    fun `given cart with coupon, when updatedCoupons contains matching code, then formattedDiscount is updated`() =
        runTest {
            // GIVEN
            val coupon = generateCoupon(code = "COUPON1")
            val itemsInCart = listOf(coupon)
            val updatedCoupons = listOf(
                generateCouponLine(
                    code = "COUPON1",
                    discountAmount = "5.0"
                )
            )

            // WHEN
            val result = updater.invoke(itemsInCart, emptyList(), updatedCoupons)

            // THEN
            assertThat(result).hasSize(1)
            val updatedCoupon = result[0] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon.formattedDiscount).isEqualTo("-5.0$")
        }

    @Test
    fun `given cart with coupon, when updatedCoupons does not contain matching code, then formattedDiscount remains null`() =
        runTest {
            // GIVEN
            val itemsInCart = listOf(generateCoupon("COUPON2"))

            // WHEN
            val result = updater.invoke(itemsInCart, emptyList(), updatedCoupons = emptyList())

            // THEN
            assertThat(result).hasSize(1)
            val updatedCoupon = result[0] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon.formattedDiscount).isNull()
        }

    @Test
    fun `given cart with coupon, when updatedCoupons does not contain matching code, then crashlytics report sent`() =
        runTest {
            // GIVEN
            val itemsInCart = listOf(generateCoupon("COUPON2"))

            // WHEN
            updater.invoke(itemsInCart, emptyList(), updatedCoupons = emptyList())

            // THEN
            verify(crashLogger).sendReport(any(), anyOrNull(), anyOrNull())
        }

    @Test
    fun `given cart with multiple coupons, then relevant coupons are updated`() =
        runTest {
            // GIVEN
            val coupon1 = generateCoupon(code = "COUPON1")
            val coupon2 = generateCoupon(code = "COUPON2")
            val itemsInCart = listOf(coupon1, coupon2)
            val updatedCoupons = listOf(
                generateCouponLine(
                    code = "COUPON1",
                    discountAmount = "5.0"
                ),
                generateCouponLine(
                    code = "COUPON2",
                    discountAmount = "10.0"
                )
            )

            // WHEN
            val result = updater.invoke(itemsInCart, emptyList(), updatedCoupons)

            // THEN
            assertThat(result).hasSize(2)
            val updatedCoupon1 = result[0] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon1.formattedDiscount).isEqualTo("-5.0$")
            val updatedCoupon2 = result[1] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon2.formattedDiscount).isEqualTo("-10.0$")
        }

    private fun generateCoupon(
        code: String = "COUPON1",
        summary: String = "Coupon Summary",
        itemNumber: Int = 1,
        id: Long = 1L,
        formattedDiscount: String? = null
    ) = WooPosCartItemViewState.Coupon(
        itemNumber = itemNumber,
        name = code,
        summary = summary,
        id = id,
        formattedDiscount = formattedDiscount
    )

    private fun generateCouponLine(
        code: String,
        discountAmount: String
    ) = ParentToChildrenEvent.OrderCreated.CouponLine(
        code = code,
        id = 1L,
        discountAmount = BigDecimal(discountAmount)
    )
}
