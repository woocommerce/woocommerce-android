package com.woocommerce.android.apifaker.adb

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.woocommerce.android.apifaker.ApiFakerConfig
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.QueryParameter
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BroadcastActionHandlerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val endpointDao: EndpointDao = mock()
    private val apiFakerConfig: ApiFakerConfig = mock()
    private val gson = Gson()

    private lateinit var logMock: MockedStatic<Log>

    private val sut = BroadcastActionHandler(
        endpointDao = endpointDao,
        apiFakerConfig = apiFakerConfig,
        gson = gson
    )

    private val defaultRequest = Request(
        id = 1L,
        type = ApiType.WPApi,
        path = "/wc/v3/products",
        httpMethod = HttpMethod.GET,
        queryParameters = listOf(QueryParameter("page", "1")),
        body = null
    )
    private val defaultResponse = Response(
        endpointId = 1L,
        statusCode = 200,
        body = "{\"id\":1}"
    )
    private val defaultEndpoint = MockedEndpoint(
        request = defaultRequest,
        response = defaultResponse
    )

    @Before
    fun setUp() {
        logMock = Mockito.mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        logMock.close()
    }

    @Test
    fun `given enabled is true, when handling SET_STATUS, then sets status to true`() = runTest {
        // GIVEN
        val intent = createIntent(Actions.SET_STATUS) {
            whenever(it.getBooleanExtra(Extras.ENABLED, false)).thenReturn(true)
        }

        // WHEN
        sut.handle(intent)

        // THEN
        verify(apiFakerConfig).setStatus(true)
    }

    @Test
    fun `given enabled is false, when handling SET_STATUS, then sets status to false`() = runTest {
        // GIVEN
        val intent = createIntent(Actions.SET_STATUS) {
            whenever(it.getBooleanExtra(Extras.ENABLED, false)).thenReturn(false)
        }

        // WHEN
        sut.handle(intent)

        // THEN
        verify(apiFakerConfig).setStatus(false)
    }

    @Test
    fun `given happy path, when handling ADD_ENDPOINT, then inserts endpoint with parsed request and response`() =
        runTest {
            // GIVEN
            val intent = createAddEndpointIntent(
                apiType = "wp-api",
                path = "/wc/v3/products",
                httpMethod = "GET",
                responseStatusCode = 200,
                responseBody = "{\"id\":1}"
            )

            // WHEN
            sut.handle(intent)

            // THEN
            verify(endpointDao).insertEndpoint(
                argThat {
                    type == ApiType.WPApi &&
                        path == "/wc/v3/products" &&
                        httpMethod == HttpMethod.GET &&
                        body == null
                },
                argThat {
                    statusCode == 200 &&
                        body == "{\"id\":1}"
                }
            )
        }

    @Test
    fun `given wp-com api type, when handling ADD_ENDPOINT, then inserts endpoint with WPCom type`() = runTest {
        // GIVEN
        val intent = createAddEndpointIntent(
            apiType = "wp-com",
            path = "/rest/v1.1/me"
        )

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).insertEndpoint(
            argThat { type == ApiType.WPCom },
            any()
        )
    }

    @Test
    fun `given custom api type with host, when handling ADD_ENDPOINT, then inserts endpoint with Custom type`() =
        runTest {
            // GIVEN
            val intent = createAddEndpointIntent(
                apiType = "custom",
                customHost = "my-api.example.com",
                path = "/v1/resource"
            )

            // WHEN
            sut.handle(intent)

            // THEN
            verify(endpointDao).insertEndpoint(
                argThat { type == ApiType.Custom("my-api.example.com") },
                any()
            )
        }

    @Test
    fun `given query parameters, when handling ADD_ENDPOINT, then inserts endpoint with parsed query params`() =
        runTest {
            // GIVEN
            val intent = createAddEndpointIntent(
                apiType = "wp-api",
                path = "/wc/v3/products",
                queryParams = "page=1,per_page=10"
            )

            // WHEN
            sut.handle(intent)

            // THEN
            verify(endpointDao).insertEndpoint(
                argThat {
                    queryParameters == listOf(
                        QueryParameter("page", "1"),
                        QueryParameter("per_page", "10")
                    )
                },
                any()
            )
        }

    @Test
    fun `given default status code, when handling ADD_ENDPOINT, then inserts response with status 200`() = runTest {
        // GIVEN
        val intent = createAddEndpointIntent(
            apiType = "wp-api",
            path = "/wc/v3/products",
            responseStatusCode = null
        )

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).insertEndpoint(
            any(),
            argThat { statusCode == 200 }
        )
    }

    @Test
    fun `given custom status code, when handling ADD_ENDPOINT, then inserts response with that status code`() =
        runTest {
            // GIVEN
            val intent = createAddEndpointIntent(
                apiType = "wp-api",
                path = "/wc/v3/products",
                responseStatusCode = 404
            )

            // WHEN
            sut.handle(intent)

            // THEN
            verify(endpointDao).insertEndpoint(
                any(),
                argThat { statusCode == 404 }
            )
        }

    @Test
    fun `given missing path, when handling ADD_ENDPOINT, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.ADD_ENDPOINT) {
            whenever(it.getStringExtra(Extras.API_TYPE)).thenReturn("wp-api")
            whenever(it.getStringExtra(Extras.PATH)).thenReturn(null)
        }

        // WHEN / THEN
        assertThrows(IllegalStateException::class.java) {
            runTest { sut.handle(intent) }
        }
    }

    @Test
    fun `given existing endpoint and new path, when handling EDIT_ENDPOINT, then updates only the path`() = runTest {
        // GIVEN
        whenever(endpointDao.getEndpoint(1L)).thenReturn(defaultEndpoint)
        val intent = createIntent(Actions.EDIT_ENDPOINT) {
            whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(1L)
            whenever(it.getStringExtra(Extras.PATH)).thenReturn("/wc/v3/orders")
            whenever(it.hasExtra(Extras.API_TYPE)).thenReturn(false)
            whenever(it.hasExtra(Extras.HTTP_METHOD)).thenReturn(false)
            whenever(it.hasExtra(Extras.QUERY_PARAMS)).thenReturn(false)
            whenever(it.hasExtra(Extras.REQUEST_BODY)).thenReturn(false)
            whenever(it.hasExtra(Extras.REQUEST_BODY_FILE)).thenReturn(false)
            whenever(it.getIntExtra(Extras.RESPONSE_STATUS_CODE, 200)).thenReturn(200)
            whenever(it.hasExtra(Extras.RESPONSE_BODY)).thenReturn(false)
            whenever(it.hasExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(false)
        }

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).insertEndpoint(
            argThat {
                path == "/wc/v3/orders" &&
                    type == defaultRequest.type &&
                    httpMethod == defaultRequest.httpMethod &&
                    queryParameters == defaultRequest.queryParameters &&
                    body == defaultRequest.body
            },
            argThat {
                statusCode == defaultResponse.statusCode &&
                    body == defaultResponse.body
            }
        )
    }

    @Test
    fun `given existing endpoint and new response body, when handling EDIT_ENDPOINT, then updates only the response body`() =
        runTest {
            // GIVEN
            whenever(endpointDao.getEndpoint(1L)).thenReturn(defaultEndpoint)
            val intent = createIntent(Actions.EDIT_ENDPOINT) {
                whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(1L)
                whenever(it.getStringExtra(Extras.PATH)).thenReturn(null)
                whenever(it.hasExtra(Extras.API_TYPE)).thenReturn(false)
                whenever(it.hasExtra(Extras.HTTP_METHOD)).thenReturn(false)
                whenever(it.hasExtra(Extras.QUERY_PARAMS)).thenReturn(false)
                whenever(it.hasExtra(Extras.REQUEST_BODY)).thenReturn(false)
                whenever(it.hasExtra(Extras.REQUEST_BODY_FILE)).thenReturn(false)
                whenever(it.getIntExtra(Extras.RESPONSE_STATUS_CODE, 200)).thenReturn(200)
                whenever(it.hasExtra(Extras.RESPONSE_BODY)).thenReturn(true)
                whenever(it.getStringExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(null)
                whenever(it.getStringExtra(Extras.RESPONSE_BODY)).thenReturn("{\"updated\":true}")
                whenever(it.hasExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(false)
            }

            // WHEN
            sut.handle(intent)

            // THEN
            verify(endpointDao).insertEndpoint(
                argThat {
                    path == defaultRequest.path &&
                        type == defaultRequest.type
                },
                argThat {
                    body == "{\"updated\":true}" &&
                        statusCode == defaultResponse.statusCode
                }
            )
        }

    @Test
    fun `given missing endpoint ID, when handling EDIT_ENDPOINT, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.EDIT_ENDPOINT) {
            whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(-1L)
        }

        // WHEN / THEN
        assertThrows(IllegalArgumentException::class.java) {
            runTest { sut.handle(intent) }
        }
    }

    @Test
    fun `given non-existent endpoint ID, when handling EDIT_ENDPOINT, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.EDIT_ENDPOINT) {
            whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(999L)
        }

        // WHEN / THEN
        assertThrows(IllegalStateException::class.java) {
            runTest {
                whenever(endpointDao.getEndpoint(999L)).thenReturn(null)
                sut.handle(intent)
            }
        }
    }

    @Test
    fun `given existing endpoint, when handling REMOVE_ENDPOINT, then deletes the request`() = runTest {
        // GIVEN
        whenever(endpointDao.getEndpoint(1L)).thenReturn(defaultEndpoint)
        val intent = createIntent(Actions.REMOVE_ENDPOINT) {
            whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(1L)
        }

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).deleteRequest(defaultRequest)
    }

    @Test
    fun `given missing endpoint ID, when handling REMOVE_ENDPOINT, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.REMOVE_ENDPOINT) {
            whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(-1L)
        }

        // WHEN / THEN
        assertThrows(IllegalArgumentException::class.java) {
            runTest { sut.handle(intent) }
        }
    }

    @Test
    fun `given non-existent endpoint ID, when handling REMOVE_ENDPOINT, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.REMOVE_ENDPOINT) {
            whenever(it.getLongExtra(Extras.ENDPOINT_ID, -1)).thenReturn(42L)
        }

        // WHEN / THEN
        assertThrows(IllegalStateException::class.java) {
            runTest {
                whenever(endpointDao.getEndpoint(42L)).thenReturn(null)
                sut.handle(intent)
            }
        }
    }

    @Test
    fun `given happy path, when handling CLEAR_ENDPOINTS, then deletes all requests`() = runTest {
        // GIVEN
        val intent = createIntent(Actions.CLEAR_ENDPOINTS)

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).deleteAllRequests()
    }

    @Test
    fun `given happy path, when handling LIST_ENDPOINTS, then calls getAllEndpoints`() = runTest {
        // GIVEN
        whenever(endpointDao.getAllEndpoints()).thenReturn(listOf(defaultEndpoint))
        val intent = createIntent(Actions.LIST_ENDPOINTS)

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).getAllEndpoints()
    }

    @Test
    fun `given empty endpoint list, when handling LIST_ENDPOINTS, then calls getAllEndpoints`() = runTest {
        // GIVEN
        whenever(endpointDao.getAllEndpoints()).thenReturn(emptyList())
        val intent = createIntent(Actions.LIST_ENDPOINTS)

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).getAllEndpoints()
    }

    @Test
    fun `given valid file, when handling IMPORT_ENDPOINTS, then inserts parsed endpoints`() = runTest {
        // GIVEN
        val file = temporaryFolder.newFile("endpoints.json")
        val endpoints = listOf(defaultEndpoint)
        file.writeText(gson.toJson(endpoints))

        val intent = createIntent(Actions.IMPORT_ENDPOINTS) {
            whenever(it.getStringExtra(Extras.FILE)).thenReturn(file.absolutePath)
        }

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao).insertEndpoints(
            argThat {
                size == 1 &&
                    first().request.path == defaultRequest.path &&
                    first().response.statusCode == defaultResponse.statusCode
            }
        )
    }

    @Test
    fun `given missing file extra, when handling IMPORT_ENDPOINTS, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.IMPORT_ENDPOINTS) {
            whenever(it.getStringExtra(Extras.FILE)).thenReturn(null)
        }

        // WHEN / THEN
        assertThrows(IllegalStateException::class.java) {
            runTest { sut.handle(intent) }
        }
    }

    @Test
    fun `given non-existent file, when handling IMPORT_ENDPOINTS, then throws error`() {
        // GIVEN
        val intent = createIntent(Actions.IMPORT_ENDPOINTS) {
            whenever(it.getStringExtra(Extras.FILE)).thenReturn("/non/existent/file.json")
        }

        // WHEN / THEN
        assertThrows(IllegalArgumentException::class.java) {
            runTest { sut.handle(intent) }
        }
    }

    @Test
    fun `given unknown action, when handling intent, then no dao or config interaction occurs`() = runTest {
        // GIVEN
        val intent = createIntent("com.unknown.ACTION")

        // WHEN
        sut.handle(intent)

        // THEN
        verify(endpointDao, never()).insertEndpoint(any(), any())
        verify(endpointDao, never()).deleteRequest(any())
        verify(endpointDao, never()).deleteAllRequests()
        verify(apiFakerConfig, never()).setStatus(any())
    }

    private fun createIntent(
        action: String,
        configure: (Intent) -> Unit = {}
    ): Intent = mock<Intent> {
        on { getAction() } doReturn action
    }.also(configure)

    private fun createAddEndpointIntent(
        apiType: String,
        path: String,
        httpMethod: String? = null,
        queryParams: String? = null,
        requestBody: String? = null,
        responseStatusCode: Int? = 200,
        responseBody: String? = null,
        customHost: String? = null
    ): Intent = createIntent(Actions.ADD_ENDPOINT) { intent ->
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn(apiType)
        whenever(intent.getStringExtra(Extras.PATH)).thenReturn(path)
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn(httpMethod)
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn(queryParams)
        whenever(intent.getStringExtra(Extras.REQUEST_BODY_FILE)).thenReturn(null)
        whenever(intent.getStringExtra(Extras.REQUEST_BODY)).thenReturn(requestBody)
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(null)
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY)).thenReturn(responseBody)
        whenever(intent.getStringExtra(Extras.CUSTOM_HOST)).thenReturn(customHost)
        whenever(intent.getIntExtra(eq(Extras.RESPONSE_STATUS_CODE), any()))
            .thenReturn(responseStatusCode ?: 200)
    }
}
