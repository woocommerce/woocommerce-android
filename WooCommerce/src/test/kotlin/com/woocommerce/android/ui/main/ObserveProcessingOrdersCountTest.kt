package com.woocommerce.android.ui.main

import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveProcessingOrdersCountTest : BaseUnitTest() {
    private val dispatcher = FakeDispatcher { action ->
        if (action.type == WCOrderAction.FETCH_ORDER_STATUS_OPTIONS) {
            emitChange(
                OnOrderStatusOptionsChanged(0)
            )
        }
    }
    private val site = SiteModel()
    private val orderStore: WCOrderStore = mock {
        on { observeOrderCountForSite(site) } doReturn emptyFlow()
    }
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(site)
    }

    private val sut = ObserveProcessingOrdersCount(
        dispatcher = dispatcher,
        wcOrderStore = orderStore,
        selectedSite = selectedSite,
        dispatchers = coroutinesTestRule.testDispatchers
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
    fun `when order statuses are fetched, then re-emit new count`() = testBlocking {
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
    fun `when push notification is received, then re-fetch orders status options`() = testBlocking {
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
            EventBus.getDefault().post(
                NotificationReceivedEvent(
                    siteId = site.siteId,
                    channel = NotificationChannelType.NEW_ORDER
                )
            )
            advanceUntilIdle()
        }

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `when an order status is updated, then re-fetch orders status options`() = testBlocking {
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
            @Suppress("DEPRECATION")
            dispatcher.emitChange(OnOrderChanged(causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS))
            advanceUntilIdle()
        }

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `when orders count change, then re-fetch orders status options`() = testBlocking {
        whenever(orderStore.observeOrderCountForSite(site))
            .thenReturn(
                flow {
                    emit(1)
                    delay(1000)
                    emit(2)
                }
            )
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
            advanceUntilIdle()
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
