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
    private lateinit var productsListCache: WooPosProductsListIndex

    @Before
    fun setup() {
        productsCache = mock()
        productsListCache = WooPosProductsListIndex(productsCache)
    }

    @Test
    fun `when cache is empty, hasProducts returns false`() = runTest {
        assertFalse(productsListCache.hasProducts())
    }

    @Test
    fun `when products are stored, hasProducts returns true`() = runTest {
        productsListCache.storeProductList(listOf(1L, 2L))
        assertTrue(productsListCache.hasProducts())
    }

    @Test
    fun `when products are stored, getProductList returns products from cache`() = runTest {
        val product1 = mock<Product>()
        val product2 = mock<Product>()

        whenever(productsCache.getProductById(1L)).thenReturn(product1)
        whenever(productsCache.getProductById(2L)).thenReturn(product2)

        productsListCache.storeProductList(listOf(1L, 2L))

        val result = productsListCache.getProductList()
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

        productsListCache.storeProductList(listOf(1L, 2L))
        productsListCache.storeProductList(listOf(2L, 3L))

        val result = productsListCache.getProductList()
        assertEquals(listOf(product1, product2, product3), result)
    }

    @Test
    fun `when cache is cleared, hasProducts returns false`() = runTest {
        productsListCache.storeProductList(listOf(1L, 2L))
        productsListCache.clearCache()
        assertFalse(productsListCache.hasProducts())
    }

    @Test
    fun `when cache is cleared, getProductList returns empty list`() = runTest {
        val product1 = mock<Product>()
        whenever(productsCache.getProductById(1L)).thenReturn(product1)

        productsListCache.storeProductList(listOf(1L))
        productsListCache.clearCache()

        val result = productsListCache.getProductList()
        assertTrue(result.isEmpty())
    }
}
