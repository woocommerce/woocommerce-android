package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
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
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds = mock()

    private val sut = WooPosOrdersDataSource(
        restClient = orderRestClient,
        selectedSite = selectedSite,
        orderMapper = orderMapper,
        ordersCache = ordersCache,
        retrieveOrderRefunds = retrieveOrderRefunds
    )

    @Before
    fun setUp() = runTest {
        whenever(retrieveOrderRefunds.invoke(any(), any())).thenReturn(Result.success(emptyList()))
    }

    @Test
    fun `when cache has data and fetch succeeds, then emit SuccessCache then SuccessRemote and store mapped in cache`() = runTest {
        // GIVEN
        val cachedOrder = OrderTestUtils.generateTestOrder(orderId = 1L)
        whenever(ordersCache.getAll()).thenReturn(listOf(cachedOrder))

        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 11L)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 22L)
        val entities = listOf(e1 to emptyList<WCMetaData>(), e2 to emptyList())

        val mapped1 = OrderTestUtils.generateTestOrder(orderId = 11L)
        val mapped2 = OrderTestUtils.generateTestOrder(orderId = 22L)
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
                searchQuery = anyOrNull(),
                decimalPoints = anyInt(),
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)

        val first = emissions[0] as LoadOrdersResult.SuccessCache
        assertThat(first.ordersWithRefunds.keys).containsExactly(cachedOrder)

        val second = emissions[1] as LoadOrdersResult.SuccessRemote
        assertThat(second.ordersWithRefunds.keys).containsExactly(mapped1, mapped2)

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
            searchQuery = anyOrNull(),
            decimalPoints = anyInt(),
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
                searchQuery = anyOrNull(),
                decimalPoints = anyInt(),
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)

        val first = emissions[0] as LoadOrdersResult.SuccessCache
        assertThat(first.ordersWithRefunds.keys).containsExactly(cachedOrder)

        val second = emissions[1] as LoadOrdersResult.Error
        assertThat(second.message).isEqualTo("[GENERIC_ERROR] generic error")

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
            searchQuery = anyOrNull(),
            decimalPoints = anyInt(),
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
                searchQuery = anyOrNull(),
                decimalPoints = anyInt(),
            )
        ).thenReturn(payload)

        val emissions = sut.loadOrders().toList(mutableListOf())

        assertThat(emissions).hasSize(1)

        val first = emissions[0] as LoadOrdersResult.SuccessRemote
        assertThat(first.ordersWithRefunds).isEmpty()

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
            searchQuery = anyOrNull(),
            decimalPoints = anyInt(),
        )
    }

    @Test
    fun `given search query, when searchOrders succeeds, then return SearchOrdersResult Success with mapped orders`() = runTest {
        // GIVEN
        val query = "test order"
        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 11)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 22)
        val entities = listOf(e1 to emptyList<WCMetaData>(), e2 to emptyList())

        val mapped1 = OrderTestUtils.generateTestOrder(orderId = 11L)
        val mapped2 = OrderTestUtils.generateTestOrder(orderId = 22L)
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
                searchQuery = query,
                decimalPoints = 8,
            )
        ).thenReturn(payload)

        // WHEN
        val result = sut.searchOrders(query)

        // THEN
        assertThat(result).isInstanceOf(SearchOrdersResult.Success::class.java)
        val success = result as SearchOrdersResult.Success
        assertThat(success.ordersWithRefunds.keys).containsExactly(mapped1, mapped2)

        verify(orderRestClient).fetchOrders(
            site = siteModel,
            count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
            page = 1,
            orderBy = OrderRestClient.OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api",
            searchQuery = query,
            decimalPoints = 8,
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
                searchQuery = query,
                decimalPoints = 8,
            )
        ).thenReturn(payload)

        // WHEN
        val result = sut.searchOrders(query)

        // THEN
        assertThat(result).isInstanceOf(SearchOrdersResult.Error::class.java)
        val error = result as SearchOrdersResult.Error
        assertThat(error.message).isEqualTo("[GENERIC_ERROR] search error")
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
                searchQuery = query,
                decimalPoints = 8,
            )
        ).thenReturn(payload)

        // WHEN
        val result = sut.searchOrders(query)

        // THEN
        assertThat(result).isInstanceOf(SearchOrdersResult.Success::class.java)
        val success = result as SearchOrdersResult.Success
        assertThat(success.ordersWithRefunds).isEmpty()
    }

    @Test
    fun `when loadOrders then fetches page 1, caches mapped, then sets hasMorePages`() = runTest {
        whenever(ordersCache.getAll()).thenReturn(emptyList())

        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), orderId = 1L)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), orderId = 2L)
        whenever(orderMapper.toAppModel(e1)).thenReturn(OrderTestUtils.generateTestOrder(orderId = 1))
        whenever(orderMapper.toAppModel(e2)).thenReturn(OrderTestUtils.generateTestOrder(orderId = 2))

        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = listOf(e1 to emptyList(), e2 to emptyList()),
            canLoadMore = true
        )
        whenever(orderRestClient.fetchOrders(any(), any(), eq(1), any(), any(), anyOrNull(), any(), isNull(), eq(8)))
            .thenReturn(payload)

        val emissions = sut.loadOrders().toList(mutableListOf())
        val remote = emissions.last() as LoadOrdersResult.SuccessRemote
        assertThat(remote.ordersWithRefunds.keys.map { it.id }).containsExactly(1L, 2L)
        assertThat(sut.hasMorePages).isTrue
    }

    @Test
    fun `given loadOrders, when loadMore called, then fetches page 2 and updates hasMorePages`() = runTest {
        whenever(ordersCache.getAll()).thenReturn(emptyList())

        val firstPayload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = emptyList(),
            canLoadMore = true
        )
        whenever(orderRestClient.fetchOrders(any(), any(), eq(1), any(), any(), anyOrNull(), any(), isNull(), eq(8)))
            .thenReturn(firstPayload)
        sut.loadOrders().toList(mutableListOf())
        assertThat(sut.hasMorePages).isTrue

        val e3 = OrderEntity(LocalOrRemoteId.LocalId(1), 3L)
        val e4 = OrderEntity(LocalOrRemoteId.LocalId(1), 4L)
        whenever(orderMapper.toAppModel(e3)).thenReturn(OrderTestUtils.generateTestOrder(orderId = 3))
        whenever(orderMapper.toAppModel(e4)).thenReturn(OrderTestUtils.generateTestOrder(orderId = 4))

        val page2Payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = listOf(e3 to emptyList(), e4 to emptyList()),
            canLoadMore = false
        )
        whenever(orderRestClient.fetchOrders(any(), any(), eq(2), any(), any(), anyOrNull(), any(), isNull(), eq(8)))
            .thenReturn(page2Payload)

        val result = sut.loadMore()
        assertThat(result.isSuccess).isTrue
        val ordersWithRefunds = result.getOrThrow()
        assertThat(ordersWithRefunds.keys.map { it.id }).containsExactly(3L, 4L)
        assertThat(sut.hasMorePages).isFalse
    }

    @Test
    fun `when loadMore fails, then page does not advance and hasMorePages unchanged`() = runTest {
        whenever(ordersCache.getAll()).thenReturn(emptyList())

        val firstPayload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = emptyList(),
            canLoadMore = true
        )
        whenever(orderRestClient.fetchOrders(any(), any(), eq(1), any(), any(), anyOrNull(), any(), isNull(), eq(8)))
            .thenReturn(firstPayload)
        sut.loadOrders().toList(mutableListOf())
        assertThat(sut.hasMorePages).isTrue

        val error = WCOrderStore.OrderError(
            type = WCOrderStore.OrderErrorType.GENERIC_ERROR,
            message = "boom"
        )
        val errorPayload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            error = error
        )
        whenever(orderRestClient.fetchOrders(any(), any(), eq(2), any(), any(), anyOrNull(), any(), isNull(), eq(8)))
            .thenReturn(errorPayload)

        val result = sut.loadMore()
        assertThat(result.isFailure).isTrue
        assertThat(sut.hasMorePages).isTrue // unchanged
    }

    @Test
    @Suppress("LongMethod")
    fun `given search first page with more pages, when loadMore with same query succeeds, then maps page2 and updates hasMorePages`() = runTest {
        // GIVEN
        val query = "abc"
        val e1 = OrderEntity(LocalOrRemoteId.LocalId(1), 11L)
        val e2 = OrderEntity(LocalOrRemoteId.LocalId(1), 22L)
        val mapped1 = OrderTestUtils.generateTestOrder(orderId = 11)
        val mapped2 = OrderTestUtils.generateTestOrder(orderId = 22)
        whenever(orderMapper.toAppModel(e1)).thenReturn(mapped1)
        whenever(orderMapper.toAppModel(e2)).thenReturn(mapped2)
        val page1Payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = listOf(e1 to emptyList(), e2 to emptyList()),
            canLoadMore = true
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
                searchQuery = query,
                decimalPoints = 8,
            )
        ).thenReturn(page1Payload)
        val first = sut.searchOrders(query)
        assertThat(first).isInstanceOf(SearchOrdersResult.Success::class.java)
        assertThat(sut.hasMorePages).isTrue
        val e3 = OrderEntity(LocalOrRemoteId.LocalId(1), 33L)
        val e4 = OrderEntity(LocalOrRemoteId.LocalId(1), 44L)
        val mapped3 = OrderTestUtils.generateTestOrder(orderId = 33)
        val mapped4 = OrderTestUtils.generateTestOrder(orderId = 44)
        whenever(orderMapper.toAppModel(e3)).thenReturn(mapped3)
        whenever(orderMapper.toAppModel(e4)).thenReturn(mapped4)
        val page2Payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = listOf(e3 to emptyList(), e4 to emptyList()),
            canLoadMore = false
        )
        whenever(
            orderRestClient.fetchOrders(
                site = siteModel,
                count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
                page = 2,
                orderBy = OrderRestClient.OrderBy.DATE,
                sortOrder = OrderRestClient.SortOrder.DESCENDING,
                statusFilter = null,
                createdVia = "pos-rest-api",
                searchQuery = query,
                decimalPoints = 8,
            )
        ).thenReturn(page2Payload)

        // WHEN
        val result = sut.loadMore(query)

        // THEN
        assertThat(result.isSuccess).isTrue
        val ordersWithRefunds = result.getOrThrow()
        assertThat(ordersWithRefunds.keys.map { it.id }).containsExactly(33L, 44L)
        assertThat(sut.hasMorePages).isFalse
        verify(orderRestClient).fetchOrders(
            site = siteModel,
            count = WooPosOrdersDataSource.POS_ORDERS_PAGE_SIZE,
            page = 2,
            orderBy = OrderRestClient.OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api",
            searchQuery = query,
            decimalPoints = 8,
        )
    }

    @Test
    fun `given orderId, when refreshOrderById succeeds, then returns mapped order`() = runTest {
        // GIVEN
        val orderId = 123L
        val entity = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), orderId = orderId)
        val mapped = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderMapper.toAppModel(entity)).thenReturn(mapped)

        val payload = WCOrderStore.RemoteOrderPayload.Fetching(
            orderWithMeta = entity to emptyList<WCMetaData>(),
            site = siteModel
        )
        whenever(orderRestClient.fetchSingleOrder(siteModel, orderId, 8)).thenReturn(payload)

        // WHEN
        val result = sut.refreshOrderById(orderId)

        // THEN
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrThrow()).isEqualTo(mapped)

        verify(selectedSite).get()
        verify(orderRestClient).fetchSingleOrder(siteModel, orderId, 8)
        verify(orderMapper).toAppModel(entity)
    }

    @Test
    fun `given order in cache, when refreshOrderById succeeds, then replaces cached order and keeps list size`() = runTest {
        // GIVEN
        val orderId = 42L
        val site = siteModel

        val cached1 = OrderTestUtils.generateTestOrder(orderId = 1)
        val cachedTarget = OrderTestUtils.generateTestOrder(orderId = orderId)
        val cached3 = OrderTestUtils.generateTestOrder(orderId = 3)
        whenever(ordersCache.getAll()).thenReturn(listOf(cached1, cachedTarget, cached3))

        val entity = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), orderId = orderId)
        val mappedUpdated = OrderTestUtils.generateTestOrder(orderId = orderId) // different instance
        whenever(orderMapper.toAppModel(entity)).thenReturn(mappedUpdated)

        val payload = WCOrderStore.RemoteOrderPayload.Fetching(
            orderWithMeta = entity to emptyList<WCMetaData>(),
            site = site
        )
        whenever(orderRestClient.fetchSingleOrder(site, orderId, 8)).thenReturn(payload)

        // WHEN
        val result = sut.refreshOrderById(orderId)

        // THEN
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrThrow()).isEqualTo(mappedUpdated)

        val listCaptor = argumentCaptor<List<com.woocommerce.android.model.Order>>()
        verify(ordersCache).getAll()
        verify(ordersCache).setAll(listCaptor.capture())

        val finalList = listCaptor.firstValue
        assertThat(finalList).hasSize(3)

        assertThat(finalList[0]).isEqualTo(cached1)
        assertThat(finalList[1]).isEqualTo(mappedUpdated)
        assertThat(finalList[2]).isEqualTo(cached3)
    }
}
