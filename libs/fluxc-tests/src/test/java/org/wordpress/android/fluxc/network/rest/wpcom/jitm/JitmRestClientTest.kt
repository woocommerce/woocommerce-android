package org.wordpress.android.fluxc.network.rest.wpcom.jitm

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMContent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMCta
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JitmRestClient

class JitmRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private lateinit var jitmRestClient: JitmRestClient

    @Before
    fun setup() {
        jitmRestClient = JitmRestClient(wooNetwork)
    }

    private fun provideJitmApiResponse(
        template: String = "",
        content: JITMContent = provideJitmContent(),
        jitmCta: JITMCta = provideJitmCta(),
        timeToLive: Int = 0,
        id: String = "",
        featureClass: String = "",
        expires: Long = 0L,
        maxDismissal: Int = 2,
        isDismissible: Boolean = false,
        url: String = "",
        jitmStatsUrl: String = "",
        assets: Map<String, String>? = null
    ) = JITMApiResponse(
        template = template,
        content = content,
        cta = jitmCta,
        timeToLive = timeToLive,
        id = id,
        featureClass = featureClass,
        expires = expires,
        maxDismissal = maxDismissal,
        isDismissible = isDismissible,
        url = url,
        jitmStatsUrl = jitmStatsUrl,
        assets = assets
    )

    private fun provideJitmContent(
        message: String = "",
        description: String = "",
        icon: String = "",
        title: String = "",
        iconPath: String = ""
    ) = JITMContent(
        message = message,
        description = description,
        icon = icon,
        title = title,
        iconPath = iconPath,
    )

    private fun provideJitmCta(
        message: String = "",
        link: String = ""
    ) = JITMCta(
        message = message,
        link = link
    )

    @Test
    fun `given success response, when fetch jitm, return success`() {
        runBlocking {
            val site = SiteModel().apply { siteId = 1234 }
            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = site,
                    path = JPAPI.jitm.pathV4,
                    params = mapOf(
                        "message_path" to "",
                        "query" to "",
                    ),
                    clazz = Array<JITMApiResponse>::class.java,
                )
            ).thenReturn(
                WPAPIResponse.Success(
                    arrayOf(provideJitmApiResponse())
                )
            )

            val actualResponse = jitmRestClient.fetchJitmMessage(
                site,
                "",
                "",
            )

            assertThat(actualResponse.isError).isFalse
            assertThat(actualResponse.result).isNotNull
        }
    }

    @Test
    fun `given error response, when fetch jitm, return error`() {
        val site = SiteModel().apply { siteId = 1234 }
        runBlocking {
            val expectedError = mock<WPAPINetworkError>().apply {
                type = mock()
            }
            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = site,
                    path = JPAPI.jitm.pathV4,
                    params = mapOf(
                        "message_path" to "",
                        "query" to "",
                    ),
                    clazz = Array<JITMApiResponse>::class.java,
                )
            ).thenReturn(
                WPAPIResponse.Error(expectedError)
            )

            val actualResponse = jitmRestClient.fetchJitmMessage(site, "", "")

            assertThat(actualResponse.isError).isTrue
            assertThat(actualResponse.error).isNotNull
        }
    }

    @Test
    fun `given success response, when dismiss jitm, return success`() {
        runBlocking {
            val site = SiteModel().apply { siteId = 1234 }
            whenever(
                wooNetwork.executePostGsonRequest(
                    site = site,
                    path = JPAPI.jitm.pathV4,
                    body = mapOf(
                        "id" to "",
                        "feature_class" to ""
                    ),
                    clazz = Any::class.java,
                )
            ).thenReturn(
                WPAPIResponse.Success(
                    arrayOf(provideJitmApiResponse())
                )
            )

            val actualResponse = jitmRestClient.dismissJitmMessage(
                site,
                "",
                ""
            )

            assertThat(actualResponse.isError).isFalse
            assertThat(actualResponse.result).isTrue
        }
    }

    @Test
    fun `given error response, when dismiss jitm, return error`() {
        runBlocking {
            val site = SiteModel().apply { siteId = 1234 }
            val expectedError = mock<WPAPINetworkError>().apply {
                type = mock()
            }
            whenever(
                wooNetwork.executePostGsonRequest(
                    site = site,
                    path = JPAPI.jitm.pathV4,
                    body = mapOf(
                        "id" to "",
                        "feature_class" to ""
                    ),
                    clazz = Any::class.java,
                )
            ).thenReturn(
                WPAPIResponse.Error(expectedError)
            )

            val actualResponse = jitmRestClient.dismissJitmMessage(
                site,
                "",
                ""
            )

            assertThat(actualResponse.isError).isTrue
            assertThat(actualResponse.result).isNull()
        }
    }
}
