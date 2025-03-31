package com.woocommerce.android.ui.woopos.home.items.common

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq

class WooPosBaseDataSourceTest {

    private val testDataSource = object : WooPosBaseDataSource<String>() {
        override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = listOf("Cached Item")
        override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> =
            Result.success(listOf("Remote Item"))
        override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) = Unit
    }

    @Test
    fun `given cached data and remote success when fetchData then emits cached and remote data`() = runTest {
        // WHEN
        val result = testDataSource.fetchData(FetchOptions()).toList()

        // THEN
        assertEquals(2, result.size)
        assertTrue(result[0] is FetchResult.Cached)
        assertTrue(result[1] is FetchResult.Remote)
        assertEquals(listOf("Cached Item"), (result[0] as FetchResult.Cached).data)
        assertEquals(listOf("Remote Item"), (result[1] as FetchResult.Remote).result.getOrThrow())
    }

    @Test
    fun `given cached data and remote failure when fetchData then emits cached data and failure`() = runTest {
        // GIVEN
        val testDataSource = object : WooPosBaseDataSource<String>() {
            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = listOf("Cached Item")
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> =
                Result.failure(Exception("Network Error"))
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) = Unit
        }

        // WHEN
        val result = testDataSource.fetchData(FetchOptions()).toList()

        // THEN
        assertEquals(2, result.size)
        assertTrue(result[0] is FetchResult.Cached)
        assertTrue(result[1] is FetchResult.Remote)
        assertTrue((result[1] as FetchResult.Remote).result.isFailure)
    }

    @Test
    fun `given forceRefresh true when fetchData then clears cache and fetches new data`() = runTest {
        // GIVEN
        val mockCacheUpdate = mock<(Long?, List<String>) -> Unit>()
        val testDataSource = object : WooPosBaseDataSource<String>() {
            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = emptyList()
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> =
                Result.success(listOf("New Item"))
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) =
                mockCacheUpdate(fetchOptions.productId, data)
        }

        // WHEN
        val result = testDataSource.fetchData(FetchOptions(forceRefresh = true)).toList()

        // THEN
        verify(mockCacheUpdate).invoke(eq(null), eq(emptyList()))
        verify(mockCacheUpdate).invoke(eq(null), eq(listOf("New Item")))
        assertTrue(result[0] is FetchResult.Cached)
        assertTrue(result[1] is FetchResult.Remote)
    }

    @Test
    fun `given remote success when fetchData then updates cache with remote data`() = runTest {
        // GIVEN
        val mockCacheUpdate = mock<(Long?, List<String>) -> Unit>()
        val testDataSource = object : WooPosBaseDataSource<String>() {
            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = listOf("Cached Item")
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> =
                Result.success(listOf("New Item"))
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) =
                mockCacheUpdate(fetchOptions.productId, data)
        }

        // WHEN
        val result = testDataSource.fetchData(FetchOptions()).toList()

        // THEN
        verify(mockCacheUpdate).invoke(eq(null), eq(listOf("New Item")))
        assertEquals(listOf("New Item"), (result[1] as FetchResult.Remote).result.getOrThrow())
        assertTrue(result[1] is FetchResult.Remote)
    }

    @Test
    fun `given no cached data and remote failure when fetchData then emits empty cache and failure`() = runTest {
        // GIVEN
        val testDataSource = object : WooPosBaseDataSource<String>() {
            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = emptyList()
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> =
                Result.failure(Exception("Network Error"))
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) = Unit
        }

        // WHEN
        val result = testDataSource.fetchData(FetchOptions()).toList()

        // THEN
        assertEquals(2, result.size)
        assertTrue(result[0] is FetchResult.Cached)
        assertEquals(emptyList<String>(), (result[0] as FetchResult.Cached).data)
        assertTrue(result[1] is FetchResult.Remote)
        assertTrue((result[1] as FetchResult.Remote).result.isFailure)
    }

    @Test
    fun `given successful fetchMore then updates cache and returns merged data`() = runTest {
        val testDataSource = object : WooPosBaseDataSource<String>() {
            private val cache = mutableListOf<String>()

            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = cache.toList()
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> = TODO()
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) {
                cache.addAll(data)
            }
        }

        val result = testDataSource.fetchMore(
            fetchMore = { Result.success(listOf("A", "B")) }
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("A", "B"), result.getOrThrow())
    }

    @Test
    fun `given failed fetchMore when loadMore then returns failure and does not update cache`() = runTest {
        val testDataSource = object : WooPosBaseDataSource<String>() {
            private val cache = mutableListOf("Old")

            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = cache.toList()
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> = TODO()
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) {
                cache.addAll(data)
            }
            fun getCache(): List<String> = cache.toList()
        }

        val result = testDataSource.fetchMore(
            fetchMore = { Result.failure(Exception("loadMore failed")) }
        )

        assertTrue(result.isFailure)
        assertEquals("loadMore failed", result.exceptionOrNull()?.message)
        assertEquals(listOf("Old"), testDataSource.getCache())
    }

    @Test
    fun `given exception inside fetchMore then catches and returns failure`() = runTest {
        val testDataSource = object : WooPosBaseDataSource<String>() {
            override suspend fun fetchFromCache(fetchOptions: FetchOptions): List<String> = emptyList()
            override suspend fun fetchFromRemote(fetchOptions: FetchOptions): Result<List<String>> = TODO()
            override suspend fun updateCache(fetchOptions: FetchOptions, data: List<String>) = Unit
        }

        val result = runCatching {
            testDataSource.fetchMore(
                fetchMore = { Result.failure(Exception("Unexpected crash")) }
            )
        }.getOrNull()

        assertNotNull(result)
        assertTrue(result!!.isFailure)
        assertEquals("Unexpected crash", result.exceptionOrNull()?.message)
    }
}
