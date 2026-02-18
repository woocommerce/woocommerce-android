package com.woocommerce.android.ui.login.jetpack

import org.wordpress.android.fluxc.utils.extensions.slashJoin

object JetpackConnectionUrlResolver {
    const val ACCOUNT_CONNECTION_URL_PREFIX = "https://jetpack.wordpress.com/jetpack.authorize"

    private const val SITE_CONNECTION_PATH = "wp-admin/admin.php?page=jetpack"

    fun resolveConnectionWebViewUrl(
        connectionUrl: String,
        siteUrl: String
    ): String {
        return if (connectionUrl.startsWith(ACCOUNT_CONNECTION_URL_PREFIX)) {
            connectionUrl
        } else {
            siteUrl.slashJoin(SITE_CONNECTION_PATH)
        }
    }
}
