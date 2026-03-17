package org.wordpress.android.fluxc.network.rest.wpapi.media

import kotlinx.coroutines.Job
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
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
    private val crashLogger: FluxCCrashLogger
) {
    private val currentUploads = ConcurrentHashMap<Int, Job>()

    fun uploadMedia(site: SiteModel, media: MediaModel) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            launchUpload(wpComV2MediaRestClient, site, media)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            launchUpload(applicationPasswordsMediaRestClient, site, media)
        } else {
            reportXmlrpcTry()
        }
    }

    fun fetchMediaList(site: SiteModel, number: Int, offset: Int, mimeType: MimeType.Type?) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            launchFetchMediaList(wpComV2MediaRestClient, site, number, offset, mimeType)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            launchFetchMediaList(applicationPasswordsMediaRestClient, site, number, offset, mimeType)
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

    private fun launchUpload(client: BaseWPV2MediaRestClient, site: SiteModel, media: MediaModel) {
        val uploadJob = coroutineEngine.launch(AppLog.T.MEDIA, this, "Uploading media") {
            try {
                client.uploadMedia(site, media).collect { payload ->
                    dispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload))
                }
            } finally {
                currentUploads.remove(media.id)
            }
        }
        currentUploads[media.id] = uploadJob
    }

    private fun launchFetchMediaList(
        client: BaseWPV2MediaRestClient,
        site: SiteModel,
        number: Int,
        offset: Int,
        mimeType: MimeType.Type?
    ) {
        coroutineEngine.launch(AppLog.T.MEDIA, this, "Fetching media list") {
            val payload = client.fetchMediaList(site, number, offset, mimeType)
            dispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload))
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

    private fun reportXmlrpcTry() {
        crashLogger.sendReport(
            null,
            Collections.emptyMap(),
            "Requested MediaStore XMLRPC connection. This should not happen."
        )
    }
}
