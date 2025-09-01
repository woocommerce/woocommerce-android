package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import javax.inject.Inject

class HeadersParser @Inject constructor() {
    companion object Companion {
        private const val TOTAL_PAGES_HEADER = "x-wp-totalpages"
        private const val SERVER_DATE_HEADER = "date"
    }

    fun <T> getTotalPages(result: WooResult<T>): Int? = result.headers
        .findLast { TOTAL_PAGES_HEADER.equals(it.key, ignoreCase = true) }
        ?.value?.toInt()

    fun getServerDate(response: WooResult<Array<ProductApiResponse>>): String? = response.headers
        .findLast { SERVER_DATE_HEADER.equals(it.key, ignoreCase = true) }
        ?.value


}
