package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import com.google.gson.Gson
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.media.MediaTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WPComV2MediaRestClientTest {
    private val accessToken: AccessToken = mock()
    private val okHttpClient: OkHttpClient = mock()
    private val wpComNetwork: WPComNetwork = mock()
    private val gson: Gson = Gson()
    private val mockedCall: Call = mock()
    private lateinit var restClient: WPComV2MediaRestClient

    @Before
    fun setup() {
        restClient = WPComV2MediaRestClient(
            okHttpClient = okHttpClient,
            accessToken = accessToken,
            wpComNetwork = wpComNetwork,
            gson = gson
        )
    }

    @Test
    fun `when upload finishes, then emit success payload`() {
        createFileThenRunTest {
            whenever(okHttpClient.newCall(any())).thenReturn(mockedCall)
            whenever(mockedCall.enqueue(any())).then {
                (it.arguments.first() as Callback).onResponse(
                    mockedCall,
                    mock {
                        on { body } doReturn UnitTestUtils.getStringFromResourceFile(
                            this::class.java,
                            "media/media-upload-wp-api-success.json"
                        ).toResponseBody("application/json".toMediaType())
                        on { isSuccessful } doReturn true
                    }
                )
            }

            val payloads = runBlocking {
                restClient.uploadMedia(
                    SiteModel(),
                    MediaTestUtils.generateMediaFromPath(0, 0L, "./image.jpg")
                ).toList()
            }

            assertThat(payloads.last().completed).isTrue()
        }
    }

    @Test
    fun `when upload fails, then emit failure payload`() {
        createFileThenRunTest {
            whenever(okHttpClient.newCall(any())).thenReturn(mockedCall)
            whenever(mockedCall.enqueue(any())).then {
                (it.arguments.first() as Callback).onFailure(mock(), IOException())
            }

            val payloads = runBlocking {
                restClient.uploadMedia(
                    SiteModel(),
                    MediaTestUtils.generateMediaFromPath(0, 0L, "./image.jpg")
                ).toList()
            }

            assertThat(payloads.last().error).isNotNull()
        }
    }

    @Test
    fun `when response cannot be parsed, then emit failure payload`() {
        createFileThenRunTest {
            whenever(okHttpClient.newCall(any())).thenReturn(mockedCall)
            whenever(mockedCall.enqueue(any())).then {
                (it.arguments.first() as Callback).onResponse(
                    mockedCall,
                    mock {
                        on { body } doReturn "".toResponseBody("application/json".toMediaType())
                        on { isSuccessful } doReturn true
                    }
                )
            }

            val payloads = runBlocking {
                restClient.uploadMedia(
                    SiteModel(),
                    MediaTestUtils.generateMediaFromPath(0, 0L, "./image.jpg")
                ).toList()
            }

            assertThat(payloads.last().error).isNotNull()
        }
    }

    private fun createFileThenRunTest(test: () -> Unit) {
        val file = File("./image.jpg")
        file.createNewFile()
        try {
            test()
        } finally {
            file.delete()
        }
    }
}
