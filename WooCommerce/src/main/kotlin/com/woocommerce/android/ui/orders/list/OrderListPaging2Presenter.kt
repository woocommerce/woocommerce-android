package com.woocommerce.android.ui.orders.list

import androidx.annotation.MainThread
import androidx.paging.PagedList
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.LoadingItem
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.SectionHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

@MainThread
internal class OrderListPaging2Presenter : AutoCloseable {
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var currentList: PagedList<OrderListItemUIType>? = null
    private var currentCallback: PagedList.Callback? = null
    private var isClosed = false

    fun submit(pagedList: PagedList<OrderListItemUIType>?) {
        if (isClosed || pagedList === currentList) return

        removeCurrentCallback()
        currentList = pagedList
        val previousState = mutableState.value
        val generation = previousState.generation + 1
        val contentRevision = previousState.contentRevision + 1

        mutableState.value = pagedList.toState(
            generation = generation,
            contentRevision = contentRevision,
        )

        if (pagedList != null) {
            val callback = callbackFor(pagedList)
            currentCallback = callback
            pagedList.addWeakCallback(null, callback)
        }
    }

    fun itemAt(capturedState: State, index: Int): OrderListItemUIType? {
        if (index !in 0 until capturedState.itemCount) return null

        if (capturedState === mutableState.value) {
            currentList
                ?.takeIf { index in it.indices }
                ?.loadAround(index)
        }
        return capturedState.items[index]
    }

    fun keyAt(capturedState: State, index: Int): String? {
        if (index !in 0 until capturedState.itemCount) return null
        return capturedState.items[index].stableKey(capturedState.generation, index)
    }

    fun indexOfOrder(capturedState: State, orderId: Long): Int? {
        return (0 until capturedState.itemCount).firstOrNull { index ->
            val item = capturedState.items[index]
            item is OrderListItemUI && item.orderId == orderId
        }
    }

    fun loadedOrderIds(capturedState: State): List<Long> {
        val orderIds = ArrayList<Long>()
        capturedState.items.forEach { item ->
            if (item is OrderListItemUI) {
                orderIds += item.orderId
            }
        }
        return Collections.unmodifiableList(orderIds)
    }

    fun markContentChanged() {
        if (isClosed) return
        val capturedState = mutableState.value
        mutableState.value = State(
            generation = capturedState.generation,
            contentRevision = capturedState.contentRevision + 1,
            items = capturedState.items,
        )
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        removeCurrentCallback()
        currentList = null
        val previousState = mutableState.value
        mutableState.value = State(
            generation = previousState.generation + 1,
            contentRevision = previousState.contentRevision + 1,
        )
    }

    private fun callbackFor(pagedList: PagedList<OrderListItemUIType>) = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) = onListChanged(pagedList)

        override fun onInserted(position: Int, count: Int) = onListChanged(pagedList)

        override fun onRemoved(position: Int, count: Int) = onListChanged(pagedList)
    }

    private fun onListChanged(pagedList: PagedList<OrderListItemUIType>) {
        if (isClosed || pagedList !== currentList) return
        val previousState = mutableState.value
        mutableState.value = pagedList.toState(
            generation = previousState.generation,
            contentRevision = previousState.contentRevision + 1,
        )
    }

    private fun removeCurrentCallback() {
        val callback = currentCallback ?: return
        currentList?.removeWeakCallback(callback)
        currentCallback = null
    }

    private fun PagedList<OrderListItemUIType>?.toState(
        generation: Long,
        contentRevision: Long,
    ): State {
        if (this == null) {
            return State(
                generation = generation,
                contentRevision = contentRevision,
            )
        }
        return State(
            generation = generation,
            contentRevision = contentRevision,
            items = snapshot(),
        )
    }

    private fun OrderListItemUIType?.stableKey(generation: Long, index: Int): String = when (this) {
        is LoadingItem -> "$ORDER_KEY_PREFIX$orderId"
        is OrderListItemUI -> "$ORDER_KEY_PREFIX$orderId"
        is SectionHeader -> "$SECTION_KEY_PREFIX${title.name}"
        null -> "$PLACEHOLDER_KEY_PREFIX$generation:$index"
    }

    class State(
        val generation: Long = 0L,
        val contentRevision: Long = 0L,
        internal val items: List<OrderListItemUIType?> = emptyList(),
    ) {
        val itemCount: Int
            get() = items.size
    }

    private companion object {
        const val ORDER_KEY_PREFIX = "order-list-order:"
        const val SECTION_KEY_PREFIX = "order-list-section:"
        const val PLACEHOLDER_KEY_PREFIX = "order-list-placeholder:"
    }
}
