package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WooPosProductSearchPredicateTest {

    private lateinit var getWooCoreVersion: GetWooCorePluginCachedVersion
    private lateinit var searchPredicate: WooPosProductSearchPredicate

    private val mockProduct by lazy {
        val product = ProductTestUtils.generateProduct(
            productId = 1L,
            productName = "Test Product"
        )
        product.copy(sku = "ABC123")
    }

    @Before
    fun setup() {
        getWooCoreVersion = mock()
        searchPredicate = WooPosProductSearchPredicate(getWooCoreVersion)
    }

    @Test
    fun `given blank query, when search predicate is invoked, then it should match all products`() {
        // GIVEN
        val query = "   "

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given older WC version, when search predicate is invoked with matching name, then it should match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.8.0")
        val query = "test"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given older WC version, when search predicate is invoked with non-matching name, then it should not match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.8.0")
        val query = "xyz"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertFalse(result(mockProduct))
    }

    @Test
    fun `given older WC version, when search predicate is invoked with matching SKU, then it should not match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.8.0")
        val query = "abc123"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertFalse(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with matching name, then it should match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "test"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with matching SKU, then it should match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "abc123"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with matching description, then it should match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "product est"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with matching short description, then it should not match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "short desc"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertFalse(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with non-matching term, then it should not match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "nonexistent"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertFalse(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with multiple matching terms, then it should match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "test abc123"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with multiple terms and one non-matching, then it should not match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "test xyz"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertFalse(result(mockProduct))
    }

    @Test
    fun `given null WC version, when search predicate is invoked, then it should use simple search predicate`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)
        val query = "test"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given null WC version, when search predicate is invoked with matching SKU, then it should not match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)
        val query = "abc123"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertFalse(result(mockProduct))
    }

    @Test
    fun `given newer WC version, when search predicate is invoked with case-insensitive match, then it should match`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        val query = "TEST product"

        // WHEN
        val result = searchPredicate(query)

        // THEN
        assertTrue(result(mockProduct))
    }

    @Test
    fun `given WC version, when isWooCoreSupportsNameOrSkuSearch invoked multiple times, it should cache the result`() {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")

        val query1 = "test"
        searchPredicate(query1)

        whenever(getWooCoreVersion()).thenReturn("9.8.0")

        // WHEN
        val query2 = "abc123"
        val result = searchPredicate(query2)

        // THEN
        assertTrue(result(mockProduct))
    }
}
