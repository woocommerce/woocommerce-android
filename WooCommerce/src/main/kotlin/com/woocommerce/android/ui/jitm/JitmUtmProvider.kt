package com.woocommerce.android.ui.jitm

import com.woocommerce.android.util.UtmProvider
import dagger.Reusable
import javax.inject.Inject

/**
 * This is just a wrapper around the UtmProvider to allow mocking the implementation in tests
 */
@Reusable
class JitmUtmProvider @Inject constructor() {
    fun getUrlWithUtmParams(
        source: String,
        id: String,
        featureClass: String,
        siteId: Long?,
        url: String
    ): String {
        return UtmProvider(
            campaign = if (featureClass.isNotEmpty()) "jitm_group_$featureClass" else featureClass,
            source = source,
            content = if (id.isNotEmpty()) "jitm_$id" else id,
            siteId = siteId
        ).getUrlWithUtmParams(url)
    }
}
