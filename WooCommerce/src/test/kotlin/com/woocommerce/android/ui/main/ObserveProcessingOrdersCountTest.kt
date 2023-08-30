package com.woocommerce.android.ui.main

import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.greenrobot.eventbus.EventBus
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersCountResult

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveProcessingOrdersCountTest : BaseUnitTest() {
    private val dispatcher = FakeDispatcher()
    private val orderStore: WCOrderStore = mock()
    private val site = SiteModel()
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(site)
    }

    private val sut = ObserveProcessingOrdersCount(
        dispatcher = dispatcher,
        wcOrderStore = orderStore,
        selectedSite = selectedSite
    )

    @Test
    fun `when no site is selected, then emit null`() = testBlocking {
        whenever(selectedSite.observe()).thenReturn(flowOf(null))

        val count = sut.invoke().first()

        assertThat(count).isNull()
    }

    @Test
    fun `when observation begins, then emit the cached count`() = testBlocking {
        whenever(
            orderStore.fetchOrdersCount(site, CoreOrderStatus.PROCESSING.value)
        ).thenReturn(OrdersCountResult.Success(2))
        whenever(
            orderStore.getOrderStatusForSiteAndKey(site, CoreOrderStatus.PROCESSING.value)
        ).thenReturn(
            WCOrderStatusModel(0).apply {
                statusCount = 1
            }
        )

        val count = sut.invoke().first()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `when observation begins, then fetch orders count from API`() = testBlocking {
        whenever(
            orderStore.fetchOrdersCount(site, CoreOrderStatus.PROCESSING.value)
        ).thenReturn(OrdersCountResult.Success(1))

        val count = sut.invoke().drop(1).first()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `when order statuses are fetched, then re-emit new count`() = testBlocking {
        whenever(
            orderStore.fetchOrdersCount(site, CoreOrderStatus.PROCESSING.value)
        ).thenReturn(OrdersCountResult.Success(1))

        whenever(
            orderStore.getOrderStatusForSiteAndKey(site, CoreOrderStatus.PROCESSING.value)
        ).thenReturn(
            WCOrderStatusModel(0).apply {
                statusCount = 1
            }
        ).thenReturn(
            WCOrderStatusModel(0).apply {
                statusCount = 2
            }
        )

        val count = runAndReturnLastValue {
            dispatcher.emitChange(OnOrderStatusOptionsChanged(0))
        }

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `when push notification is received, then re-fetch orders count`() = testBlocking {
        whenever(orderStore.fetchOrdersCount(site, CoreOrderStatus.PROCESSING.value))
            .thenReturn(OrdersCountResult.Success(1))
            .thenReturn(OrdersCountResult.Success(2))

        val count = runAndReturnLastValue {
            EventBus.getDefault().post(
                NotificationReceivedEvent(
                    siteId = site.siteId,
                    channel = NotificationChannelType.NEW_ORDER
                )
            )
        }

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `when an order status is updated, then re-fetch orders count`() = testBlocking {
        whenever(orderStore.fetchOrdersCount(site, CoreOrderStatus.PROCESSING.value))
            .thenReturn(OrdersCountResult.Success(1))
            .thenReturn(OrdersCountResult.Success(2))

        val count = runAndReturnLastValue {
            @Suppress("DEPRECATION")
            dispatcher.emitChange(OnOrderChanged(causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS))
        }

        assertThat(count).isEqualTo(2)
    }

    private suspend fun runAndReturnLastValue(block: suspend () -> Unit) = coroutineScope {
        var count: Int? = null
        val countTask = async {
            sut.invoke().collect {
                count = it
            }
        }

        block()

        countTask.cancel()
        return@coroutineScope count
    }
}
