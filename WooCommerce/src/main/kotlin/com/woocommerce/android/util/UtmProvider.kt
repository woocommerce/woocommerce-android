package com.woocommerce.android.util

import android.net.Uri
import androidx.core.net.toUri

/**
 * Make sure to write tests in WooCommerceComUTMProviderTest class if you change the behaviour of this class.
 * This class makes use of Uri class and needs an android device/emulator to run the tests.
 * WooCommerceComUTMProviderTest will not be run as part of PR checks, it's important to run it manually when
 * the behaviour of this class changes.
 */
class UtmProvider(
    val campaign: String,
    val source: String,
    val content: String?,
    val siteId: Long?,
) {
    val parameters: Map<String, Any?>
        get() {
            return mapOf<String, Any?>(
                "utm_campaign" to if (campaign.isNotEmpty()) {
                    "jitm_group_$campaign"
                } else {
                    campaign
                },
                "utm_source" to source,
                "utm_content" to if (!content.isNullOrEmpty()) {
                    "jitm_$content"
                } else {
                    content
                },
                "utm_term" to siteId,
                "utm_medium" to defaultUTMMedium
            )
        }

    fun getUrlWithUtmParams(uriString: String): String {
        val uri = uriString.toUri()
        val uriBuilder = (uri.scheme + "://" + uri.host + uri.path).toUri().buildUpon()
        filterAndBuildValidExistingQueries(uri, uriBuilder)
        filterAndBuildValidUtmQueries(uriBuilder)
        return uriBuilder.build().toString()
    }

    /**
     * These are existing queries which already are a part of the url
     *
     * example: https://www.woocommerce.com?utm_campaign=payments_menu&username=abcd&utm_source=null
     *
     * Valid existing queries are those which are:
     * 1. NOT Null and NOT an empty string
     * 2. If the query is not present in the UTM properties list
     * 3. The query is present in the UTM properties list but the UTM property query has a null or empty value.
     * The one with the valid value will get the precedence here.
     *
     * From the above example url:
     *
     * utm_campaign=payments_menu -  is valid only if the "utm_campaign" in the UTM properties list is null or empty.
     * Else, the one in UTM properties list will get preference.
     *
     * username=abcd -  is a valid query
     *
     * utm_source=null -  is an invalid query
     */
    private fun isValidQuery(query: String): Boolean {
        return query.isNotEmpty() && (parameters[query]?.toString().isNullOrEmpty() || query !in parameters)
    }

    private fun filterAndBuildValidExistingQueries(uri: Uri, uriBuilder: Uri.Builder) {
        uri.queryParameterNames.filter { query ->
            isValidQuery(query)
        }.forEach { validQuery ->
            uriBuilder.appendQueryParameter(validQuery, uri.getQueryParameter(validQuery))
        }
    }

    private fun filterAndBuildValidUtmQueries(uriBuilder: Uri.Builder) {
        parameters.forEach { entry ->
            if (!entry.value?.toString().isNullOrEmpty()) {
                uriBuilder.appendQueryParameter(entry.key, (entry.value)?.toString())
            }
        }
    }

    companion object {
        private const val defaultUTMMedium = "woo_android"
    }
}
