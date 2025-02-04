package com.woocommerce.android.ui.woopos.cashpayment

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.SiteParameters
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WooCommerceStore

class WooPosCashPaymentRepositoryTest {

    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val orderStore: WCOrderStore = mock()
    private val orderMapper: OrderMapper = mock()
    private val gatewayStore: WCGatewayStore = mock()

    private lateinit var repository: WooPosCashPaymentRepository

    @Before
    fun setUp() {
        repository = WooPosCashPaymentRepository(
            selectedSite,
            wooCommerceStore,
            orderStore,
            orderMapper,
            gatewayStore
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
    fun `given valid orderId, when completeOrder, then return success`() = runTest {
        val orderId = 123L
        val site: SiteModel = mock()
        val gatewayTitle = "Pay in Person"
        val codGateway: WCGatewayModel = mock { on { title }.thenReturn(gatewayTitle) }
        val statusModel = WCOrderStatusModel(statusKey = Order.Status.Completed.value)
        val updateResult = UpdateOrderResult.RemoteUpdateResult(mock { on { isError }.thenReturn(false) })

        whenever(selectedSite.get()).thenReturn(site)
        whenever(gatewayStore.getGateway(site, "cod")).thenReturn(codGateway)
        whenever(orderStore.getOrderStatusForSiteAndKey(site, Order.Status.Completed.value)).thenReturn(statusModel)
        whenever(
            orderStore.updateOrderStatusAndPaymentMethod(
                orderId = orderId,
                site = site,
                newStatus = statusModel,
                newPaymentMethodId = "cod",
                newPaymentMethodTitle = gatewayTitle
            )
        ).thenReturn(flowOf(updateResult))

        val result = repository.completeOrder(orderId)

        assertThat(result.isSuccess).isTrue()
        verify(orderStore).updateOrderStatusAndPaymentMethod(
            orderId = orderId,
            site = site,
            newStatus = statusModel,
            newPaymentMethodId = "cod",
            newPaymentMethodTitle = gatewayTitle
        )
    }

    @Test
    fun `given valid orderId, when completeOrder, then return failure`() = runTest {
        val orderId = 123L
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
            orderStore.updateOrderStatusAndPaymentMethod(
                orderId = orderId,
                site = site,
                newStatus = statusModel,
                newPaymentMethodId = "cod",
                newPaymentMethodTitle = gatewayTitle
            )
        ).thenReturn(flowOf(updateResult))

        val result = repository.completeOrder(orderId)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(errorMessage)
        verify(orderStore).updateOrderStatusAndPaymentMethod(
            orderId = orderId,
            site = site,
            newStatus = statusModel,
            newPaymentMethodId = "cod",
            newPaymentMethodTitle = gatewayTitle
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
        val site: SiteModel = mock()
        val siteSettings: WCSettingsModel = mock {
            on { currencyCode }.thenReturn("USD")
            on { currencyThousandSeparator }.thenReturn(",")
            on { currencyDecimalSeparator }.thenReturn(".")
            on { currencyPosition }.thenReturn(WCSettingsModel.CurrencyPosition.LEFT)
        }
        val currencySymbol = "$"

        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooCommerceStore.getSiteSettings(site)).thenReturn(siteSettings)
        whenever(wooCommerceStore.getSiteCurrency(site, "USD")).thenReturn(currencySymbol)
        whenever(wooCommerceStore.getProductSettings(site)).thenReturn(mock())

        val result = repository.getCurrencySymbol()

        assertThat(result).isEqualTo(currencySymbol)
    }
}
