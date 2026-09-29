package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Credentials
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.ResponseWithHeaders
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.utils.HttpsUrlNormalizer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class WPApiApplicationPasswordsRestClientTest {
    private val testSite = SiteModel().apply {
        url = "http://test-site.com"
    }
    private val credentialsWithoutUuid = ApplicationPasswordCredentials(
        userName = "username",
        password = "password",
        uuid = null
    )

    private var lastRequest: WPAPIGsonRequest<Any>? = null

    private val noCookieRequestQueue: RequestQueue = mock()
    private val cookieNonceAuthenticator: CookieNonceAuthenticator = mock()
    private val restClient = WPApiApplicationPasswordsRestClient(
        wpApiGsonRequestBuilder = mock(),
        cookieNonceAuthenticator = cookieNonceAuthenticator,
        httpsUrlNormalizer = HttpsUrlNormalizer(),
        noCookieRequestQueue = noCookieRequestQueue,
        requestQueue = mock(),
        dispatcher = mock<Dispatcher>(),
        userAgent = mock<UserAgent>()
    )

    @Test
    fun `given the introspect response has no uuid, when deleting a password, then return an error payload`() =
        runTest {
            // GIVEN the server answers 200 but omits the uuid field, so Gson leaves it null
            givenSuccessResponse(
                Gson().fromJson("""{"name":"woo-app"}""", ApplicationPasswordsFetchResponse::class.java)
            )

            // WHEN
            val payload = restClient.deleteApplicationPassword(testSite, credentialsWithoutUuid)

            // THEN
            assertTrue(payload.isError)
            assertFalse(payload.isDeleted)
            assertEquals("UUID missing from response", payload.error.message)
        }

    @Test
    fun `given the introspect response is empty, when deleting a password, then return an error payload`() =
        runTest {
            // GIVEN the server answers 200 with no body at all
            givenSuccessResponse(null)

            // WHEN
            val payload = restClient.deleteApplicationPassword(testSite, credentialsWithoutUuid)

            // THEN
            assertTrue(payload.isError)
            assertEquals("Response is empty", payload.error.message)
        }

    @Test
    fun `given the introspect response has a uuid, when deleting a password, then the password is deleted`() =
        runTest {
            val introspectResponse = Gson().fromJson(
                """{"uuid":"the-uuid","name":"woo-app"}""",
                ApplicationPasswordsFetchResponse::class.java
            )
            givenSuccessResponses(introspectResponse, ApplicationPasswordDeleteResponse(deleted = true))

            val payload = restClient.deleteApplicationPassword(testSite, credentialsWithoutUuid)

            assertFalse(payload.isError)
            assertTrue(payload.isDeleted)
        }

    @Test
    fun `given the listed password has no uuid, when fetching its uuid, then return an error payload`() =
        runTest {
            // GIVEN the server lists the password but omits the uuid field
            val listedPassword = Gson().fromJson(
                """{"name":"woo-app"}""",
                ApplicationPasswordsFetchResponse::class.java
            )
            givenCookieAuthReturns(WPAPIResponse.Success(arrayOf(listedPassword), emptyList()))

            // WHEN
            val payload = restClient.fetchApplicationPasswordUUID(testSite, "woo-app")

            // THEN
            assertTrue(payload.isError)
            assertEquals("UUID missing from response", payload.error.message)
        }

    @Test
    fun `given the password is listed with a uuid, when fetching its uuid, then return it`() =
        runTest {
            // GIVEN
            val listedPassword = Gson().fromJson(
                """{"uuid":"the-uuid","name":"woo-app"}""",
                ApplicationPasswordsFetchResponse::class.java
            )
            givenCookieAuthReturns(WPAPIResponse.Success(arrayOf(listedPassword), emptyList()))

            // WHEN
            val payload = restClient.fetchApplicationPasswordUUID(testSite, "woo-app")

            // THEN
            assertFalse(payload.isError)
            assertEquals("the-uuid", payload.uuid)
        }

    @Test
    fun `given no password matches the app name, when fetching its uuid, then say it was not found`() =
        runTest {
            // GIVEN a list that does not contain our application name
            val other = Gson().fromJson(
                """{"uuid":"other-uuid","name":"some-other-app"}""",
                ApplicationPasswordsFetchResponse::class.java
            )
            givenCookieAuthReturns(WPAPIResponse.Success(arrayOf(other), emptyList()))

            // WHEN
            val payload = restClient.fetchApplicationPasswordUUID(testSite, "woo-app")

            // THEN
            assertTrue(payload.isError)
            assertEquals("UUID for application password woo-app was not found", payload.error.message)
        }

    @Test
    fun `given the list request fails, when fetching a uuid, then the network error is propagated`() =
        runTest {
            // GIVEN
            val networkError = WPAPINetworkError(
                BaseNetworkError(VolleyError(NetworkResponse(500, byteArrayOf(), true, 0, emptyList())))
            )
            givenCookieAuthReturns(WPAPIResponse.Error(networkError))

            // WHEN
            val payload = restClient.fetchApplicationPasswordUUID(testSite, "woo-app")

            // THEN
            assertTrue(payload.isError)
            assertEquals(networkError, payload.error)
        }

    @Test
    fun `given the introspect request fails, when deleting a password, then the network error is propagated`() =
        runTest {
            // GIVEN
            val networkError = VolleyError(NetworkResponse(500, byteArrayOf(), true, 0, emptyList()))
            givenErrorResponse(networkError)

            // WHEN
            val payload = restClient.deleteApplicationPassword(testSite, credentialsWithoutUuid)

            // THEN the original network error surfaces, not a synthesised "missing uuid" one
            assertTrue(payload.isError)
            assertFalse(payload.isDeleted)
            assertEquals(networkError, payload.error.volleyError)
        }

    @Test
    fun `given the credentials already hold a uuid, when deleting a password, then the introspect call is skipped`() =
        runTest {
            // GIVEN
            givenSuccessResponse(ApplicationPasswordDeleteResponse(deleted = true))

            // WHEN
            val payload = restClient.deleteApplicationPassword(
                testSite,
                credentialsWithoutUuid.copy(uuid = "the-uuid")
            )

            // THEN only the DELETE is issued
            assertFalse(payload.isError)
            assertTrue(payload.isDeleted)
            verify(noCookieRequestQueue, times(1)).add(any<WPAPIGsonRequest<Any>>())
        }

    @Test
    fun `given the site accepts the credentials, when checking their validity, then report them as valid`() =
        runTest {
            // GIVEN
            givenSuccessResponse(
                Gson().fromJson(
                    """{"uuid":"the-uuid","name":"woo-app"}""",
                    ApplicationPasswordsFetchResponse::class.java
                )
            )

            // WHEN
            val validity = restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN
            assertEquals(ApplicationPasswordValidity.VALID, validity)
        }

    @Test
    fun `given the site rejects the credentials with 401, when checking their validity, then report as invalid`() =
        runTest {
            // GIVEN
            givenErrorResponse(VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList())))

            // WHEN
            val validity = restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN
            assertEquals(ApplicationPasswordValidity.INVALID, validity)
        }

    @Test
    fun `given the site answers 403, when checking their validity, then report the result as unknown`() =
        runTest {
            // GIVEN a 403, which on this endpoint means we did authenticate but were denied for another
            // reason, so it says nothing about the credentials being revoked
            givenErrorResponse(VolleyError(NetworkResponse(403, byteArrayOf(), true, 0, emptyList())))

            // WHEN
            val validity = restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN
            assertEquals(ApplicationPasswordValidity.UNKNOWN, validity)
        }

    @Test
    fun `given the validity check itself fails, when checking validity, then report the result as unknown`() =
        runTest {
            // GIVEN the introspect endpoint is unavailable, which says nothing about the credentials
            givenErrorResponse(VolleyError(NetworkResponse(404, byteArrayOf(), true, 0, emptyList())))

            // WHEN
            val validity = restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN
            assertEquals(ApplicationPasswordValidity.UNKNOWN, validity)
        }

    @Test
    fun `given the site is unreachable, when checking their validity, then report the result as unknown`() =
        runTest {
            // GIVEN a transport failure, so the error carries no NetworkResponse at all
            givenErrorResponse(VolleyError())

            // WHEN
            val validity = restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN
            assertEquals(ApplicationPasswordValidity.UNKNOWN, validity)
        }

    @Test
    fun `given the site answers with an empty body, when checking their validity, then report them as valid`() =
        runTest {
            // GIVEN the credentials were accepted, even though nothing was parsed out of the response
            givenSuccessResponse(null)

            // WHEN
            val validity = restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN the status code answers the question, not the body
            assertEquals(ApplicationPasswordValidity.VALID, validity)
        }

    @Test
    fun `when checking the credentials validity, then use basic auth against the introspect endpoint`() =
        runTest {
            // GIVEN
            givenSuccessResponse(
                Gson().fromJson(
                    """{"uuid":"the-uuid","name":"woo-app"}""",
                    ApplicationPasswordsFetchResponse::class.java
                )
            )

            // WHEN
            restClient.checkApplicationPasswordValidity(testSite, credentialsWithoutUuid)

            // THEN
            val request = checkNotNull(lastRequest)
            assertEquals(
                "https://test-site.com/wp-json/wp/v2/users/me/application-passwords/introspect/",
                request.url
            )
            assertEquals(Request.Method.GET, request.method)
            assertEquals(
                Credentials.basic(credentialsWithoutUuid.userName, credentialsWithoutUuid.password),
                request.headers["Authorization"]
            )
        }

    private fun givenSuccessResponse(response: Any?) = givenSuccessResponses(response)

    @Suppress("UNCHECKED_CAST")
    private fun givenSuccessResponses(vararg responses: Any?) {
        val remaining = responses.toMutableList()
        whenever(noCookieRequestQueue.add(any<WPAPIGsonRequest<Any>>())).thenAnswer { invocation ->
            val request = invocation.arguments.first() as WPAPIGsonRequest<Any>
            lastRequest = request
            val deliverMethod = Request::class.java.getDeclaredMethod("deliverResponse", Any::class.java)
            deliverMethod.isAccessible = true
            deliverMethod.invoke(request, ResponseWithHeaders(remaining.removeAt(0), emptyList()))
            return@thenAnswer request
        }
    }

    private suspend fun givenCookieAuthReturns(response: WPAPIResponse<Array<ApplicationPasswordsFetchResponse>>) {
        whenever(
            cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest<Array<ApplicationPasswordsFetchResponse>>(
                eq(testSite),
                any()
            )
        ).thenReturn(response)
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenErrorResponse(error: VolleyError) {
        whenever(noCookieRequestQueue.add(any<WPAPIGsonRequest<Any>>())).thenAnswer { invocation ->
            val request = invocation.arguments.first() as WPAPIGsonRequest<Any>
            lastRequest = request
            request.deliverError(error)
            return@thenAnswer request
        }
    }
}
