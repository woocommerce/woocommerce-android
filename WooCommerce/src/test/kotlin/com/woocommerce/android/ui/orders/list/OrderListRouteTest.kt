package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OrderListRouteTest {
    @Test
    fun `when empty view types are mapped, then Compose content states are returned`() {
        assertThat(contentState(EmptyViewType.ORDER_LIST_LOADING))
            .isEqualTo(OrderListContentState.InitialLoading)
        assertThat(contentState(EmptyViewType.ORDER_LIST))
            .isEqualTo(OrderListContentState.Empty(OrderListEmptyState.NoOrders))
        assertThat(contentState(EmptyViewType.ORDER_LIST_FILTERED))
            .isEqualTo(OrderListContentState.Empty(OrderListEmptyState.Filtered))
        assertThat(contentState(EmptyViewType.SEARCH_RESULTS))
            .isEqualTo(OrderListContentState.Empty(OrderListEmptyState.Search(QUERY)))
        assertThat(contentState(EmptyViewType.SEARCH_RESULTS_GUEST))
            .isEqualTo(OrderListContentState.Empty(OrderListEmptyState.GuestSearch(QUERY)))
        assertThat(contentState(EmptyViewType.NETWORK_OFFLINE))
            .isEqualTo(OrderListContentState.Empty(OrderListEmptyState.Offline))
        assertThat(contentState(EmptyViewType.NETWORK_ERROR))
            .isEqualTo(OrderListContentState.Empty(OrderListEmptyState.NetworkError))
    }

    @Test
    fun `when no empty state is mapped, then content retains append and revision state`() {
        assertThat(contentState(null))
            .isEqualTo(
                OrderListContentState.Content(
                    isAppending = true,
                    contentRevision = CONTENT_REVISION,
                )
            )
    }

    private fun contentState(emptyViewType: EmptyViewType?) = emptyViewType.toOrderListContentState(
        query = QUERY,
        isAppending = true,
        contentRevision = CONTENT_REVISION,
    )

    private companion object {
        const val QUERY = "guest"
        const val CONTENT_REVISION = 42L
    }
}
