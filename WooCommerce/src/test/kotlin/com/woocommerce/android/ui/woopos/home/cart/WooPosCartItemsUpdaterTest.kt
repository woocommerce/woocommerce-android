package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.home.WooPosOrderCreatedData
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Coupon.CouponValidationState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
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

    private val formatPrice: WooPosFormatPrice = mock {
        on { invoke(argThat { this == BigDecimal("10.0") }) }.thenReturn("10.0$")
        on { invoke(argThat { this == BigDecimal("5.0") }) }.thenReturn("5.0$")
    }
    private val productsCache: WooPosProductsCache = mock()
    private val localCatalogStore: WooPosLocalCatalogStore = mock {
        on { deleteProducts(any(), any()) }.thenReturn(Result.success(Unit))
        on { deleteVariations(any(), any()) }.thenReturn(Result.success(Unit))
    }
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }
    private val crashLogger: CrashLogging = mock()
    private val logger: WooPosLogWrapper = mock()

    private val updater = WooPosCartItemsUpdater(
        formatPrice = formatPrice,
        productsCache = productsCache,
        localCatalogStore = localCatalogStore,
        selectedSite = selectedSite,
        wooPosLogWrapper = logger,
        crashLogger = crashLogger,
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
        val updatedInfo = WooPosOrderCreatedData.ProductInfo.Simple(
            id = 1L,
            name = "Updated Name",
            finalPrice = BigDecimal("10.0"),
            basePrice = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = generateWooPosProduct()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result.updatedItems).hasSize(1)
        val updatedItem = result.updatedItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem.name).isEqualTo("Updated Name")
        assertThat(updatedItem.price).isEqualTo("10.0$")
        assertThat(result.productsChanged).isTrue()
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Name",
                pricing = WooPosProductModel.WooPosPricing.RegularPricing(BigDecimal("10.0"))
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
        val updatedInfo = WooPosOrderCreatedData.ProductInfo.Variation(
            id = 1L,
            variationId = 2L,
            name = "Updated Variation",
            finalPrice = BigDecimal("10.0"),
            basePrice = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = generateWooPosProduct()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result.updatedItems).hasSize(1)
        val updatedItem = result.updatedItems[0] as WooPosCartItemViewState.Product.Variation
        assertThat(updatedItem.name).isEqualTo("Updated Variation")
        assertThat(updatedItem.price).isEqualTo("10.0$")
        assertThat(result.productsChanged).isTrue()
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Variation",
                pricing = WooPosProductModel.WooPosPricing.RegularPricing(BigDecimal("10.0"))
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
        assertThat(result.updatedItems).hasSize(1)
        val updatedItem = result.updatedItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem.productDoesNotExist).isTrue()
        assertThat(result.productsChanged).isTrue()
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
        val updatedInfo = WooPosOrderCreatedData.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product 1",
            finalPrice = BigDecimal("10.0"),
            basePrice = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = generateWooPosProduct()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result.updatedItems).hasSize(2)

        val updatedItem1 = result.updatedItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem1.name).isEqualTo("Updated Product 1")
        assertThat(updatedItem1.price).isEqualTo("10.0$")

        val updatedItem2 = result.updatedItems[1] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem2.productDoesNotExist).isTrue()

        assertThat(result.productsChanged).isTrue()
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Product 1",
                pricing = WooPosProductModel.WooPosPricing.RegularPricing(BigDecimal("10.0"))
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
        val updatedInfo = WooPosOrderCreatedData.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product",
            finalPrice = BigDecimal("10.0"),
            basePrice = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = generateWooPosProduct()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        assertThat(result.updatedItems).hasSize(2)

        val firstItem = result.updatedItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(firstItem.name).isEqualTo("Updated Product")
        assertThat(firstItem.price).isEqualTo("10.0$")
        assertThat(firstItem.productDoesNotExist).isFalse()

        val secondItem = result.updatedItems[1] as WooPosCartItemViewState.Product.Simple
        assertThat(secondItem.productDoesNotExist).isTrue()

        assertThat(result.productsChanged).isTrue()
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Product",
                pricing = WooPosProductModel.WooPosPricing.RegularPricing(BigDecimal("10.0"))
            )
        )
        verify(productsCache).deleteProduct(1L)
    }

    @Test
    fun `given no changes in product info, when called, then cache is not updated and products not changed`() =
        runTest {
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
            val updatedInfo = WooPosOrderCreatedData.ProductInfo.Simple(
                id = 1L,
                name = "Product",
                finalPrice = BigDecimal("10.0"),
                basePrice = BigDecimal("10.0"),
                quantity = 1f
            )

            // WHEN
            val result = updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

            // THEN
            assertThat(result.updatedItems).hasSize(1)
            verify(productsCache, never()).updateProduct(any())
            assertThat(result.productsChanged).isFalse()
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
    fun `given simple product not existing, when deleted, then also deletes from Local Catalog DB`() = runTest {
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
        verify(localCatalogStore).deleteProducts(
            argThat { siteId -> siteId.value == 1 },
            argThat { productIds ->
                productIds.size == 1 && productIds[0].value == 1L
            }
        )
    }

    @Test
    fun `given variation product not existing, when deleted, then deletes only variation from Local Catalog DB`() =
        runTest {
            // GIVEN
            val variationProduct = WooPosCartItemViewState.Product.Variation(
                itemNumber = 1,
                id = 1L,
                variationId = 2L,
                name = "Variation Name",
                price = "9.0$",
                imageUrl = "url",
                description = null
            )
            val itemsInCart = listOf(variationProduct)

            // WHEN
            updater.invoke(itemsInCart, emptyList(), emptyList())

            // THEN
            verify(productsCache).deleteProduct(1L)
            verify(localCatalogStore).deleteVariations(
                argThat { siteId -> siteId.value == 1 },
                argThat { variations ->
                    variations.size == 1 &&
                        variations[0].first.value == 1L &&
                        variations[0].second.value == 2L
                }
            )
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
        val updatedInfo = WooPosOrderCreatedData.ProductInfo.Simple(
            id = 1L,
            name = "Updated Name",
            finalPrice = BigDecimal("10.0"),
            basePrice = BigDecimal("10.0"),
            quantity = 1f
        )
        val cachedProduct = generateWooPosProduct()
        whenever(productsCache.getProductById(1L)).thenReturn(cachedProduct)

        // WHEN
        updater.invoke(itemsInCart, listOf(updatedInfo), emptyList())

        // THEN
        verify(productsCache).updateProduct(
            cachedProduct.copy(
                name = "Updated Name",
                pricing = WooPosProductModel.WooPosPricing.RegularPricing(BigDecimal("10.0"))
            )
        )
    }

    @Test
    fun `given cart with coupon, when updatedCoupons contains matching code, then Valid state returned`() =
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
            assertThat(result.updatedItems).hasSize(1)
            val updatedCoupon = result.updatedItems[0] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon.validationState).isEqualTo(CouponValidationState.Valid("-5.0$"))
        }

    @Test
    fun `given cart with coupon, when updatedCoupons does not contain matching code, then Unknown state returned`() =
        runTest {
            // GIVEN
            val itemsInCart = listOf(generateCoupon("COUPON2"))

            // WHEN
            val result = updater.invoke(itemsInCart, emptyList(), updatedCoupons = emptyList())

            // THEN
            assertThat(result.updatedItems).hasSize(1)
            val updatedCoupon = result.updatedItems[0] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon.validationState).isEqualTo(CouponValidationState.Unknown)
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
            assertThat(result.updatedItems).hasSize(2)
            val updatedCoupon1 = result.updatedItems[0] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon1.validationState).isEqualTo(CouponValidationState.Valid("-5.0$"))
            val updatedCoupon2 = result.updatedItems[1] as WooPosCartItemViewState.Coupon
            assertThat(updatedCoupon2.validationState).isEqualTo(CouponValidationState.Valid("-10.0$"))
        }

    @Test
    fun `given cart with coupon, when called, then couponsChanged is true`() = runTest {
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
        assertThat(result.couponsChanged).isTrue()
    }

    @Test
    fun `given cart without coupon, when called, then couponsChanged is false`() = runTest {
        // GIVEN

        // WHEN
        val result = updater.invoke(emptyList(), emptyList(), emptyList())

        // THEN
        assertThat(result.couponsChanged).isFalse()
    }

    @Test
    fun `given empty cart, when called, then productsChanged and couponsChanged flags are false`() = runTest {
        // GIVEN
        val itemsInCart = emptyList<WooPosCartItemViewState>()

        // WHEN
        val result = updater.invoke(itemsInCart, emptyList(), emptyList())

        // THEN
        assertThat(result.productsChanged).isFalse()
        assertThat(result.couponsChanged).isFalse()
    }

    private fun generateCoupon(
        code: String = "COUPON1",
        summary: String = "Coupon Summary",
        itemNumber: Int = 1,
        id: Long = 1L,
        validationState: CouponValidationState = CouponValidationState.Unknown
    ) = WooPosCartItemViewState.Coupon(
        itemNumber = itemNumber,
        name = code,
        summary = summary,
        id = id,
        validationState = validationState
    )

    private fun generateCouponLine(
        code: String,
        discountAmount: String
    ) = WooPosOrderCreatedData.CouponInfo(
        code = code,
        id = 1L,
        discountAmount = BigDecimal(discountAmount)
    )
}
