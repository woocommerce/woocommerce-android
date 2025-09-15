package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersDataSourceTest {

    @Rule @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val orderRestClient: OrderRestClient = mock()
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(siteModel) }
    private val orderMapper: OrderMapper = mock()
    private val ordersCache: WooPosOrdersInMemoryCache = mock()

    private val sut = WooPosOrdersDataSource(
        restClient = orderRestClient,
        selectedSite = selectedSite,
        orderMapper = orderMapper,
        ordersCache = ordersCache
    )

    @Test
    fun `when cache has data and fetch succeeds, then emit SuccessCache then SuccessRemote and store mapped in cache`() = runTest {
        val cachedOrder = OrderTestUtils.generateTestOrder()
        whenever(ordersCache.getAll()).thenReturn(listOf(cachedOrder))

        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 11)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 22)
        val entities = listOf(e1 to emptyList<WCMetaData>(), e2 to emptyList())
        val mapped1 = OrderTestUtils.generateTestOrder()
        val mapped2 = OrderTestUtils.generateTestOrder()
        whenever(orderMapper.toAppModel(e1)).thenReturn(mapped1)
        whenever(orderMapper.toAppModel(e2)).thenReturn(mapped2)

        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = entities
        )
        whenever(
            orderRestClient.fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                createdVia = any(),
                searchQuery = anyOrNull()
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)

        val first = emissions[0] as LoadOrdersResult.SuccessCache
        assertThat(first.orders).containsExactly(cachedOrder)

        val second = emissions[1] as LoadOrdersResult.SuccessRemote
        assertThat(second.orders).containsExactly(mapped1, mapped2)

        verify(selectedSite).get()
        verify(ordersCache).getAll()
        verify(ordersCache).setAll(listOf(mapped1, mapped2))
        verify(orderRestClient).fetchOrders(
            site = any(),
            count = any(),
            page = any(),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            createdVia = any(),
            searchQuery = anyOrNull()
        )
    }

    @Test
    fun `when cache has data and fetch fails, then emit SuccessCache then Error and do not store`() = runTest {
        val cachedOrder = OrderTestUtils.generateTestOrder()
        whenever(ordersCache.getAll()).thenReturn(listOf(cachedOrder))

        val orderError = WCOrderStore.OrderError(
            type = WCOrderStore.OrderErrorType.GENERIC_ERROR,
            message = "generic error"
        )
        val payload = WCOrderStore.FetchOrdersResponsePayload(
            error = orderError,
            site = siteModel
        )
        whenever(
            orderRestClient.fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                createdVia = any(),
                searchQuery = anyOrNull()
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)

        val first = emissions[0] as LoadOrdersResult.SuccessCache
        assertThat(first.orders).containsExactly(cachedOrder)

        val second = emissions[1] as LoadOrdersResult.Error
        assertThat(second.message).isEqualTo("generic error")

        verify(ordersCache).getAll()
        verify(ordersCache, never()).setAll(any())
        verify(orderRestClient).fetchOrders(
            site = any(),
            count = any(),
            page = any(),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            createdVia = any(),
            searchQuery = anyOrNull()
        )
    }

    @Test
    fun `when cache empty and fetch returns empty, then SuccessRemote empty and store empty`() = runTest {
        whenever(ordersCache.getAll()).thenReturn(emptyList())

        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = emptyList()
        )
        whenever(
            orderRestClient.fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                createdVia = any(),
                searchQuery = anyOrNull()
            )
        ).thenReturn(payload)

        val emissions = sut.loadOrders().toList(mutableListOf())

        assertThat(emissions).hasSize(1)

        val first = emissions[0] as LoadOrdersResult.SuccessRemote
        assertThat(first.orders).isEmpty()

        verify(selectedSite).get()
        verify(ordersCache).getAll()
        verify(ordersCache).setAll(emptyList())
        verify(orderRestClient).fetchOrders(
            site = any(),
            count = any(),
            page = any(),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            createdVia = any(),
            searchQuery = anyOrNull()
        )
    }

    @Test
    fun `given search query, when searchOrders succeeds, then return SearchOrdersResult Success with mapped orders`() = runTest {
        // GIVEN
        val query = "test order"
        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 11)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 22)
        val entities = listOf(e1 to emptyList<WCMetaData>(), e2 to emptyList())
        val mapped1 = OrderTestUtils.generateTestOrder()
        val mapped2 = OrderTestUtils.generateTestOrder()
        whenever(orderMapper.toAppModel(e1)).thenReturn(mapped1)
        whenever(orderMapper.toAppModel(e2)).thenReturn(mapped2)

        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = entities
        )
        whenever(
            orderRestClient.fetchOrders(
                site = siteModel,
                count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
                page = 1,
                orderBy = OrderRestClient.OrderBy.DATE,
                sortOrder = OrderRestClient.SortOrder.DESCENDING,
                statusFilter = null,
                createdVia = "pos-rest-api",
                searchQuery = query
            )
        ).thenReturn(payload)

        // WHEN
        val result = sut.searchOrders(query)

        // THEN
        assertThat(result).isInstanceOf(SearchOrdersResult.Success::class.java)
        val success = result as SearchOrdersResult.Success
        assertThat(success.orders).containsExactly(mapped1, mapped2)

        verify(orderRestClient).fetchOrders(
            site = siteModel,
            count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
            page = 1,
            orderBy = OrderRestClient.OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api",
            searchQuery = query
        )
    }

    @Test
    fun `given search query, when searchOrders fails, then return SearchOrdersResult Error`() = runTest {
        // GIVEN
        val query = "test order"
        val orderError = WCOrderStore.OrderError(
            type = WCOrderStore.OrderErrorType.GENERIC_ERROR,
            message = "search error"
        )
        val payload = WCOrderStore.FetchOrdersResponsePayload(
            error = orderError,
            site = siteModel
        )
        whenever(
            orderRestClient.fetchOrders(
                site = siteModel,
                count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
                page = 1,
                orderBy = OrderRestClient.OrderBy.DATE,
                sortOrder = OrderRestClient.SortOrder.DESCENDING,
                statusFilter = null,
                createdVia = "pos-rest-api",
                searchQuery = query
            )
        ).thenReturn(payload)

        // WHEN
        val result = sut.searchOrders(query)

        // THEN
        assertThat(result).isInstanceOf(SearchOrdersResult.Error::class.java)
        val error = result as SearchOrdersResult.Error
        assertThat(error.message).isEqualTo("search error")
    }

    @Test
    fun `given empty search query, when searchOrders returns no results, then return SearchOrdersResult Success with empty list`() = runTest {
        // GIVEN
        val query = ""
        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = emptyList()
        )
        whenever(
            orderRestClient.fetchOrders(
                site = siteModel,
                count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
                page = 1,
                orderBy = OrderRestClient.OrderBy.DATE,
                sortOrder = OrderRestClient.SortOrder.DESCENDING,
                statusFilter = null,
                createdVia = "pos-rest-api",
                searchQuery = query
            )
        ).thenReturn(payload)

        // WHEN
        val result = sut.searchOrders(query)

        // THEN
        assertThat(result).isInstanceOf(SearchOrdersResult.Success::class.java)
        val success = result as SearchOrdersResult.Success
        assertThat(success.orders).isEmpty()
    }
}
