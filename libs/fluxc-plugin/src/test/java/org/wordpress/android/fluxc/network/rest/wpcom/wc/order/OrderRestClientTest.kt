package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooExperimentalNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.dto.OrderSummaryApiResponse
import org.wordpress.android.fluxc.utils.CoroutineEngine
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@OptIn(ExperimentalCoroutinesApi::class)
class OrderRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val wooExperimentalNetwork: WooExperimentalNetwork = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderDtoMapper: OrderDtoMapper = mock()
    private val coroutineEngine: CoroutineEngine = initCoroutineEngine()
    private val testSite = SiteModel()
    
    private lateinit var orderRestClient: OrderRestClient
    
    @Before
    fun setUp() {
        orderRestClient = OrderRestClient(
            dispatcher = dispatcher,
            orderDtoMapper = orderDtoMapper,
            wooNetwork = wooNetwork,
            wooExperimentalNetwork = wooExperimentalNetwork,
            coroutineEngine = coroutineEngine
        )
    }
    
    @Test
    fun `when createdViaFilter is provided, then created_via parameter is sent to API`() = runTest {
        // Given
        val expectedCreatedVia = "pos-rest-api"
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>())
        
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = paramsCaptor.capture()
            )
        ).thenReturn(mockResponse)
        
        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = expectedCreatedVia
        )
        
        // When
        orderRestClient.fetchOrderListSummaries(listDescriptor, 0, false)
        
        // Then
        verify(wooNetwork).executeGetGsonRequest(
            site = testSite,
            path = WOOCOMMERCE.orders.pathV3,
            clazz = Array<OrderSummaryApiResponse>::class.java,
            params = paramsCaptor.capture()
        )
        
        assertThat(paramsCaptor.firstValue["created_via"]).isEqualTo(expectedCreatedVia)
    }
    
    @Test
    fun `when createdViaFilter is null, then created_via parameter is not sent to API`() = runTest {
        // Given
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>())
        
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = paramsCaptor.capture()
            )
        ).thenReturn(mockResponse)
        
        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = null
        )
        
        // When
        orderRestClient.fetchOrderListSummaries(listDescriptor, 0, false)
        
        // Then
        verify(wooNetwork).executeGetGsonRequest(
            site = testSite,
            path = WOOCOMMERCE.orders.pathV3,
            clazz = Array<OrderSummaryApiResponse>::class.java,
            params = paramsCaptor.capture()
        )
        
        assertThat(paramsCaptor.firstValue).doesNotContainKey("created_via")
    }
    
    @Test
    fun `when createdViaFilter is blank, then created_via parameter is not sent to API`() = runTest {
        // Given
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>())
        
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = paramsCaptor.capture()
            )
        ).thenReturn(mockResponse)
        
        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = ""
        )
        
        // When
        orderRestClient.fetchOrderListSummaries(listDescriptor, 0, false)
        
        // Then
        verify(wooNetwork).executeGetGsonRequest(
            site = testSite,
            path = WOOCOMMERCE.orders.pathV3,
            clazz = Array<OrderSummaryApiResponse>::class.java,
            params = paramsCaptor.capture()
        )
        
        assertThat(paramsCaptor.firstValue).doesNotContainKey("created_via")
    }
    
    @Test
    fun `when createdViaFilter is provided for first page fetch, then created_via parameter is sent to API`() = runTest {
        // Given
        val expectedCreatedVia = "pos-rest-api"
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        val mockResponse = WPAPIResponse.Success(arrayOf<OrderSummaryApiResponse>())
        
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(Array<OrderSummaryApiResponse>::class.java),
                params = paramsCaptor.capture()
            )
        ).thenReturn(mockResponse)
        
        val listDescriptor = WCOrderListDescriptor(
            site = testSite,
            createdViaFilter = expectedCreatedVia
        )
        
        // When
        orderRestClient.fetchOrdersListFirstPage(listDescriptor, 20)
        
        // Then
        verify(wooNetwork).executeGetGsonRequest(
            site = testSite,
            path = WOOCOMMERCE.orders.pathV3,
            clazz = Array<OrderSummaryApiResponse>::class.java,
            params = paramsCaptor.capture()
        )
        
        assertThat(paramsCaptor.firstValue["created_via"]).isEqualTo(expectedCreatedVia)
    }
}