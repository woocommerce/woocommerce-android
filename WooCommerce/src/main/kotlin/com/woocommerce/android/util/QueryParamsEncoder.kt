package com.woocommerce.android.util

import com.woocommerce.android.BuildConfig
import java.net.URLEncoder
import javax.inject.Inject

class QueryParamsEncoder @Inject constructor() {
    fun getEncodedQueryParams(): String {
        val query = if (BuildConfig.DEBUG) {
            "build_type=developer&platform=android&version=${BuildConfig.VERSION_NAME}"
        } else {
            "platform=android&version=${BuildConfig.VERSION_NAME}"
        }
        return URLEncoder.encode(query, Charsets.UTF_8.name())
    }
}
