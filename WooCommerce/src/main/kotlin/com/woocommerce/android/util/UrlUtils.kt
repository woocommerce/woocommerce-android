package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppUrls
import dagger.Reusable
import org.wordpress.android.fluxc.network.discovery.DiscoveryUtils
import org.wordpress.android.util.LanguageUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@Reusable
class UrlUtils @Inject constructor(
    private val context: Context
) {
    val tosUrlWithLocale by lazy {
        "${AppUrls.AUTOMATTIC_TOS}?locale=${LanguageUtils.getPatchedCurrentDeviceLanguage(context)}"
    }

    /**
     * Basic sanitization of the URL based on the same logic we use in the XMLRPC discovery
     * see: https://github.com/wordpress-mobile/WordPress-FluxC-Android/blob/94601a5d4c1c98068adde0352ecc25e6d0046f35/fluxc/src/main/java/org/wordpress/android/fluxc/network/discovery/SelfHostedEndpointFinder.java#L292
     */
    fun sanitiseUrl(url: String): String {
        return url
            .trim()
            .trimEnd('/')
            .let {
                // Convert IDN names to punycode if necessary
                UrlUtils.convertUrlToPunycodeIfNeeded(it)
            }.let {
                // Strip url from known usual trailing paths
                DiscoveryUtils.stripKnownPaths(it)
            }
    }
}
