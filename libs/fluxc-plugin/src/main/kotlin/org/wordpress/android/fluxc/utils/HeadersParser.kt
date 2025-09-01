package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class HeadersParser @Inject constructor(
    val logger: AppLogWrapper
) {
    companion object Companion {
        private const val TOTAL_PAGES_HEADER = "x-wp-totalpages"
        private const val SERVER_DATE_HEADER = "date"
    }

    fun <T> getTotalPages(result: WooResult<T>): Int? {
        return try {
            result.headers
                .findLast { TOTAL_PAGES_HEADER.equals(it.key, ignoreCase = true) }
                ?.value?.toInt()
        } catch (e: NumberFormatException) {
            logger.e(AppLog.T.API, "Failed to parse total pages from headers", e)
            null
        }
    }

    fun <T> getServerDate(response: WooResult<T>): String? = response.headers
        .findLast { SERVER_DATE_HEADER.equals(it.key, ignoreCase = true) }
        ?.value


}
