package com.woocommerce.android.wear.ui.orders

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.ui.login.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCWearableStore

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersRepositoryTest : BaseUnitTest() {
    private lateinit var sut: OrdersRepository
    private val ordersDataStore: DataStore<Preferences> = mock()
    private val loginRepository: LoginRepository = mock()
    private val wearableStore: WCWearableStore = mock()
    private val orderStore: WCOrderStore = mock()
    private val refundStore: WCRefundStore = mock()

    @Before
    fun setUp() {
        sut = OrdersRepository(ordersDataStore, loginRepository, wearableStore, orderStore, refundStore)
    }

    @Test
    fun `getStoredOrders returns orders sorted by descending dateCreated`() = testBlocking {
        val site = SiteModel()
        val order1 = mock<OrderEntity> { on { dateCreated } doReturn "1000" }
        val order2 = mock<OrderEntity> { on { dateCreated } doReturn "2000" }
        val order3 = mock<OrderEntity> { on { dateCreated } doReturn "1500" }
        whenever(orderStore.getOrdersForSite(site)).thenReturn(listOf(order1, order2, order3))

        val result = sut.getStoredOrders(site)

        assertThat(result).containsExactly(order2, order3, order1)
    }

    @Test
    fun `getStoredOrders returns empty list when no orders are available`() = testBlocking {
        val site = SiteModel()
        whenever(orderStore.getOrdersForSite(site)).thenReturn(emptyList())

        val result = sut.getStoredOrders(site)

        assertThat(result).isEmpty()
    }
}
