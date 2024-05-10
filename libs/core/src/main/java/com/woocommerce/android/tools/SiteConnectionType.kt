package com.woocommerce.android.tools

import com.woocommerce.android.core.BuildConfig
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.SiteModel

enum class SiteConnectionType {
    Jetpack, JetpackConnectionPackage, ApplicationPasswords
}

val SiteModel.connectionType
    get() = when {
        origin != SiteModel.ORIGIN_WPCOM_REST -> SiteConnectionType.ApplicationPasswords
        isJetpackConnected -> SiteConnectionType.Jetpack
        isJetpackCPConnected -> SiteConnectionType.JetpackConnectionPackage
        else -> {
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
    }
