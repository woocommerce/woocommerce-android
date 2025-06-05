package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosGetProductByGtinOrSkuTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val productsCache: WooPosProductsCache = mock()
    private val getProductByGtinOrSku = WooPosGetProductByGtinOrSku(productsCache)

    @Test
    fun `when product exists in cache, then return it`() = runTest {
        // GIVEN
        val gtin = "123456789"
        val expectedProduct: Product = mock()
        whenever(productsCache.getProductByGtin(gtin)).thenReturn(expectedProduct)

        // WHEN
        val result = getProductByGtinOrSku(gtin)

        // THEN
        assertEquals(expectedProduct, result)
    }

    @Test
    fun `when product does not exist in cache, then throw not implemented error`() = runTest {
        // GIVEN
        val gtin = "123456789"
        whenever(productsCache.getProductByGtin(gtin)).thenReturn(null)

        // WHEN/THEN
        assertFailsWith<NotImplementedError> {
            getProductByGtinOrSku(gtin)
        }
    }
}
