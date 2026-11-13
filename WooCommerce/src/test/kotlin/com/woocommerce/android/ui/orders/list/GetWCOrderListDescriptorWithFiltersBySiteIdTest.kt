package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.tools.ResolveSiteBySiteId
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.SalesChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

class GetWCOrderListDescriptorWithFiltersBySiteIdTest {
    private val orderFiltersRepository: OrderFiltersRepository = mock {
        on { getCurrentFilterSelection(any()) } doReturn emptyList()
        on { getCurrentFilterSelection(OrderListFilterCategory.SALES_CHANNEL) } doReturn listOf(SalesChannel.POS.key)
    }
    private val resolveSiteBySiteId: ResolveSiteBySiteId = mock {
        on { invoke(SITE_ID) } doReturn SiteModel()
    }
    private val sut = GetWCOrderListDescriptorWithFiltersBySiteId(
        orderFiltersRepository = orderFiltersRepository,
        dateUtils = mock(),
        resolveSiteBySiteId = resolveSiteBySiteId,
    )

    @Test
    fun `given a sales channel filter, when invoked, then the descriptor filters by that channel`() {
        val result = sut(SITE_ID)

        assertThat(result?.createdViaFilter).isEqualTo(SalesChannel.POS.apiValue)
    }

    private companion object {
        const val SITE_ID = 1L
    }
}
