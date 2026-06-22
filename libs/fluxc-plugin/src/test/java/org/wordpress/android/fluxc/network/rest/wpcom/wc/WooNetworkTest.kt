package org.wordpress.android.fluxc.network.rest.wpcom.wc

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import java.util.Optional

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WooNetworkTest {
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration = mock()
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork = mock()
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork = mock()
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport = mock()
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler = mock()
    private val unknownBlogListener: UnknownBlogListener = mock()

    private val sut = WooNetwork(
        applicationPasswordsConfiguration,
        applicationPasswordsNetwork,
        jetpackTunnelWPAPINetwork,
        jetpackApplicationPasswordsSupport,
        jetpackApplicationPasswordsErrorHandler,
        Optional.of(unknownBlogListener)
    )

    @Test
    fun `given an app-password site returns unknown_blog, when a request is made, then onUnknownBlog is reported`() =
        runBlocking {
            val site = SiteModel().apply {
                origin = SiteModel.ORIGIN_WPAPI
                siteId = SITE_ID
            }
            whenever(applicationPasswordsNetwork.executePostGsonRequest<Any>(any(), any(), any(), any()))
                .thenReturn(errorResponse(UNKNOWN_BLOG))

            sut.executePostGsonRequest(site, "path", Any::class.java, emptyMap())

            verify(unknownBlogListener).onUnknownBlog(SITE_ID)
        }

    @Test
    fun `given a Jetpack site returns unknown_blog, when a request is made, then onUnknownBlog is reported`() =
        runBlocking {
            val site = SiteModel().apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
                siteId = SITE_ID
            }
            whenever(applicationPasswordsConfiguration.isEnabledForJetpackAccess()).thenReturn(false)
            whenever(jetpackTunnelWPAPINetwork.executePostGsonRequest<Any>(any(), any(), any(), any()))
                .thenReturn(errorResponse(UNKNOWN_BLOG))

            sut.executePostGsonRequest(site, "path", Any::class.java, emptyMap())

            verify(unknownBlogListener).onUnknownBlog(SITE_ID)
        }

    @Test
    fun `given app-passwords falls back to Jetpack tunnel, when the tunnel returns unknown_blog, then it is reported`() =
        runBlocking {
            val site = SiteModel().apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
                siteId = SITE_ID
                url = "https://example.com"
            }
            whenever(applicationPasswordsConfiguration.isEnabledForJetpackAccess()).thenReturn(true)
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(any())).thenReturn(true)
            whenever(applicationPasswordsNetwork.executePostGsonRequest<Any>(any(), any(), any(), any()))
                .thenReturn(errorResponse("rest_cannot_access"))
            whenever(jetpackTunnelWPAPINetwork.executePostGsonRequest<Any>(any(), any(), any(), any()))
                .thenReturn(errorResponse(UNKNOWN_BLOG))

            sut.executePostGsonRequest(site, "path", Any::class.java, emptyMap())

            verify(unknownBlogListener).onUnknownBlog(SITE_ID)
        }

    @Test
    fun `given a site returns a successful response, when a request is made, then nothing is reported`() =
        runBlocking {
            val site = SiteModel().apply {
                origin = SiteModel.ORIGIN_WPAPI
                siteId = SITE_ID
            }
            whenever(applicationPasswordsNetwork.executePostGsonRequest<Any>(any(), any(), any(), any()))
                .thenReturn(WPAPIResponse.Success(data = Any(), headers = emptyList()))

            sut.executePostGsonRequest(site, "path", Any::class.java, emptyMap())

            verify(unknownBlogListener, never()).onUnknownBlog(any())
        }

    @Test
    fun `given a site returns a different error, when a request is made, then nothing is reported`() =
        runBlocking {
            val site = SiteModel().apply {
                origin = SiteModel.ORIGIN_WPAPI
                siteId = SITE_ID
            }
            whenever(applicationPasswordsNetwork.executePostGsonRequest<Any>(any(), any(), any(), any()))
                .thenReturn(errorResponse("rest_no_route"))

            sut.executePostGsonRequest(site, "path", Any::class.java, emptyMap())

            verify(unknownBlogListener, never()).onUnknownBlog(any())
        }

    private fun errorResponse(errorCode: String?): WPAPIResponse<Any> = WPAPIResponse.Error(
        WPAPINetworkError(
            baseError = BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.UNKNOWN),
            errorCode = errorCode
        )
    )

    companion object {
        private const val SITE_ID = 123L
        private const val UNKNOWN_BLOG = "unknown_blog"
    }
}
