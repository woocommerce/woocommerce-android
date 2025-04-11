package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class WooPosSearchResultsIndexTest {

    private lateinit var searchResultsCache: WooPosSearchResultsIndex
    private val productsCache: WooPosProductsCache = mock()
    private val mockProducts = listOf<Product>(mock(), mock(), mock())

    @Before
    fun setUp() = runTest {
        searchResultsCache = WooPosSearchResultsIndex(productsCache)

        // Setup mock products
        mockProducts.forEachIndexed { index, product ->
            whenever(product.remoteId).thenReturn((index + 1).toLong())
            whenever(productsCache.getProductById((index + 1).toLong())).thenReturn(product)
        }
    }

    @Test
    fun `when storing search results and retrieving them, then correct products are returned`() = runTest {
        // Given
        val query = "test query"
        val productIds = listOf(1L, 3L)

        // When
        searchResultsCache.storeSearchResults(query, productIds)
        val searchResults = searchResultsCache.getSearchResults(query)

        // Then
        assertEquals(2, searchResults.size)
        assertTrue(searchResults.any { it.remoteId == 1L })
        assertTrue(searchResults.any { it.remoteId == 3L })
    }

    @Test
    fun `when checking if query has search results, then returns correct value`() = runTest {
        // Given
        val query = "test query"
        val productIds = listOf(1L, 2L)
        searchResultsCache.storeSearchResults(query, productIds)

        // When & Then
        assertTrue(searchResultsCache.hasSearchResults(query))
        assertFalse(searchResultsCache.hasSearchResults("other query"))
    }

    @Test
    fun `when clearing cache, then search results are removed`() = runTest {
        // Given
        val query = "test query"
        val productIds = listOf(1L, 2L)
        searchResultsCache.storeSearchResults(query, productIds)
        assertTrue(searchResultsCache.hasSearchResults(query))

        // When
        searchResultsCache.clearCache()

        // Then
        assertFalse(searchResultsCache.hasSearchResults(query))
    }

    @Test
    fun `when storing results for same query multiple times, then results are combined without duplicates`() = runTest {
        // Given
        val query = "test query"
        val firstPage = listOf(1L, 2L)
        val secondPage = listOf(2L, 3L)

        // When
        searchResultsCache.storeSearchResults(query, firstPage)
        searchResultsCache.storeSearchResults(query, secondPage)
        val searchResults = searchResultsCache.getSearchResults(query)

        // Then
        assertEquals(3, searchResults.size)
        assertTrue(searchResults.any { it.remoteId == 1L })
        assertTrue(searchResults.any { it.remoteId == 2L })
        assertTrue(searchResults.any { it.remoteId == 3L })
    }

    @Test
    fun `when query changes, then previous pagination results are cleared`() = runTest {
        // Given
        val firstQuery = "first query"
        val secondQuery = "second query"
        val firstQueryIds = listOf(1L, 2L)

        // When
        searchResultsCache.storeSearchResults(firstQuery, firstQueryIds)
        assertTrue(searchResultsCache.hasSearchResults(firstQuery))

        // Then
        searchResultsCache.storeSearchResults(secondQuery, listOf(3L))
        assertTrue(searchResultsCache.hasSearchResults(secondQuery))
        assertFalse(searchResultsCache.hasSearchResults(firstQuery))
    }
}
