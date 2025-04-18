package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosProductsListCacheTest {
    private lateinit var productsCache: WooPosProductsCache
    private lateinit var productListIndex: WooPosProductsIndex

    @Before
    fun setup() {
        productsCache = mock()
        productListIndex = WooPosProductsIndex(productsCache)
    }

    @Test
    fun `when cache is empty, hasProducts returns false`() = runTest {
        assertFalse(productListIndex.hasProducts())
    }

    @Test
    fun `when products are stored, hasProducts returns true`() = runTest {
        productListIndex.storeProductList(listOf(1L, 2L))
        assertTrue(productListIndex.hasProducts())
    }

    @Test
    fun `when products are stored, getProductList returns products from cache`() = runTest {
        val product1 = mock<Product>()
        val product2 = mock<Product>()

        whenever(productsCache.getProductById(1L)).thenReturn(product1)
        whenever(productsCache.getProductById(2L)).thenReturn(product2)

        productListIndex.storeProductList(listOf(1L, 2L))

        val result = productListIndex.getProductList()
        assertEquals(listOf(product1, product2), result)
    }

    @Test
    fun `when adding more products, duplicates are removed`() = runTest {
        val product1 = mock<Product>()
        val product2 = mock<Product>()
        val product3 = mock<Product>()

        whenever(productsCache.getProductById(1L)).thenReturn(product1)
        whenever(productsCache.getProductById(2L)).thenReturn(product2)
        whenever(productsCache.getProductById(3L)).thenReturn(product3)

        productListIndex.storeProductList(listOf(1L, 2L))
        productListIndex.storeProductList(listOf(2L, 3L))

        val result = productListIndex.getProductList()
        assertEquals(listOf(product1, product2, product3), result)
    }

    @Test
    fun `when cache is cleared, hasProducts returns false`() = runTest {
        productListIndex.storeProductList(listOf(1L, 2L))
        productListIndex.clearCache()
        assertFalse(productListIndex.hasProducts())
    }

    @Test
    fun `when cache is cleared, getProductList returns empty list`() = runTest {
        val product1 = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(product1)

        productListIndex.storeProductList(listOf(1L))
        productListIndex.clearCache()

        val result = productListIndex.getProductList()
        assertTrue(result.isEmpty())
    }
}
