package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.background.LastUpdateDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StoreOrdersListLastUpdateTest : BaseUnitTest() {
    private val lastUpdateDataStore: LastUpdateDataStore = mock()
    val sut = StoreOrdersListLastUpdate(lastUpdateDataStore)

    @Test
    fun `when storeLastUpdate is call, then the value is saved as expected`() = testBlocking {
        val listId = 1
        val key = "key-1"
        whenever(lastUpdateDataStore.getLastUpdateKeyByOrdersListId(eq(listId))).doReturn(key)

        sut(listId)

        verify(lastUpdateDataStore).storeLastUpdate(key)
    }
}
