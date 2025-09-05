package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosOrdersInMemoryCache
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

    @Rule
    @JvmField
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
    fun `given cache and successful fetch, when loadOrders collected, then emits cache first then mapped network and stores in cache`() = runTest {
        // GIVEN
        val cachedOrder = OrderTestUtils.generateTestOrder()
        whenever(ordersCache.getAll()).thenReturn(listOf(cachedOrder))

        // Network returns two entities
        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 1)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 2)
        val entities = listOf(
            e1 to emptyList<WCMetaData>(),
            e2 to emptyList<WCMetaData>()
        )

        val firstOrder = OrderTestUtils.generateTestOrder()
        val secondOrder = OrderTestUtils.generateTestOrder()

        whenever(orderMapper.toAppModel(e1)).thenReturn(firstOrder)
        whenever(orderMapper.toAppModel(e2)).thenReturn(secondOrder)

        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = entities
        )

        whenever(
            orderRestClient.fetchOrders(
                site = eq(siteModel),
                count = eq(25),
                page = eq(1),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                createdVia = eq("pos-rest-api")
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)
        // First emission = cache
        val first = emissions[0]
        assertThat(first).isInstanceOf(LoadOrdersResult.Success::class.java)
        assertThat((first as LoadOrdersResult.Success).orders).containsExactly(cachedOrder)

        // Second emission = network mapped
        val second = emissions[1]
        assertThat(second).isInstanceOf(LoadOrdersResult.Success::class.java)
        assertThat((second as LoadOrdersResult.Success).orders).containsExactly(firstOrder, secondOrder)

        verify(selectedSite).get()
        verify(ordersCache).getAll()
        verify(ordersCache).addAll(listOf(firstOrder, secondOrder))
        verify(orderRestClient).fetchOrders(
            site = eq(siteModel),
            count = eq(25),
            page = eq(1),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            createdVia = eq("pos-rest-api")
        )
    }

    @Test
    fun `given cache and fetch error, when loadOrders collected, then emits cache then error without caching`() = runTest {
        // GIVEN
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
                site = eq(siteModel),
                count = eq(25),
                page = eq(1),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                createdVia = eq("pos-rest-api")
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)

        val first = emissions[0]
        assertThat(first).isInstanceOf(LoadOrdersResult.Success::class.java)
        assertThat((first as LoadOrdersResult.Success).orders).containsExactly(cachedOrder)

        val second = emissions[1]
        assertThat(second).isInstanceOf(LoadOrdersResult.Error::class.java)
        assertThat((second as LoadOrdersResult.Error).message).isEqualTo("generic error")

        verify(ordersCache).getAll()
        verify(ordersCache, never()).addAll(any())
        verify(orderRestClient).fetchOrders(
            site = eq(siteModel),
            count = eq(25),
            page = eq(1),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            createdVia = eq("pos-rest-api")
        )
    }

    @Test
    fun `given empty cache, when loadOrders collected, then forwards params including createdVia and emits empty then empty`() = runTest {
        // GIVEN
        whenever(ordersCache.getAll()).thenReturn(emptyList())

        val payload = WCOrderStore.FetchOrdersResponsePayload(
            site = siteModel,
            ordersWithMeta = emptyList()
        )
        whenever(
            orderRestClient.fetchOrders(
                site = eq(siteModel),
                count = eq(25),
                page = eq(1),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                createdVia = eq("pos-rest-api")
            )
        ).thenReturn(payload)

        // WHEN
        val emissions = sut.loadOrders().toList(mutableListOf())

        // THEN
        assertThat(emissions).hasSize(2)

        val first = emissions[0]
        assertThat(first).isInstanceOf(LoadOrdersResult.Success::class.java)
        assertThat((first as LoadOrdersResult.Success).orders).isEmpty()

        val second = emissions[1]
        assertThat(second).isInstanceOf(LoadOrdersResult.Success::class.java)
        assertThat((second as LoadOrdersResult.Success).orders).isEmpty()

        verify(selectedSite).get()
        verify(ordersCache).getAll()
        verify(ordersCache).addAll(emptyList())
        verify(orderRestClient).fetchOrders(
            site = eq(siteModel),
            count = eq(25),
            page = eq(1),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            createdVia = eq("pos-rest-api")
        )
    }
}
