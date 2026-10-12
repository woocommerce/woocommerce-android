package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class JetpackApplicationPasswordsRestClientTest {
    private val testSite = SiteModel().apply {
        url = "http://test-site.com"
    }

    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder = mock()
    private val restClient = JetpackApplicationPasswordsRestClient(
        jetpackTunnelGsonRequestBuilder = jetpackTunnelGsonRequestBuilder,
        appContext = mock(),
        dispatcher = mock(),
        requestQueue = mock(),
        accessToken = mock(),
        userAgent = mock()
    )

    @Test
    fun `given the creation response has no password, when creating a password, then return an error payload`() =
        runTest {
            // GIVEN the site answers 200 but omits the password field, so Gson leaves it null
            givenCreationResponse("""{"uuid":"the-uuid","name":"woo-app"}""")

            // WHEN
            val payload = restClient.createApplicationPassword(testSite, "woo-app")

            // THEN
            assertTrue(payload.isError)
            assertEquals("Password missing from response", payload.error.message)
        }

    @Test
    fun `given the creation response has a password, when creating a password, then return it with its uuid`() =
        runTest {
            // GIVEN
            givenCreationResponse("""{"uuid":"the-uuid","name":"woo-app","password":"the-password"}""")

            // WHEN
            val payload = restClient.createApplicationPassword(testSite, "woo-app")

            // THEN
            assertFalse(payload.isError)
            assertEquals("the-password", payload.password)
            assertEquals("the-uuid", payload.uuid)
        }

    private suspend fun givenCreationResponse(json: String) {
        whenever(
            jetpackTunnelGsonRequestBuilder.syncPostRequest(
                restClient = eq(restClient),
                site = eq(testSite),
                url = any(),
                body = any(),
                clazz = eq(ApplicationPasswordCreationResponse::class.java)
            )
        ).thenReturn(
            JetpackSuccess(Gson().fromJson(json, ApplicationPasswordCreationResponse::class.java), emptyList())
        )
    }
}
