package com.woocommerce.android.ui.woopos.cashpayment

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.notifications.push.MarkedAsPaidOrdersCache
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.SiteParameters
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils.generateSettings

class WooPosCashPaymentRepositoryTest {

    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val orderStore: WCOrderStore = mock()
    private val orderMapper: OrderMapper = mock()
    private val gatewayStore: WCGatewayStore = mock()
    private val markedAsPaidOrdersCache: MarkedAsPaidOrdersCache = mock()

    private lateinit var repository: WooPosCashPaymentRepository

    @Before
    fun setUp() {
        repository = WooPosCashPaymentRepository(
            selectedSite,
            wooCommerceStore,
            orderStore,
            orderMapper,
            gatewayStore,
            markedAsPaidOrdersCache
        )
    }

    @Test
    fun `given valid orderId and site, when getOrderById, then return mapped order`() = runTest {
        val orderId = 123L
        val site: SiteModel = mock()
        val mockOrder: OrderEntity = mock()
        val mappedOrder: Order = mock()

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.getOrderByIdAndSite(orderId, site)).thenReturn(mockOrder)
        whenever(orderMapper.toAppModel(mockOrder)).thenReturn(mappedOrder)

        val result = repository.getOrderById(orderId)

        assertThat(result).isEqualTo(mappedOrder)
        verify(orderStore).getOrderByIdAndSite(orderId, site)
        verify(orderMapper).toAppModel(mockOrder)
    }

    @Test
    fun `given invalid orderId, when getOrderById, then return null`() = runTest {
        val orderId = 456L
        val site: SiteModel = mock()

        whenever(selectedSite.get()).thenReturn(site)
        whenever(orderStore.getOrderByIdAndSite(orderId, site)).thenReturn(null)

        val result = repository.getOrderById(orderId)

        assertThat(result).isNull()
        verify(orderStore).getOrderByIdAndSite(orderId, site)
    }

    @Test
    fun `given valid orderId and cashPaymentChangeDueAmount, when completeOrder, then return success`() = runTest {
        val orderId = 123L
        val cashPaymentChangeDueAmount = "5"
        val site: SiteModel = mock()
        val gatewayTitle = "Pay in Person"
        val codGateway: WCGatewayModel = mock { on { title }.thenReturn(gatewayTitle) }
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(mock { on { isError }.thenReturn(false) })

        whenever(selectedSite.get()).thenReturn(site)
        whenever(gatewayStore.getGateway(site, "cod")).thenReturn(codGateway)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
        whenever(
            orderStore.updateOrderStatusAndPaymentDetails(
                orderId = orderId,
                site = site,
                newStatus = statusModel,
                newPaymentMethodId = "cod",
                newPaymentMethodTitle = gatewayTitle,
                cashPaymentChangeDueAmount = cashPaymentChangeDueAmount
            )
        ).thenReturn(flowOf(updateResult))

        val result = repository.completeOrder(orderId, cashPaymentChangeDueAmount = cashPaymentChangeDueAmount)

        assertThat(result.isSuccess).isTrue()
        verify(orderStore).updateOrderStatusAndPaymentDetails(
            orderId = orderId,
            site = site,
            newStatus = statusModel,
            newPaymentMethodId = "cod",
            newPaymentMethodTitle = gatewayTitle,
            cashPaymentChangeDueAmount = cashPaymentChangeDueAmount
        )
    }

    @Test
    fun `given valid orderId and cashPaymentChangeDueAmount, when completeOrder, then return failure`() = runTest {
        val orderId = 123L
        val cashPaymentChangeDueAmount = "5"
        val site: SiteModel = mock()
        val gatewayTitle = "Pay in Person"
        val codGateway: WCGatewayModel = mock { on { title }.thenReturn(gatewayTitle) }
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
        val errorMessage = "Order update failed"
        val updateResult = UpdateOrderResult.RemoteUpdateResult(
            event = OnOrderChanged(
                orderError = WCOrderStore.OrderError(
                    message = errorMessage
                )
            )
        )

        whenever(selectedSite.get()).thenReturn(site)
        whenever(gatewayStore.getGateway(site, "cod")).thenReturn(codGateway)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
        whenever(
            orderStore.updateOrderStatusAndPaymentDetails(
                orderId = orderId,
                site = site,
                newStatus = statusModel,
                newPaymentMethodId = "cod",
                newPaymentMethodTitle = gatewayTitle,
                cashPaymentChangeDueAmount = cashPaymentChangeDueAmount
            )
        ).thenReturn(flowOf(updateResult))

        val result = repository.completeOrder(orderId, cashPaymentChangeDueAmount = cashPaymentChangeDueAmount)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(errorMessage)
        verify(orderStore).updateOrderStatusAndPaymentDetails(
            orderId = orderId,
            site = site,
            newStatus = statusModel,
            newPaymentMethodId = "cod",
            newPaymentMethodTitle = gatewayTitle,
            cashPaymentChangeDueAmount = cashPaymentChangeDueAmount
        )
        verifyNoInteractions(markedAsPaidOrdersCache)
    }

    @Test
    fun `given order completion succeeds, when completeOrder, then order is recorded as paid`() = runTest {
        // GIVEN
        val orderId = 123L
        val siteId = 999L
        val site: SiteModel = mock { on { this.siteId }.thenReturn(siteId) }
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())

        whenever(selectedSite.get()).thenReturn(site)
        whenever(gatewayStore.getGateway(site, "cod")).thenReturn(null)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
        whenever(
            orderStore.updateOrderStatusAndPaymentDetails(
                orderId = orderId,
                site = site,
                newStatus = statusModel,
                newPaymentMethodId = "cod",
                newPaymentMethodTitle = "Pay in Person",
                cashPaymentChangeDueAmount = "5"
            )
        ).thenReturn(flowOf(updateResult))

        // WHEN
        repository.completeOrder(orderId, cashPaymentChangeDueAmount = "5")

        // THEN
        verify(markedAsPaidOrdersCache).onOrderMovedToPaidStatus(
            siteId = siteId,
            orderId = orderId,
            newStatusKey = Order.Status.Completed.value,
        )
    }

    @Test
    fun `given site parameters cached, when getCurrencySymbol, then return currency symbol`() = runTest {
        val cachedParams = mock<SiteParameters> {
            on { currencySymbol }.thenReturn("$")
        }
        repository.javaClass.getDeclaredField("cachedParameters").apply {
            isAccessible = true
            set(repository, cachedParams)
        }

        val result = repository.getCurrencySymbol()

        assertThat(result).isEqualTo("$")
    }

    @Test
    fun `given no cached site parameters, when getCurrencySymbol, then load and return currency symbol`() = runTest {
        val site: SiteModel = SiteModel().apply { id = 1 }
        val siteSettings = generateSettings(LocalId(1))
        val currencySymbol = "$"

        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooCommerceStore.getSiteSettings(site)).thenReturn(siteSettings)
        whenever(wooCommerceStore.getSiteCurrency(site, "USD")).thenReturn(currencySymbol)
        whenever(wooCommerceStore.getProductSettings(site)).thenReturn(mock())

        val result = repository.getCurrencySymbol()

        assertThat(result).isEqualTo(currencySymbol)
    }
}
