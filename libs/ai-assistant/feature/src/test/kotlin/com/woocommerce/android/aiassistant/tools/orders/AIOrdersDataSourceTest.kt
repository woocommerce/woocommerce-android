package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.SortOrder
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore

@ExperimentalCoroutinesApi
class AIOrdersDataSourceTest {

    private val site: SiteModel = SiteModel().apply { id = 42 }
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(site) }
    private val orderStore: WCOrderStore = mock()

    private val dataSource = AIOrdersDataSource(
        selectedSite = selectedSite,
        orderStore = orderStore,
    )

    private suspend fun stubFetchOrders(result: WooResult<List<OrderEntity>>) {
        whenever(
            orderStore.fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        ).thenReturn(result)
    }

    @Test
    fun `given no search query, when fetchOrders is called, then store is queried with null search and 20 page size`() =
        runTest {
            val entity = OrderEntity(localSiteId = LocalId(1), orderId = 1L)
            stubFetchOrders(WooResult(listOf(entity)))

            val result = dataSource.fetchOrders(search = null)

            assertThat(result.isSuccess).isTrue
            assertThat(result.getOrThrow().orders).containsExactly(entity)
            verify(orderStore).fetchOrders(
                site = any(),
                count = eq(20),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = eq(null),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = eq(false),
            )
        }

    @Test
    fun `given a search query, when fetchOrders is called, then search term is forwarded to the store`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(search = "alice")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = eq("alice"),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given a blank search query, when fetchOrders is called, then search is normalised to null`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(search = "   ")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = eq(null),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given the store returns an error, when fetchOrders is called, then a failure result is returned`() =
        runTest {
            stubFetchOrders(WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom")))

            val result = dataSource.fetchOrders(search = null)

            assertThat(result.isFailure).isTrue
        }

    @Test
    fun `given 20 orders returned, when fetchOrders is called, then canLoadMore is true`() =
        runTest {
            val entities = (1..20).map { i -> OrderEntity(localSiteId = LocalId(1), orderId = i.toLong()) }
            stubFetchOrders(WooResult(entities))

            val result = dataSource.fetchOrders(search = null)

            assertThat(result.getOrThrow().canLoadMore).isTrue
        }

    @Test
    fun `given fewer than 20 orders returned, when fetchOrders is called, then canLoadMore is false`() =
        runTest {
            val entities = (1..5).map { i -> OrderEntity(localSiteId = LocalId(1), orderId = i.toLong()) }
            stubFetchOrders(WooResult(entities))

            val result = dataSource.fetchOrders(search = null)

            assertThat(result.getOrThrow().canLoadMore).isFalse
        }

    @Test
    fun `given customer filter, when fetchOrders is called, then customer is forwarded to the store`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(customer = 99L)

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = eq(99L),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given include list, when fetchOrders is called, then include is forwarded to the store`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(include = listOf(1L, 2L, 3L))

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = eq(listOf(1L, 2L, 3L)),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given after and before filters, when fetchOrders is called, then they are forwarded to the store`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(after = "2024-01-01T00:00:00", before = "2024-12-31T23:59:59")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = eq("2024-01-01T00:00:00"),
                before = eq("2024-12-31T23:59:59"),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given orderby = modified, when fetchOrders is called, then OrderBy MODIFIED is forwarded`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(orderby = "modified")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = eq(OrderBy.MODIFIED),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given order = asc, when fetchOrders is called, then SortOrder ASCENDING is forwarded`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(order = "asc")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = eq(SortOrder.ASCENDING),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given unknown orderby value, when fetchOrders is called, then OrderBy DATE is used as fallback`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(orderby = "unknown_value")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = eq(OrderBy.DATE),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given unknown order value, when fetchOrders is called, then SortOrder DESCENDING is used as fallback`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(order = "DESC")

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = eq(SortOrder.DESCENDING),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = anyOrNull(),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `given empty include list, when fetchOrders is called, then include is normalised to null`() =
        runTest {
            stubFetchOrders(WooResult(emptyList()))

            dataSource.fetchOrders(include = emptyList())

            verify(orderStore).fetchOrders(
                site = any(),
                count = any(),
                page = any(),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                searchQuery = anyOrNull(),
                customer = anyOrNull(),
                include = eq(null),
                after = anyOrNull(),
                before = anyOrNull(),
                deleteOldData = any(),
            )
        }

    @Test
    fun `when getOrder is called, then fetchSingleOrderSync is called`() =
        runTest {
            val entity = OrderEntity(localSiteId = LocalId(1), orderId = 123L)
            whenever(orderStore.getOrderByIdAndSite(123L, site)).thenReturn(null)
            whenever(orderStore.fetchSingleOrderSync(site, 123L)).thenReturn(WooResult(entity))

            val result = dataSource.getOrder(orderId = 123L)

            assertThat(result.isSuccess).isTrue
            assertThat(result.getOrThrow()).isEqualTo(entity)
            verify(orderStore).fetchSingleOrderSync(site, 123L)
        }

    @Test
    fun `given network returns an error, when getOrder is called, then a failure result is returned`() =
        runTest {
            whenever(orderStore.getOrderByIdAndSite(123L, site)).thenReturn(null)
            whenever(orderStore.fetchSingleOrderSync(site, 123L))
                .thenReturn(WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom")))

            val result = dataSource.getOrder(orderId = 123L)

            assertThat(result.isFailure).isTrue
        }

    @Test
    fun `given remote update succeeds, when updateOrderStatus is called, then success result is returned`() =
        runTest {
            val successFlow = flowOf(
                WCOrderStore.UpdateOrderResult.OptimisticUpdateResult(WCOrderStore.OnOrderChanged()),
                WCOrderStore.UpdateOrderResult.RemoteUpdateResult(WCOrderStore.OnOrderChanged()),
            )
            whenever(orderStore.updateOrderStatus(eq(123L), eq(site), any())).thenReturn(successFlow)

            val result = dataSource.updateOrderStatus(orderId = 123L, newStatus = "processing")

            assertThat(result.isSuccess).isTrue
        }

    @Test
    fun `given remote update fails, when updateOrderStatus is called, then failure result is returned`() =
        runTest {
            val errorEvent = WCOrderStore.OnOrderChanged(
                orderError = WCOrderStore.OrderError(message = "update failed")
            )
            val errorFlow = flowOf(
                WCOrderStore.UpdateOrderResult.RemoteUpdateResult(errorEvent),
            )
            whenever(orderStore.updateOrderStatus(eq(123L), eq(site), any())).thenReturn(errorFlow)

            val result = dataSource.updateOrderStatus(orderId = 123L, newStatus = "processing")

            assertThat(result.isFailure).isTrue
        }

    @Test
    fun `given order not found in store, when updateOrderStatus is called, then failure result is returned`() =
        runTest {
            val notFoundFlow = flowOf(
                WCOrderStore.UpdateOrderResult.OptimisticUpdateResult(
                    WCOrderStore.OnOrderChanged(
                        orderError = WCOrderStore.OrderError(message = "Order not found")
                    )
                )
            )
            whenever(orderStore.updateOrderStatus(eq(123L), eq(site), any())).thenReturn(notFoundFlow)

            val result = dataSource.updateOrderStatus(orderId = 123L, newStatus = "processing")

            assertThat(result.isFailure).isTrue
        }
}
