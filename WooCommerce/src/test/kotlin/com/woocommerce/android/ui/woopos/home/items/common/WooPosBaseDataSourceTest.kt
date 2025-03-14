package com.woocommerce.android.ui.woopos.home.items.common

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WooPosBaseDataSourceTest {

    private val testDataSource = object : BaseDataSource<String>() {
        override suspend fun fetchFromCache(productId: Long?): List<String> = listOf("Cached Item")
        override suspend fun fetchFromRemote(productId: Long?): Result<List<String>> =
            Result.success(listOf("Remote Item"))
        override suspend fun updateCache(productId: Long?, data: List<String>) = Unit
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
}
