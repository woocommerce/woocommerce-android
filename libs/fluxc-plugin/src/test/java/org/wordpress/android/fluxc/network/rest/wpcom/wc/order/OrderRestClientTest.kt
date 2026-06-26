package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderFulfillmentModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.fluxc.utils.initCoroutineEngine
import kotlin.collections.emptyList

@OptIn(ExperimentalCoroutinesApi::class)
class OrderRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderDtoMapper: OrderDtoMapper = mock()
    private val coroutineEngine: CoroutineEngine = initCoroutineEngine()
    private val testSite = SiteModel()

    private lateinit var orderRestClient: OrderRestClient

    @Before
    fun setUp() {
        testSite.id = 6
        orderRestClient = OrderRestClient(
            dispatcher = dispatcher,
            orderDtoMapper = orderDtoMapper,
            wooNetwork = wooNetwork,
            coroutineEngine = coroutineEngine
        )
    }

    @Test
    fun `when createdViaFilter is provided, then created_via parameter is sent to API`() = runTest {
        // Given
        val expectedCreatedVia = "pos-rest-api"
        val expectedParams = mapOf(
            "per_page" to "60",
            "offset" to "0",
            "orderby" to "date",
            "order" to "desc",
            "_fields" to "id,date_created_gmt,date_modified_gmt",
            "created_via" to expectedCreatedVia
        )
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>(), emptyList())

        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(mockResponse)

        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = expectedCreatedVia
        )

        // When
        orderRestClient.fetchOrderListSummaries(listDescriptor, 0)

        // Then
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        verify(wooNetwork).executeGetGsonRequest(
            site = eq(testSite),
            path = eq(WOOCOMMERCE.orders.pathV3),
            clazz = eq(Array<OrderSummaryApiResponse>::class.java),
            params = paramsCaptor.capture(),
            enableCaching = any(),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )

        assertThat(paramsCaptor.firstValue).containsExactlyInAnyOrderEntriesOf(expectedParams)
    }

    @Test
    fun `when fetching order fulfillments, then fields are parsed`() = runTest {
        val orderId = 123L
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-fulfillments.json")
        val response = WPAPIResponse.Success<Array<OrderFulfillmentApiResponse>>(
            Gson().fromJson(json, Array<OrderFulfillmentApiResponse>::class.java),
            emptyList()
        )

        whenever(
            wooNetwork.executeGetGsonRequest(
                site = eq(testSite),
                path = eq(WOOCOMMERCE.orders.id(orderId).fulfillments.pathV3),
                clazz = eq(Array<OrderFulfillmentApiResponse>::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
        )
        ).thenReturn(response)

        val result = orderRestClient.fetchOrderFulfillments(testSite, orderId)

        assertThat(result.fulfillments).containsExactly(
            generateOrderFulfillment(
                siteId = testSite.id,
                orderId = orderId,
                fulfillmentId = 42L,
                status = "fulfilled",
                isFulfilled = true,
                dateUpdated = "2026-03-18 21:00:00",
                dateFulfilled = "2026-03-18 14:30:00",
                trackingNumber = "1Z999AA10123456784",
                shipmentProvider = "ups",
                trackingUrl = "https://www.ups.com/track?tracknum=1Z999AA10123456784"
            ),
            generateOrderFulfillment(
                siteId = testSite.id,
                orderId = orderId,
                fulfillmentId = 43L,
                status = "unfulfilled",
                isFulfilled = false,
                dateUpdated = null,
                dateFulfilled = null,
                trackingNumber = null,
                shipmentProvider = null,
                trackingUrl = null
            )
        )
    }

    @Test
    fun `when createdViaFilter is null, then created_via parameter is not sent to API`() = runTest {
        // Given
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>(), emptyList())

        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(mockResponse)

        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = null
        )

        // When
        orderRestClient.fetchOrderListSummaries(listDescriptor, 0)

        // Then
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        verify(wooNetwork).executeGetGsonRequest(
            site = eq(testSite),
            path = eq(WOOCOMMERCE.orders.pathV3),
            clazz = eq(Array<OrderSummaryApiResponse>::class.java),
            params = paramsCaptor.capture(),
            enableCaching = any(),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )

        assertThat(paramsCaptor.firstValue).doesNotContainKey("created_via")
    }

    @Test
    fun `when createdViaFilter is blank, then created_via parameter is not sent to API`() = runTest {
        // Given
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>(), emptyList())

        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(mockResponse)

        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = ""
        )

        // When
        orderRestClient.fetchOrderListSummaries(listDescriptor, 0)

        // Then
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        verify(wooNetwork).executeGetGsonRequest(
            site = eq(testSite),
            path = eq(WOOCOMMERCE.orders.pathV3),
            clazz = eq(Array<OrderSummaryApiResponse>::class.java),
            params = paramsCaptor.capture(),
            enableCaching = any(),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )

        assertThat(paramsCaptor.firstValue).doesNotContainKey("created_via")
    }

    @Test
    fun `when createdViaFilter is provided for first page fetch, then created_via parameter is sent to API`() = runTest {
        // Given
        val expectedCreatedVia = "pos-rest-api"
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderDto>(), emptyList())

        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderDto>::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(mockResponse)

        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = expectedCreatedVia
        )

        // When
        orderRestClient.fetchOrdersListFirstPage(listDescriptor)

        // Then
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        verify(wooNetwork).executeGetGsonRequest(
            site = eq(testSite),
            path = eq(WOOCOMMERCE.orders.pathV3),
            clazz = eq(Array<OrderDto>::class.java),
            params = paramsCaptor.capture(),
            enableCaching = any(),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )

        assertThat(paramsCaptor.firstValue["created_via"]).isEqualTo(expectedCreatedVia)
    }

    @Test
    fun `when createdVia is provided, then created_via parameter is sent to API in fetchOrders`() = runTest {
        // Given
        val expectedCreatedVia = "pos-rest-api"
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderDto>(),emptyList())

        whenever(
            wooNetwork.executeGetGsonRequest(
                site = eq(testSite),
                path = eq(WOOCOMMERCE.orders.pathV3),
                clazz = eq(Array<OrderDto>::class.java),
                params = any(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(mockResponse)

        // When
        orderRestClient.fetchOrders(
            site = testSite,
            count = 60,
            page = 1,
            orderBy = OrderRestClient.OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = expectedCreatedVia
        )

        // Then
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        verify(wooNetwork).executeGetGsonRequest(
            site = eq(testSite),
            path = eq(WOOCOMMERCE.orders.pathV3),
            clazz = eq(Array<OrderDto>::class.java),
            params = paramsCaptor.capture(),
            enableCaching = any(),
            cacheTimeToLive = any(),
            forced = any(),
            requestTimeout = any(),
            retries = any()
        )
        assertThat(paramsCaptor.firstValue["created_via"]).isEqualTo(expectedCreatedVia)
    }

    @Test
    fun `when updateOrderBillingEmail is called, then only billing email is sent in request body`() = runTest {
        // Given
        val orderId = 123L
        val email = "test@example.com"
        val expectedPath = WOOCOMMERCE.orders.id(orderId).pathV3
        val expectedBody = mapOf(
            "billing" to mapOf(
                "email" to email
            )
        )
        val mockResponse = WPAPIResponse.Success(Unit, emptyList())

        whenever(
            wooNetwork.executePutGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Unit::class.java),
                body = any()
            )
        ).thenReturn(mockResponse)

        // When
        orderRestClient.updateOrderBillingEmail(testSite, orderId, email)

        // Then
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePutGsonRequest(
            site = eq(testSite),
            path = eq(expectedPath),
            clazz = eq(Unit::class.java),
            body = bodyCaptor.capture()
        )

        assertThat(bodyCaptor.firstValue).isEqualTo(expectedBody)
    }

    @Test
    fun `when sendOrderPOSSpecificReceipt is called with templateId, then template_id is included in request body`() = runTest {
        // Given
        val orderId = 123L
        val email = "test@example.com"
        val templateId = "customer_pos_completed_order"
        val expectedPath = WOOCOMMERCE.orders.id(orderId).actions.send_email.pathV3
        val expectedBody = mapOf(
            "email" to email,
            "force_email_update" to true,
            "template_id" to templateId
        )
        val mockResponse = WPAPIResponse.Success(Unit, emptyList())

        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Unit::class.java),
                body = any()
            )
        ).thenReturn(mockResponse)

        // When
        orderRestClient.sendOrderPOSSpecificReceipt(
            testSite, orderId, email, forceEmailUpdate = true, templateId = templateId
        )

        // Then
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(testSite),
            path = eq(expectedPath),
            clazz = eq(Unit::class.java),
            body = bodyCaptor.capture()
        )

        assertThat(bodyCaptor.firstValue).isEqualTo(expectedBody)
    }

    @Test
    fun `when sendOrderPOSSpecificReceipt is called without templateId, then template_id is not in request body`() = runTest {
        // Given
        val orderId = 123L
        val email = "test@example.com"
        val expectedPath = WOOCOMMERCE.orders.id(orderId).actions.send_email.pathV3
        val expectedBody = mapOf(
            "email" to email,
            "force_email_update" to true
        )
        val mockResponse = WPAPIResponse.Success(Unit, emptyList())

        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Unit::class.java),
                body = any()
            )
        ).thenReturn(mockResponse)

        // When
        orderRestClient.sendOrderPOSSpecificReceipt(testSite, orderId, email, forceEmailUpdate = true, templateId = null)

        // Then
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(testSite),
            path = eq(expectedPath),
            clazz = eq(Unit::class.java),
            body = bodyCaptor.capture()
        )

        assertThat(bodyCaptor.firstValue).isEqualTo(expectedBody)
        assertThat(bodyCaptor.firstValue).doesNotContainKey("template_id")
    }

    @Test
    fun `given the proxy returns no_response_body with raw_body, when fetching summaries fails, then error is PARSE_ERROR`() =
        runTest {
            // Given
            val errorData = mock<JSONObject> {
                on { has("raw_body") } doReturn true
            }
            val error = WPAPINetworkError(
                baseError = BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.UNKNOWN),
                errorCode = "no_response_body",
                errorData = errorData
            )
            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                    params = any(),
                    enableCaching = any(),
                    cacheTimeToLive = any(),
                    forced = any(),
                    requestTimeout = any(),
                    retries = any()
                )
            ).thenReturn(WPAPIResponse.Error(error))

            // When
            orderRestClient.fetchOrderListSummaries(WCOrderListDescriptor(site = testSite), 0)

            // Then
            val actionCaptor = argumentCaptor<Action<FetchOrderListResponsePayload>>()
            verify(dispatcher).dispatch(actionCaptor.capture())
            assertThat(actionCaptor.firstValue.payload.error?.type).isEqualTo(OrderErrorType.PARSE_ERROR)

            val reportCaptor = argumentCaptor<OnUnexpectedError>()
            verify(dispatcher).emitChange(reportCaptor.capture())
            assertThat(reportCaptor.firstValue.description).isEqualTo("Order API response parse error")
        }

    @Test
    fun `given proxy returns no_response_body without raw_body, when fetching fails, then error is GENERIC and not reported`() =
        runTest {
            // Given
            val errorData = mock<JSONObject> {
                on { has("raw_body") } doReturn false
            }
            val error = WPAPINetworkError(
                baseError = BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.UNKNOWN),
                errorCode = "no_response_body",
                errorData = errorData
            )
            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                    params = any(),
                    enableCaching = any(),
                    cacheTimeToLive = any(),
                    forced = any(),
                    requestTimeout = any(),
                    retries = any()
                )
            ).thenReturn(WPAPIResponse.Error(error))

            // When
            orderRestClient.fetchOrderListSummaries(WCOrderListDescriptor(site = testSite), 0)

            // Then
            val actionCaptor = argumentCaptor<Action<FetchOrderListResponsePayload>>()
            verify(dispatcher).dispatch(actionCaptor.capture())
            assertThat(actionCaptor.firstValue.payload.error?.type).isEqualTo(OrderErrorType.GENERIC_ERROR)
            verify(dispatcher, never()).emitChange(any<OnUnexpectedError>())
        }

    /* HELPER */

    @Suppress("LongParameterList")
    private fun generateOrderFulfillment(
        siteId: Int,
        orderId: Long,
        fulfillmentId: Long = 42L,
        status: String = "fulfilled",
        isFulfilled: Boolean = true,
        dateUpdated: String? = "2026-03-18 21:00:00",
        dateFulfilled: String? = "2026-03-18 14:30:00",
        trackingNumber: String? = "1Z999AA10123456784",
        shipmentProvider: String? = "ups",
        trackingUrl: String? = "https://www.ups.com/track?tracknum=1Z999AA10123456784"
    ) = WCOrderFulfillmentModel(
        localSiteId = LocalId(siteId),
        orderId = RemoteId(orderId),
        fulfillmentId = fulfillmentId,
        status = status,
        isFulfilled = isFulfilled,
        dateUpdated = dateUpdated,
        dateFulfilled = dateFulfilled,
        trackingNumber = trackingNumber,
        shipmentProvider = shipmentProvider,
        trackingUrl = trackingUrl
    )
}
