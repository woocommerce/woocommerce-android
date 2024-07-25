package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.background.LastUpdateDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldUpdateOrdersListTest : BaseUnitTest() {
    private val lastUpdateDataStore: LastUpdateDataStore = mock()
    val sut = ShouldUpdateOrdersList(lastUpdateDataStore)

    @Test
    fun `when should update return true, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.shouldUpdateData(eq(key), any())).doReturn(flowOf(true))

        val result = sut.invoke(listId)

        assertTrue(result)
    }

    @Test
    fun `when should update return false, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.shouldUpdateData(eq(key), any())).doReturn(flowOf(false))

        val result = sut.invoke(listId)

        assertFalse(result)
    }
}
