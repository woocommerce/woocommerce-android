package com.woocommerce.android.tools

import com.woocommerce.android.util.WooLog
import com.woocommerce.commons.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel

enum class SiteConnectionType {
    Jetpack, JetpackConnectionPackage, ApplicationPasswords
}

/**
 * The connection type, or `null` when the site does not fall into any of the known cases.
 *
 * Prefer this over [connectionType] when the value is going to be *reported* rather than acted on: [connectionType]
 * has to return something, so in production it answers `Jetpack` for a site it could not classify, and a caller
 * cannot tell that apart from a real Jetpack connection.
 */
val SiteModel.connectionTypeOrNull: SiteConnectionType?
    get() = when {
        origin != SiteModel.ORIGIN_WPCOM_REST -> SiteConnectionType.ApplicationPasswords
        isJetpackConnected -> SiteConnectionType.Jetpack
        isJetpackCPConnected -> SiteConnectionType.JetpackConnectionPackage
        else -> null
    }

val SiteModel.connectionType
    get() = connectionTypeOrNull ?: run {
        if (BuildConfig.DEBUG) {
            error("Can't determine site connection status")
        } else {
            WooLog.w(
                WooLog.T.UTILS,
                """Can't determine site connection status:
                    "Origin: $origin, Jetpack Connected: $isJetpackConnected,
                    "Jetpack CP Connected: $isJetpackCPConnected"""
            )
            // A site that doesn't fall into the above conditions, it shouldn't happen,
            // but if it does in production, pretend it's a Jetpack connection
            SiteConnectionType.Jetpack
        }
    }
