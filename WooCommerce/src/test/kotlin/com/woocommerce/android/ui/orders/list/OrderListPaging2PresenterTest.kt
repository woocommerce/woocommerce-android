package com.woocommerce.android.ui.orders.list

import androidx.paging.PagedList
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OrderListPaging2PresenterTest {
    @Test
    fun `given a same-instance item mutation, when content changes, then snapshot generation and key stay stable`() {
        // GIVEN
        val order = order(1L)
        val pagedList = pagedList(listOf(order))
        val presenter = OrderListPaging2Presenter()
        presenter.submit(pagedList)
        val initialState = presenter.state.value
        val initialKey = presenter.keyAt(initialState, 0)

        // WHEN
        order.status = "completed"
        presenter.markContentChanged()
        val updatedState = presenter.state.value

        // THEN
        assertThat(updatedState.generation).isEqualTo(initialState.generation)
        assertThat(updatedState.contentRevision).isEqualTo(initialState.contentRevision + 1)
        assertThat(updatedState.items).isSameAs(initialState.items)
        assertThat(updatedState.itemCount).isEqualTo(initialState.itemCount)
        assertThat((updatedState.items.single() as OrderListItemUI).status).isEqualTo("completed")
        assertThat(presenter.keyAt(updatedState, 0)).isEqualTo(initialKey)

        presenter.close()
    }

    @Test
    fun `given a callback shrinks the list, when states are queried, then each keeps its snapshot contract`() {
        // GIVEN
        val initialItems = listOf(order(1L), order(2L), order(3L))
        val updatedItems = listOf(order(1L))
        val pagedList = pagedList(initialItems)
        val presenter = OrderListPaging2Presenter()
        presenter.submit(pagedList)
        val initialState = presenter.state.value
        val callback = callbackFor(pagedList)

        // WHEN
        whenever(pagedList.snapshot()).thenReturn(updatedItems)
        callback.onRemoved(1, 2)
        val updatedState = presenter.state.value

        // THEN
        assertThat(initialState.itemCount).isEqualTo(3)
        assertThat(presenter.keyAt(initialState, 2)).isEqualTo("order-list-order:3")
        assertThat(updatedState.generation).isEqualTo(initialState.generation)
        assertThat(updatedState.contentRevision).isEqualTo(initialState.contentRevision + 1)
        assertThat(updatedState.itemCount).isEqualTo(1)
        assertThat(presenter.keyAt(updatedState, 0)).isEqualTo("order-list-order:1")
        assertThat(presenter.keyAt(updatedState, 2)).isNull()

        presenter.close()
    }

    @Test
    fun `given a valid placeholder, when key and type are read, then loading identity is saveable without loading`() {
        // GIVEN
        val pagedList = pagedList(listOf(order(1L), null))
        val presenter = OrderListPaging2Presenter()
        presenter.submit(pagedList)
        val state = presenter.state.value

        // WHEN
        val key = presenter.keyAt(state, 1)
        val contentType = presenter.contentTypeAt(state, 1)

        // THEN
        assertThat(state.itemCount).isEqualTo(2)
        assertThat(state.generation).isEqualTo(1L)
        assertThat(key).isEqualTo("order-list-placeholder:1:1")
        assertThat(key).isInstanceOf(String::class.java)
        assertThat(contentType).isEqualTo(OrderListPaging2Presenter.ItemContentType.Loading)
        verify(pagedList, never()).loadAround(any())

        presenter.close()
    }

    @Test
    fun `given a replacement, when stale access and callback arrive, then replacement state and loads are preserved`() {
        // GIVEN
        val initialItems = listOf(order(1L), order(2L), order(3L))
        val replacementItems = listOf(order(4L))
        val initialList = pagedList(initialItems)
        val replacementList = pagedList(replacementItems)
        val presenter = OrderListPaging2Presenter()
        presenter.submit(initialList)
        val initialState = presenter.state.value
        val initialCallback = callbackFor(initialList)
        presenter.submit(replacementList)
        val replacementState = presenter.state.value
        val replacementCallback = callbackFor(replacementList)

        // WHEN
        initialCallback.onRemoved(0, initialItems.size)
        val stateAfterStaleCallback = presenter.state.value
        val staleItem = presenter.itemAt(initialState, 2)
        val currentItem = presenter.itemAt(replacementState, 0)
        presenter.close()

        // THEN
        assertThat(stateAfterStaleCallback).isSameAs(replacementState)
        assertThat(replacementState.generation).isEqualTo(initialState.generation + 1)
        assertThat(presenter.keyAt(initialState, 2)).isEqualTo("order-list-order:3")
        assertThat(presenter.keyAt(replacementState, 0)).isEqualTo("order-list-order:4")
        assertThat(staleItem).isSameAs(initialItems[2])
        assertThat(currentItem).isSameAs(replacementItems[0])
        verify(initialList).removeWeakCallback(initialCallback)
        verify(replacementList).removeWeakCallback(replacementCallback)
        verify(initialList, never()).loadAround(any())
        verify(replacementList).loadAround(0)
    }

    private fun callbackFor(pagedList: PagedList<OrderListItemUIType>): PagedList.Callback {
        val callback = argumentCaptor<PagedList.Callback>()
        verify(pagedList).addWeakCallback(anyOrNull(), callback.capture())
        return callback.firstValue
    }

    @Suppress("UNCHECKED_CAST")
    private fun pagedList(items: List<OrderListItemUIType?>): PagedList<OrderListItemUIType> = mock {
        on { snapshot() } doReturn items as List<OrderListItemUIType>
        on { size } doReturn items.size
    }

    private fun order(orderId: Long) = OrderListItemUI(
        orderId = orderId,
        orderNumber = orderId.toString(),
        orderName = "Order $orderId",
        orderTotal = orderId.toString(),
        status = "processing",
        dateCreated = null,
        currencyCode = "USD",
    )
}
