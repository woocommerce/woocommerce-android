package org.wordpress.android.fluxc.network.rest.wpapi.media

import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient
import org.wordpress.android.fluxc.utils.MimeType
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooMediaNetwork @Inject constructor(
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration,
    private val applicationPasswordsMediaRestClient: ApplicationPasswordsMediaRestClient,
    private val wpComV2MediaRestClient: WPComV2MediaRestClient,
    private val crashLogger: FluxCCrashLogger
) {
    fun uploadMedia(site: SiteModel, media: MediaModel) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            wpComV2MediaRestClient.uploadMedia(site, media)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            applicationPasswordsMediaRestClient.uploadMedia(site, media)
        } else {
            reportXmlrpcTry()
        }
    }

    fun fetchMediaList(site: SiteModel, number: Int, offset: Int, mimeType: MimeType.Type?) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            wpComV2MediaRestClient.fetchMediaList(site, number, offset, mimeType)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            applicationPasswordsMediaRestClient.fetchMediaList(site, number, offset, mimeType)
        } else {
            reportXmlrpcTry()
        }
    }

    fun cancelUpload(site: SiteModel, media: MediaModel) {
        if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            wpComV2MediaRestClient.cancelUpload(media)
        } else if (site.origin == SiteModel.ORIGIN_WPAPI
            && applicationPasswordsConfiguration.isEnabledForDirectAccess()
        ) {
            applicationPasswordsMediaRestClient.cancelUpload(media)
        } else {
            reportXmlrpcTry()
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
