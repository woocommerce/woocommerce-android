package org.wordpress.android.fluxc.network.rest.wpapi.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.MimeType
import java.security.GeneralSecurityException
import org.wordpress.android.fluxc.annotations.action.Action as FluxAction

@RunWith(RobolectricTestRunner::class)
class WooMediaNetworkTest {
    private val dispatcher: Dispatcher = mock()
    private val coroutineEngine = CoroutineEngine(Dispatchers.Unconfined, mock<AppLogWrapper>())
    private val applicationPasswordsConfiguration = FakeApplicationPasswordsConfiguration()
    private val applicationPasswordsMediaRestClient: ApplicationPasswordsMediaRestClient = mock()
    private val wpComV2MediaRestClient: WPComV2MediaRestClient = mock()
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport = mock()
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler = mock()
    private val crashLogger: FluxCCrashLogger = mock()

    private val sut = WooMediaNetwork(
        dispatcher = dispatcher,
        coroutineEngine = coroutineEngine,
        applicationPasswordsConfiguration = applicationPasswordsConfiguration,
        applicationPasswordsMediaRestClient = applicationPasswordsMediaRestClient,
        wpComV2MediaRestClient = wpComV2MediaRestClient,
        jetpackApplicationPasswordsSupport = jetpackApplicationPasswordsSupport,
        jetpackApplicationPasswordsErrorHandler = jetpackApplicationPasswordsErrorHandler,
        crashLogger = crashLogger
    )

    private val jetpackSite = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        siteId = 123L
        url = "https://example.com"
    }

    private val wpapiSite = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPAPI
        siteId = 456L
        url = "https://direct.example.com"
    }

    @Test
    fun `given supported jetpack site, when fetching media list, then use app passwords first`() = runTest {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
        whenever(
            applicationPasswordsMediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
        ).thenReturn(
            FetchMediaListResponsePayload(jetpackSite, emptyList(), false, false, MimeType.Type.IMAGE)
        )

        sut.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)

        verify(applicationPasswordsMediaRestClient).fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
        verify(wpComV2MediaRestClient, never()).fetchMediaList(any(), any(), any(), any())
    }

    @Test
    fun `given supported jetpack site, when app passwords fetch fails and fallback succeeds, then flag site`() =
        runTest {
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
            whenever(
                applicationPasswordsMediaRestClient.fetchMediaList(jetpackSite, 20, 5, MimeType.Type.IMAGE)
            ).thenReturn(
                FetchMediaListResponsePayload(
                    jetpackSite,
                    createMediaError(statusCode = 500, apiErrorCode = "incorrect_password"),
                    MimeType.Type.IMAGE
                )
            )
            whenever(
                wpComV2MediaRestClient.fetchMediaList(jetpackSite, 20, 5, MimeType.Type.IMAGE)
            ).thenReturn(
                FetchMediaListResponsePayload(jetpackSite, emptyList(), false, true, MimeType.Type.IMAGE)
            )

            sut.fetchMediaList(jetpackSite, 20, 5, MimeType.Type.IMAGE)

            verify(jetpackApplicationPasswordsErrorHandler).handleError(
                eq(jetpackSite),
                argThat {
                    errorCode == "incorrect_password" &&
                        volleyError?.networkResponse?.statusCode == 500
                }
            )
        }

    @Test
    fun `given unsupported jetpack site, when fetching media list, then skip app passwords`() = runTest {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(false)
        whenever(
            wpComV2MediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
        ).thenReturn(
            FetchMediaListResponsePayload(jetpackSite, emptyList(), false, false, MimeType.Type.IMAGE)
        )

        sut.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)

        verify(applicationPasswordsMediaRestClient, never()).fetchMediaList(any(), any(), any(), any())
        verify(wpComV2MediaRestClient).fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
    }

    @Test
    fun `given jetpack access disabled, when fetching media list, then use wpcom client directly`() = runTest {
        applicationPasswordsConfiguration.jetpackAccessEnabled = false
        whenever(
            wpComV2MediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
        ).thenReturn(
            FetchMediaListResponsePayload(jetpackSite, emptyList(), false, false, MimeType.Type.IMAGE)
        )

        sut.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)

        verify(applicationPasswordsMediaRestClient, never()).fetchMediaList(any(), any(), any(), any())
        verify(wpComV2MediaRestClient).fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
    }

    @Test
    fun `given supported jetpack site, when app passwords fetch throws GeneralSecurityException, then fallback`() =
        runTest {
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
            whenever(
                applicationPasswordsMediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
            ).thenAnswer { throw GeneralSecurityException("keystore error") }
            whenever(
                wpComV2MediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
            ).thenReturn(
                FetchMediaListResponsePayload(jetpackSite, emptyList(), false, false, MimeType.Type.IMAGE)
            )

            sut.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)

            verify(wpComV2MediaRestClient).fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
            verify(jetpackApplicationPasswordsErrorHandler).handleError(
                eq(jetpackSite),
                argThat {
                    errorCode == ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR
                }
            )
        }

    @Test
    fun `given supported jetpack site, when both fetch paths fail, then do not flag site`() = runTest {
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
        whenever(
            applicationPasswordsMediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
        ).thenReturn(
            FetchMediaListResponsePayload(
                jetpackSite,
                createMediaError(statusCode = 500, apiErrorCode = "server_error"),
                MimeType.Type.IMAGE
            )
        )
        whenever(
            wpComV2MediaRestClient.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)
        ).thenReturn(
            FetchMediaListResponsePayload(
                jetpackSite,
                createMediaError(statusCode = 502, apiErrorCode = "bad_gateway"),
                MimeType.Type.IMAGE
            )
        )

        sut.fetchMediaList(jetpackSite, 10, 0, MimeType.Type.IMAGE)

        verify(jetpackApplicationPasswordsErrorHandler, never()).handleError(any(), any())
    }

    @Test
    fun `given WPAPI site, when fetching media list, then use app passwords client`() = runTest {
        whenever(
            applicationPasswordsMediaRestClient.fetchMediaList(wpapiSite, 10, 0, MimeType.Type.IMAGE)
        ).thenReturn(
            FetchMediaListResponsePayload(wpapiSite, emptyList(), false, false, MimeType.Type.IMAGE)
        )

        sut.fetchMediaList(wpapiSite, 10, 0, MimeType.Type.IMAGE)

        verify(applicationPasswordsMediaRestClient).fetchMediaList(wpapiSite, 10, 0, MimeType.Type.IMAGE)
        verify(wpComV2MediaRestClient, never()).fetchMediaList(any(), any(), any(), any())
    }

    @Test
    fun `given WPAPI site with direct access disabled, when fetching media list, then report xmlrpc try`() =
        runTest {
            applicationPasswordsConfiguration.directAccessEnabled = false
            sut.fetchMediaList(wpapiSite, 10, 0, MimeType.Type.IMAGE)
            verify(crashLogger).sendReport(anyOrNull(), any(), any())
            verify(applicationPasswordsMediaRestClient, never()).fetchMediaList(any(), any(), any(), any())
        }

    @Test
    fun `given supported jetpack site, when app passwords upload fails and fallback succeeds, then flag site`() =
        runTest {
            val media: MediaModel = mock {
                on { id } doReturn 7
            }
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
            whenever(applicationPasswordsMediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
                flowOf(
                    ProgressPayload(media, 0.4f, false, null),
                    ProgressPayload(media, 1f, false, createMediaError(statusCode = 403, apiErrorCode = "forbidden"))
                )
            )
            whenever(wpComV2MediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
                flowOf(ProgressPayload(media, 1f, true, false))
            )

            sut.uploadMedia(jetpackSite, media)

            verify(jetpackApplicationPasswordsErrorHandler).handleError(
                eq(jetpackSite),
                argThat {
                    errorCode == "forbidden" &&
                        volleyError?.networkResponse?.statusCode == 403
                }
            )

            val dispatchCaptor = argumentCaptor<FluxAction<*>>()
            verify(dispatcher, times(2)).dispatch(dispatchCaptor.capture())
            val payloads = dispatchCaptor.allValues.map { it.payload as ProgressPayload }
            // First dispatch: progress from app passwords attempt
            assertThat(payloads[0].progress).isEqualTo(0.4f)
            // Second dispatch: completed from fallback
            assertThat(payloads[1].progress).isEqualTo(1f)
            assertThat(payloads[1].completed).isTrue()
        }

    @Test
    fun `given supported jetpack site, when app passwords upload succeeds, then dispatch all progress`() =
        runTest {
            val media: MediaModel = mock {
                on { id } doReturn 8
            }
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
            whenever(applicationPasswordsMediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
                flowOf(
                    ProgressPayload(media, 0.4f, false, null),
                    ProgressPayload(media, 1f, true, false)
                )
            )

            sut.uploadMedia(jetpackSite, media)

            val dispatchCaptor = argumentCaptor<FluxAction<*>>()
            verify(dispatcher, times(2)).dispatch(dispatchCaptor.capture())
            verify(wpComV2MediaRestClient, never()).uploadMedia(any(), any())
            verify(jetpackApplicationPasswordsErrorHandler, never()).handleError(any(), any())

            val payloads = dispatchCaptor.allValues.map { it.payload as ProgressPayload }
            assertThat(payloads).hasSize(2)
            assertThat(payloads[0].progress).isEqualTo(0.4f)
            assertThat(payloads[0].completed).isFalse()
            assertThat(payloads[1].progress).isEqualTo(1f)
            assertThat(payloads[1].completed).isTrue()
        }

    @Test
    fun `given jetpack access disabled, when uploading, then use wpcom client directly`() = runTest {
        applicationPasswordsConfiguration.jetpackAccessEnabled = false
        val media: MediaModel = mock {
            on { id } doReturn 9
        }
        whenever(wpComV2MediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
            flowOf(ProgressPayload(media, 1f, true, false))
        )

        sut.uploadMedia(jetpackSite, media)

        verify(applicationPasswordsMediaRestClient, never()).uploadMedia(any(), any())
        verify(wpComV2MediaRestClient).uploadMedia(jetpackSite, media)
    }

    @Test
    fun `given supported jetpack site, when app passwords upload throws GeneralSecurityException, then fallback`() =
        runTest {
            val media: MediaModel = mock {
                on { id } doReturn 10
            }
            whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
            whenever(applicationPasswordsMediaRestClient.uploadMedia(jetpackSite, media))
                .thenReturn(flow { throw GeneralSecurityException("keystore error") })
            whenever(wpComV2MediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
                flowOf(ProgressPayload(media, 1f, true, false))
            )

            sut.uploadMedia(jetpackSite, media)

            verify(wpComV2MediaRestClient).uploadMedia(jetpackSite, media)
            verify(jetpackApplicationPasswordsErrorHandler).handleError(
                eq(jetpackSite),
                argThat {
                    errorCode == ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR
                }
            )
        }

    @Test
    fun `given supported jetpack site, when both upload paths fail, then do not flag site`() = runTest {
        val media: MediaModel = mock {
            on { id } doReturn 11
        }
        whenever(jetpackApplicationPasswordsSupport.supportsAppPasswords(jetpackSite)).thenReturn(true)
        whenever(applicationPasswordsMediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
            flowOf(ProgressPayload(media, 1f, false, createMediaError(statusCode = 500, apiErrorCode = "error")))
        )
        whenever(wpComV2MediaRestClient.uploadMedia(jetpackSite, media)).thenReturn(
            flowOf(ProgressPayload(media, 1f, false, createMediaError(statusCode = 502, apiErrorCode = "error")))
        )

        sut.uploadMedia(jetpackSite, media)

        verify(jetpackApplicationPasswordsErrorHandler, never()).handleError(any(), any())
    }

    @Test
    fun `given WPAPI site, when uploading media, then use app passwords client`() = runTest {
        val media: MediaModel = mock {
            on { id } doReturn 12
        }
        whenever(applicationPasswordsMediaRestClient.uploadMedia(wpapiSite, media)).thenReturn(
            flowOf(ProgressPayload(media, 1f, true, false))
        )

        sut.uploadMedia(wpapiSite, media)

        verify(applicationPasswordsMediaRestClient).uploadMedia(wpapiSite, media)
        verify(wpComV2MediaRestClient, never()).uploadMedia(any(), any())
    }

    @Test
    fun `given WPAPI site with direct access disabled, when uploading, then report xmlrpc try`() = runTest {
        applicationPasswordsConfiguration.directAccessEnabled = false
        val media: MediaModel = mock()
        sut.uploadMedia(wpapiSite, media)
        verify(crashLogger).sendReport(anyOrNull(), any(), any())
        verify(applicationPasswordsMediaRestClient, never()).uploadMedia(any(), any())
    }

    @Test
    fun `given no active upload, when cancelling upload, then do not dispatch`() {
        val media: MediaModel = mock {
            on { id } doReturn 13
        }

        sut.cancelUpload(jetpackSite, media)

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `given unsupported origin, when cancelling upload, then report xmlrpc try`() {
        val site = SiteModel().apply { origin = 999 }
        val media: MediaModel = mock()
        sut.cancelUpload(site, media)
        verify(crashLogger).sendReport(anyOrNull(), any(), any())
    }

    private fun createMediaError(statusCode: Int, apiErrorCode: String): MediaError {
        return MediaError(MediaErrorType.GENERIC_ERROR).apply {
            this.statusCode = statusCode
            this.apiErrorCode = apiErrorCode
        }
    }

    private class FakeApplicationPasswordsConfiguration : ApplicationPasswordsConfiguration {
        override val applicationName: String = "WooCommerce"
        var directAccessEnabled: Boolean = true
        var jetpackAccessEnabled: Boolean = true

        override fun isEnabledForDirectAccess(): Boolean = directAccessEnabled
        override suspend fun isEnabledForJetpackAccess(): Boolean = jetpackAccessEnabled
    }
}
