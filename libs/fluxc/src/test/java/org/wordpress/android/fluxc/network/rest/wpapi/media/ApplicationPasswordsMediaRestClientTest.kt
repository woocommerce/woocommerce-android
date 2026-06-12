package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCreationResult
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError

@ExperimentalCoroutinesApi
class ApplicationPasswordsMediaRestClientTest {
    private val okHttpClient: OkHttpClient = mock()
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork = mock()
    private val applicationPasswordsManager: ApplicationPasswordsManager = mock()

    private val site = SiteModel().apply {
        url = "https://example.com"
    }
    private val media = MediaModel(0, null, null, null, null, null).apply {
        id = 123
    }

    private lateinit var sut: ApplicationPasswordsMediaRestClient

    @Before
    fun setup() {
        sut = ApplicationPasswordsMediaRestClient(
            okHttpClient = okHttpClient,
            applicationPasswordsNetwork = applicationPasswordsNetwork,
            gson = Gson()
        ).apply {
            applicationPasswordsManager = this@ApplicationPasswordsMediaRestClientTest.applicationPasswordsManager
        }
    }

    @Test
    fun `given credential generation fails, when uploading media, then emit one prefixed error without request`() =
        runTest {
            val networkError = WPAPINetworkError(
                BaseNetworkError(
                    GenericErrorType.NOT_AUTHENTICATED,
                    "Unable to create application password",
                    VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList()))
                ),
                "incorrect_password"
            )
            whenever(applicationPasswordsManager.getApplicationCredentials(site)).thenReturn(
                ApplicationPasswordCreationResult.Failure(networkError)
            )

            val payloads = sut.uploadMedia(site, media).toList()

            assertThat(payloads).hasSize(1)
            val payload = payloads.single()
            assertThat(payload.media).isSameAs(media)
            assertThat(payload.progress).isEqualTo(1f)
            assertThat(payload.completed).isFalse()
            val error = requireNotNull(payload.error)
            assertThat(error.statusCode).isEqualTo(401)
            assertThat(error.apiErrorCode).isEqualTo(
                ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX + "incorrect_password"
            )
            verify(okHttpClient, never()).newCall(any())
        }

    @Test
    fun `given app passwords unsupported, when uploading media, then emit one prefixed error without request`() =
        runTest {
            val networkError = WPComGsonNetworkError(
                BaseNetworkError(
                    GenericErrorType.SERVER_ERROR,
                    "Application passwords are disabled",
                    VolleyError(NetworkResponse(403, byteArrayOf(), true, 0, emptyList()))
                )
            ).apply {
                apiError = "application_passwords_disabled"
            }
            whenever(applicationPasswordsManager.getApplicationCredentials(site)).thenReturn(
                ApplicationPasswordCreationResult.NotSupported(networkError)
            )

            val payloads = sut.uploadMedia(site, media).toList()

            assertThat(payloads).hasSize(1)
            val payload = payloads.single()
            assertThat(payload.media).isSameAs(media)
            assertThat(payload.progress).isEqualTo(1f)
            assertThat(payload.completed).isFalse()
            val error = requireNotNull(payload.error)
            assertThat(error.statusCode).isEqualTo(403)
            assertThat(error.apiErrorCode).isEqualTo(
                ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX +
                    "application_passwords_disabled"
            )
            verify(okHttpClient, never()).newCall(any())
        }
}
