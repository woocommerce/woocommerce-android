package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
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
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import java.security.GeneralSecurityException
import java.util.Collections
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
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler,
    private val crashLogger: FluxCCrashLogger
) {
    private val currentUploads = ConcurrentHashMap<Int, Job>()

    fun uploadMedia(site: SiteModel, media: MediaModel) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            launchUpload(site, media, useAppPasswords = false)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            launchUpload(site, media, useAppPasswords = true)
        } else {
            reportXmlrpcTry()
        }
    }

    fun fetchMediaList(site: SiteModel, number: Int, offset: Int, mimeType: MimeType.Type?) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            coroutineEngine.launch(AppLog.T.MEDIA, this, "Fetching media list for Jetpack site") {
                val payload = fetchMediaListWithJetpackFallback(site, number, offset, mimeType)
                dispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload))
            }
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            coroutineEngine.launch(AppLog.T.MEDIA, this, "Fetching media list") {
                val payload = applicationPasswordsMediaRestClient.fetchMediaList(
                    site, number, offset, mimeType
                )
                dispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload))
            }
        } else {
            reportXmlrpcTry()
        }
    }

    fun cancelUpload(site: SiteModel, media: MediaModel) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            performCancelUpload(media)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            performCancelUpload(media)
        } else {
            reportXmlrpcTry()
        }
    }

    private fun performCancelUpload(media: MediaModel) {
        val job = currentUploads.remove(media.id)
        if (job != null) {
            job.cancel()
            val payload = ProgressPayload(media, 0f, false, true)
            dispatcher.dispatch(MediaActionBuilder.newCanceledMediaUploadAction(payload))
        }
    }

    private fun launchUpload(site: SiteModel, media: MediaModel, useAppPasswords: Boolean) {
        val uploadJob = coroutineEngine.launch(AppLog.T.MEDIA, this, "Uploading media") {
            try {
                if (useAppPasswords) {
                    dispatchUploadFlow(applicationPasswordsMediaRestClient.uploadMedia(site, media))
                } else {
                    uploadForJetpackSite(site, media)
                }
            } finally {
                currentUploads.remove(media.id)
            }
        }
        currentUploads[media.id] = uploadJob
    }

    private suspend fun uploadForJetpackSite(site: SiteModel, media: MediaModel) {
        if (!shouldUseAppPasswordsForJetpack(site)) {
            dispatchUploadFlow(wpComV2MediaRestClient.uploadMedia(site, media))
            return
        }

        var appPasswordsError: MediaError? = null

        try {
            applicationPasswordsMediaRestClient.uploadMedia(site, media).collect { payload ->
                if (payload.isError) {
                    appPasswordsError = payload.error
                } else {
                    dispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload))
                }
            }
        } catch (e: GeneralSecurityException) {
            AppLog.e(AppLog.T.MEDIA, "Error setting up Application Passwords encryption", e)
            appPasswordsError = createKeystoreEncryptionError()
        }

        if (appPasswordsError == null) return

        logFailedAppPasswordsRequest("upload", site, appPasswordsError)

        var fallbackSucceeded = false
        wpComV2MediaRestClient.uploadMedia(site, media).collect { payload ->
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

    private suspend fun fetchMediaListWithJetpackFallback(
        site: SiteModel,
        number: Int,
        offset: Int,
        mimeType: MimeType.Type?
    ): FetchMediaListResponsePayload {
        if (!shouldUseAppPasswordsForJetpack(site)) {
            return wpComV2MediaRestClient.fetchMediaList(site, number, offset, mimeType)
        }

        val appPasswordsPayload = try {
            applicationPasswordsMediaRestClient.fetchMediaList(site, number, offset, mimeType)
        } catch (e: GeneralSecurityException) {
            AppLog.e(AppLog.T.MEDIA, "Error setting up Application Passwords encryption", e)
            FetchMediaListResponsePayload(site, createKeystoreEncryptionError(), mimeType)
        }

        if (!appPasswordsPayload.isError) {
            return appPasswordsPayload
        }

        logFailedAppPasswordsRequest("fetch media list", site, appPasswordsPayload.error)

        val fallbackPayload = wpComV2MediaRestClient.fetchMediaList(site, number, offset, mimeType)
        if (!fallbackPayload.isError) {
            jetpackApplicationPasswordsErrorHandler.handleError(
                site,
                appPasswordsPayload.error.toWPAPINetworkError()
            )
        }

        return fallbackPayload
    }

    private suspend fun shouldUseAppPasswordsForJetpack(site: SiteModel): Boolean {
        return applicationPasswordsConfiguration.isEnabledForJetpackAccess() &&
            jetpackApplicationPasswordsSupport.supportsAppPasswords(site)
    }

    private suspend fun dispatchUploadFlow(flow: Flow<ProgressPayload>) {
        flow.collect { payload ->
            dispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload))
        }
    }

    private fun reportXmlrpcTry() {
        crashLogger.sendReport(
            null,
            Collections.emptyMap(),
            "Requested MediaStore XMLRPC connection. This should not happen."
        )
    }

    private fun createKeystoreEncryptionError() = MediaError(MediaErrorType.GENERIC_ERROR).apply {
        apiErrorCode = ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR
    }

    private fun logFailedAppPasswordsRequest(operation: String, site: SiteModel, error: MediaError?) {
        AppLog.w(
            AppLog.T.MEDIA,
            "Media $operation failed using Application Passwords for Jetpack Site,\n" +
                "site: ${site.url},\n" +
                "error: HTTP status code ${error?.statusCode}, " +
                "error message: ${error?.apiErrorCode?.ifEmpty { null } ?: error?.message}"
        )
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
            HTTP_STATUS_UNAUTHORIZED -> GenericErrorType.AUTHORIZATION_REQUIRED
            HTTP_STATUS_FORBIDDEN -> GenericErrorType.NOT_AUTHENTICATED
            HTTP_STATUS_NOT_FOUND -> GenericErrorType.NOT_FOUND
            HTTP_STATUS_REQUEST_TIMEOUT -> GenericErrorType.TIMEOUT
            in HTTP_STATUS_SERVER_ERROR_MIN..HTTP_STATUS_SERVER_ERROR_MAX -> GenericErrorType.SERVER_ERROR
            else -> when (error.type) {
                MediaErrorType.NOT_AUTHENTICATED -> GenericErrorType.NOT_AUTHENTICATED
                MediaErrorType.AUTHORIZATION_REQUIRED -> GenericErrorType.AUTHORIZATION_REQUIRED
                MediaErrorType.NOT_FOUND -> GenericErrorType.NOT_FOUND
                MediaErrorType.TIMEOUT -> GenericErrorType.TIMEOUT
                MediaErrorType.SERVER_ERROR -> GenericErrorType.SERVER_ERROR
                MediaErrorType.PARSE_ERROR -> GenericErrorType.PARSE_ERROR
                else -> GenericErrorType.UNKNOWN
            }
        }
    }

    private fun volleyErrorFrom(error: MediaError): VolleyError {
        return if (error.statusCode > 0) {
            VolleyError(NetworkResponse(error.statusCode, null, false, 0, emptyList()))
        } else {
            VolleyError(error.message)
        }
    }

    companion object {
        private const val HTTP_STATUS_UNAUTHORIZED = 401
        private const val HTTP_STATUS_FORBIDDEN = 403
        private const val HTTP_STATUS_NOT_FOUND = 404
        private const val HTTP_STATUS_REQUEST_TIMEOUT = 408
        private const val HTTP_STATUS_SERVER_ERROR_MIN = 500
        private const val HTTP_STATUS_SERVER_ERROR_MAX = 599
    }
}
