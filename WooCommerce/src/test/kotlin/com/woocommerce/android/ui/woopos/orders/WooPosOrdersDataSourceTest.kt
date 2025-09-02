package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
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

    private val sut = WooPosOrdersDataSource(
        restClient = orderRestClient,
        selectedSite = selectedSite,
        orderMapper = orderMapper
    )

    @Test
    fun `given rest client returns entities, when loadOrders called, then should map them to app models`() = runTest {
        // GIVEN
        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 1)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 2)
        val entities = listOf(
            e1 to emptyList<WCMetaData>(),
            e2 to emptyList<WCMetaData>()
        )
        val o1 = mock<Order>()
        val o2 = mock<Order>()

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

        whenever(orderMapper.toAppModel(e1)).thenReturn(o1)
        whenever(orderMapper.toAppModel(e2)).thenReturn(o2)

        // WHEN
        val result = sut.loadOrders()

        // THEN
        assertThat(result).isInstanceOf(LoadOrdersResult.Success::class.java)
        val success = result as LoadOrdersResult.Success
        assertThat(success.orders).containsExactly(o1, o2)

        verify(selectedSite).get()
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
    fun `given store returns error, when loadOrders called, then should return error result`() = runTest {
        // GIVEN
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
        val result = sut.loadOrders()

        // THEN
        assertThat(result).isInstanceOf(LoadOrdersResult.Error::class.java)
        val error = result as LoadOrdersResult.Error
        assertThat(error.error).isEqualTo(
            WooError(
                org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR,
                org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR,
                orderError.message
            )
        )

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
    fun `given default site and pagination, when loadOrders called, then should forward params including createdVia`() = runTest {
        // GIVEN
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
        val result = sut.loadOrders()

        // THEN
        assertThat(result).isInstanceOf(LoadOrdersResult.Success::class.java)
        val success = result as LoadOrdersResult.Success
        assertThat(success.orders).isEmpty()

        verify(selectedSite).get()
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
