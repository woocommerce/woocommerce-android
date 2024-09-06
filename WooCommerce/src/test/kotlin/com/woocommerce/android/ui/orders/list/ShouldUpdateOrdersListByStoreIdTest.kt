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
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.store.ListStore
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldUpdateOrdersListByStoreIdTest : BaseUnitTest() {
    private val lastUpdateDataStore: LastUpdateDataStore = mock()
    private val lisStore: ListStore = mock()
    val sut = ShouldUpdateOrdersList(lastUpdateDataStore, lisStore)

    @Test
    fun `when should update return true and list state is not refresh, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        val listDescriptor: WCOrderListDescriptor = mock{
            on { uniqueIdentifier }.doReturn(ListDescriptorUniqueIdentifier(listId))
        }
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.shouldUpdateData(eq(key), any())).doReturn(flowOf(true))
        whenever(lisStore.getListState(eq(listDescriptor))).doReturn(ListState.CAN_LOAD_MORE)

        val result = sut.invoke(listDescriptor)

        assertTrue(result)
    }

    @Test
    fun `when should update return true and list state is refresh, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        val listDescriptor: WCOrderListDescriptor = mock{
            on { uniqueIdentifier }.doReturn(ListDescriptorUniqueIdentifier(listId))
        }
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.shouldUpdateData(eq(key), any())).doReturn(flowOf(true))
        whenever(lisStore.getListState(eq(listDescriptor))).doReturn(ListState.NEEDS_REFRESH)

        val result = sut.invoke(listDescriptor)

        assertTrue(result)
    }

    @Test
    fun `when should update return false and list state is refresh, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        val listDescriptor: WCOrderListDescriptor = mock{
            on { uniqueIdentifier }.doReturn(ListDescriptorUniqueIdentifier(listId))
        }
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.shouldUpdateData(eq(key), any())).doReturn(flowOf(false))
        whenever(lisStore.getListState(eq(listDescriptor))).doReturn(ListState.NEEDS_REFRESH)

        val result = sut.invoke(listDescriptor)

        assertTrue(result)
    }

    @Test
    fun `when should update return false and list state is not refresh, then the result is the expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        val listDescriptor: WCOrderListDescriptor = mock{
            on { uniqueIdentifier }.doReturn(ListDescriptorUniqueIdentifier(listId))
        }
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)
        whenever(lastUpdateDataStore.shouldUpdateData(eq(key), any())).doReturn(flowOf(false))
        whenever(lisStore.getListState(eq(listDescriptor))).doReturn(ListState.CAN_LOAD_MORE)

        val result = sut.invoke(listDescriptor)

        assertFalse(result)
    }
}
