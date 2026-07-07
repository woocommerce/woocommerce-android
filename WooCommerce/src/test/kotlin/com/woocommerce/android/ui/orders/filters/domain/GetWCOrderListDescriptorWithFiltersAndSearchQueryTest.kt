package com.woocommerce.android.ui.orders.filters.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor

class GetWCOrderListDescriptorWithFiltersAndSearchQueryTest {
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters = mock()
    private val sut = GetWCOrderListDescriptorWithFiltersAndSearchQuery(
        getWCOrderListDescriptorWithFilters = getWCOrderListDescriptorWithFilters,
    )

    @Test
    fun `given text search, when invoked, then keeps filters and adds search query`() {
        // GIVEN
        val descriptor = filteredDescriptor(customerId = CUSTOMER_ID)
        whenever(getWCOrderListDescriptorWithFilters()).thenReturn(descriptor)

        // WHEN
        val result = sut(SEARCH_QUERY)

        // THEN
        assertThat(result.searchQuery).isEqualTo(SEARCH_QUERY)
        assertThat(result.customerId).isEqualTo(CUSTOMER_ID)
        assertThat(result.statusFilter).isEqualTo(STATUS_FILTER)
        assertThat(result.beforeFilter).isEqualTo(BEFORE_FILTER)
        assertThat(result.afterFilter).isEqualTo(AFTER_FILTER)
        assertThat(result.productId).isEqualTo(PRODUCT_ID)
        assertThat(result.excludeFutureOrders).isTrue()
        assertThat(result.excludedIds).isEqualTo(EXCLUDED_IDS)
        assertThat(result.createdViaFilter).isEqualTo(CREATED_VIA_FILTER)
    }

    @Test
    fun `given guest orders search, when invoked, then filters by guest customer instead of text search`() {
        // GIVEN
        val descriptor = filteredDescriptor(customerId = null)
        whenever(getWCOrderListDescriptorWithFilters()).thenReturn(descriptor)

        // WHEN
        val result = sut(SEARCH_QUERY, searchGuestOrders = true)

        // THEN
        assertThat(result.searchQuery).isNull()
        assertThat(result.customerId).isEqualTo(GUEST_CUSTOMER_ID)
        assertThat(result.statusFilter).isEqualTo(STATUS_FILTER)
        assertThat(result.beforeFilter).isEqualTo(BEFORE_FILTER)
        assertThat(result.afterFilter).isEqualTo(AFTER_FILTER)
        assertThat(result.productId).isEqualTo(PRODUCT_ID)
        assertThat(result.excludeFutureOrders).isTrue()
        assertThat(result.excludedIds).isEqualTo(EXCLUDED_IDS)
        assertThat(result.createdViaFilter).isEqualTo(CREATED_VIA_FILTER)
    }

    @Test
    fun `given guest orders search, when a customer filter is active, then replaces it with the guest customer`() {
        // GIVEN
        val descriptor = filteredDescriptor(customerId = CUSTOMER_ID)
        whenever(getWCOrderListDescriptorWithFilters()).thenReturn(descriptor)

        // WHEN
        val result = sut(SEARCH_QUERY, searchGuestOrders = true)

        // THEN
        assertThat(result.searchQuery).isNull()
        assertThat(result.customerId).isEqualTo(GUEST_CUSTOMER_ID)
    }

    private fun filteredDescriptor(customerId: Long?) = WCOrderListDescriptor(
        site = SiteModel().apply { id = SITE_ID },
        statusFilter = STATUS_FILTER,
        searchQuery = null,
        excludeFutureOrders = true,
        beforeFilter = BEFORE_FILTER,
        afterFilter = AFTER_FILTER,
        productId = PRODUCT_ID,
        customerId = customerId,
        excludedIds = EXCLUDED_IDS,
        createdViaFilter = CREATED_VIA_FILTER,
    )

    private companion object {
        const val SITE_ID = 1
        const val CUSTOMER_ID = 123L
        const val GUEST_CUSTOMER_ID = 0L
        const val PRODUCT_ID = 456L
        const val SEARCH_QUERY = "Joe Doe"
        const val STATUS_FILTER = "processing"
        const val BEFORE_FILTER = "2026-07-03T23:59:59"
        const val AFTER_FILTER = "2026-07-03T00:00:00"
        const val CREATED_VIA_FILTER = "pos-rest-api"
        val EXCLUDED_IDS = listOf(789L)
    }
}
