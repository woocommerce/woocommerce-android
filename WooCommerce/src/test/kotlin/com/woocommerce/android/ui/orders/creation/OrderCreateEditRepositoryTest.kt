package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderAttributionOrigin
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.notifications.push.NewOrderNotificationSuppressionCache
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.taxes.TaxBasedOnSetting
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.model.taxes.TaxBasedOnSettingEntity
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_GIFT_CARDS
import org.wordpress.android.fluxc.wp.site.SitePluginFixtures.createTestSitePlugin
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class OrderCreateEditRepositoryTest : BaseUnitTest() {
    companion object {
        const val DEFAULT_ERROR_MESSAGE = "error_message"
    }

    private lateinit var sut: OrderCreateEditRepository
    private lateinit var trackerWrapper: AnalyticsTrackerWrapper
    private lateinit var orderUpdateStore: OrderUpdateStore
    private lateinit var selectedSite: SelectedSite
    private lateinit var wooCommerceStore: WooCommerceStore
    private val getWooVersion: GetWooCorePluginCachedVersion = mock()
    private val isCurrencyQueryParamSupported: IsCurrencyQueryParamSupported = mock()
    private val orderMapper: OrderMapper = mock {
        on { toAppModel(any()) } doReturn Order.getEmptyOrder(Date(), Date())
    }
    private val newOrderNotificationSuppressionCache: NewOrderNotificationSuppressionCache = mock()

    private val defaultSiteModel = SiteModel()

    @Before
    fun setUp() {
        trackerWrapper = mock()
        selectedSite = mock {
            on { get() } doReturn defaultSiteModel
        }

        orderUpdateStore = mock {
            on {
                createSimplePayment(eq(defaultSiteModel), eq("1"), eq(true), eq(null), eq(null))
            } doReturn WooResult(
                WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR, DEFAULT_ERROR_MESSAGE)
            )
        }

        wooCommerceStore = mock()

        sut = OrderCreateEditRepository(
            selectedSite = selectedSite,
            orderStore = mock(),
            orderUpdateStore = orderUpdateStore,
            orderMapper = orderMapper,
            dispatchers = coroutinesTestRule.testDispatchers,
            wooCommerceStore = wooCommerceStore,
            analyticsTrackerWrapper = trackerWrapper,
            listItemMapper = mock(),
            getWooVersion = getWooVersion,
            isCurrencyQueryParamSupported = isCurrencyQueryParamSupported,
            newOrderNotificationSuppressionCache = newOrderNotificationSuppressionCache,
        )
    }

    @Test
    fun `given simple payment order created, when error, then error track event is tracked`() = testBlocking {
        sut.createSimplePaymentOrder(BigDecimal.ONE)

        verify(trackerWrapper).track(
            AnalyticsEvent.PAYMENTS_FLOW_FAILED,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW
            )
        )
    }

    @Test
    fun `given simple payment order with note, when created, then note is passed`() = testBlocking {
        // GIVEN
        val note = "note"
        whenever(
            orderUpdateStore.createSimplePayment(
                eq(defaultSiteModel),
                eq("1"),
                eq(true),
                eq(null),
                eq(note)
            )
        )
            .thenReturn(WooResult(OrderTestUtils.generateOrder()))

        // WHEN
        sut.createSimplePaymentOrder(BigDecimal.ONE, note)

        // THEN
        verify(orderUpdateStore).createSimplePayment(
            eq(defaultSiteModel),
            eq("1"),
            eq(true),
            eq(null),
            eq(note)
        )
    }

    @Test
    fun `when AUTO_DRAFT is not supported then status is changed to PENDING`() = testBlocking {
        // Given a site using a version that doesn't support AUTO_DRAFT
        whenever(getWooVersion()).thenReturn("6.2.0")
        whenever(orderUpdateStore.createOrder(any(), any(), anyOrNull()))
            .thenReturn(WooResult(OrderTestUtils.generateOrder()))

        val order = Order.getEmptyOrder(Date(), Date()).copy(
            id = 0L,
            status = Order.Status.Custom(Order.Status.AUTO_DRAFT)
        )

        // When the createOrUpdateOrder method is call
        sut.createOrUpdateOrder(order, source = OrderCreationSource.STORE_MANAGEMENT)

        // Then the order status is changed to PENDING
        val request = UpdateOrderRequest(
            status = WCOrderStatusModel(statusKey = CoreOrderStatus.PENDING.value),
            lineItems = emptyList(),
            shippingAddress = null,
            billingAddress = null,
            customerNote = order.customerNote,
            shippingLines = emptyList(),
            feeLines = emptyList(),
            couponLines = emptyList(),
        )

        verify(orderUpdateStore).createOrder(defaultSiteModel, request, OrderAttributionOrigin.Mobile.SOURCE_TYPE_VALUE)
    }

    @Test
    fun `when AUTO_DRAFT is supported then status is keep as AUTO_DRAFT`() = testBlocking {
        // Given a site using a version that support AUTO_DRAFT
        whenever(getWooVersion()).thenReturn(OrderCreateEditRepository.AUTO_DRAFT_SUPPORTED_VERSION)
        whenever(orderUpdateStore.createOrder(any(), any(), anyOrNull()))
            .thenReturn(WooResult(OrderTestUtils.generateOrder()))

        val order = Order.getEmptyOrder(Date(), Date()).copy(
            id = 0L,
            status = Order.Status.Custom(Order.Status.AUTO_DRAFT)
        )

        // When the createOrUpdateOrder method is call
        sut.createOrUpdateOrder(order, source = OrderCreationSource.STORE_MANAGEMENT)

        // Then the order status is not changed
        val request = UpdateOrderRequest(
            status = WCOrderStatusModel(statusKey = Order.Status.AUTO_DRAFT),
            lineItems = emptyList(),
            shippingAddress = null,
            billingAddress = null,
            customerNote = order.customerNote,
            shippingLines = emptyList(),
            feeLines = emptyList(),
            couponLines = emptyList(),
        )

        verify(orderUpdateStore).createOrder(defaultSiteModel, request, OrderAttributionOrigin.Mobile.SOURCE_TYPE_VALUE)
    }

    @Test
    fun `given the order is updated, when source is store management then createdVia value is null`() = testBlocking {
        // GIVEN
        whenever(orderUpdateStore.createOrder(any(), any(), anyOrNull()))
            .thenReturn(WooResult(OrderTestUtils.generateOrder()))

        val order = Order.getEmptyOrder(Date(), Date()).copy(
            id = 0L
        )

        // WHEN the createOrUpdateOrder method is call
        sut.createOrUpdateOrder(order, source = OrderCreationSource.STORE_MANAGEMENT)

        // THEN the order status is not changed
        val request = UpdateOrderRequest(
            status = WCOrderStatusModel(statusKey = Order.Status.Pending.value, label = Order.Status.Pending.value),
            lineItems = emptyList(),
            shippingAddress = null,
            billingAddress = null,
            customerNote = order.customerNote,
            shippingLines = emptyList(),
            feeLines = emptyList(),
            couponLines = emptyList(),
            createdVia = null,
        )

        verify(orderUpdateStore).createOrder(defaultSiteModel, request, OrderAttributionOrigin.Mobile.SOURCE_TYPE_VALUE)
    }

    @Test
    fun `when source is point of sale then createdVia value is pos-rest-api`() = testBlocking {
        whenever(orderUpdateStore.createOrder(any(), any(), anyOrNull()))
            .thenReturn(WooResult(OrderTestUtils.generateOrder()))

        val order = Order.getEmptyOrder(Date(), Date()).copy(
            id = 0L
        )

        // When the createOrUpdateOrder method is call
        sut.createOrUpdateOrder(order, source = OrderCreationSource.POINT_OF_SALE)

        // Then the order status is not changed
        val request = UpdateOrderRequest(
            status = WCOrderStatusModel(statusKey = Order.Status.Pending.value, label = Order.Status.Pending.value),
            lineItems = emptyList(),
            shippingAddress = null,
            billingAddress = null,
            customerNote = order.customerNote,
            shippingLines = emptyList(),
            feeLines = emptyList(),
            couponLines = emptyList(),
            createdVia = "pos-rest-api",
        )

        verify(orderUpdateStore).createOrder(defaultSiteModel, request, OrderAttributionOrigin.Mobile.SOURCE_TYPE_VALUE)
    }

    @Test
    fun `given an order update succeeds, when it moves to a paid status, then the order is recorded as paid`() =
        testBlocking {
            // GIVEN
            val paidOrder = Order.getEmptyOrder(Date(), Date()).copy(id = 123L, status = Order.Status.Processing)
            whenever(isCurrencyQueryParamSupported()).thenReturn(false)
            whenever(orderMapper.toAppModel(any())).thenReturn(paidOrder)
            whenever(orderUpdateStore.updateOrder(any(), any(), any(), anyOrNull()))
                .thenReturn(WooResult(OrderTestUtils.generateOrder()))

            // WHEN
            sut.createOrUpdateOrder(paidOrder, source = OrderCreationSource.STORE_MANAGEMENT)

            // THEN
            verify(newOrderNotificationSuppressionCache).recordOrderMovedToNotifiableStatus(
                siteId = defaultSiteModel.siteId,
                orderId = 123L,
                newStatusKey = Order.Status.Processing.value,
            )
        }

    @Test
    fun `given an order update fails, when it moves to a paid status, then the order is not recorded as paid`() =
        testBlocking {
            // GIVEN
            whenever(isCurrencyQueryParamSupported()).thenReturn(false)
            whenever(orderUpdateStore.updateOrder(any(), any(), any(), anyOrNull())).thenReturn(
                WooResult(
                    WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR, DEFAULT_ERROR_MESSAGE)
                )
            )
            val order = Order.getEmptyOrder(Date(), Date()).copy(id = 123L, status = Order.Status.Processing)

            // WHEN
            sut.createOrUpdateOrder(order, source = OrderCreationSource.STORE_MANAGEMENT)

            // THEN
            verifyNoInteractions(newOrderNotificationSuppressionCache)
        }

    @Test
    fun `given the store takes the currency query param, when an order is updated, then the currency is sent`() =
        testBlocking {
            // GIVEN
            whenever(isCurrencyQueryParamSupported()).thenReturn(true)
            whenever(orderUpdateStore.updateOrder(any(), any(), any(), anyOrNull()))
                .thenReturn(WooResult(OrderTestUtils.generateOrder()))
            val order = Order.getEmptyOrder(Date(), Date()).copy(id = 123L, currency = "EUR")

            // WHEN
            sut.createOrUpdateOrder(order, source = OrderCreationSource.STORE_MANAGEMENT)

            // THEN
            verify(orderUpdateStore).updateOrder(eq(defaultSiteModel), eq(123L), any(), eq("EUR"))
        }

    @Test
    fun `given the store can't take the currency query param, when an order is updated, then it isn't sent`() =
        testBlocking {
            // GIVEN
            whenever(isCurrencyQueryParamSupported()).thenReturn(false)
            whenever(orderUpdateStore.updateOrder(any(), any(), any(), anyOrNull()))
                .thenReturn(WooResult(OrderTestUtils.generateOrder()))
            val order = Order.getEmptyOrder(Date(), Date()).copy(id = 123L, currency = "EUR")

            // WHEN
            sut.createOrUpdateOrder(order, source = OrderCreationSource.STORE_MANAGEMENT)

            // THEN
            verify(orderUpdateStore).updateOrder(eq(defaultSiteModel), eq(123L), any(), isNull())
        }

    @Test
    fun `when tax based on store address fetched, then it should be parsed correctly`() = testBlocking {
        whenever(wooCommerceStore.fetchTaxBasedOnSettings(any())).thenReturn(
            WooResult(TaxBasedOnSettingEntity(localSiteId = LocalOrRemoteId.LocalId(1), selectedOption = "base"))
        )
        sut.fetchTaxBasedOnSetting().also { setting ->
            assertThat(setting).isNotNull
            assertThat(setting).isInstanceOf(TaxBasedOnSetting.StoreAddress::class.java)
        }
    }

    @Test
    fun `when tax based on shipping address fetched, then it should be parsed correctly`() = testBlocking {
        whenever(wooCommerceStore.fetchTaxBasedOnSettings(any())).thenReturn(
            WooResult(
                TaxBasedOnSettingEntity(localSiteId = LocalOrRemoteId.LocalId(1), selectedOption = "shipping")
            )
        )

        sut.fetchTaxBasedOnSetting().also { setting ->
            assertThat(setting).isNotNull
            assertThat(setting).isInstanceOf(TaxBasedOnSetting.ShippingAddress::class.java)
        }
    }

    @Test
    fun `when tax based on billing address fetched, then it should be parsed correctly`() = testBlocking {
        whenever(wooCommerceStore.fetchTaxBasedOnSettings(any())).thenReturn(
            WooResult(
                TaxBasedOnSettingEntity(localSiteId = LocalOrRemoteId.LocalId(1), selectedOption = "billing")
            )
        )

        sut.fetchTaxBasedOnSetting().also { setting ->
            assertThat(setting).isNotNull
            assertThat(setting).isInstanceOf(TaxBasedOnSetting.BillingAddress::class.java)
        }
    }

    @Test
    fun `when isGiftCardExtensionEnabled is called, then it should return the correct value`() = testBlocking {
        // Given
        val giftCardPluginName = "woocommerce-gift-cards/woocommerce-gift-cards"
        val pluginMock = createTestSitePlugin(
            name = giftCardPluginName,
            isActive = true,
            version = "1.16.6"
        )
        whenever(wooCommerceStore.getSitePlugins(defaultSiteModel, listOf(WOO_GIFT_CARDS)))
            .thenReturn(listOf(pluginMock))

        // When
        val plugins = sut.fetchOrderSupportedPlugins()

        // Then
        assertThat(plugins).isNotEmpty
        assertThat(plugins["woocommerce-gift-cards/woocommerce-gift-cards"]).isEqualTo(
            WooPlugin(
                isInstalled = true,
                isActive = true,
                version = "1.16.6"
            )
        )
    }
}
