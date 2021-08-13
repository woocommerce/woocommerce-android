package com.woocommerce.android.push

import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.PROCESSING
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload

@RunWith(MockitoJUnitRunner::class)
class NotificationHandlerTest {
    private lateinit var sut: NotificationHandler

    private val siteStore: SiteStore = mock {
        on { getSiteBySiteId(any()) } doReturn SiteModel()
    }
    private val dispatcher: Dispatcher = mock()
    private val actionCaptor: KArgumentCaptor<Action<*>> = argumentCaptor()

    @Before
    fun setUp() {
        sut = NotificationHandler(
            notificationStore = mock(),
            siteStore = siteStore,
            dispatcher = dispatcher,
            wcOrderStore = mock()
        )
    }

    @Test
    fun `should request all orders diff fetch when dispatching new order events`() {
        sut.dispatchNewOrderEvents(model = NotificationModel())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(FetchOrderListPayload::class.java)
            assertThat((it as FetchOrderListPayload).listDescriptor.statusFilter).isNull()
        }
    }

    @Test
    fun `should request processing orders diff fetch when dispatching new order events`() {
        sut.dispatchNewOrderEvents(model = NotificationModel())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(FetchOrderListPayload::class.java)
            assertThat((it as FetchOrderListPayload).listDescriptor.statusFilter).isEqualTo(PROCESSING.value)
        }
    }
}
