package com.woocommerce.android.ui.mystore

import com.woocommerce.android.util.UtmProvider
import dagger.Reusable
import javax.inject.Inject

/**
 * This is just a wrapper around the UtmProvider to allow mocking the implementation in tests
 */
@Reusable
class MyStoreUtmProvider @Inject constructor() {
    fun getUrlWithUtmParams(
        source: String,
        id: String,
        featureClass: String,
        siteId: Long?,
        url: String
    ): String {
        return UtmProvider(
            campaign = featureClass,
            source = source,
            content = id,
            siteId = siteId
        ).getUrlWithUtmParams(url)
    }
}
