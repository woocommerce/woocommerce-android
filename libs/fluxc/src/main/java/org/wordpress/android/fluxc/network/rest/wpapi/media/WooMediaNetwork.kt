package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.Job
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooMediaNetwork @Inject constructor(
    private val dispatcher: Dispatcher,
    private val coroutineEngine: CoroutineEngine,
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration,
    private val applicationPasswordsMediaRestClient: ApplicationPasswordsMediaRestClient,
    private val wpComV2MediaRestClient: WPComV2MediaRestClient,
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport,
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler
) {
    private val currentUploads = ConcurrentHashMap<Int, Job>()

    fun uploadMedia(site: SiteModel, media: MediaModel): Boolean {
        return when (site.origin) {
            SiteModel.ORIGIN_WPAPI, SiteModel.ORIGIN_XMLRPC -> {
                if (!applicationPasswordsConfiguration.isEnabledForDirectAccess()) {
                    false
                } else {
                    launchUpload(site, media)
                    true
                }
            }

            SiteModel.ORIGIN_WPCOM_REST -> {
                launchUpload(site, media)
                true
            }

            else -> false
        }
    }

    fun fetchMediaList(site: SiteModel, number: Int, offset: Int, mimeType: MimeType.Type?): Boolean {
        return when (site.origin) {
            SiteModel.ORIGIN_WPAPI, SiteModel.ORIGIN_XMLRPC -> {
                if (!applicationPasswordsConfiguration.isEnabledForDirectAccess()) {
                    false
                } else {
                    coroutineEngine.launch(
                        AppLog.T.MEDIA,
                        this,
                        "Fetching media list via Application Passwords"
                    ) {
                        val payload = applicationPasswordsMediaRestClient.executeFetchMediaList(
                            site,
                            number,
                            offset,
                            mimeType
                        )
                        dispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload))
                    }
                    true
                }
            }

            SiteModel.ORIGIN_WPCOM_REST -> {
                coroutineEngine.launch(AppLog.T.MEDIA, this, "Fetching media list for Jetpack site") {
                    val payload = fetchJetpackMediaList(site, number, offset, mimeType)
                    dispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload))
                }
                true
            }

            else -> false
        }
    }

    fun cancelUpload(site: SiteModel, media: MediaModel): Boolean {
        return when (site.origin) {
            SiteModel.ORIGIN_WPAPI, SiteModel.ORIGIN_XMLRPC, SiteModel.ORIGIN_WPCOM_REST -> {
                currentUploads.remove(media.id)?.cancel()
                val payload = ProgressPayload(media, 0f, false, true)
                dispatcher.dispatch(MediaActionBuilder.newCanceledMediaUploadAction(payload))
                true
            }

            else -> false
        }
    }

    private fun launchUpload(site: SiteModel, media: MediaModel) {
        val uploadJob = coroutineEngine.launch(AppLog.T.MEDIA, this, "Uploading media") {
            try {
                when (site.origin) {
                    SiteModel.ORIGIN_WPAPI, SiteModel.ORIGIN_XMLRPC -> {
                        dispatchUploadFlow(applicationPasswordsMediaRestClient.getUploadMediaFlow(site, media))
                    }

                    SiteModel.ORIGIN_WPCOM_REST -> {
                        uploadForJetpackSite(site, media)
                    }
                }
            } finally {
                currentUploads.remove(media.id)
            }
        }
        currentUploads[media.id] = uploadJob
    }

    private suspend fun uploadForJetpackSite(site: SiteModel, media: MediaModel) {
        if (!applicationPasswordsConfiguration.isEnabledForJetpackAccess() ||
            !jetpackApplicationPasswordsSupport.supportsAppPasswords(site)
        ) {
            dispatchUploadFlow(wpComV2MediaRestClient.getUploadMediaFlow(site, media))
            return
        }

        var appPasswordsError: MediaError? = null
        applicationPasswordsMediaRestClient.getUploadMediaFlow(site, media).collect { payload ->
            if (payload.isError) {
                appPasswordsError = payload.error
            } else {
                dispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload))
            }
        }

        if (appPasswordsError == null) {
            return
        }

        var fallbackSucceeded = false
        wpComV2MediaRestClient.getUploadMediaFlow(site, media).collect { payload ->
            if (payload.completed && !payload.isError && !payload.canceled) {
                fallbackSucceeded = true
            }
            dispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload))
        }

        if (fallbackSucceeded) {
            jetpackApplicationPasswordsErrorHandler.handleError(
                site,
                appPasswordsError.toWPAPINetworkError()
            )
        }
    }

    private suspend fun fetchJetpackMediaList(
        site: SiteModel,
        number: Int,
        offset: Int,
        mimeType: MimeType.Type?
    ): FetchMediaListResponsePayload {
        if (!applicationPasswordsConfiguration.isEnabledForJetpackAccess() ||
            !jetpackApplicationPasswordsSupport.supportsAppPasswords(site)
        ) {
            return wpComV2MediaRestClient.executeFetchMediaList(site, number, offset, mimeType)
        }

        val appPasswordsPayload = applicationPasswordsMediaRestClient.executeFetchMediaList(
            site,
            number,
            offset,
            mimeType
        )
        if (!appPasswordsPayload.isError) {
            return appPasswordsPayload
        }

        val fallbackPayload = wpComV2MediaRestClient.executeFetchMediaList(site, number, offset, mimeType)
        if (!fallbackPayload.isError) {
            jetpackApplicationPasswordsErrorHandler.handleError(
                site,
                appPasswordsPayload.error.toWPAPINetworkError()
            )
        }

        return fallbackPayload
    }

    private suspend fun dispatchUploadFlow(flow: kotlinx.coroutines.flow.Flow<ProgressPayload>) {
        flow.collect { payload ->
            dispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload))
        }
    }

    private fun MediaError?.toWPAPINetworkError(): WPAPINetworkError {
        requireNotNull(this) { "MediaError is required to build a WPAPINetworkError" }

        val baseError = BaseNetworkError(
            errorTypeFrom(this),
            message.orEmpty(),
            volleyErrorFrom(this)
        )
        return WPAPINetworkError(baseError, apiErrorCode)
    }

    private fun errorTypeFrom(error: MediaError): GenericErrorType {
        return when (error.statusCode) {
            401 -> GenericErrorType.NOT_AUTHENTICATED
            403 -> GenericErrorType.AUTHORIZATION_REQUIRED
            404 -> GenericErrorType.NOT_FOUND
            408 -> GenericErrorType.TIMEOUT
            in 500..599 -> GenericErrorType.SERVER_ERROR
            else -> when (error.type) {
                org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.NOT_AUTHENTICATED ->
                    GenericErrorType.NOT_AUTHENTICATED

                org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.AUTHORIZATION_REQUIRED ->
                    GenericErrorType.AUTHORIZATION_REQUIRED

                org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.NOT_FOUND ->
                    GenericErrorType.NOT_FOUND

                org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.TIMEOUT ->
                    GenericErrorType.TIMEOUT

                org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.SERVER_ERROR ->
                    GenericErrorType.SERVER_ERROR

                org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.PARSE_ERROR ->
                    GenericErrorType.PARSE_ERROR

                else -> GenericErrorType.UNKNOWN
            }
        }
    }

    private fun volleyErrorFrom(error: MediaError): VolleyError {
        return if (error.statusCode > 0) {
            VolleyError(NetworkResponse(error.statusCode, null, true, 0, emptyList()))
        } else {
            VolleyError(error.message)
        }
    }
}
