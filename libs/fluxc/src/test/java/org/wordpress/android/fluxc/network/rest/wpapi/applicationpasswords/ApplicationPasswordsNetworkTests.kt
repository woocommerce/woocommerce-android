package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.ResponseWithHeaders
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.utils.HttpsUrlNormalizer
import kotlin.test.assertEquals
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ApplicationPasswordsNetworkTests {
    private val testSite = SiteModel().apply {
        url = "http://test-site.com"
    }
    private val testCredentials = ApplicationPasswordCredentials(
        userName = "username",
        password = "password",
        uuid = "uuid"
    )

    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()
    private val mApplicationPasswordsManager: ApplicationPasswordsManager = mock()
    private var lastRequestUrl: String? = null
    private lateinit var network: ApplicationPasswordsNetwork

    @Before
    fun setup() {
        network = ApplicationPasswordsNetwork(
            requestQueue = requestQueue,
            userAgent = userAgent,
            httpsUrlNormalizer = HttpsUrlNormalizer(),
        ).apply {
            mApplicationPasswordsManager = this@ApplicationPasswordsNetworkTests.mApplicationPasswordsManager
        }
    }

    @Test
    fun `when sending a new request, then fetch the application password`() = runTest {
        givenSuccessResponse()
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
            .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))

        network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

        verify(mApplicationPasswordsManager).getApplicationCredentials(testSite, "path")
    }

    @Test
    fun `given HTTP site, when sending request, then use HTTPS`() = runTest {
        givenSuccessResponse()
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))

        network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

        assertEquals("https://test-site.com/wp-json/path", lastRequestUrl)
    }

    @Test
    fun `given a locally existing password, when password is revoked, then regenerate a new one`() = runTest {
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))
        whenever(
            mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                "path",
                ApplicationPasswordGenerationScenario.REGENERATION
            )
        )
            .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))
        givenPasswordIsRevoked()
        val networkError = VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList()))
        givenErrorResponse(networkError)

        network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

        verify(mApplicationPasswordsManager).deleteLocalApplicationPassword(testSite, testCredentials)
        verify(mApplicationPasswordsManager).getApplicationCredentials(testSite, "path")
        verify(mApplicationPasswordsManager).getApplicationCredentials(
            testSite,
            "path",
            ApplicationPasswordGenerationScenario.REGENERATION
        )
    }

    @Test
    fun `given request succeeds, when request is executed, then return the response`() = runTest {
        val expectedResponse = TestResponse("value")
        givenSuccessResponse(expectedResponse)
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))

        val response = network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

        assertIs<WPAPIResponse.Success<TestResponse>>(response)
        assertEquals(expectedResponse, response.data)
    }

    @Test
    fun `given request fails, when request is executed, then return the error`() = runTest {
        val networkError = VolleyError(NetworkResponse(500, byteArrayOf(), true, 0, emptyList()))
        givenErrorResponse(networkError)
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))

        val response = network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

        verify(mApplicationPasswordsManager, never()).shouldRegenerateApplicationPassword(any(), any())
        assertIs<WPAPIResponse.Error<TestResponse>>(response)
        assertEquals(networkError, response.error.volleyError)
    }

    @Test
    fun `given site doesn't support application passwords, when a new request, then return the prefixed error`() =
        runTest {
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(501, byteArrayOf(), true, 0, emptyList())))
            whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
                .thenReturn(ApplicationPasswordCreationResult.NotSupported(BaseNetworkError(networkError)))

            val response = network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

            assertIs<WPAPIResponse.Error<TestResponse>>(response)
            assertEquals(
                ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX,
                response.error.errorCode
            )
        }

    @Test
    fun `given the password is still valid, when an endpoint returns 401, then keep it and return the error`() =
        runTest {
            whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
                .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))
            whenever(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(testSite, testCredentials))
                .thenReturn(false)
            val networkError = VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList()))
            givenErrorResponse(networkError)

            val response = network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

            verify(mApplicationPasswordsManager).shouldRegenerateApplicationPassword(testSite, testCredentials)
            verify(mApplicationPasswordsManager, never()).deleteLocalApplicationPassword(any(), any())
            verify(mApplicationPasswordsManager, times(1)).getApplicationCredentials(testSite, "path")
            assertIs<WPAPIResponse.Error<TestResponse>>(response)
            assertEquals(networkError, response.error.volleyError)
        }

    @Test
    fun `given a freshly created password, when an endpoint returns 401, then don't verify it or regenerate`() =
        runTest {
            whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite, "path"))
                .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))
            givenErrorResponse(VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList())))

            network.executeGetGsonRequest(testSite, "path", TestResponse::class.java)

            verify(mApplicationPasswordsManager, never()).shouldRegenerateApplicationPassword(any(), any())
            verify(mApplicationPasswordsManager, never()).deleteLocalApplicationPassword(any(), any())
            verify(mApplicationPasswordsManager, times(1)).getApplicationCredentials(testSite, "path")
        }

    private suspend fun givenPasswordIsRevoked() {
        whenever(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(eq(testSite), eq(testCredentials)))
            .thenReturn(true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenSuccessResponse(response: TestResponse = TestResponse("")) {
        whenever(requestQueue.add(any<WPAPIGsonRequest<TestResponse>>())).thenAnswer { invocation ->
            val request = (invocation.arguments.first() as WPAPIGsonRequest<TestResponse>)
            lastRequestUrl = request.url

            val deliverMethod = Request::class.java.getDeclaredMethod("deliverResponse", Any::class.java)
            deliverMethod.isAccessible = true
            deliverMethod.invoke(request, ResponseWithHeaders(response, emptyList()))

            return@thenAnswer request
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenErrorResponse(error: VolleyError) {
        whenever(requestQueue.add(any<WPAPIGsonRequest<TestResponse>>())).thenAnswer { invocation ->
            val request = (invocation.arguments.first() as WPAPIGsonRequest<TestResponse>)
            lastRequestUrl = request.url
            request.deliverError(error)

            return@thenAnswer request
        }
    }

    data class TestResponse(val value: String)
}
