package com.woocommerce.android.ui.jitm

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.BuildConfigWrapper
import java.net.URLEncoder
import javax.inject.Inject

class JitmQueryParamsEncoder @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun getEncodedQueryParams(): String {
        val query = if (buildConfigWrapper.debug) {
            "build_type=developer&platform=android&version=${BuildConfig.VERSION_NAME}"
        } else {
            "platform=android&version=${BuildConfig.VERSION_NAME}"
        }
        return URLEncoder.encode(query, Charsets.UTF_8.name())
    }
}
