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
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersDataSourceTest {

    @Rule @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val orderStore: WCOrderStore = mock()
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(siteModel) }
    private val orderMapper: OrderMapper = mock()

    private val sut = WooPosOrdersDataSource(
        orderStore = orderStore,
        selectedSite = selectedSite,
        orderMapper = orderMapper
    )

    @Test
    fun `given store returns entities, when loadOrders called, then should map them to app models`() = runTest {
        // GIVEN
        val e1 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 1)
        val e2 = OrderEntity(localSiteId = LocalOrRemoteId.LocalId(1), 2)
        val entities = listOf(e1, e2)
        val o1 = mock<Order>()
        val o2 = mock<Order>()

        whenever(
            orderStore.fetchOrders(
                site = eq(siteModel),
                count = eq(25),
                page = eq(1),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                deleteOldData = eq(true),
                createdVia = eq("pos-rest-api")
            )
        ).thenReturn(WooResult(entities))

        whenever(orderMapper.toAppModel(e1)).thenReturn(o1)
        whenever(orderMapper.toAppModel(e2)).thenReturn(o2)

        // WHEN
        val result = sut.loadOrders()

        // THEN
        assertThat(result.isError).isFalse()
        assertThat(result.model).containsExactly(o1, o2)
        verify(selectedSite).get()
        verify(orderStore).fetchOrders(
            site = eq(siteModel),
            count = eq(25),
            page = eq(1),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            deleteOldData = eq(true),
            createdVia = eq("pos-rest-api")
        )
    }

    @Test
    fun `given store returns error, when loadOrders called, then should return error result`() = runTest {
        // GIVEN
        val wooError = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.UNKNOWN,
            message = "Network down"
        )

        whenever(
            orderStore.fetchOrders(
                site = eq(siteModel),
                count = eq(25),
                page = eq(1),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                deleteOldData = eq(true),
                createdVia = eq("pos-rest-api")
            )
        ).thenReturn(WooResult(wooError))

        // WHEN
        val result = sut.loadOrders()

        // THEN
        assertThat(result.isError).isTrue()
        assertThat(result.error).isEqualTo(wooError)
        verify(orderStore).fetchOrders(
            site = eq(siteModel),
            count = eq(25),
            page = eq(1),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            deleteOldData = eq(true),
            createdVia = eq("pos-rest-api")
        )
    }

    @Test
    fun `given default site and pagination, when loadOrders called, then should forward params including createdVia`() = runTest {
        // GIVEN
        whenever(
            orderStore.fetchOrders(
                site = eq(siteModel),
                count = eq(25),
                page = eq(1),
                orderBy = any(),
                sortOrder = any(),
                statusFilter = anyOrNull(),
                deleteOldData = eq(true),
                createdVia = eq("pos-rest-api")
            )
        ).thenReturn(WooResult(emptyList<OrderEntity>()))

        // WHEN
        val result = sut.loadOrders()

        // THEN
        assertThat(result.isError).isFalse()
        assertThat(result.model).isEmpty()
        verify(selectedSite).get()
        verify(orderStore).fetchOrders(
            site = eq(siteModel),
            count = eq(25),
            page = eq(1),
            orderBy = any(),
            sortOrder = any(),
            statusFilter = anyOrNull(),
            deleteOldData = eq(true),
            createdVia = eq("pos-rest-api")
        )
    }
}
