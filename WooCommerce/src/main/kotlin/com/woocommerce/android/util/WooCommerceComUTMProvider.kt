package com.woocommerce.android.util

import android.net.Uri
import androidx.core.net.toUri
import dagger.Reusable

interface UtmProvider {
    val campaign: String
    val source: String
    val content: String?
    val siteId: Long?
}

@Reusable
class WooCommerceComUTMProvider(
    override val campaign: String,
    override val source: String,
    override val content: String?,
    override val siteId: Long?
) : UtmProvider {
    private val parameters = mapOf<String, Any?>(
        "utm_campaign" to campaign,
        "utm_source" to source,
        "utm_content" to content,
        "utm_term" to siteId,
        "utm_medium" to defaultUTMMedium
    )
    fun getUrlWithUtmParams(uri: Uri): Uri {
        val uriBuilder = (uri.scheme + "://" + uri.host + uri.path).toUri().buildUpon()
        // remove any null, empty query items and existing utm query items to avoid duplicates
        uri.queryParameterNames.filter { query ->
            !query.isNullOrEmpty() && (parameters[query]?.toString().isNullOrEmpty() || query !in parameters)
        }.forEach { params ->
            uriBuilder.appendQueryParameter(params, uri.getQueryParameter(params))
        }
        // append new utm query params to the uri
        parameters.forEach { entry ->
            if (!entry.value?.toString().isNullOrEmpty()) {
                uriBuilder.appendQueryParameter(entry.key, (entry.value)?.toString())
            }
        }
        return uriBuilder.build()
    }

    companion object {
        private const val defaultUTMMedium = "woo_android"
    }
}
