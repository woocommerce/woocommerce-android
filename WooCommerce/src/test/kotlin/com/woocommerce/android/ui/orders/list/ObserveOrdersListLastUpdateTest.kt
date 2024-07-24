package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.background.LastUpdateDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOrdersListLastUpdateTest : BaseUnitTest() {
    private val lastUpdateDataStore: LastUpdateDataStore = mock()
    val sut = ObserveOrdersListLastUpdate(lastUpdateDataStore)

    @Test
    fun `when observe last update, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        val lastUpdate = listOf(123L, 456L, 789L)
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.observeLastUpdate(eq(key))).doReturn(lastUpdate.asFlow())

        val result = sut.invoke(listId).toList()

        assertEquals(result, lastUpdate)
    }
}
